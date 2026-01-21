# CogniCrypt

The SecAI plugin integrates the SAST tool [CogniCrypt](https://github.com/CROSSINGTUD/CryptoAnalysis). The tool is included as a Maven dependency and executed from a custom `Sensor` during the [SonarQube](https://www.sonarsource.com/products/sonarqube/) analysis.

As the motivation behind the creation of this plugin was to improve CogniCrypt's usability, there are some additional steps to be taken before reporting the analysis results to SonarQube.

The following JSON snippet is an error extracted from a CogniCrypt analysis report:

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

As seen above, the error message can be quite long and often contains information made superfluous by the integration into SonarQube. Therefore, we [edit the message](cc-message-edit.md) first and [extract additional, useful information](cc-message-deconstruction.md). Since the report only provides us with the starting line, we also have to search for a [more accurate location](cc-location.md), which is where the previously extracted information gets used. The data is also utilized when computing the [severity](severity.md) of each issue. On top of this, we built a [false positive detector](false-positive-detection.md).

This additional information is included when the issue is reported to SonarQube. However, for this we had to define [custom SonarQube rules](cc-rules.md) specific to CogniCrypt.