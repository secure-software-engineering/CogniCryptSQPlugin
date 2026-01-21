package org.sonarsource.plugins.secai.code_generation;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class documentProcessor {
    

    private final Path tempFolder;

    public documentProcessor(Path tempFolder) {
        this.tempFolder = tempFolder;
    }

    public String readCryslRules(Set<String> rules) throws Exception
    {
        String context = "";
        String base_folderpath = "/org/sonarsource/plugins/secai/cognicrypt/crysl_rules_txt/";

        for (String filename : rules) 
        {
            try (InputStream in = getClass().getResourceAsStream(base_folderpath + filename)) 
            {
                if (in == null) 
                {
                    System.out.println("CrySL rule not found: " + filename);
                    continue;
                }
                context += "\nCrysl Rule : " + filename.split("\\.")[0]+"\n";
                context += new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }   
        return context;
    }

    public String readAllCryslRules() throws Exception
    {
        List<String> ruleList = List.of( "AlgorithmParameterGenerator.txt", "AlgorithmParameters.txt", "CertificateFactory.txt", "CertPathTrustManagerParameters.txt", "Cipher.txt", "CipherInputStream.txt", "CipherOutputStream.txt", "Cookie.txt", "DHGenParameterSpec.txt", "DHParameterSpec.txt", "DigestInputStream.txt", "DigestOutputStream.txt", "DSAGenParameterSpec.txt", "DSAParameterSpec.txt", "ECGenParameterSpec.txt", "ECParameterSpec.txt", "GCMParameterSpec.txt", "HMACParameterSpec.txt", "IvParameterSpec.txt", "Key.txt", "KeyAgreement.txt", "KeyFactory.txt", "KeyGenerator.txt", "KeyManagerFactory.txt", "KeyPair.txt", "KeyPairGenerator.txt", "KeyStore.txt", "KeyStoreBuilderParameters.txt", "Mac.txt", "MessageDigest.txt", "MGF1ParameterSpec.txt", "OAEPParameterSpec.txt", "PasswordAuthentication.txt", "PBEKeySpec.txt", "PBEParameterSpec.txt", "PKIXBuilderParameters.txt", "PKIXParameters.txt", "PrivateKey.txt", "PublicKey.txt", "RSAKeyGenParameterSpec.txt", "SecretKey.txt", "SecretKeyFactory.txt", "SecretKeySpec.txt", "SecureRandom.txt", "Signature.txt", "SSLContext.txt", "SSLEngine.txt", "SSLParameters.txt", "TrustAnchor.txt", "TrustManagerFactory.txt", "X509EncodedKeySpec.txt");
        String context = "";
        String base_folderpath = "/org/sonarsource/plugins/secai/cognicrypt/crysl_rules_txt/";
        for (String name : ruleList) 
        {
            try (InputStream in = getClass().getResourceAsStream(base_folderpath + name)) 
            {
                if (in == null) 
                {
                    System.out.println("CrySL rule not found: " + name);
                    continue;
                }
                context += "\nCrysl Rule : " + name.replace(".txt", "")+"\n";
                context += new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return context;
    }

    public String splitGPTResponse(String response)
    {
        Pattern pattern = Pattern.compile("```java\\s*([\\s\\S]*?)\\s*```");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Error";
    }

    public void saveCodeToJava(String code) throws Exception 
    {
        Path javaFile = tempFolder.resolve("demo.java");
        Files.writeString(javaFile, code);
    }

    public String processCCReport() throws Exception
    {
        Path report_path = tempFolder.resolve("CryptoAnalysis-Report.json");
        // String report_path = "src\\main\\resources\\org\\sonarsource\\plugins\\secai\\cognicrypt\\codegen_report\\CryptoAnalysis-Report.json";
        String context = "";
        
        try (FileReader reader = new FileReader(report_path.toFile())) 
        {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            JsonArray runs = root.getAsJsonArray("runs");
            if (runs.size() == 0) {
                System.out.println("No runs found.");
                return "No runs found in report. Error.";
            }
            JsonObject firstRun = runs.get(0).getAsJsonObject();
            JsonArray results = firstRun.getAsJsonArray("results");

            if (results.size() == 0)
            {
                return "No violations detected";
            }

            for (JsonElement elem : results) 
            {
                JsonObject result = elem.getAsJsonObject();

                String violatedRule = result.get("ruleId").getAsString();
                String errorType = result.get("errorType").getAsString();

                // Extract nested location info
                JsonObject region = result.getAsJsonArray("locations")
                                          .get(0).getAsJsonArray()
                                          .get(0).getAsJsonObject()
                                          .getAsJsonObject("physicalLocation")
                                          .getAsJsonObject("region");

                String startLine = region.get("startLine").getAsString();
                String method = result.getAsJsonArray("locations")
                    .get(0).getAsJsonArray()
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("logicalLocation")
                    .get("fullyQualifiedLogicalName").getAsString();

                // Message fields
                JsonObject message = result.getAsJsonObject("message");
                String text = message.get("text").getAsString();
                String markdown = message.get("markdown").getAsString();

                context += "\nViolated Rule : " + violatedRule+"\nError Type : "+errorType+"\nStart Line : "+startLine+
                "\nMethod : "+method+"\nText : "+text+"\n"+markdown+"\n";
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return context;
    }

    public Set<String> cryslViolations(String context) throws Exception
    {
        Set<String> rules = new HashSet<>();
        Pattern rule_regex = Pattern.compile("Violated Rule\\s*:\\s*([\\w\\.]+)");
        Matcher rule_matcher = rule_regex.matcher(context);
        while (rule_matcher.find())
        {
            String[] parts = rule_matcher.group(1).split("\\.");
            String s = parts[parts.length - 1]+".txt";
            rules.add(s);
        }
        Set<String> rules2 = new HashSet<>(checkCryslInstances());
        rules.addAll(rules2);
        return rules;
    }

    public Set<String> checkCryslInstances() throws Exception
    {
        Set<String> crysl = new HashSet<>();
        Path javaFile = tempFolder.resolve("demo.java");
        // String javaFilePath = "src\\main\\resources\\org\\sonarsource\\plugins\\secai\\cognicrypt\\codegen_files\\demo.java";
        List<String> lines = Files.readAllLines(javaFile);
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("import javax.crypto.")) {
                // Remove 'import' and ';'
                line = line.replace("import", "").replace(";", "").trim();

                // Split by dot and take the last part
                String[] parts = line.split("\\.");
                String className = parts[parts.length - 1]+".txt";

                crysl.add(className);
            }
        }
        return crysl;
    }
    
    public Set<String> extractInfo() throws Exception 
    {
        Path report_path = tempFolder.resolve("CryptoAnalysis-Report.json");
        // String report_path = "src\\main\\resources\\org\\sonarsource\\plugins\\secai\\cognicrypt\\codegen_report\\CryptoAnalysis-Report.json";
        Set<String> combinations = new HashSet<>();
        JsonElement rootElement = JsonParser.parseReader(new FileReader(report_path.toFile()));
        JsonObject root = rootElement.getAsJsonObject();

        JsonArray runs = root.getAsJsonArray("runs");
        for (JsonElement runElement : runs) {
            JsonObject run = runElement.getAsJsonObject();
            if (!run.has("results")) continue;

            JsonArray results = run.getAsJsonArray("results");
            for (JsonElement resultElement : results) {
                JsonObject result = resultElement.getAsJsonObject();
                if (result.has("ruleId") && result.has("errorType")) {
                    String violatedRule = result.get("ruleId").getAsString();
                    String errorType = result.get("errorType").getAsString();

                    // Extract only class name (last part)
                    String simpleRuleName = violatedRule.substring(violatedRule.lastIndexOf('.') + 1);
                    String combo = simpleRuleName + " - " + errorType;
                    combinations.add(combo);
                }
            }
        }

        return combinations;
    }

    public String readErrorDescriptions(Set<String> info) throws Exception
    {
    StringBuilder finalOutput = new StringBuilder();
    
    String resource = "/org/sonarsource/plugins/secai/cognicrypt/CogniCryptRules.json";
    try (InputStream file = getClass().getResourceAsStream(resource)) 
    {
        if (file == null) {
            throw new FileNotFoundException("Resource not found: " + resource);
        }
        JsonElement rootElement = JsonParser.parseReader(new InputStreamReader(file));
        JsonObject root = rootElement.getAsJsonObject();

        for (String element : info) 
        {
            String[] array = element.split(" - ");
            if (array.length != 2) {
                System.out.println("Invalid format for: " + element);
                finalOutput.append("CrySL Rule: ").append(element).append("\n")
                        .append("Error Type: Invalid Format\n\n");
                continue;
            }

            String rule = array[0].trim();
            String type = array[1].trim();
            System.out.println("Processing -> Rule: " + rule + ", Error Type: " + type);
            String lookupType = type.equalsIgnoreCase("AlternativeReqPredicateError")
                    ? "RequiredPredicateError"
                    : type;
            JsonObject errorTypeObject = root.getAsJsonObject(lookupType);
            if (errorTypeObject == null) {
                String notFound = "Error type not found: " + lookupType;
                System.out.println(notFound);
                finalOutput.append("CrySL Rule: ").append(rule).append("\n")
                        .append("Error Type: ").append(type).append("\n")
                        .append(notFound).append("\n\n");
                continue;
            }

            String description = errorTypeObject.has("description")
                    ? errorTypeObject.get("description").getAsString()
                    : "No description available";
            JsonArray examples = errorTypeObject.getAsJsonArray("examples");

            boolean found = false;
            for (JsonElement ex : examples) {
                JsonObject example = ex.getAsJsonObject();
                if (example.get("rule").getAsString().equalsIgnoreCase(rule)) {
                    String misuse = example.has("misuse")
                            ? example.get("misuse").getAsString()
                            : "No misuse info";
                    String solution = example.has("solution")
                            ? example.get("solution").getAsString()
                            : "No solution info";

                    finalOutput.append("CrySL Rule: ").append(rule).append("\n")
                            .append("Error Type: ").append(type).append("\n")
                            .append("Description:\n").append(description).append("\n\n")
                            .append("Misuse:\n").append(misuse).append("\n\n")
                            .append("Solution:\n").append(solution).append("\n\n");

                    found = true;
                    break;
                }
            }

            if (!found) {
                finalOutput.append("CrySL Rule: ").append(rule).append("\n")
                        .append("Error Type: ").append(type).append("\n")
                        .append("No example found for Rule '").append(rule)
                        .append("' with Error Type '").append(type).append("'\n\n");
            }
        }
    }

    return finalOutput.toString();
    }

    public String readCodeLines(String lineNumber) throws Exception
    {
        
        Path javaFile = tempFolder.resolve("demo.java");

        StringBuilder codeLines = new StringBuilder();
        Integer line = Integer.parseInt(lineNumber.trim());
        List<String> lines = Files.readAllLines(javaFile);

        for(int i=line-1;i<=line+1;i++)
        {
            if (i >= 0 && i < lines.size()) 
            {
                codeLines.append("Line ").append(i + 1).append(": ").append(lines.get(i).trim()).append("\n");
            }
        }
        return codeLines.toString();

    }

}
