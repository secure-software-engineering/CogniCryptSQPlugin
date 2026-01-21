package org.example;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Map;

// Workspace Service Implementation
class SecaiWorkspaceService implements WorkspaceService {

    private static final Logger logger = Logger.getLogger(SecaiWorkspaceService.class.getName());
    private final SecaiService secaiService = new SecaiService();

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        logger.info("Executing command: " + params.getCommand());

        switch (params.getCommand()) {
            case "secai.openFile":
                return handleOpenFile(params.getArguments());
            case "secai.openProject":
                return handleOpenProject(params.getArguments());
            case "secai.showFix":
                return handleShowFix(params.getArguments());
            default:
                logger.warning("Unknown command: " + params.getCommand());
                return CompletableFuture.completedFuture("Unknown command");
        }
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams didChangeConfigurationParams) {
        // TODO
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams didChangeWatchedFilesParams) {
        // TODO
    }

    private CompletableFuture<Object> handleOpenFile(List<Object> arguments) {
        try {
            if (arguments == null || arguments.isEmpty()) {
                logger.severe("No arguments provided for openFile");
                return CompletableFuture.completedFuture(Map.of("success", false, "error", "No arguments provided"));
            }

            String rawFileArg = arguments.get(0).toString();
            String filePath = null;
            try {
                // Accept both file URIs (file:///C:/path/...) and plain paths
                if (rawFileArg.startsWith("file://")) {
                    URI uri = URI.create(rawFileArg);
                    filePath = Paths.get(uri).toAbsolutePath().toString();
                } else {
                    filePath = Paths.get(rawFileArg).toAbsolutePath().toString();
                }
            } catch (Exception e) {
                logger.warning("Failed to convert argument to path: " + rawFileArg + " (" + e.getMessage() + ")");
                filePath = rawFileArg; // fallback, but NOT recommended
            }

            int lineNumber = arguments.size() > 1 ? Integer.parseInt(arguments.get(1).toString()) : 1;
            int columnNumber = arguments.size() > 2 ? Integer.parseInt(arguments.get(2).toString()) : 1;

            logger.info("Opening file: " + filePath + " at line " + lineNumber + ", column " + columnNumber);

            boolean success = secaiService.openInVsCode(filePath, lineNumber, columnNumber);

            if (success) {
                return CompletableFuture
                        .completedFuture(Map.of("success", true, "message", "File opened successfully"));
            } else {
                return CompletableFuture.completedFuture(Map.of("success", false, "error", "Failed to open file"));
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error opening file", e);
            return CompletableFuture.completedFuture(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private CompletableFuture<Object> handleOpenProject(List<Object> arguments) {
        try {
            if (arguments == null || arguments.isEmpty()) {
                return CompletableFuture.completedFuture("Error: No project path provided");
            }

            String projectPath = arguments.get(0).toString();
            logger.info("Opening project: " + projectPath);

            boolean success = secaiService.openProjectInVsCode(projectPath);

            if (success) {
                return CompletableFuture.completedFuture("Project opened successfully");
            } else {
                return CompletableFuture.completedFuture("Failed to open project");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error opening project", e);
            return CompletableFuture.completedFuture("Error: " + e.getMessage());
        }
    }

    private CompletableFuture<Object> handleShowFix(List<Object> arguments) {
        // Handle showing AI fix suggestions
        return CompletableFuture.completedFuture("Fix shown");
    }
}
