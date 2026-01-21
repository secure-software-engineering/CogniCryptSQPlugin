package org.sonarsource.plugins.secai.utils.jargeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.utils.exceptions.BaseDirNotSetException;
import org.sonarsource.plugins.secai.utils.exceptions.JarGenerationException;

class JarGeneratorTest {

    private JarGenerator jarGenerator;
    private Configuration config;

    @BeforeEach
    void setUp() {
        // mock SecAISettings
        config = mock(Configuration.class);
        // Always supply defaults for every config key your plugin uses
        when(config.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            switch (key) {
                case "sonar.secai.build.system":
                    return Optional.of("AUTO");
                case "sonar.secai.ai.model":
                    return Optional.of("DefaultModel");
                case "sonar.secai.ai.iterations":
                    return Optional.of("1");
                case "sonar.projectKey":
                    return Optional.of("test_project");
                case "sonar.secai.cognicrypt.messages":
                    return Optional.of("Shortened");
                default:
                    return Optional.of("test_value");
            }
        });
        when(config.getStringArray(anyString())).thenReturn(new String[] {});
        SecAISettings.newInstance(config);

        // Reset singleton for each test (dangerous in prod, but good for unit tests)
        // Use reflection to set INSTANCE = null if needed
        try {
            var field = JarGenerator.class.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception ignored) {
        }
        jarGenerator = JarGenerator.getInstance();
    }

    /**
     * Checks that gerInstance always returns same object -> singleTon is necessary for GLOBAL state
     */
    @Test
    void testSingletonInstance() {
        JarGenerator jg1 = JarGenerator.getInstance();
        JarGenerator jg2 = JarGenerator.getInstance();
        assertSame(jg1, jg2, "Should return the same singleton instance");
    }


    /**
     * To ensure path compatibility across different OS
     */
    @Test
    void testSetBaseDir_NormalizesSlashes() {
        String input = "C:\\Users\\foo\\bar";
        jarGenerator.setBaseDir(input);
        assertEquals("C:/Users/foo/bar", jarGenerator.getBaseDir());
    }

    /**
     * Prevents mysterious errors and enforces that a required base directory is always provided
     */
    @Test
    void testGenerateJar_ThrowsBaseDirNotSetException() {
        jarGenerator.setBaseDir(null);
        when(config.get("sonar.secai.build.system")).thenReturn(Optional.of("MAVEN"));
        SecAISettings.newInstance(config);
        assertThrows(BaseDirNotSetException.class, () -> jarGenerator.generateJar());
    }

    /**
     * Verifies that Maven builds are triggered correctly and the result is returned
     * @throws Exception if project doesn't have pom.xml file then it will throw Exception
     */
    @Test
    void testGenerateJar_Maven_Success() throws Exception {
        jarGenerator.setBaseDir(System.getProperty("java.io.tmpdir"));
        when(config.get("sonar.secai.build.system")).thenReturn(Optional.of("MAVEN"));
        SecAISettings.newInstance(config);
        // Create a dummy pom.xml to simulate a Maven project
        File pom = new File(jarGenerator.getBaseDir() + "/pom.xml");
        pom.createNewFile();

        MavenGenerator mavenGen = mock(MavenGenerator.class);
        var mavenField = JarGenerator.class.getDeclaredField("MAVEN");
        mavenField.setAccessible(true);
        mavenField.set(jarGenerator, mavenGen);

        when(mavenGen.start()).thenReturn("dummy.jar");
        String result = jarGenerator.generateJar();
        assertEquals("dummy.jar", result);
        pom.delete();
    }

    /**
     * Confirms Gradle build system is detected and works as expected
     * @throws Exception if project doesn't have build.gradle file then it will throw Exception
     */
    @Test
    void testGenerateJar_Gradle_Success() throws Exception {
        jarGenerator.setBaseDir(System.getProperty("java.io.tmpdir"));
        when(config.get("sonar.secai.build.system")).thenReturn(Optional.of("GRADLE"));
        SecAISettings.newInstance(config);
        File gradle = new File(jarGenerator.getBaseDir() + "/build.gradle");
        gradle.createNewFile();

        GradleGenerator gradleGen = mock(GradleGenerator.class);
        var gradleField = JarGenerator.class.getDeclaredField("GRADLE");
        gradleField.setAccessible(true);
        gradleField.set(jarGenerator, gradleGen);

        when(gradleGen.start()).thenReturn("dummy-gradle.jar");
        String result = jarGenerator.generateJar();
        assertEquals("dummy-gradle.jar", result);
        gradle.delete();
    }

    // Ensures AUTO-detection logic works and can pick the right build system
    @Test
    void testGenerateJar_AutoFallsBackToGradle() throws Exception {
        jarGenerator.setBaseDir(System.getProperty("java.io.tmpdir"));
        File gradle = new File(jarGenerator.getBaseDir() + "/build.gradle");
        gradle.createNewFile();

        GradleGenerator gradleGen = mock(GradleGenerator.class);
        var gradleField = JarGenerator.class.getDeclaredField("GRADLE");
        gradleField.setAccessible(true);
        gradleField.set(jarGenerator, gradleGen);

        when(gradleGen.start()).thenReturn("auto-gradle.jar");

        String result = jarGenerator.generateJar();
        assertEquals("auto-gradle.jar", result);
        gradle.delete();
    }

    // Catches misconfiguration early and reports it with a clear error
    @Test
    void testGenerateJar_Maven_FileNotFound_Throws() throws Exception {
        jarGenerator.setBaseDir(System.getProperty("java.io.tmpdir"));
        when(config.get("sonar.secai.build.system")).thenReturn(Optional.of("MAVEN"));
        SecAISettings.newInstance(config);
        File pom = new File(jarGenerator.getBaseDir() + "/pom.xml");
        if (pom.exists())
            pom.delete(); // Ensure it's missing
        assertThrows(JarGenerationException.class, () -> jarGenerator.generateJar());
    }

    // Same as above—makes sure missing build configs are handled with clear errors
    @Test
    void testGenerateJar_Gradle_FileNotFound_Throws() throws Exception {
        jarGenerator.setBaseDir(System.getProperty("java.io.tmpdir"));
        when(config.get("sonar.secai.build.system")).thenReturn(Optional.of("GRADLE"));
        SecAISettings.newInstance(config);
        File gradle = new File(jarGenerator.getBaseDir() + "/build.gradle");
        if (gradle.exists())
            gradle.delete();
        assertThrows(JarGenerationException.class, () -> jarGenerator.generateJar());
    }

    // Makes sure user preferences are correctly stored and retrieved
    @Test
    void testGetAndSetSelectedJar() {
        jarGenerator.setSelectedJar("foo-bar.jar");
        assertEquals("foo-bar.jar", jarGenerator.getSelectedJar());
    }

    // Validates that jar generation state is accessible after builds
    @Test
    void testGetGeneratedJars() {
        // Forwards to MavenGenerator
        assertNotNull(jarGenerator.getGeneratedJars());
    }
}
