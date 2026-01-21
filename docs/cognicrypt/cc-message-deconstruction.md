# Extracting Additional Information

The report returned by [CogniCrypt](https://github.com/CROSSINGTUD/CryptoAnalysis) does not have all required information nicely separated out in the JSON file (see singular error below). Some of the data needs to be manually extracted from the given error message and Jimple statement.

```json
{
  "locations" : [ {
    "physicalLocation" : {
      "region" : {
        "snippet" : {
          "text" : "cipher = getInstance($stack5)"
        },
        "startLine" : 31
      }, 
      "artifactLocation" : {
        "index" : 169,
        "uri" : "org/cambench/cap/mixedsensitivities/fieldflow/truepositive/brokencrypto/BrokenCrypto2.java"
      }
    }
  }, {
    "logicalLocations" : [ {
      "kind" : "class",
      "name" : "org.cambench.cap.mixedsensitivities.fieldflow.truepositive.brokencrypto.BrokenCrypto2"
    }, {
      "kind" : "method",
      "name" : "void main(java.lang.String[])"
    } ]
  } ],
  "ruleId" : "javax.crypto.Cipher",
  "message" : {
    "markdown" : "ConstraintError violating CrySL rule for javax.crypto.Cipher",
    "text" : "Constraint \"instanceOf[key, javax.crypto.SecretKey] => alg(transformation) in {AES, PBEWithHmacSHA224AndAES_128, PBEWithHmacSHA256AndAES_128, PBEWithHmacSHA384AndAES_128, PBEWithHmacSHA512AndAES_128, PBEWithHmacSHA224AndAES_256, PBEWithHmacSHA256AndAES_256, PBEWithHmacSHA384AndAES_256, PBEWithHmacSHA512AndAES_256}\" on object cipher is violated due to the following reason:\\n|- Constraint \"instanceOf[key, javax.crypto.SecretKey] => alg(transformation) in {AES, PBEWithHmacSHA224AndAES_128, PBEWithHmacSHA256AndAES_128, PBEWithHmacSHA384AndAES_128, PBEWithHmacSHA512AndAES_128, PBEWithHmacSHA224AndAES_256, PBEWithHmacSHA256AndAES_256, PBEWithHmacSHA384AndAES_256, PBEWithHmacSHA512AndAES_256}\" evaluates to <false>:\\n\\t|- The left side \"instanceOf[key, javax.crypto.SecretKey]\" evaluates to <true>:\\n\\t\\t|- Second parameter @ cipher.init(varReplacer1038,$stack7) is not an instance of class javax.crypto.SecretKey\\n\\t|- The right side \"alg(transformation) in {AES, PBEWithHmacSHA224AndAES_128, PBEWithHmacSHA256AndAES_128, PBEWithHmacSHA384AndAES_128, PBEWithHmacSHA512AndAES_128, PBEWithHmacSHA224AndAES_256, PBEWithHmacSHA256AndAES_256, PBEWithHmacSHA384AndAES_256, PBEWithHmacSHA512AndAES_256}\" evaluates to <false>:\\n\\t\\t|- First parameter \"$stack5\" (transformation) with value \"Blowfish\" violates the constraint in class org.cambench.cap.mixedsensitivities.fieldflow.truepositive.brokencrypto.BrokenCrypto2 @ cipher = getInstance($stack5) @ line 31"
  },
  "properties" : {
    "errorType" : "ConstraintError",
    "subsequentErrors" : [ ],
    "errorId" : 4476,
    "precedingErrors" : [ ]
  }
}
```

---

## Report Location

CogniCrypt only returns the start line of the violating statement. However, in many cases the error message contains enough information to provide a more [accurate location](cc-location.md).

### Parameter Index

The error messages for _RequiredPredicateErrors_ and _AlternativeReqPredicateErrors_ always start with the exact parameter index by specifying the `Return value` or `Second parameter`. This makes the index very easy to extract.

As seen below, _ConstraintErrors_ also frequently contain the parameter index. However, sometimes no mention of the index is made. This is always the case for _ImpreciseValueExtractionErrors_.

```text
Constraint \"algorithm in {SHA-256, SHA-384, SHA-512}\" on object messageDigest is violated due to the following reason:
|- First parameter \"$stack7\" with value \"MD5\" should be any of {SHA-256, SHA-384, SHA-512} (extracted @ varReplacer1043 = \"MD5\" @ line 28)
```

In these situations we take the method call to try and match the exact event defined in the **EVENTS** section of the violated [CrySL rule](https://github.com/CROSSINGTUD/Crypto-API-Rules) and extract the name of the parameter from (the right side of) the [violated constraint](#violated-constraint) to get the parameter index from the event. Should this fail, we revert to simply marking the entire code snippet.

### Additional Parameters

In some cases, such as the one below, there is more than one parameter relevant to the error.

```text
Constraint \"exponentSize < primeSize\" on object $stack2 is violated due to the following reason:
|- Extracted the following violating values for parameter  \"varReplacer41\" (exponentSize) @$stack2.<init>(varReplacer40,varReplacer41) @ line 53:
    |- Value 10 in class \"com.example.violations.ConstraintViolations\" @varReplacer41 = 10 @ line 53
|- Extracted the following violating values for parameter  \"varReplacer40\" (primeSize) @$stack2.<init>(varReplacer40,varReplacer41) @ line 53:
    |- Value 5 in class \"com.example.violations.ConstraintViolations\" @varReplacer40 = 5 @ line 53
```

Here, we also attempt to locate these additional parameters using the line, the class, and the Jimple statement and parameters given in the error message.

---

## Violated Predicates

From the error message of _RequiredPredicateErrors_ and _AlternativeReqPredicateErrors_ we can extract the violated predicates. These are used when computing the [severity](severity.md).

```text
Second parameter was not properly generated as generatedPubkey OR generatedKey OR generatedPrivkey
```

---

## Expected Methods

From the error message of _IncompleteOperationErrors_ (below) and _TypestateErrors_ we can extract the list of expected methods. These are utilized when computing the [severity](severity.md) and the list is immediately reused during the [editing of the error message](cc-message-edit.md).

```text
Operation on object of type javax.crypto.Cipher not completed. Expected call to one of the methods {doFinal(byte[]), update(byte[], int, int, byte[], int), doFinal(byte[], int, int, byte[], int), doFinal(byte[], int, int, byte[]), update(byte[]), update(byte[], int, int, byte[]), updateAAD(byte[], int, int), update(byte[], int, int), update(java.nio.ByteBuffer, java.nio.ByteBuffer), wrap(java.security.Key), updateAAD(byte[]), updateAAD(java.nio.ByteBuffer), doFinal(byte[], int, int), doFinal(java.nio.ByteBuffer, java.nio.ByteBuffer)}
```

---

## Violated Constraint

From the error message of _ConstraintErrors_ and _ImpreciseValueExtractionErrors_ we can extract the violated constraint. This is utilized when computing the [severity](severity.md). In some cases it is used to calculate the [parameter index](#parameter-index) or when [extracting quick fixes](#quick-fixes).

```text
Constraint on object 'cipher' was violated because: Algorithm of first parameter 'cryptoClass.cipher1' should be any of {AES, PBEWithHmacSHA224AndAES_128, PBEWithHmacSHA256AndAES_128, PBEWithHmacSHA384AndAES_128, PBEWithHmacSHA512AndAES_128, PBEWithHmacSHA224AndAES_256, PBEWithHmacSHA256AndAES_256, PBEWithHmacSHA384AndAES_256, PBEWithHmacSHA512AndAES_256}
```

---

## Quick Fixes

Some _ConstraintErrors_ are a result of set constraints being violated. As CogniCrypt employs an allow-list approach, we know that the values given in the [violated constraint](#violated-constraint) are secure alternatives. Using the [parameter index](#parameter-index) to get the exact [parameter location](../extension-guide/code-snippet.md#parameters) we can collect all relevant information to offer a quick fix, though SonarQube does not support it natively.

---

## Violated Jimple Statement

While CogniCrypt does provide us with a Jimple statement, this is slightly edited and thus does not match the actual Jimple line upon generating a new Jimple representation using [SootUp](https://github.com/soot-oss/SootUp). As knowing the exact statement is necessary for [detecting false positives](false-positive-detection.md) we search for this statement by iterating over the statements of the Jimple representation of the method in which the error occurs. Since we know the violating method call from the Jimple statement