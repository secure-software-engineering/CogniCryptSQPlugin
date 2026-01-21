package org.sonarsource.plugins.secai.code_generation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class cogniCryptRunner {

    private static Map<String, String> env = System.getenv();
    private final Path tempFolder;

    public cogniCryptRunner(Path tempFolder)
    {
        this.tempFolder = tempFolder;
    }

    public Path compileJavaFile(Path source) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classesDir = tempFolder.resolve("classes");
        Files.createDirectories(classesDir);
        if (compiler == null)
        {
            // ---- NEW: fallback to external javac (works even if SQ JVM is a JRE) ----
            String javaHome = env.getOrDefault("JAVA_HOME", "/usr/lib/jvm/java-21-openjdk-amd64");
            String javac = javaHome + "/bin/javac";

            List<String> cmd = List.of(
                javac,
                "-d", classesDir.toString(),
                source.toString()
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().putAll(env);
            Process p = pb.start();

            String stdout = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            int exit = p.waitFor();
            if (exit != 0) {
                throw new Exception("javac failed (exit " + exit + ")\n" + stdout + stderr);
            }
        }
        else
        {
            DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
            try (StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, null)) {
                Iterable<? extends JavaFileObject> cu =
                    fm.getJavaFileObjectsFromFiles(List.of(source.toFile()));

                boolean ok = compiler.getTask(null, fm, diags,
                                            List.of("-d", classesDir.toString()),
                                            null, cu).call();
                if (!ok) {
                    StringBuilder msg = new StringBuilder("Compilation failed:\n");
                    for (Diagnostic<?> d : diags.getDiagnostics()) msg.append(d).append('\n');
                    throw new Exception(msg.toString());
                }
            }
        }
    
        return classesDir.resolve("demo.class");      // old method returned String
    }

    /* ------------------------------------------------------------------ */
    /* 2 · classes/ → demo.jar                                             */
    /* ------------------------------------------------------------------ */
    public Path createJar(Path classFile) throws IOException {
        Path jar = tempFolder.resolve("demo.jar");

        try (JarOutputStream out = new JarOutputStream(
                new FileOutputStream(jar.toFile()))) {

            JarEntry entry = new JarEntry(classFile.getFileName().toString());
            out.putNextEntry(entry);
            out.write(Files.readAllBytes(classFile));
            out.closeEntry();
        }
        return jar;
    }

    /* ------------------------------------------------------------------ */
    /* 3 · run CogniCrypt CLI                                              */
    /* ------------------------------------------------------------------ */
    public void runCC(Path jar) throws Exception {
        /* static resources packaged in the plugin:                        */
        String rulesDir = "/org/sonarsource/plugins/secai/cognicrypt/crysl_rules";
        String headless = "/org/sonarsource/plugins/secai/cognicrypt/"
                        + "HeadlessJavaScanner-5.0.1-SNAPSHOT-jar-with-dependencies.jar";

        /* copy Headless scanner & rules into workspace just for this run  */
        Path headlessJar = copyResource(headless, "HeadlessScanner.jar");
        Path rulesPath   = copyResourceDir(rulesDir, "crysl_rules");

        Path sarif = tempFolder;

        List<String> cmd = List.of(
            "java", "-jar", headlessJar.toString(),
            "--rulesDir",   rulesPath.toString(),
            "--appPath",    jar.toString(),
            "--reportFormat","SARIF",
            "--reportPath", sarif.toString()
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().putAll(env);
        Process process = pb.start();

        /* read output & error streams into memory */
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

        int exit = process.waitFor();
        if (exit != 0) {
            throw new Exception("CogniCrypt failed (exit " + exit + ")\n" + stdout + stderr);
        }
    }

    /* ------------------------------------------------------------------ */
    /* helper: copy one resource file out of the JAR                      */
    /* ------------------------------------------------------------------ */
    private Path copyResource(String res, String fileName) throws IOException {
        Path dest = tempFolder.resolve(fileName);
        try (InputStream in = getClass().getResourceAsStream(res)) {
            if (in == null) throw new FileNotFoundException(res);
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return dest;
    }

    /* helper: copy an entire resource directory (rules)                  */
    private Path copyResourceDir(String base, String dirname) throws IOException 
    {
        Path dir = tempFolder.resolve(dirname);
        Files.createDirectories(dir);

        /* 1 · list every rule you package (one filename per element) */
        List<String> RULE_FILES = List.of("AlgorithmParameterGenerator.crysl", "AlgorithmParameters.crysl", "CertificateFactory.crysl", "CertPathTrustManagerParameters.crysl", "Cipher.crysl", "CipherInputStream.crysl", "CipherOutputStream.crysl", "Cookie.crysl", "DHGenParameterSpec.crysl", "DHParameterSpec.crysl", "DigestInputStream.crysl", "DigestOutputStream.crysl", "DSAGenParameterSpec.crysl", "DSAParameterSpec.crysl", "ECGenParameterSpec.crysl", "ECParameterSpec.crysl", "GCMParameterSpec.crysl", "HMACParameterSpec.crysl", "IvParameterSpec.crysl", "Key.crysl", "KeyAgreement.crysl", "KeyFactory.crysl", "KeyGenerator.crysl", "KeyManagerFactory.crysl", "KeyPair.crysl", "KeyPairGenerator.crysl", "KeyStore.crysl", "KeyStoreBuilderParameters.crysl", "Mac.crysl", "MessageDigest.crysl", "MGF1ParameterSpec.crysl", "OAEPParameterSpec.crysl", "PasswordAuthentication.crysl", "PBEKeySpec.crysl", "PBEParameterSpec.crysl", "PKIXBuilderParameters.crysl", "PKIXParameters.crysl", "PrivateKey.crysl", "PublicKey.crysl", "RSAKeyGenParameterSpec.crysl", "SecretKey.crysl", "SecretKeyFactory.crysl", "SecretKeySpec.crysl", "SecureRandom.crysl", "Signature.crysl", "SSLContext.crysl", "SSLEngine.crysl", "SSLParameters.crysl", "TrustAnchor.crysl", "TrustManagerFactory.crysl", "X509EncodedKeySpec.crysl" );

        /* 2 · copy each resource to <workspace>/crysl_rules/ */
        for (String name : RULE_FILES) 
        {
            String resPath = base + "/" + name;
            Path   dest    = dir.resolve(name);

            try (InputStream in = getClass().getResourceAsStream(resPath)) 
            {
                if (in == null) 
                {
                    System.out.println("Rule file missing in JAR: " + resPath);
                    continue;                     // skip silently or throw, your call
                }
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return dir;                               // returned path used by runCC
    }
}
