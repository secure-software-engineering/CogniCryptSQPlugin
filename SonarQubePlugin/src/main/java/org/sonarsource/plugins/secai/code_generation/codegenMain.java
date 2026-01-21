package org.sonarsource.plugins.secai.code_generation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import io.github.cdimascio.dotenv.Dotenv;

public class codegenMain {

   static Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private static final String OPEN_AI_API_KEY = getEnvVariable("OPEN_AI_API_KEY");
    private static final String GOOGLE_API_KEY = getEnvVariable("GOOGLE_API_KEY");

    private static String getEnvVariable(String key) {
        String value = dotenv.get(key);
        if (value == null || value.isEmpty()) {
            value = System.getenv(key);
        }
        if (value == null || value.isEmpty()) {
            System.err.println("Warning: Missing environment variable: " + key);
        }
        return value;
    }

    public static String runPipeline(String userQuery, String provider, String model, int maxIterations) throws Exception
    {
        Path workDir = Files.createTempDirectory("secai_codegen_");
        documentProcessor doc = new documentProcessor(workDir);
        cogniCryptRunner cc = new cogniCryptRunner(workDir);

        boolean useOllama = provider != null && provider.equalsIgnoreCase("ollama");
        boolean useGemini = provider != null && provider.equalsIgnoreCase("gemini");

        String outText = "";
        try {
            Path classpath;
            Path jarpath;
            String processedReport = "";
            int iterations = 0;
            boolean noViolations = false;
            String generatedCode = "";
            String cryslRules = "";
            String descriptions = "";

            if (useOllama) {
                generatedCode = doc.splitGPTResponse(new ollama_LLM().Generate(userQuery, doc.readAllCryslRules()));
            } else if (useGemini) {
                gemini_LLM llm = new gemini_LLM(GOOGLE_API_KEY, model);
                generatedCode = doc.splitGPTResponse(llm.Generate(userQuery, doc.readAllCryslRules()));
            }
            else {
                openAI_LLM llm = new openAI_LLM(OPEN_AI_API_KEY, model);
                generatedCode = doc.splitGPTResponse(llm.Generate(userQuery, doc.readAllCryslRules()));
            }

            doc.saveCodeToJava(generatedCode);
            try {
                classpath = cc.compileJavaFile(workDir.resolve("demo.java"));
            } catch (Exception e) {
                outText = "Unable to compile, try to use a stronger LLM model";
                return outText;
            }

            jarpath = cc.createJar(classpath);
            cc.runCC(jarpath);
            processedReport = doc.processCCReport();
            if ("No violations detected".equals(processedReport)) {
                noViolations = true;
            } else {
                cryslRules   = doc.readCryslRules(doc.cryslViolations(processedReport));
                descriptions = doc.readErrorDescriptions(doc.extractInfo());
            }

            if (!noViolations)
            {
                // Use the user-provided maxIterations to control the loop
                for (; iterations < maxIterations; ++iterations)
                {
                    if (useOllama)
                    {
                        generatedCode = new ollama_LLM().GenerateIterations(generatedCode, processedReport, cryslRules, descriptions);
                    }
                    else if (useGemini)
                    {
                        gemini_LLM llm = new gemini_LLM(GOOGLE_API_KEY, model);
                        generatedCode = llm.GenerateIterations(generatedCode, processedReport, cryslRules, descriptions);
                    }
                    else
                    {
                        openAI_LLM llm = new openAI_LLM(OPEN_AI_API_KEY, model);
                        generatedCode = llm.GenerateIterations(generatedCode, processedReport, cryslRules, descriptions);
                    }
                    generatedCode = doc.splitGPTResponse(generatedCode);

                    doc.saveCodeToJava(generatedCode);
                    try
                    {
                        classpath = cc.compileJavaFile(workDir.resolve("demo.java"));
                    }
                    catch (Exception e)
                    {
                        outText = "Unable to compile, try to use a stronger LLM model";
                        noViolations = false;
                        processedReport = "";
                        break;
                    }

                    jarpath = cc.createJar(classpath);
                    cc.runCC(jarpath);

                    processedReport = doc.processCCReport();
                    if ("No violations detected".equals(processedReport))
                    {
                        noViolations = true;
                        break;
                    }
                    else
                    {
                        cryslRules   = doc.readCryslRules(doc.cryslViolations(processedReport));
                        descriptions = doc.readErrorDescriptions(doc.extractInfo());
                    }
                }
            }

            if (outText.isEmpty()) {
                if (noViolations)
                {
                    outText = generatedCode + "\n\n// CogniCrypt analysis completed: no violations found.";
                }
                else if (processedReport != null && !processedReport.isBlank())
                {
                    outText = generatedCode + "\n\n/* CogniCrypt analysis results:\n" + processedReport + "\n*/";
                }
                else
                {
                    outText = "Unable to compile, try to use a stronger LLM model";
                }
            }

            return outText;
        }
        finally {
            try {
                Files.walk(workDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException ioEx) {
                System.err.println("Could not delete temp dir " + workDir + ": " + ioEx);
            }
        }
    }

    // Overloaded methods for backward compatibility
    public static String runPipeline(String userQuery, String provider, String model) throws Exception
    {
        return runPipeline(userQuery, provider, model, 4); // Default to 4 iterations
    }

    public static String runPipeline(String userQuery, String provider) throws Exception
    {
        return runPipeline(userQuery, provider, null, 4);
    }
    
    public static String runPipeline(String userQuery) throws Exception
    {
        return runPipeline(userQuery, "openai", null, 4);
    }


    public static void main(String[] args) throws Exception {
        String finalCode = runPipeline("Write a program to securely encrypt strings");
        System.out.println(finalCode);
    }
}