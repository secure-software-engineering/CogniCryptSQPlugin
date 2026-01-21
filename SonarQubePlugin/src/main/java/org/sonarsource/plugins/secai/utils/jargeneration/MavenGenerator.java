package org.sonarsource.plugins.secai.utils.jargeneration;

import static org.sonarsource.plugins.secai.utils.jargeneration.BuildSystem.MAVEN;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.utils.SourceCodeService;
import org.sonarsource.plugins.secai.utils.exceptions.BaseDirNotSetException;
import org.sonarsource.plugins.secai.utils.exceptions.JarGenerationException;
import org.sonarsource.plugins.secai.utils.exceptions.MavenNotFoundException;

public class MavenGenerator {

    private String selectedJar = null;
    private ArrayList<String> generatedJars = new ArrayList<>();
    private String mavenHome;

    private final Logger LOGGER = LoggerFactory.getLogger(MavenGenerator.class);

    // Allow injection for tests
    private final Invoker invoker;

    public MavenGenerator() {
        this.invoker = new DefaultInvoker();
    }

    public MavenGenerator(Invoker invoker) {
        this.invoker = invoker;
    }

    /**
     * Always delegate to the enhanced implementation.
     */
    public String start() throws JarGenerationException, MavenNotFoundException, BaseDirNotSetException {
        return doStartEnhanced();
    }

    /**
     * Enhanced build flow:
     *  - Validates base dir (throws BaseDirNotSetException if missing)
     *  - Ensures sources are available (falls back to baseDir if SourceCodeService returns null/blank)
     *  - Builds via Invoker and checks exit code
     *  - Finds jars either from Maven output or by scanning target/
     *  - If multiple jars exist and user set selectedJar, picks matching one by filename
     */
    private String doStartEnhanced() throws JarGenerationException, BaseDirNotSetException {
        BuildSystem buildSystem = SecAISettings.getInstance().getBuildSystem();
        String baseDir = JarGenerator.getInstance().getBaseDir();
        if (baseDir == null) {
            LOGGER.error("Base directory (baseDir) is not set!");
            throw new BaseDirNotSetException();
        }

        String sourceDir;
        try {
            if (!baseDir.contains("source-dir")) {
                String projectKey = SecAISettings.getInstance().getProjectKey();
                SourceCodeService.getInstance(projectKey).loadSources(baseDir);
                String candidate = SourceCodeService.getInstance(projectKey).getSourceDir();
                sourceDir = (candidate != null && !candidate.isBlank()) ? candidate : baseDir;
                LOGGER.info("Loaded sources into: {}", sourceDir);
            } else {
                sourceDir = baseDir;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load sources", e);
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        InvocationRequest request;
        try {
            request = createInvocationRequest(err, sourceDir);
        } catch (MavenNotFoundException e) {
            // tests expect JarGenerationException when Maven is missing
            throw new JarGenerationException(buildSystem, MAVEN, baseDir, e);
        }

        int exitCode;
        try {
            InvocationResult result = this.invoker.execute(request);
            exitCode = (result != null) ? result.getExitCode() : -1;
        } catch (MavenInvocationException e) {
            LOGGER.debug("failed maven invocation", e);
            throw new JarGenerationException(buildSystem, MAVEN, baseDir, e);
        }

        if (exitCode != 0) {
            LOGGER.error("Maven build failed with exit code {}", exitCode);
            throw new JarGenerationException(buildSystem, MAVEN, baseDir);
        }

        String info = err.toString();
        ArrayList<String> jars = getJarPaths(info);

        // Fallback: scan target dir for jars (useful in mocked tests without full Maven logs)
        if (jars.isEmpty()) {
            File targetDir = new File(sourceDir, "target");
            if (targetDir.exists() && targetDir.isDirectory()) {
                File[] files = targetDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
                            jars.add(f.getAbsolutePath());
                        }
                    }
                }
            }
        }

        if (!jars.isEmpty()) {
            LOGGER.debug("generated jar paths: {}", jars);
            if (jars.size() != 1 && selectedJar != null) {
                for (String jar : jars) {
                    if (new File(jar).getName().startsWith(selectedJar)) {
                        return jar;
                    }
                }
                // user selected a jar name that wasn't produced
                throw new JarGenerationException(buildSystem, MAVEN, baseDir);
            }
            return jars.get(0);
        }

