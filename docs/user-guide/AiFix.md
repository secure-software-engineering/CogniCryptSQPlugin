# AIFix

This feature can be accessed through the custom *SecAI* web pages. If you are unsure how to reach this part of the interface, refer to the [this overview](overview.md#custom-web-pages-within-sonarqube).

More specifically, you can find the *AIFix* feature by selecting the **AIFix** tab when viewing an issue in the [vulnerabilities list](./vulnerabilities-list.md) or by clicking an error node in the [error tree](error-tree.md).

![AIFix tab in the detailed issue view](./images/aifix-in-detail-view.PNG)

![AIFix in error tree node]()

At the top you can select which model to use. Also, similar to [*Code Generation*](code-gen.md), a *CogniCrypt<sub>SAST</sub>* analysis is run on the generated code to check for unresolved issues. If you increase the number of iterations the AI will attempt to fix persisting issues before returning a result.

The result includes an explanation, the code fix and *CWE* mappings. You will also be able to whether or not the code passed the final *CogniCrypt<sub>SAST</sub>* analysis.

![Generated AIFix](./images/aifix-generated-fix.PNG)

At the bottom, you can also choose to generate a GitHub pull request for this fix. For more details see [here](./github-pr-integration.md)

![Generated AIFix - GitHub PR](./images/aifix-generated-fix-bottom.PNG)

A diff view comparing the proposed changes to the original code can viewed in the newly created **Diff View** tab in the detailed error view, or, if you used the shortcut from the error tree ...

![Diff view](./images/diff-view-in-detail-view.PNG)

---

## Common Issues

### AIFix returns *Unexpected Error*

In the `Flaskapp` folder of your backend there is a log file called `aifix.log`. This may provide additional insight into the problem. Furthermore, the file `app.log` logs all http requests received and sent by the flask server.
