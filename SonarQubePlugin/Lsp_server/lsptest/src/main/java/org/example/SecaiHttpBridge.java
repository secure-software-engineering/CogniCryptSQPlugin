package org.example;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Simple HTTP bridge to forward open file requests to the running LSP server.
 */
public class SecaiHttpBridge {
    private static final Logger logger = Logger.getLogger(SecaiHttpBridge.class.getName());
    private final LanguageServer lspServer;
    private final Gson gson = new Gson();

    public SecaiHttpBridge(LanguageServer lspServer) {

        this.lspServer = lspServer;
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/open-file", this::handleOpenFile);
        server.setExecutor(Executors.newFixedThreadPool(2));
        server.start();
        logger.info("SecAI HTTP Bridge listening on port " + port);
    }

    private void handleOpenFile(HttpExchange exchange) throws IOException {

        // Add CORS headers for all responses
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            // Preflight request, just return headers
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder body = new StringBuilder();
        String lineString;
        while ((lineString = br.readLine()) != null) {
            body.append(lineString);
        }

        // Expecting JSON: { "fileUri": "file:///path/to/file", "line": 1, "column": 1 }
        Map<String, Object> params = gson.fromJson(body.toString(), Map.class);
        String fileUri = (String) params.get("fileUri");
        Double line = params.get("line") != null ? (Double) params.get("line") : 1.0;
        Double column = params.get("column") != null ? (Double) params.get("column") : 1.0;

        // Send executeCommand to LSP server
        ExecuteCommandParams cmdParams = new ExecuteCommandParams();
        cmdParams.setCommand("secai.openFile");
        cmdParams.setArguments(Arrays.asList(fileUri, line.intValue(), column.intValue()));

        lspServer.getWorkspaceService().executeCommand(cmdParams);

        String response = "{\"status\":\"ok\"}";
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}