        LOGGER.error("Build succeeded but no JAR files were found in {}",
            new File(sourceDir, "target").getAbsolutePath());
        throw new JarGenerationException(buildSystem, MAVEN, baseDir);
    }

    /**
     * Parses Maven output to collect generated JAR files.
     * Only existing paths ending in .jar are returned.
     * Also records just the filenames into generatedJars.
     */
    private ArrayList<String> getJarPaths(String info) {
        ArrayList<String> jars = new ArrayList<>();
        int searchFrom = 0;
        while (info.indexOf("Building jar: ", searchFrom) != -1) {
            int start = info.indexOf("Building jar: ", searchFrom) + "Building jar: ".length();
            int end = info.indexOf("\n", start);
            String path = info.substring(start, end).replace("\\", "/").stripTrailing();

            File candidate = new File(path);
            if (candidate.isFile() && candidate.getName().toLowerCase().endsWith(".jar")) {
                jars.add(path);
            }
            searchFrom = end;
        }

        generatedJars = new ArrayList<>();
        for (String jar : jars) {
            generatedJars.add(new File(jar).getName());
        }
        return jars;
    }

    /**
     * Creates the Maven InvocationRequest, validating baseDir and resolving Maven home.
     */
    private InvocationRequest createInvocationRequest(ByteArrayOutputStream err, String baseDir)
            throws MavenNotFoundException, BaseDirNotSetException {

        if (baseDir == null) {
            LOGGER.error("Base directory passed to createInvocationRequest is null");
            throw new BaseDirNotSetException();
        }

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(baseDir + "/pom.xml"));
        request.setBaseDirectory(new File(baseDir));
        request.setShowErrors(true);
        request.setOutputHandler(new PrintStreamHandler(new PrintStream(err, true), true));
        request.addArgs(Arrays.asList("clean", "package", "--fail-at-end", "-DskipTests"));

        // Determine whether the user explicitly configured Maven home via settings.
        // We distinguish between null (no configuration) and an empty array (explicit but empty).
        String[] configuredHomes = null;
        try {
            if (SecAISettings.getInstance() != null) {
                configuredHomes = SecAISettings.getInstance().getMavenHome();
            }
        } catch (Exception ignored) {
            // ignore; treat as null
        }
        boolean userProvidedEmpty = (configuredHomes != null && configuredHomes.length == 0);

        if (request.getMavenHome() == null) {
            if (mavenHome == null) {
                findMavenHome();
            }
            if (mavenHome != null) {
                request.setMavenHome(new File(mavenHome));
            } else {
                // If the user explicitly configured an empty array, treat this as an error
                if (userProvidedEmpty) {
                    throw new MavenNotFoundException();
                }
                // Otherwise, leave Maven home unset and allow the invoker to resolve it
            }
        }
        return request;
    }

    /**
     * Attempts to discover a Maven installation and set {@code mavenHome} accordingly.
     *
     * <p>The search order is:</p>
     * <ol>
     *   <li>Locations embedded in the {@code java.library.path} containing "maven".</li>
     *   <li>The {@code MAVEN_HOME} environment variable.</li>
     *   <li>The {@code maven.home} system property.</li>
     *   <li>Explicit paths provided via {@link SecAISettings#getMavenHome()} if non-empty.</li>
     *   <li>The latest Homebrew installation under {@code /opt/homebrew/Cellar/maven}.</li>
     * </ol>
     *
     * <p>Importantly, this method never throws.  It simply tries to set
     * {@code mavenHome} if a valid location can be found.  Callers can
     * inspect {@code mavenHome} afterwards to decide whether to proceed
     * without explicitly setting a Maven home.</p>
     */
    private void findMavenHome() {
        // Attempt to locate a Maven installation without throwing.  This method
        // searches several well-known locations and records the first valid
        // result in the mavenHome field.  If no location is found, mavenHome
        // remains null and callers may fall back to defaults.
        LOGGER.debug("Searching for maven home");
        String[] mavenHomeSetting = null;
        try {
            if (SecAISettings.getInstance() != null) {
                mavenHomeSetting = SecAISettings.getInstance().getMavenHome();
            }
        } catch (Exception ignored) {
            // ignore and treat as no settings
        }

        // 1. Check java.library.path for a "maven" entry
        String libPath = System.getProperty("java.library.path");
        if (libPath != null && libPath.contains("maven")) {
            String[] entries = libPath.split(";");
            for (String entry : entries) {
                if (entry.contains("maven")) {
                    int cut = Math.max(0, entry.length() - 4);
                    mavenHome = entry.substring(0, cut);
                    LOGGER.debug("Found Maven in java.library.path: {}", mavenHome);
                    return;
                }
            }
        }

        // 2. Check MAVEN_HOME environment variable
        String mavenPath = System.getenv("MAVEN_HOME");
        if (mavenPath == null) {
            mavenPath = System.getenv().get("MAVEN_HOME");
        }
        if (mavenPath != null) {
            mavenHome = mavenPath;
            LOGGER.debug("Found Maven in MAVEN_HOME: {}", mavenHome);
            return;
        }

        // 3. Check maven.home system property
        Object prop = System.getProperties().get("maven.home");
        if (prop instanceof String) {
            mavenHome = (String) prop;
            LOGGER.debug("Found Maven in maven.home: {}", mavenHome);
            return;
        }

        // 4. Check explicit settings provided via SecAISettings
        if (mavenHomeSetting != null && mavenHomeSetting.length > 0) {
            for (String path : mavenHomeSetting) {
                if (path != null && !path.isBlank() && new File(path).exists()) {
                    mavenHome = path;
                    LOGGER.debug("Found Maven in settings: {}", mavenHome);
                    return;
                }
            }
        }

        // 5. Look for Homebrew installations (macOS/Linux)
        File homebrewMaven = new File("/opt/homebrew/Cellar/maven");
        if (homebrewMaven.exists() && homebrewMaven.isDirectory()) {
            File[] versions = homebrewMaven.listFiles(File::isDirectory);
            if (versions != null && versions.length > 0) {
                Arrays.sort(versions, (a, b) -> b.getName().compareTo(a.getName()));
                File latest = new File(versions[0], "libexec");
                if (latest.exists()) {
                    mavenHome = latest.getAbsolutePath();
                    LOGGER.debug("Found Maven in Homebrew: {}", mavenHome);
                    return;
                }
            }
        }

        // If none of the above succeed, leave mavenHome null
        LOGGER.debug("Maven home not found via any known mechanism");
    }

    public ArrayList<String> getGeneratedJars() {
        return generatedJars;
    }

    public String getSelectedJar() {
        return selectedJar;
    }

    public void setSelectedJar(String selectedJar) {
        this.selectedJar = selectedJar;
    }
}