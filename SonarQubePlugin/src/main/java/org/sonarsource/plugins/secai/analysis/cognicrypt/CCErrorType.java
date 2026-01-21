package org.sonarsource.plugins.secai.analysis.cognicrypt;

import org.sonarsource.plugins.secai.reporting.MalformedInputException;

public enum CCErrorType {
    ALTERNATIVE_REQ_PREDICATE ("AlternativeReqPredicateError"),
    CONSTRAINT ("ConstraintError"),
    FORBIDDEN_METHOD ("ForbiddenMethodError"),
    IMPRECISE_VALUE_EXTRACTION ("ImpreciseValueExtractionError"),
    INCOMPLETE_OPERATION ("IncompleteOperationError"),
    REQUIRED_PREDICATE ("RequiredPredicateError"),
    TYPESTATE ("TypestateError");

    private final String stringName;

    CCErrorType(String stringName) {
        this.stringName = stringName;
    }

    public static CCErrorType byName(String errorType) throws MalformedInputException {
        if (errorType == null) throw new MalformedInputException("Error type is null");
        return switch (errorType) {
            case "AlternativeReqPredicateError" -> ALTERNATIVE_REQ_PREDICATE;
            case "ConstraintError" -> CONSTRAINT;
            case "ForbiddenMethodError" -> FORBIDDEN_METHOD;
            case "ImpreciseValueExtractionError" -> IMPRECISE_VALUE_EXTRACTION;
            case "IncompleteOperationError" -> INCOMPLETE_OPERATION;
            case "RequiredPredicateError" -> REQUIRED_PREDICATE;
            case "TypestateError" -> TYPESTATE;
            default -> throw new MalformedInputException("Unknown CogniCrypt error type \"" + errorType + "\"");
        };
    }

    @Override
    public String toString() {
        return stringName;
    }
}
