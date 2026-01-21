package org.sonarsource.plugins.secai.code_generation;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

public class gemini_LLM {

    private final ChatModel model;

    public gemini_LLM(String apiKey, String modelName) {
        if (apiKey == null || apiKey.isBlank())
        {
            // Updated error message to reflect the correct key name
            throw new IllegalStateException("GOOGLE_API_KEY not set");
        }

        // Use the provided modelName, or the specified default if it's null/empty
        String finalModelName = (modelName != null && !modelName.isBlank()) ? modelName : "gemini-2.5-flash";

        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(finalModelName)
                .temperature(0.2)
                .build();
    }

    public String Generate(String query, String context)
    {
        String prompt = """
        You are a Java Cryptographic Architecture developer.
        
        Your task:
        - Create a secure Java class based on the following user request.
        - The class must be valid, compile without errors, and follow best practices in cryptography.
        - Use only Java for all tasks. Do not include any other language.
        - Ensure the code complies with secure programming principles (e.g., strong keys, secure cipher modes).
        - Class name must be: demo
        - Do not include any text explanations.
        - Only generate a single, standalone Java file.
        Query: %s
        
        Cognicrypt, a static analysis tool which specializes in cryptograhpy uses 51 CrySL rules to ensure that any code is
        compliant with security standards. Using these rules generate your code such that it is secure.
        Ensure all the rules are followed

        All 51 CrySL rules: %s

        """.formatted(query, context);
        return model.chat(prompt);
    }

     public String GenerateIterations(String previous, String issues, String crysl, String desc) {
        String prompt = """
        You previously generated a Java class which was analysed using Cognicrypt, a static analysis tool.
        The tool has found issues with the code which are provided below.

        Previously generated code : %s

        Your goal is to fix all the reported issues.
        Only generate one valid, standalone Java file with class name: 'demo'
        Class name should always be demo do not change it
        Do not generate any text explanations.
        Reported issues : %s

        Additional context to help you:
        - The raw CrySL rules which were violated
        - Descriptions of the errors
        - Linked CWEs which may give an idea about the issue

        Raw CrySL rules : %s

        Error descriptions : %s

        """.formatted(previous, issues, crysl, desc);

        return model.chat(prompt);
    }
}