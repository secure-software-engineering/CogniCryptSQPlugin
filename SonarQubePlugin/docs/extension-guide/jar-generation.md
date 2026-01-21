# Jar Generation

The [util class](../../src/main/java/org/sonarsource/plugins/secai/utils/jargeneration/JarGenerator.java) `JarGenerator` can generate jars for Maven and Gradle.

---

## Setting Up the Generator

The `JarGenerator` is set up as a Singleton. If upon calling `JarGenerator.getInstance()` the singleton instance is not yet created, a new instance is instantiated and an attempt is made to retrieve the project base directory from the sonar system property. However, depending on the environment `System.getProperties()` does not include any Sonar properties. This is why, if you are aware that your call to `getInstance` is the first, a call to `setBaseDir` is necessary. The base directory is a `String` of the absolute path of the root directory of the project that needs to be analyzed.

When creating a `JarGenerator` from the Sensor, you can retrieve the path of the base directory with `sensorContext.fileSystem().baseDir().getAbsolutePath()`. If you are working from an API call, then the source code must first be loaded into a temporary directory, as the user's local project is not accessible. This can be done using `SourceCodeService.getInstance(projectKey).loadSources(request)`. Afterwards, you can retrieve the directory path using `SourceCodeServioce.getInstance(projectKey).getSourceDir()`.

---

## Generating the Jar

The jar generation is started with a call to `JarGenerator.getInstance().generateJar()`. This method attempts to build a jar in the project base directory and returns the absolute path to the generated jar as a `String`. If the project base directory is not set a **`BaseDirNotSetException`** will be thrown.

The `JarGenerator` has three modes for generating jars, the default is **AUTO**:
- **AUTO**: tries to auto-detect build system by attempting to retrieve the `pom.xml`, or if that isn’t available, the `build.gradle(.kts)` file. When none of these files are found a `UnsupportedBuildSystemException` is thrown.
- **MAVEN**: tries to force a Maven build
- **GRADLE**: tries to force a Gradle build

This setting can be changed by the user, in case both build system are at least partially configured in their project. It is tied to the custom SonarQube property `sonar.secai.build.system`, which only accepts the values specified in [the enum](../../src/main/java/org/sonarsource/plugins/secai/utils/jargeneration/BuildSystem.java) `BuildSystem`.

If the build fails or a specific build system (**MAVEN** or **GRADLE**) was given, a `JarGenerationException` is thrown.

Building with Maven requires access to Maven home. In most cases, this can be automatically detected from `MAVEN_HOME` or `PATH` but should those fail the custom SonarQube setting `sonar.secai.maven.home` is used. If none of these options return a viable path a **`MavenNotFoundException`** is thrown.

Also, while Maven can build multiple jars with one `pom.xml`, this method only returns the path to the first successfully generated jar. A list of all generated jars can be retrieved with `getgeneratedJars()`.