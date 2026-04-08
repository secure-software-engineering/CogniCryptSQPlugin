# AIFix

This feature can be accessed through the custom *SecAI* web pages. If you are unsure how to reach this part of the interface, refer to the [this overview](overview.md).

When viewing an issue in the vulnerabilities list or in the error tree

![AIFix tab in the detailed issue view]()

![AIFix button in error tree node]()

At the top you can select which model to use. Also, similar to [*Code Generation*](code-gen.md), a *CogniCrypt<sub>SAST</sub>* analysis is run on the generated code to check for unresolved issues. If you increase the number of iterations the AI will attempt to fix persisting issues before returning a result.

The result includes an explanation, the code fix and *CWE* mappings.

![Generated AIFix]()

diff view