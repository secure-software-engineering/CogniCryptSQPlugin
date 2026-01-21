// SecAI LSP Server - Complete Language Server Protocol implementation
// This LSP server handles IDE integration for your SonarQube SecAI plugin

package org.example;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

// Main class to start the LSP server
public class SecaiLspMain {

    private static final Logger logger = Logger.getLogger(SecaiLspMain.class.getName());

    public static void main(String[] args) {
        try {
            logger.info("Starting SecAI LSP Server");

            // Create language server
            SecaiLanguageServer server = new SecaiLanguageServer();

            // Create launcher
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);

            // Connect to client
            server.connect(launcher.getRemoteProxy());

            // Start listening
            launcher.startListening();

            logger.info("SecAI LSP Server started successfully");

            SecaiHttpBridge bridge = new SecaiHttpBridge(server);
            bridge.start(8081);

            logger.info("SecAI HTTP Bridge started on port 8081");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start LSP server", e);
            System.exit(1);
        }
    }
}