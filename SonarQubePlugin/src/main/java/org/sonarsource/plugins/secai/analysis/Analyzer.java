package org.sonarsource.plugins.secai.analysis;

import org.sonar.api.server.ws.Request;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarsource.plugins.secai.analysis.cognicrypt.CogniCrypt;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.utils.SourceCodeService;
import org.sonarsource.plugins.secai.utils.exceptions.MavenNotFoundException;
import org.sonarsource.plugins.secai.utils.exceptions.UnsupportedBuildSystemException;
import org.sonarsource.plugins.secai.utils.jargeneration.JarGenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Analyzer {

    public SecAISettings settings;
    public WsClient wsClient;

    public Analyzer(SecAISettings settings, Request request) {
        this.settings = settings;
        this.wsClient = WsClientFactories.getLocal().newClient(request.localConnector());
    }

    public void runAnalysis(OutputStream output) throws IOException {
        // load the source code of the project
        SourceCodeService codeService = SourceCodeService.getInstance(settings.getProjectKey());
        codeService.loadSources(wsClient);

        JarGenerator gen = JarGenerator.getInstance().setBaseDir(codeService.getSourceDir());

        try (PrintWriter writer = new PrintWriter(output, true, StandardCharsets.UTF_8)) {
            String jarPath = "";
            /* We don't want to start a jar generation when it's not needed, and
             * we don't want to do it more than once. So, a jar is only generated
             * if at least one of the selected tools requires one (if no tools are
             * selected, it defaults to CogniCrypt, which requires a jar).
             */
            if (List.of(AnalysisTool.toolsAnalyzingJars).stream().anyMatch(
                    tool -> settings.getTools().contains(tool))
                    || settings.getTools().isEmpty()) {
                jarPath = gen.generateJar();
                writer.println("data: jar path: " + jarPath + "\n\n");
            }

            if (settings.getTools().contains(AnalysisTool.COGNICRYPT) || settings.getTools().isEmpty()) {
                CogniCrypt cognicrypt = new CogniCrypt();

                // Run analysis and stream logs in real-time
                cognicrypt.generateWarnings(jarPath, (logLine) -> {
                    // Stream each log line as an SSE event
                    // NOTE: don't change here anything will cause problem with SSE events
                    writer.println("data: " + logLine + "\n\n");
                    writer.flush();
                });
            }
        } catch (MavenNotFoundException | UnsupportedBuildSystemException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            try (PrintWriter writer = new PrintWriter(output, true, StandardCharsets.UTF_8)) {
                //response.stream().setStatus(500);
                writer.println("{ \"status\": \"error\", \"message\": \"Failed to analyze JAR: "
                        + e.getMessage() + "\" }" + "\n\n");
            }
        }
    }
}
