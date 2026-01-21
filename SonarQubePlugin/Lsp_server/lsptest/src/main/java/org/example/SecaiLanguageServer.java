package org.example;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

// Main LSP Server class
public class SecaiLanguageServer implements LanguageServer, LanguageClientAware {

    private static final Logger logger = Logger.getLogger(SecaiLanguageServer.class.getName());
    private LanguageClient client;
    private final TextDocumentService textDocumentService;
    private final WorkspaceService workspaceService;
    private final SecaiService secaiService;

    public SecaiLanguageServer() {
        this.textDocumentService = new SecaiTextDocumentService();
        this.workspaceService = new SecaiWorkspaceService();
        this.secaiService = new SecaiService();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        logger.info("Initializing SecAI LSP Server");

        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        capabilities.setExecuteCommandProvider(new ExecuteCommandOptions(Arrays.asList(
                "secai.openFile",
                "secai.openProject",
                "secai.showFix"
        )));

        // Add custom capabilities for SecAI
        capabilities.setWorkspaceSymbolProvider(true);
        capabilities.setDocumentSymbolProvider(true);

        InitializeResult result = new InitializeResult(capabilities);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        logger.info("Shutting down SecAI LSP Server");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        logger.info("Exiting SecAI LSP Server");
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
        this.secaiService.setClient(client);
        logger.info("Language client connected");
    }

    // Custom SecAI service for handling IDE operations
    public SecaiService getSecaiService() {
        return secaiService;
    }
}
