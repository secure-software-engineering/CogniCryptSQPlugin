package org.sonarsource.plugins.secai.api;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.sonar.api.server.ws.WebService;
import org.sonarsource.plugins.secai.analysis.Analyzer;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.utils.ResourceService;
import org.sonarsource.plugins.secai.code_generation.codegenMain;

public class SecAIWebService implements WebService {

    private static String escapeJson(String s) {  // helper used elsewhere
    return "\"" + s.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n") + "\"";
    }

    @Override
    public void define(Context context) {
        NewController controller = context.createController("api/secai");

        controller.createAction("triggerAnalysis")
                .setSince("SecAI version 1.0.0")
                .setHandler((request, response) -> {
                    response.stream().setMediaType("text/event-stream");
                    response.setHeader("Connection", "keep-alive");
                    response.setHeader("Cache-Control", "no-cache");

                    SecAISettings settings = SecAISettings.newInstance(request);
                    Analyzer analyzer = new Analyzer(settings, request);
                    analyzer.runAnalysis(response.stream().output());
                })
                .setDescription("Triggers CogniCrypt analysis for the specified JAR file");

        controller.createAction("getFullSarifReport")
                .setSince("SecAI version 1.0.0")
                .setDescription("Returns the full SARIF analysis report from the generated file")
                .setHandler((request, response) -> {
                    try {
                        // Get the report file path
                        ResourceService resourceService = ResourceService.getInstance();

                        String reportPath = resourceService.getReportDirectory();
                        File sarifFile = new File(reportPath, "CryptoAnalysis-Report.json");

                        System.out.println(sarifFile.getAbsolutePath() + " " + sarifFile.exists());

                        if (!sarifFile.exists()) {
                            response.stream().setStatus(404);
                            response.stream().setMediaType("application/json");

                            PrintWriter writer = new PrintWriter(response.stream().output());
                            writer.println("{\"error\": \"SARIF file not found\"}");
                            writer.flush();
                            return;
                        }

                        // Read and return full SARIF file as JSON
                        JsonElement json = JsonParser.parseReader(new FileReader(sarifFile));

                        response.stream().setStatus(200);
                        response.stream().setMediaType("application/json");

                        PrintWriter writer = new PrintWriter(response.stream().output());
                        writer.println(json.toString()); // full SARIF JSON
                        writer.flush();

                    } catch (Exception e) {

                        response.stream().setStatus(404);
                        response.stream().setMediaType("application/json");

                        PrintWriter writer = new PrintWriter(response.stream().output());
                        writer.println("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
                        writer.flush();
                    }
                });

        controller.createAction("getAIConfiguration")
                .setDescription("Returns whether API keys were set for OpenAI and Google API " +
                        "(true/false, not the keys themselves) and the IP address of the AIFix and Confidence Score backend.")
                .setSince("SecAI version 1.1.0")
                .setInternal(true)
                .setHandler(((request, response) -> {
                    response.stream().setMediaType("application/json");

                    try {
                        Map<String, Object> json = new HashMap<>();
                        Map<String, String> env = System.getenv();
                        json.put("openai", env.containsKey("OPEN_AI_API_KEY")
                                && !env.get("OPEN_AI_API_KEY").equals("your_openai_api_key_here"));
                        json.put("google", env.containsKey("GOOGLE_API_KEY")
                                && !env.get("GOOGLE_API_KEY").equals("your_google_api_key_here"));
                        json.put("flask_ip", env.getOrDefault("FLASK_IP", "127.0.0.1"));

                        response.stream().setStatus(200);

                        PrintWriter writer = new PrintWriter(response.stream().output());
                        writer.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json));
                        writer.flush();
                    } catch (Exception e) {
                        response.stream().setStatus(404);

                        PrintWriter writer = new PrintWriter(response.stream().output());
                        writer.println("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
                        writer.flush();
                    }
                }));
        
        NewAction run = controller.createAction("runCodeGeneration")
        .setDescription("Run the full SecAI code-generation pipeline")
        .setSince("SecAI version 1.0.0")
        .setPost(true);

        run.createParam("prompt")               // 1 required parameter
        .setRequired(true)
        .setDescription("Natural-language prompt for the generator");

        run.createParam("provider")
        .setRequired(false)
        .setDescription("LLM provider: 'openai', 'ollama', or 'gemini' (default: openai)");
        
        run.createParam("model")
        .setRequired(false)
        .setDescription("The specific LLM model to use (e.g., 'gpt-4o').");

        run.createParam("iterations")
        .setRequired(false)
        .setDescription("The maximum number of refinement iterations (default: 4).");

        run.setHandler((req, res) -> {
            String prompt   = req.mandatoryParam("prompt");
            String provider = req.param("provider");
            String model    = req.param("model");
            String iterationsStr = req.param("iterations"); // Get iterations as a string

            try {
                // Convert iterations to int, with a default value
                int iterations = 4; // Default value
                if (iterationsStr != null && !iterationsStr.isBlank()) {
                    try {
                        iterations = Integer.parseInt(iterationsStr);
                    } catch (NumberFormatException e) {
                        // Keep default value if parsing fails
                    }
                }

                // Pass all parameters to the pipeline
                String code = codegenMain.runPipeline(prompt, provider, model, iterations);

                res.stream().setMediaType("application/json");
                try (PrintWriter out = new PrintWriter(res.stream().output())) {
                    out.printf("{\"code\":%s}", escapeJson(code));
                }
            } catch (Exception ex) {
                res.stream().setStatus(500);
                res.stream().setMediaType("application/json");
                try (PrintWriter out = new PrintWriter(res.stream().output())) {
                    out.printf("{\"error\":%s}", escapeJson(ex.getMessage()));
                }
            }
        });

        controller.done();
           
    }
}

