# Editing the Error Message

The structure of the error message is determined by the error type. While in some cases the existing message is simply shortened or even reused, a lot of the time additional data is utilized that was [previously extracted from the original message](cc-message-deconstruction.md).

---

## _RequiredPredicateError_ and _AlternativeReqPredicateError_

As you can see from the following _AlternativeReqPredicateError_ message, the messages for these two error types are already very short and can be left as they are.

```text
Second parameter was not properly generated as generatedPubkey OR generatedKey OR generatedPrivkey
```

---

## _ImpreciseValueExtractionError_

If the analysis was unable to evaluate a parameter, an _ImpreciseValueExtractionError_ is thrown with a message like the one below.

```text
Could not evaluate the constraint "exponentSize < primeSize" due to insufficient information:
|- Could not evaluate expression \"<unknown>\" in class \"com.example.violations.OtherViolations\" @ $stack3.<init>(prime,varReplacer1) @ line 15
```

Since the integration into SonarQube includes highlighting the exact error location, the location information after the colon in the message above is superfluous.

Also, while the constraint is used internally to determine the [parameter index](cc-message-deconstruction.md#parameter-index) it does not contain information that the user requires, especially since constraints can get quite lengthy. Instead, we replace the constraint with the parameter index. The result is the message below.

```text
Could not evaluate the constraint on first parameter due to insufficient information
```

---

## _ForbiddenMethodError_

Below you can see the error message for the type _ForbiddenMethodError_. 

```text
Detected call to forbidden method <javax.crypto.spec.PBEKeySpec: void <init>(char[])> of class javax.crypto.spec.PBEKeySpec. Instead, call one of the methods {PBEKeySpec(char[], byte[], int, int)}
```

To improve readability we edited method signature and shorten the fully qualified name.

```text
Detected call to forbidden method "void PBEKeySpec(char[])" of class PBEKeySpec. Instead, call one of the methods [PBEKeySpec]
```

---

## _IncompleteOperationError_ and _TypestateError_

The following is an error message for an _IncompleteOperationError_. _TypestateErrors_ produce the same message except for the first sentence, which instead reads as `Unexpected call to method \"doFinal\" on object of type javax.crypto.Cipher.`.

```text
Operation on object of type javax.crypto.Cipher not completed. Expected call to one of the methods {doFinal(byte[]), update(byte[], int, int, byte[], int), doFinal(byte[], int, int, byte[], int), doFinal(byte[], int, int, byte[]), update(byte[]), update(byte[], int, int, byte[]), updateAAD(byte[], int, int), update(byte[], int, int), update(java.nio.ByteBuffer, java.nio.ByteBuffer), wrap(java.security.Key), updateAAD(byte[]), updateAAD(java.nio.ByteBuffer), doFinal(byte[], int, int), doFinal(java.nio.ByteBuffer, java.nio.ByteBuffer)}
```

While the first step is again to reduce the fully qualified reference to `Cipher` to just the class name, the majority of the message is taken up by the various different method configurations. To shorten the message, we just collapse the list to only the method names, as can be seen in the edited message below.

```text
Operation on object of type Cipher not completed. Expected call to one of the methods [doFinal, update, updateAAD, wrap]
```

---

## _ConstraintError_

_ConstraintErrors_ can produce very long error messages, especially if the violated constraint is an implication. This can be very clearly seen in the message below.

```text
Constraint \"instanceOf[key, javax.crypto.SecretKey] => alg(transformation) in {AES, PBEWithHmacSHA224AndAES_128, PBEWithHmacSHA256AndAES_128, PBEWithHmacSHA384AndAES_128, PBEWithHmacSHA512AndAES_128, PBEWithHmacSHA224AndAES_256, PBEWithHmacSHA256AndAES_256, PBEWithHmacSHA384AndAES_256, PBEWithHmacSHA512AndAES_256}\" on object cipher is violated due to the following reason:
|- Constraint \"instanceOf[key, javax.crypto.SecretKey] => alg(transformation) in {AES, PBEWithHmacSHA224AndAES_128, PBEWithHmacSHA256AndAES_128, PBEWithHmacSHA384AndAES_128, PBEWithHmacSHA512AndAES_128, PBEWithHmacSHA224AndAES_256, PBEWithHmacSHA256AndAES_256, PBEWithHmacSHA384AndAES_256, PBEWithHmacSHA512AndAES_256}\" evaluates to <false>:
    |- The left side \"instanceOf[key, javax.crypto.SecretKey]\" evaluates to <true>:
        |- Second parameter @ cipher.init(varReplacer1038,$stack7) is not an instance of class javax.crypto.SecretKey
    |- The right side \"alg(transformation) in {AES, PBEWithHmacSHA224AndAES_128, PBEWithHmacSHA256AndAES_128, PBEWithHmacSHA384AndAES_128, PBEWithHmacSHA512AndAES_128, PBEWithHmacSHA224AndAES_256, PBEWithHmacSHA256AndAES_256, PBEWithHmacSHA384AndAES_256, PBEWithHmacSHA512AndAES_256}\" evaluates to <false>:
        |- First parameter \"$stack5\" (transformation) with value \"Blowfish\" violates the constraint in class org.cambench.cap.mixedsensitivities.fieldflow.truepositive.brokencrypto.BrokenCrypto2 @ cipher = getInstance($stack5) @ line 31
```

Comparing the original message (above) and the edited version (below) shows that while the final segment of the above message refers to the violated parameter by its Jimple reference the message below uses the actual name of the parameter.

Another thing of note is that in the original version the last three lines consist mostly of location information, which due to our integration into SonarQube is superfluous. 

Furthermore, previously the violated constraint was mentioned twice in full and then each part separate once. As the constraint is an implication, for there to be an error, the left side must have been correctly configured. For brevity, the edited message only includes the right side of the constraint.

```text
Constraint on object 'cipher' was violated because: Algorithm of first parameter 'cryptoClass.cipher1' should be any of {AES, PBEWithHmacSHA224AndAES_128, PBEWithHmacSHA256AndAES_128, PBEWithHmacSHA384AndAES_128, PBEWithHmacSHA512AndAES_128, PBEWithHmacSHA224AndAES_256, PBEWithHmacSHA256AndAES_256, PBEWithHmacSHA384AndAES_256, PBEWithHmacSHA512AndAES_256}
```