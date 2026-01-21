package org.example;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// Core SecAI Service for IDE operations
class SecaiService {

    private static final Logger logger = Logger.getLogger(SecaiService.class.getName());
    private LanguageClient client;

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    /**
     * Open a file in VS Code at specific line and column
     */
    public boolean openInVsCode(String filePath, int lineNumber, int columnNumber) {
        try {
            // Validate file exists
            if (!Files.exists(Paths.get(filePath))) {
                logger.warning("File does not exist: " + filePath);
                showMessage("File does not exist: " + filePath, MessageType.Error);
                return false;
            }

            // Build VS Code command
            List<String> command = buildVsCodeCommand(filePath, lineNumber, columnNumber);

            // Execute command
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Wait for process completion
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                logger.info("Successfully opened VS Code");
                showMessage("File opened in VS Code", MessageType.Info);
                return true;
            } else {
                logger.warning("VS Code command failed or timed out");
                showMessage("Failed to open VS Code", MessageType.Error);
                return false;
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO Exception while opening VS Code", e);
            showMessage("IO Error: " + e.getMessage(), MessageType.Error);
            return false;
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted while waiting for VS Code", e);
            Thread.currentThread().interrupt();
            showMessage("Process interrupted", MessageType.Error);
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error opening VS Code", e);
            showMessage("Unexpected error: " + e.getMessage(), MessageType.Error);
            return false;
        }
    }

    /**
     * Open a project in VS Code
     */
    public boolean openProjectInVsCode(String projectPath) {
        try {
            // Validate project directory exists
            if (!Files.exists(Paths.get(projectPath))) {
                logger.warning("Project directory does not exist: " + projectPath);
                showMessage("Project directory does not exist: " + projectPath, MessageType.Error);
                return false;
            }

            String vsCodeCmd = getVsCodeCommand();
            List<String> command = Arrays.asList(vsCodeCmd, projectPath);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                logger.info("Successfully opened project in VS Code");
                showMessage("Project opened in VS Code", MessageType.Info);
                return true;
            } else {
                logger.warning("Failed to open project in VS Code");
                showMessage("Failed to open project in VS Code", MessageType.Error);
                return false;
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error opening project in VS Code", e);
            showMessage("Error opening project: " + e.getMessage(), MessageType.Error);
            return false;
        }
    }

    private List<String> buildVsCodeCommand(String filePath, int lineNumber, int columnNumber) {
        String vsCodeCmd = getVsCodeCommand();

        // Build file argument with line:column
        String fileArg = filePath + ":" + lineNumber + ":" + columnNumber;

        return Arrays.asList(vsCodeCmd, "--goto", fileArg);
    }

    private String getVsCodeCommand() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "code.cmd"; // Windows
        } else if (os.contains("mac")) {
            return "code"; // macOS
        } else {
            return "code"; // Linux
        }
    }

    private void showMessage(String message, MessageType type) {
        if (client != null) {
            MessageParams params = new MessageParams(type, message);
            client.showMessage(params);
        }
    }
}
