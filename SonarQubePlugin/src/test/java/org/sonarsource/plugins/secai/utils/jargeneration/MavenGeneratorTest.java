package org.sonarsource.plugins.secai.utils.jargeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.config.Configuration;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.utils.SourceCodeService;
import org.sonarsource.plugins.secai.utils.exceptions.BaseDirNotSetException;
import org.sonarsource.plugins.secai.utils.exceptions.JarGenerationException;

@ExtendWith(MockitoExtension.class)
class MavenGeneratorTest {

    private MavenGenerator generator;
    @Mock
    private Invoker mockInvoker;
    private JarGenerator mockJarGenerator;
    private Configuration config;
    private MockedStatic<JarGenerator> jarGenStatic;
    private MockedStatic<SourceCodeService> sourceStatic;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        generator = new MavenGenerator(mockInvoker);

        mockJarGenerator = mock(JarGenerator.class);

        // mock SecAISettings
        config = mock(Configuration.class);
        // Always supply defaults for every config key your plugin uses
        when(config.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            switch (key) {
                case "sonar.secai.build.system":
                    return Optional.of("MAVEN");
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

        jarGenStatic = mockStatic(JarGenerator.class);
        jarGenStatic.when(JarGenerator::getInstance).thenReturn(mockJarGenerator);

        sourceStatic = mockStatic(SourceCodeService.class);
        sourceStatic.when(() -> SourceCodeService.getInstance(any())).thenReturn(mock(SourceCodeService.class));
    }

    @AfterEach
    void tearDown() {
        jarGenStatic.close();
        sourceStatic.close();
    }

    @Test
    void testSetAndGetSelectedJar() {
        generator.setSelectedJar("target/custom.jar");
        assertEquals("target/custom.jar", generator.getSelectedJar());
    }

    @Test
    void testThrowsIfBaseDirMissing() {
        when(mockJarGenerator.getBaseDir()).thenReturn(null);
        assertThrows(BaseDirNotSetException.class, generator::start);
    }

    @Test
    void testThrowsIfMavenHomeMissing() {
        when(mockJarGenerator.getBaseDir()).thenReturn(tempDir.toString());
        when(config.getStringArray("sonar.secai.maven.home")).thenReturn(new String[] {});
        SecAISettings.newInstance(config);
        assertThrows(JarGenerationException.class, generator::start);
    }

    @Test
    void testStartFailsIfBuildFails() throws MavenInvocationException {
        when(mockJarGenerator.getBaseDir()).thenReturn(tempDir.toString());

        InvocationResult failedResult = mock(InvocationResult.class);
        when(failedResult.getExitCode()).thenReturn(1);
        when(mockInvoker.execute(any())).thenReturn(failedResult);

        assertThrows(JarGenerationException.class, generator::start);
    }

    @Test
    void testStartSucceedsIfJarBuilt() throws Exception {
        File targetDir = new File(tempDir.toFile(), "target");
        targetDir.mkdirs();
        File jarFile = new File(targetDir, "sonar-secai-plugin-1.0.0.jar");
        assertTrue(jarFile.createNewFile());

        when(mockJarGenerator.getBaseDir()).thenReturn(tempDir.toString());

        InvocationResult successResult = mock(InvocationResult.class);
        when(successResult.getExitCode()).thenReturn(0);
        when(mockInvoker.execute(any())).thenReturn(successResult);

        String result = generator.start();
        assertNotNull(result);
        assertTrue(result.endsWith(".jar"));
        assertTrue(new File(result).exists());
    }

    @Test
    void testGetJarPathsReturnsOnlyJars() throws Exception {
        File target = new File(tempDir.toFile(), "target");
        target.mkdir();
        File jar = new File(target, "a.jar");
        File txt = new File(target, "readme.txt");
        jar.createNewFile();
        txt.createNewFile();

        Method m = MavenGenerator.class.getDeclaredMethod("getJarPaths", String.class);
        m.setAccessible(true);

        String log = "Building jar: " + jar.getAbsolutePath() + "\n" +
                "Building jar: " + txt.getAbsolutePath() + "\n";

        @SuppressWarnings("unchecked")
        ArrayList<String> jars = (ArrayList<String>) m.invoke(generator, log);
        assertEquals(1, jars.size());
        assertTrue(jars.get(0).endsWith("a.jar"));
    }

    @Test
    void testStartSelectsSpecifiedJarWhenMultipleExist() throws Exception {
        File targetDir = new File(tempDir.toFile(), "target");
        targetDir.mkdirs();
        File jarA = new File(targetDir, "alpha.jar");
        File jarB = new File(targetDir, "beta.jar");
        assertTrue(jarA.createNewFile());
        assertTrue(jarB.createNewFile());

        when(mockJarGenerator.getBaseDir()).thenReturn(tempDir.toString());

        InvocationResult successResult = mock(InvocationResult.class);
        when(successResult.getExitCode()).thenReturn(0);
        when(mockInvoker.execute(any())).thenReturn(successResult);

        generator.setSelectedJar("beta.jar");

        String resultPath = generator.start();
        assertNotNull(resultPath);
        assertTrue(resultPath.endsWith("beta.jar"));
    }

    @Test
    void testStartThrowsWhenSelectedJarMissing() throws Exception {
        File targetDir = new File(tempDir.toFile(), "target");
        targetDir.mkdirs();
        File jarA = new File(targetDir, "gamma.jar");
        File jarB = new File(targetDir, "delta.jar");
        assertTrue(jarA.createNewFile());
        assertTrue(jarB.createNewFile());

        when(mockJarGenerator.getBaseDir()).thenReturn(tempDir.toString());

        InvocationResult successResult = mock(InvocationResult.class);
        when(successResult.getExitCode()).thenReturn(0);
        when(mockInvoker.execute(any())).thenReturn(successResult);

        generator.setSelectedJar("nonexistent");
        assertThrows(JarGenerationException.class, generator::start);
    }

    @Test
    void testStartThrowsWhenNoJarProduced() throws Exception {
        File targetDir = new File(tempDir.toFile(), "target");
        targetDir.mkdirs();
        File dummy = new File(targetDir, "file.tmp");
        assertTrue(dummy.createNewFile());

        when(mockJarGenerator.getBaseDir()).thenReturn(tempDir.toString());

        InvocationResult successResult = mock(InvocationResult.class);
        when(successResult.getExitCode()).thenReturn(0);
        when(mockInvoker.execute(any())).thenReturn(successResult);

        assertThrows(JarGenerationException.class, generator::start);
    }
}