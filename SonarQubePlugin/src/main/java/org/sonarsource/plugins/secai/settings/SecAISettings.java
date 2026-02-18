package org.sonarsource.plugins.secai.settings;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.settings.ValuesRequest;
import org.sonarsource.plugins.secai.analysis.AnalysisTool;
import org.sonarsource.plugins.secai.utils.jargeneration.BuildSystem;

public class SecAISettings {

    private static SecAISettings INSTANCE;

    private BuildSystem buildSystem;
    private String[] mavenHome;
    private String projectKey;
    private ArrayList<AnalysisTool> tools = new ArrayList<>();
    private String ccMessageType;
    private String branch;

    private SecAISettings(Configuration config) {
        buildSystem = BuildSystem.valueOf(config.get("sonar.secai.build.system").orElse("AUTO"));
        String[] mavenArr = config.getStringArray("sonar.secai.maven.home");
        mavenHome = (mavenArr != null) ? mavenArr : new String[0];
        projectKey = config.get("sonar.projectKey").orElse("UNKNOWN_PROJECT");
        String[] toolArr = config.getStringArray("sonar.secai.tools");
        if (toolArr != null) {
            for (String entry : toolArr) {
                tools.add(AnalysisTool.getByDisplayName(entry));
            }
        }
        ccMessageType = config.get("sonar.secai.cognicrypt.messages").orElse("Shortened");
        branch = config.get("sonar.branch.name").orElse("main");
    }

    private SecAISettings(Request request) {
        projectKey = getProjectKey(request);
        WsClient wsClient = WsClientFactories.getLocal().newClient(request.localConnector());
        Settings.ValuesWsResponse valuesWsResponse = wsClient.settings()
                .values(new ValuesRequest().setComponent(projectKey));

        for (Settings.Setting setting : valuesWsResponse.getSettingsList()) {
            switch (setting.getKey()) {
                case "sonar.secai.build.system":
                    buildSystem = BuildSystem.valueOf(Optional.of(setting.getValue()).orElse("AUTO"));
                    break;
                case "sonar.secai.maven.home":
                    List<String> values = new ArrayList<>();
                    for (int i = 0; i < setting.getValues().getValuesCount(); i++) {
                        values.add(setting.getValues().getValues(i));
                    }
                    mavenHome = values.toArray(new String[0]);
                    break;
                case "sonar.secai.tools":
                    for (int i = 0; i < setting.getValues().getValuesCount(); i++) {
                        AnalysisTool tool = AnalysisTool.getByDisplayName(setting.getValues().getValues(i));
                        if (!tools.contains(tool)) {
                            tools.add(tool);
                        }
                    }
                    break;
                case "sonar.secai.cognicrypt.messages":
                    ccMessageType = Optional.of(setting.getValue()).orElse("Shortened");
                    break;
                case "sonar.branch.name":
                    branch = Optional.of(setting.getValue()).orElse("main");
                    break;
            }
        }
        // Defensive defaulting for missing settings
        if (buildSystem == null) buildSystem = BuildSystem.AUTO;
        if (mavenHome == null) mavenHome = new String[0];
        if (ccMessageType == null) ccMessageType = "Shortened";
        if (branch == null) branch = "main";
    }

    public static SecAISettings newInstance(Configuration configuration) {
        INSTANCE = new SecAISettings(configuration);
        return INSTANCE;
    }

    public static SecAISettings newInstance(Request request) {
        INSTANCE = new SecAISettings(request);
        return INSTANCE;
    }

    public static SecAISettings getInstance() {
        return INSTANCE;
    }

    public BuildSystem getBuildSystem() {
        return buildSystem;
    }

    public String[] getMavenHome() {
        return mavenHome;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public ArrayList<AnalysisTool> getTools() {
        return tools;
    }

    public String getCcMessageType() {
        return ccMessageType;
    }

    /**
     * Derives the projectKey from the Request.
     * @param request Request
     * @return projectKey
     */
    public String getProjectKey(Request request) {
        String projectKey = request.getHeaders().get("referer");
        projectKey = projectKey.substring(projectKey.indexOf("id=") + 3);
        return URLDecoder.decode(projectKey.split("&")[0], StandardCharsets.UTF_8);
    }
}
