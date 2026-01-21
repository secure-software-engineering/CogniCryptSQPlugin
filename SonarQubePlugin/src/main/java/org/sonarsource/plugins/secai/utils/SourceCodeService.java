package org.sonarsource.plugins.secai.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.components.TreeRequest;
import org.sonarqube.ws.client.sources.RawRequest;
import org.sonarqube.ws.client.sources.SourcesService;
import org.sonarsource.plugins.secai.reporting.Location;

public class SourceCodeService {

    private final Logger LOGGER = LoggerFactory.getLogger(SourceCodeService.class);

    private final String sourceDir;
    private final String projectKey;
    private ArrayList<String> paths = new ArrayList<>();

    private static HashMap<String, SourceCodeService> INSTANCES = new HashMap<>();

    private SourceCodeService(String projectKey) throws IOException {
        this.projectKey = projectKey;

        boolean temp = true; // for testing -> sometimes there are problems with the Windows temp dir but it fixes itself without me doing anything
        // create temporary directory for the source code of the project
        File tempDir;
        if (temp) {
            tempDir = Files.createTempDirectory("source-dir-").toFile();
            tempDir.deleteOnExit();
        } else {
            String safeProjectKey = projectKey.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
            tempDir = new File(ResourceService.getInstance().getResourceDir(), "source-dir-" + safeProjectKey);
            tempDir.mkdir();
        }
        this.sourceDir = tempDir.getAbsolutePath();
    }

    public static SourceCodeService getInstance(String projectKey) throws IOException {
        if (!INSTANCES.containsKey(projectKey)) {
            INSTANCES.put(projectKey, new SourceCodeService(projectKey));
        }

        return INSTANCES.get(projectKey);
    }

    /**
     * Executed serverside to get source code from client
     * @param wsClient WsClient that can be used to make requests to the web API
     */
    public void loadSources(WsClient wsClient) {
        paths = new ArrayList<>();

        // next we retrieve and save the source code of each file
        TreeRequest treeRequest = new TreeRequest()
                .setComponent(projectKey)
                .setQualifiers(List.of("FIL"));
        Components.TreeWsResponse fileTree = wsClient.components().tree(treeRequest);
        LOGGER.debug("components (files): {}", fileTree.getComponentsList());

        SourcesService sourcesService = wsClient.sources();
        for (Components.Component component : fileTree.getComponentsList()) {
            String key = component.getKey();
            RawRequest rawRequest = new RawRequest()
                    .setKey(key);
            String sourceCode = sourcesService.raw(rawRequest);
            File tempFile = new File(sourceDir, component.getPath());

            if (key.endsWith(".java")) {
                paths.add(component.getPath());
            }

            try {
                tempFile.getParentFile().mkdirs();
                tempFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file: " + tempFile.getAbsolutePath(), e);
            }
            try (FileWriter fw = new FileWriter(tempFile)) {
                fw.write(sourceCode);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load source file: " + key, e);
            }
        }
    }

    /**
     * Copies source code from user's base directory into temp dir, so that we can work on it
     * @param baseDir absolute path of the base directory
     */
    public void loadSources(String baseDir) {
        try {
            FileUtils.copyDirectory(new File(baseDir), new File(sourceDir), pathname -> !pathname.getPath().contains(".lock"));
            
            for (File file : FileUtils.listFiles(new File(sourceDir), new String[]{"java"}, true)) {
                paths.add(file.getPath().substring(sourceDir.length()));
            }
            LOGGER.debug("paths: {}", paths);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the source code in the temp source dir
     * @param newCode Map: key: full class name, value: map: key: Location with class name, code location and file path, value: new code to add
     */
    public void updateSources(Map<String, Map<Location, String>> newCode) {
        for (String key : newCode.keySet()) {
            File oldFile = new File(sourceDir, getFullPath(key));
            LOGGER.debug(oldFile.getAbsolutePath());

            try (BufferedReader br = new BufferedReader(new FileReader(oldFile))) {
                List<String> lines = br.lines().collect(Collectors.toList());

                for (Map.Entry<Location, String> entry : newCode.get(key).entrySet()) {
                    Location location = entry.getKey();

                    List<String> edit = Arrays.stream(entry.getValue().split("\n")).collect(Collectors.toList());
                    int oldLines = location.getEnd()[0] - location.getStart()[0] + 1;

                    // sometimes the edit is inserting a new line
                    if (oldLines == 1 && edit.size() > 1) {
                        // for this just concat the whole thing to the end of the line
                        lines.set(location.getStart()[0] - 1,
                                lines.get(location.getStart()[0] - 1).concat(entry.getValue()));
                    } else {
                        // replace the existing lines with the new code
                        for (int i = 0; i < oldLines; i++) {
                            int index = location.getStart()[0] + i - 1; // lists start from 0, file from 1

                            if (i == 0) { // first line
                                // start offset
                                int start = location.getStart()[1];

                                String editedLine = lines.get(index).substring(0, start) + edit.get(i);

                                // if this is also the last line also pay attention to the end offset
                                if (index == location.getEnd()[0]) {
                                    // end offset -> INCLUSIVE
                                    int end = location.getEnd()[1];

                                    editedLine += lines.get(index).substring(end + 1);
                                }

                                lines.set(index, editedLine);
                            } else if (i == edit.size() - 1) { // last line -> only if there is more than two lines
                                // end offset -> INCLUSIVE
                                int end = location.getEnd()[1];

                                lines.set(index, edit.get(i) + lines.get(index).substring(end + 1));
                            } else if (i < edit.size() - 1) { // inbetween lines -> can just replace entire line
                                lines.set(index, edit.get(i));
                            } else {
                                // in order to not mess with the line numbers, simply remove text from "deleted" lines
                                lines.set(index, "");
                            }
                        }

                        // the edit can be bigger than the original code snippet -> append rest of snippet into the last line
                        if (edit.size() > oldLines) {
                            int index = location.getStart()[0] + oldLines - 1; // lists start from 0, file from 1
                            String line = lines.get(index) + String.join("\n", edit.subList(oldLines, edit.size()));
                            lines.set(index, line);
                        }
                    }
                }

                try (FileWriter fw = new FileWriter(oldFile)) {
                    fw.write(String.join("\n", lines));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getFullPath(String className) {
        String partial = className.replace(".", File.separator) + ".java";
        return paths.stream().filter(path -> path.endsWith(partial)).collect(Collectors.joining());
    }

    public String getSourceDir() {
        return sourceDir;
    }

    public ArrayList<String> getPaths() {
        return paths;
    }

    public static void cleanAllOnExit() {
        for (SourceCodeService codeService : INSTANCES.values()) {
            codeService.cleanOnExit();
        }
    }

    private void cleanOnExit() {
        try {
            FileUtils.deleteDirectory(new File(sourceDir));
        } catch (IOException e) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            e.printStackTrace(new PrintWriter(out, true));

            if (out.toString().contains(".jar: The process cannot access the file because it is being used by another process")) {
                LOGGER.warn("The jar of the copied source code could not be deleted. To delete it manually, go to: {}", sourceDir);
            } else {
                LOGGER.error("Failed to clean up copied source code for project {}", projectKey);
                LOGGER.error(out.toString());
            }
        }
    }
}
