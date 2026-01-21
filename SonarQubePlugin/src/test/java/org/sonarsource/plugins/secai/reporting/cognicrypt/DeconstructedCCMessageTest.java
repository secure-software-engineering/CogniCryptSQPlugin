package org.sonarsource.plugins.secai.reporting.cognicrypt;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.sonar.api.config.Configuration;
import org.sonarsource.plugins.secai.analysis.cognicrypt.CCErrorType;
import org.sonarsource.plugins.secai.analysis.cognicrypt.CogniCrypt;
import org.sonarsource.plugins.secai.reporting.CodeSnippet;
import org.sonarsource.plugins.secai.reporting.Location;
import org.sonarsource.plugins.secai.reporting.MalformedInputException;
import org.sonarsource.plugins.secai.reporting.Parameter;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.utils.SourceCodeService;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DeconstructedCCMessageTest {

    /**
     * Test that malformed messages are detected and throw an error
     * @param message
     * @param errorType
     */
    @ParameterizedTest
    @CsvSource({
            ",errorType", // empty string (error type not important)
            "       \n\t  ,errorType", // blank string (error type not important)
            "ahbkej,RequiredPredicateError", // missing parameter index
            "George's parameter did stuff,AlternativeReqPredicateError", // missing extractable parameter index
            "Constraint violated for the following reason:, ConstraintError", // missing actual constraint
            "Could not evaluated the constraint due to insufficient information, ImpreciseValueExtractionError", // missing actual constraint
            "Unexpected call to method on object cipher,TypestateError", // missing method reference
            "Expected call to one of the methods but not mentioning methods,IncompleteOperationError", // missing method reference
            "Detected call to forbidden method,ForbiddenMethodError" // missing method reference
    })
    void handlesMalformedMessage(String message, String errorType) {
        String violatedRule = "rule";
        CodeSnippet cs = mock(CodeSnippet.class);
        assertThrows(MalformedInputException.class,() -> new DeconstructedCCMessage(message, errorType, violatedRule, cs));
    }

    @Test
    void handlesNullMessage() {
        String errorType = "ConstraintError";
        String violatedRule = "rule";
        CodeSnippet cs = mock(CodeSnippet.class);
        assertThrows(MalformedInputException.class, () -> new DeconstructedCCMessage(null, errorType, violatedRule, cs));
    }

    /**
     * Test handling of malformed error types. DeconstructedCCMessage relies on the handling of CCErrorType so
     * that's what will be tested here
     * @param errorType
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "", // empty
            "   \n  \t", // blank
            "constrainterror", // lowercase
            "AlterNativeReqPreDicateError", // wrong capitalization
            "ImpreciseValueExtraction", // missing "Error" at the end
            "NeverTypeOfError", // deprecated error type
            "InstanceOfError", // deprecated error type
            "HardCodedError", // deprecated error type
            "NoCallToError", // deprecated error type
            "CallToError" // deprecated error type
    })
    void handleMalformedErrorType(String errorType) {
        assertThrows(MalformedInputException.class, () -> CCErrorType.byName(errorType));
    }

    // test malformed CodeSnippet -> write CodeSnippetTest instead?

    /**
     * tests if it works on currently integrated CC version:
     * run CogniCrypt on a test jar and compare results
     * - parameterIndex
     * - message + summary
     * - expectedMethods/violatedPredicates/violatedConstraint/additionalParameters
     */
    @Test
    void testCogniCryptIntegration() throws IOException {
        // mock SecAISettings
        Configuration config = mock(Configuration.class);
        // Always supply defaults for every config key your plugin uses
        when(config.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            switch (key) {
                case "sonar.secai.build.system":
                    return Optional.of("AUTO");
                case "sonar.projectKey":
                    return Optional.of("test_project");
                case "sonar.secai.cognicrypt.messages":
                    return Optional.of("Shortened");
                default:
                    return Optional.of("test_value");
            }
        });
        when(config.getStringArray(anyString())).thenReturn(new String[] {});
        SecAISettings.newInstance(config);

        // mock SourceCodeService
        SourceCodeService codeService = mock(SourceCodeService.class);
        try (MockedStatic<SourceCodeService> codeServiceMock = mockStatic(SourceCodeService.class)) {
            codeServiceMock.when(() -> SourceCodeService.getInstance(SecAISettings.getInstance().getProjectKey())).thenReturn(codeService);
            when(codeService.getSourceDir()).thenReturn("src/test/resources/test_project");

            // run CogniCrypt
            String corruptedJar = "src/test/resources/test_project/TestProject-1.0.jar";
            Map<String, NewCCIssue> issues = new CogniCrypt().generateWarnings(corruptedJar, System.out::println);

            // compare results
            Gson gson = new Gson();
            InputStream inputStream = DeconstructedCCMessageTest.class.getClassLoader().getResourceAsStream("test_project/expected-errors.json");
            JsonArray expectedErrors = gson.fromJson(new InputStreamReader(inputStream), JsonArray.class);
            for (JsonElement e : expectedErrors) {
                JsonObject error = (JsonObject) e;
                NewCCIssue ccIssue = null;
                // match issue by:
                //      - error type
                //      - violated rule
                //      - method (includes class name)
                //      - start line (if all 3 parameters of the same method call violate different constraints from the same rule
                //                  -> test project puts conflicting parameters in different lines to make matching definite
                //                  -> if multiple parameters violate predicates, we can compare the original message as that doesn't contain code references for RequiredPredicateErrors)
                //      (can't match by jimple statement bc the numbering of jimple vars might differ)
                for (NewCCIssue issue : issues.values()) {
                    if (error.get("errorType").getAsString().equals(issue.getErrorType().toString())
                            && error.get("rule").getAsString().equals(issue.getFullRuleName())
                            && error.get("method").getAsString().equals(issue.getMethodCall())
                            && error.get("line").getAsInt() == issue.getStartLine()
                    ) {
                        // hardcoded and neverTypeOf occur in the exact same location at one point so if it's a
                        // ConstraintError also make sure that error and issue either both contain notHardCoded or both don't.
                        // If the constraint is null, it obviously doesn't contain notHardCoded
                        boolean xnor = error.get("violatedConstraint").isJsonNull() ||
                                error.get("violatedConstraint").getAsString().startsWith("notHardCoded")
                                    == issue.getEditedMessage("Original").contains("notHardCoded");
                        // if it's not a ConstraintError we can just go on
                        // Multiple RequiredPredicateErrors can occur for different parameters in the same method call
                        // Since for this error type the original message doesn't contain code references, we can compare this
                        if ((issue.getErrorType() == CCErrorType.CONSTRAINT && xnor)
                                || (issue.getErrorType() == CCErrorType.REQUIRED_PREDICATE
                                    && error.get("message").getAsString().equals(issue.getEditedMessage("Original")))
                                || (issue.getErrorType() != CCErrorType.CONSTRAINT
                                    && issue.getErrorType() != CCErrorType.REQUIRED_PREDICATE)) {
                            ccIssue = issue;

                            // remove matched issue from the issues map
                            issues.values().remove(issue);

                            break;
                        }
                    }
                }

                // check that the issue was matched
                // Note: at some point the callTo constraint and the getIV event were removed from Cipher. The relevant
                // errors for expected-error.json are commented out below in case it gets readded (the latter two replace
                // the TypestateErrors in typestate() and alternateReqPredicate())
                assert ccIssue != null;

                // compare:
                NewCCIssue finalCcIssue = ccIssue;
                assertAll(
                        // - parameter index
                        () -> assertEquals(error.get("parameterIndex").getAsInt(), finalCcIssue.getCcMessage().parameterIndex,
                        "Parameter index of expected error " + error.get("hashcode").getAsString()
                                + " and actual errorId " + finalCcIssue.getErrorID()),
                        // - message
                        () -> assertEquals(error.get("message").getAsString(), finalCcIssue.getEditedMessage("Shortened"),
                        "Message of expected error " + error.get("hashcode").getAsString()
                                + " and actual errorId " + finalCcIssue.getErrorID()),
                        // - expectedMethods
                        () -> assertTrue((error.get("expectedMethods").isJsonNull() && finalCcIssue.getCcMessage().getExpectedMethods() == null)
                            || (error.get("expectedMethods").isJsonArray() && equal(error.get("expectedMethods").getAsJsonArray(),
                                finalCcIssue.getCcMessage().getExpectedMethods())),
                                "Expected methods don't match for expected error " + error.get("hashcode").getAsString()
                                        + " and actual errorId " + finalCcIssue.getErrorID()),
                        // - violatedPredicates
                        () -> assertTrue((error.get("violatedPredicates").isJsonNull() && finalCcIssue.getCcMessage().getViolatedPredicates() == null)
                            || (error.get("violatedPredicates").isJsonArray() && equal(error.get("violatedPredicates").getAsJsonArray(),
                                                                        new ArrayList<>(finalCcIssue.getCcMessage().getViolatedPredicates()))),
                                "Predicates don't match for expected error " + error.get("hashcode").getAsString()
                                        + " and actual errorId " + finalCcIssue.getErrorID()),
                        // - violatedConstraint
                        () -> assertTrue((error.get("violatedConstraint").isJsonNull() && finalCcIssue.getCcMessage().getViolatedConstraint() == null)
                                        || error.get("violatedConstraint").getAsString().equals(finalCcIssue.getCcMessage().getViolatedConstraint()),
                        "Constraints don't match for expected error " + error.get("hashcode").getAsString()
                                + " and actual errorId " + finalCcIssue.getErrorID()),
                        // - additionalParameters
                        () -> assertTrue((error.get("additionalParameters").isJsonNull() && finalCcIssue.getCcMessage().getAdditionalParameters() == null)
                            || (error.get("additionalParameters").isJsonArray() && equal(error.get("additionalParameters").getAsJsonArray(),
                                finalCcIssue.getCcMessage().getAdditionalParameters())),
                                "Additional parameters don't match for expected error " + error.get("hashcode").getAsString()
                                        + " and actual errorId " + finalCcIssue.getErrorID())
                );
            }

            // there shouldn't be any leftover issues
            assert issues.isEmpty();
        }
    }

    private boolean equal(JsonArray json, List<?> list) {
        boolean strings = list.get(0) instanceof String;
        for (JsonElement element : json) {
            // this assumes that the method is only called with Strings or Parameters
            var search = strings ? element.getAsString() : parameterFromJson(element);
            if (list.contains(search)) {
                list.remove(search);
            } else {
                return false;
            }
        }

        // if the string list is empty the lists are equal since we removed all matches
        return list.isEmpty();
    }

    private Parameter parameterFromJson(JsonElement element) {
        if (!element.isJsonObject()) throw new ClassCastException();

        JsonObject o = element.getAsJsonObject();
        JsonObject location = o.getAsJsonObject("location");
        int[] start = new int[]{location.getAsJsonArray("start").get(0).getAsInt(),
                location.getAsJsonArray("start").get(1).getAsInt()};
        int[] end = new int[]{location.getAsJsonArray("end").get(0).getAsInt(),
                location.getAsJsonArray("end").get(1).getAsInt()};
        return new Parameter(new Location(new int[][]{start, end}, location.get("className").getAsString(),
                location.get("filePath").getAsString()), o.get("value").getAsString(), o.get("index").getAsInt());
    }


    /*

}, {
  "severity" : "MEDIUM",
  "codeSnippet" : "        Cipher cipher = Cipher.getInstance(\"AES/CTR/NoPadding\");",
  "method" : "com.example.violations.ConstraintViolations: void callTo()",
  "errorType" : "ConstraintError",
  "line" : 114,
  "hashcode" : "750949253",
  "rule" : "javax.crypto.Cipher",
  "message" : "Constraint on object 'cipher' was violated because: Call to one of the methods [getIV] is missing",
  "quickFixes" : [ ],
  "precedingErrors" : [ ],
  "expectedMethods" : [ "getIV" ],
  "violatedPredicates" : null,
  "confidenceScore" : 1.0,
  "statement" : "cipher = getInstance(varReplacer27)",
  "violatedConstraint" : "mode(transformation) in {CTR, CTS, CFB, OFB} && encmode == 1 => callTo[getIV()]",
  "subsequentErrors" : [ ],
  "additionalParameters" : null,
  "class" : "com.example.violations.ConstraintViolations",
  "parameterIndex" : 0,
  "reportLocation" : {
    "filePath" : "src/main/java/com/example/violations/ConstraintViolations.java",
    "start" : [ 114, 43 ],
    "end" : [ 114, 61 ],
    "className" : "com.example.violations.ConstraintViolations"
  }
}, {
  "severity" : "MEDIUM",
  "codeSnippet" : "        cipher.getIV();",
  "method" : "com.example.violations.OrderViolations: void typestate()",
  "errorType" : "TypestateError",
  "line" : 46,
  "hashcode" : "-2095690050",
  "rule" : "javax.crypto.Cipher",
  "message" : "Wrong typestate: Unexpected call to method \"getIV\" on object of type Cipher. Expected a call to one of the following methods [init]",
  "quickFixes" : [ ],
  "precedingErrors" : [ ],
  "expectedMethods" : [ "init" ],
  "violatedPredicates" : null,
  "confidenceScore" : 1.0,
  "statement" : "cipher.getIV()",
  "violatedConstraint" : null,
  "subsequentErrors" : [ ],
  "additionalParameters" : null,
  "class" : "com.example.violations.OrderViolations",
  "parameterIndex" : -5,
  "reportLocation" : {
    "filePath" : "src/main/java/com/example/violations/OrderViolations.java",
    "start" : [ 46, 15 ],
    "end" : [ 46, 21 ],
    "className" : "com.example.violations.OrderViolations"
  }
}, {
  "severity" : "MEDIUM",
  "codeSnippet" : "        c.getIV();",
  "method" : "com.example.violations.RequiredPredicateViolations: void alternativeReqPredicate(java.security.Key)",
  "errorType" : "TypestateError",
  "line" : 45,
  "hashcode" : "362278160",
  "rule" : "javax.crypto.Cipher",
  "message" : "Wrong typestate: Unexpected call to method \"getIV\" on object of type Cipher. Expected a call to one of the following methods [init]",
  "quickFixes" : [ ],
  "precedingErrors" : [ ],
  "expectedMethods" : [ "init" ],
  "violatedPredicates" : null,
  "confidenceScore" : 1.0,
  "statement" : "c.getIV()",
  "violatedConstraint" : null,
  "subsequentErrors" : [ ],
  "additionalParameters" : null,
  "class" : "com.example.violations.RequiredPredicateViolations",
  "parameterIndex" : -5,
  "reportLocation" : {
    "filePath" : "src/main/java/com/example/violations/RequiredPredicateViolations.java",
    "start" : [ 45, 10 ],
    "end" : [ 45, 16 ],
    "className" : "com.example.violations.RequiredPredicateViolations"
  }
     */
}