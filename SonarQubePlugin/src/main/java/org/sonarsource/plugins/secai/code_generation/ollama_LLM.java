package org.sonarsource.plugins.secai.code_generation;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

@Deprecated
public class ollama_LLM {

    private final ChatModel model;

    /**
     * Convenience constructor that reads:
     * - OLLAMA_BASE_URL (defaults to "http://localhost:11434")
     * - OLLAMA_MODEL    (e.g., "gemma2:9b-instruct", "llama3.1:8b-instruct", etc.)
     *
     * Throws if OLLAMA_MODEL is missing/blank.
     */
    public ollama_LLM() {
        String baseUrl = System.getenv("OLLAMA_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:11434";
        }
        String modelName = System.getenv("OLLAMA_MODEL");
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalStateException("OLLAMA_MODEL not set (e.g., gemma2:9b-instruct or llama3.1:8b-instruct)");
        }
        this.model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .build();
    }

    /**
     * Explicit constructor in case you want to pass values directly.
     */
    public ollama_LLM(String baseUrl, String modelName) {
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalStateException("modelName must be provided");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:11434";
        }
        this.model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .build();
    }

    public String Generate(String query, String context) {
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