# First SonarQube Analysis

---

## Verify Installation

The plugin will only be detected by the server after a restart of the SonarQube instance. Then an administrator account can verify its presence under **Administration > Marketplace > Plugins > Installed**.

> **Note:** On a newly installed SonarQube server the default credentials are username *admin* and password *admin*. You will immediately be prompted to change the password.

---

## Create SonarQube Project

In order to analyze code you will need to create a new SonarQube project to receive the analysis results. If you are intending to re-analyze an existing SonarQube project you can skip this step.

On your homepage (or under the **Projects** tab) in the top right you can click **Create Project**. As **Import from DevOps Platform** has not yet been tested please select **Local Project** and finish the setup dialog.

---

## Activate SecAI Rules

The plugin will only be executed when at least one SonarQube rule from the `CogniCrypt Security Rules` repository is activated in the active quality profile (**Project Settings > Quality Profiles**) of the project.

You can choose to use the integrated `SecAI` quality profile, which contains all `CogniCrypt Security Rules` but no others. Or, you can extend an existing profile such as the default `Sonar way` profiles. For this, go to the **Quality Profiles** menu at the very top of the page. As you can see below, you then click on the three dots of the `Sonar way` profile for **Java** and select **Extend**. You will be prompted to give the new profile a name.

![Extend quality profile](images/quality-profiles.PNG)

If you already have a custom profile you can simply click on that instead. Both options should lead to the below page, where in the bottom right you can opt to **Activate More** rules.

![Activate more](images/activate-rules.PNG)

You will be shown all rules not yet activated in your profile. On the left you can filter the rules by repository to narrow down the amount. If you wish to activate all at once you can use the **Bulk Change** option at the top. You can also filter the results more using tags and CWEs.

![Activate rules](images/select-rules.PNG)

The final step is to activate your new profile for your project using **Project Settings > Quality Profiles** or by going back to the previous page and selecting your projects under **Change Projects**.

---

## Analysis Token

If this is your first time analyzing your project you will need to generate an analysis token. To do this, log into an administrator account. Click on the account icon in the top right and select **My Account**.

![Open Administrator account options](images/token-1.PNG)

Switch to the **Security** tab. Here, you can generate tokens.

![Generate tokens in Security tab](images/token-2.PNG)

You can generate a **Project Analysis Token** which will only work for the specified project or a **Global Analysis Token** which will work on all projects. Copy the token immediately and store it securely. You will not be able to view the token value at a later time.

---

## Run Analysis

For running our *SecAI* analysis only local analysis has been properly tested. However, it is confirmed that using the [*SonarQube for IDE* plugin](https://www.sonarsource.com/products/sonarqube/ide/) is not possible.

In order to execute the local analysis open a console in the root directory of your project. Which command to use depends on the build system you use. After the analysis is done the results will show in SonarQube's web view.

### Maven Project

For a Maven project use the following command and replace `<projectKey>`, `<projectName>` and `<token>` with the correct values associated with your SonarQube project.

```bash
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=<projectKey> -Dsonar.projectName='<projectName>' -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token>
  ```

### Gradle Project

For a Gradle project use the following command and replace `<projectKey>`, `<projectName>` and `<token>` with the correct values associated with your SonarQube project.

```bash
./gradlew sonar -Dsonar.projectKey=<projectKey> -Dsonar.projectName='<projectName>' -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token>
  ```

> **Note:** On Windows you may have to use `.\gradlew` instead of `./gradlew`.

You will also need to add a reference to SonarQube in your `build.gradle` or `build.gradle.kts` file:

```groovy
plugins {
  id "org.sonarqube" version "7.2.2.6593"
}
```

