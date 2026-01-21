package org.sonarsource.plugins.secai.utils;

import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.jar.JarFile;

public class ResourceService {

    private static final String BASE_DIR = System.getProperty("user.home") + File.separator + "secai";

    private static ResourceService INSTANCE;

    private ResourceService() throws IOException {

        // Creating permanent directories
        createPermanentDirectories();
        // call extract all
        extractAll();
    }

    /**
     * Creates the necessary permanent directories for storage.
     */
    private void createPermanentDirectories() {
        createDirectory(BASE_DIR);
    }

    /**
     * Creates a directory if it does not exist.
     * 
     * @param path Directory path
     */
    private void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Created directory: " + path);
        }
    }

    public static ResourceService getInstance() throws IOException {
        if (INSTANCE == null) {
            INSTANCE = new ResourceService();
        }

        return INSTANCE;
    }

    private void extractAll() throws IOException {
        // CogniCrypt
        extractCrySLRules();

        // other
    }

    private void extractCrySLRules() throws IOException {
        System.out.println("Extracting CrySL rules...");
        File dir = new File(BASE_DIR + File.separator + "crysl_rules");
        dir.mkdir();

        // update the CrySL rules if the existing files were last modified before the
        // given date
        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        String dateString = "06-09-2025 15:30:00"; // change the date when updating the resources in the jar
        long updateDate;
        try {
            updateDate = sdf.parse(dateString).getTime();
        } catch (ParseException e) {
            // fall back to "only extract if missing"
            updateDate = 0;
        }

        // resource directory _inside_ the jar
        String resourcePath = "org/sonarsource/plugins/secai/cognicrypt/crysl_rules";

        // call extract method
        extractDirectory(resourcePath, dir, updateDate);
    }

    public static String getWordTwoVec() {
        return BASE_DIR + File.separator + "jimple_word2vec.bin";
    }

    public String getCrySLRules() {
        return BASE_DIR + File.separator + "crysl_rules";
    }

    public String getReportDirectory() {
        return BASE_DIR + File.separator + "report";
    }

    public void extractResource(String resourceName, String filename, File targetDir) throws IOException {
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourceName);
        if (resourceStream == null) {
            throw new FileNotFoundException("Resource not found: " + resourceName);
        }

        File jarfile = new File(targetDir, filename + ".jar");
        if (!jarfile.exists()) {
            jarfile.createNewFile();

            try (FileOutputStream outStream = new FileOutputStream(jarfile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = resourceStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }
        }

    }

    /**
     *
     * @param dir        the path to put the extracted files
     * @param tempDir    the path where the files are to be extracted from
     * @param updateDate the date (in milliseconds) when the files inside the jar
     *                   were last updated
     * @throws IOException
     */
    public void extractDirectory(String dir, File tempDir, long updateDate) throws IOException {
        URL dirURL = getClass().getClassLoader().getResource(dir);
        if (dirURL == null) {
            throw new FileNotFoundException("Resource directory not found: " + dir);
        }

        if (dirURL.getProtocol().equals("jar")) {
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
            try (JarFile jar = new JarFile(jarPath)) {
                jar.stream()
                        .filter(e -> e.getName().startsWith(dir) && !e.isDirectory())
                        .forEach(e -> {
                            try {
                                File tempFile = new File(tempDir, e.getName().substring(dir.length() + 1));
                                tempFile.getParentFile().mkdirs();

                                // if the file already exists but was last modified before the given update,
                                // overwrite it
                                if (!tempFile.exists() || tempFile.lastModified() < updateDate) {
                                    try (InputStream is = jar.getInputStream(e);
                                            FileOutputStream fos = new FileOutputStream(tempFile)) {
                                        byte[] buffer = new byte[1024];
                                        int bytesRead;
                                        while ((bytesRead = is.read(buffer)) != -1) {
                                            fos.write(buffer, 0, bytesRead);
                                        }
                                    }
                                }
                            } catch (IOException ex) {
                                throw new RuntimeException("Failed to extract resource: " + e.getName(), ex);
                            }
                        });
            }
        } else if (dirURL.getProtocol().equals("file")) {
            // need to add this block because -> in Packaged plugin running we load resource
            // from JAR hence protocol == JAR
            // but, for testing -> we load resources from Filesystem -> hence protocol ==
            // file

            File directory = new File(dirURL.getPath());
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        File outFile = new File(tempDir, file.getName());
                        if (file.isFile() && (!outFile.exists() || outFile.lastModified() < updateDate)) {
                            try (InputStream in = new FileInputStream(file);
                                    OutputStream out = new FileOutputStream(outFile)) {
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, bytesRead);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported protocol for resource directory: " + dirURL.getProtocol());
        }
    }

    public String getResourceDir() {
        return BASE_DIR;
    }

   
}
