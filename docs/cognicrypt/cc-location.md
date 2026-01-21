# Accurate Error Location

The [CogniCrypt](https://github.com/CROSSINGTUD/CryptoAnalysis) report provides only the line in which the statement containing the violating method call starts. With nested method calls this can result in the user not actually being able to tell where a specific error occurred, especially if the statement spans multiple lines.

Because of this, we made sure to pinpoint a more accurate location depending on the type of error detected. For this, we used the [additional information extracted previously](cc-message-deconstruction.md).

---

## _ImpreciseValueExtractionError_

For _ImpreciseValueExtractionErrors_ the report location is always a [parameter](../extension-guide/code-snippet.md#parameters). 

---

## _RequiredPredicateError_ and _AlternativeReqPredicateError_

While for the most part these types of error occur on [method parameters](../extension-guide/code-snippet.md#parameters), sometimes a violation occurs for the return value of a method call. If the result of the call is assigned to an object, then that object is marked. Otherwise, the [method bounds](../extension-guide/code-snippet.md#method-bounds) are chosen as the main report location.

---

## _ForbiddenMethodError_, _IncompleteOperationError_ and _TypestateError_

These error types deal with method calls. As such, we mark the [method bounds](../extension-guide/code-snippet.md#method-bounds) as the main location.

---

## _ConstraintError_

Most _ConstraintErrors_ occur on [parameters](../extension-guide/code-snippet.md#parameters), so the report location is then decided by the detected [parameter index](cc-message-deconstruction.md#parameter-index). However, some constraints deal with methods instead, in which case the [method bounds](../extension-guide/code-snippet.md#method-bounds) are set as the report location.