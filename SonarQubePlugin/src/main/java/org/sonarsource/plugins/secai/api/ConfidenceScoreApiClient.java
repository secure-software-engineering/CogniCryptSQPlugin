package org.sonarsource.plugins.secai.api;

import com.google.gson.Gson; // Or another JSON library like Jackson
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfidenceScoreApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfidenceScoreApiClient.class);

    private static final String API_ENDPOINT = "http://131.234.29.71/fp";

    private final Gson gson = new Gson();

    /**
     * Sends the generated dot graph along with its hashcode to the API and returns the response.
     * @param hashcode A unique identifier for the dot graph.
     * @param dotGraph The DOT graph representation as a string.
     * @return The API response containing prediction, probability_score, and unique_key, or null on failure.
     */
    public Response getConfidenceResponse(String hashcode, String dotGraph) {
        HttpURLConnection connection = null;
        try {
            Thread.sleep(2000);
            
            URL url = new URL(API_ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            // Setup the connection for a POST request
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            // TODO: Add authentication header if required
            // connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setDoOutput(true);

            // dotGraph = gson.toJson(dotGraph);
            // LOGGER.warn("EDITED" + dotGraph);

            // Create the request body as a JSON object with hashcode and dotGraph.
            String requestBody = gson.toJson(new ConfidenceRequest(hashcode, dotGraph));
            
            // Write the JSON payload to the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response from the API
                Response response = gson.fromJson(
                    new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8),
                    Response.class
                );
                return response;
            } else {
                LOGGER.error("API call failed with response code: {}", responseCode);
                // Log the error stream for more details
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                     new java.io.InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder responseText = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        responseText.append(responseLine.trim());
                    }
                    LOGGER.error("API error response: {}", responseText);
                }
            }
        } catch (Exception e) {
            LOGGER.error("An exception occurred during the API call.", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null; // Return null on failure
    }

    // --- Data Transfer Objects (DTOs) for JSON serialization/deserialization ---
    private static class ConfidenceRequest {
        String hashcode;
        String dot_graph;

        public ConfidenceRequest(String hashcode, String dotGraph) {
            this.hashcode = hashcode;
            this.dot_graph = dotGraph;
        }
    }

    public static class Response {
        public int prediction;
        public double probability_score;
        public String hashcode;
    }
}