# Confidence Scores and Quick Fixes

The custom page opens to a vulnerabilities list similar to the issue list of the native SonarQube interface. However, this list only includes the issues detected by *CogniCrypt<sub>SAST</sub>*. When first opening this page it can take a little while for the issues to show as the confidence scores are calculated before any issues are shown. 

Once the list of issues is populated you can open a [detail view](#quick-fixes) for each issue which includes [*Confidence Scores*](#confidence-severity-and-priority-scores) and [*Quick Fixes*](#quick-fixes) as well as access to the [*AIFix*](./aifix.md) feature.

## Detailed Issue Information

description, code snippet, more info

### Confidence, Severity, and Priority Scores

### Quick Fixes

### AIFix

The *AIFix* feature can be accessed through the tab **AIFix**. Details on how to use it can be found [here](aifix.md).

---

## Common Issues

### No issues found

- Make sure that at least one rule from the **CogniCrypt Security Rules** repository is active in your projects quality profile. See [first analysis steps](../getting-started/first-analysis.md)
- This interface only considers issues detected by *CogniCrypt<sub>SAST</sub>*. Go to the **Issues** tab of your SonarQube project and filter for issues tagged `cognicrypt`. If there are no issues then there truly are no issues to display

### Confidence Score missing

- Network error: Make sure the flask backend is running and reachable
- No CPG available: open an issue in GitHub. This may be an actual issue

In the `Flaskapp` folder of your backend there is a log file called `fp.log`. This may provide additional insight into the problem. Furthermore, the file `app.log` logs all http requests received and sent by the flask server.

### AIFix returns *Unexpected Error*

In the `Flaskapp` folder of your backend there is a log file called `aifix.log`. This may provide additional insight into the problem. Furthermore, the file `app.log` logs all http requests received and sent by the flask server.