package org.sonarsource.plugins.secai.utils;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These tests cover SARIF reporting - with cases like:
 * 1. Correct deserialization of SARIF
 * 2. Empty SARIF
 * 3. Deliberately malformed SARIF
 * 4. Testing of missing fields
 * 5. Multiple errors in the Same line or file.
 * 6. FileNotFound
 */

class SarifDeserializationTest {
    // This test is to verify the correct deserialization of a SARIF
    // file and the extraction of violation information from it.
    @Test
    void parsesSarifAndFindsViolations() throws Exception {
        // This tests if we can correctly parse a valid SARIF file
        // with multiple violations of different types, and extract all relevant fields?

        File sarifFile = new File("src/test/resources/sarif/cognicrypt-testing-sarif-version.json");
        Gson gson = new Gson();

        SarifDeserialization.ViolationReport report =
                gson.fromJson(new FileReader(sarifFile), SarifDeserialization.ViolationReport.class);

        // Validate top-level parsing
        assertNotNull(report, "Report should not be null");
        assertNotNull(report.getRuns(), "Runs should not be null");
        assertFalse(report.getRuns().isEmpty(), "Runs list should not be empty");

        List<SarifDeserialization.Result> results = report.getRuns().get(0).getResults();
        assertNotNull(results, "Results list should not be null");
        assertEquals(2, results.size(), "There should be 2 violations");

        // Collect and assert rules and error types
        List<String> rules = results.stream()
                .map(SarifDeserialization.Result::getViolatedRule)
                .collect(Collectors.toList());
        assertTrue(rules.contains("java.security.KeyPairGenerator"));
        assertTrue(rules.contains("javax.crypto.Cipher"));

        List<String> errorTypes = results.stream()
                .map(SarifDeserialization.Result::getErrorType)
                .collect(Collectors.toList());
        assertTrue(errorTypes.contains("ConstraintError"));
        assertTrue(errorTypes.contains("RequiredPredicateError"));

        // Check detailed properties of each violation
        // We want to ensure not just that the violations exist, but their details are correct
        SarifDeserialization.Result v0 = results.get(0);
        assertEquals("java.security.KeyPairGenerator", v0.getViolatedRule());
        assertEquals("ConstraintError", v0.getErrorType());
        assertEquals("Constraint violation on KeyPairGenerator.", v0.getMessage().getText());
        assertEquals("ConstraintError violating CrySL rule for java.security.KeyPairGenerator", v0.getMessage().getRichText());
        SarifDeserialization.Location loc0 = v0.getLocations().get(0).get(0);
        assertEquals("org/example/Msg.java", loc0.getPhysicalLocation().getFileLocation().getUri());
        assertEquals("18", loc0.getPhysicalLocation().getRegion().getStartLine());
        assertEquals("r0 = getInstance(varReplacer8)", loc0.getPhysicalLocation().getRegion().getStatement());

        SarifDeserialization.Result v1 = results.get(1);
        assertEquals("javax.crypto.Cipher", v1.getViolatedRule());
        assertEquals("RequiredPredicateError", v1.getErrorType());
        assertEquals("Predicate missing for Cipher.", v1.getMessage().getText());
        assertEquals("RequiredPredicateError violating CrySL rule for javax.crypto.Cipher", v1.getMessage().getRichText());
        SarifDeserialization.Location loc1 = v1.getLocations().get(0).get(0);
        assertEquals("org/example/Msg.java", loc1.getPhysicalLocation().getFileLocation().getUri());
        assertEquals("30", loc1.getPhysicalLocation().getRegion().getStartLine());
        assertEquals("r0 = getInstance(varReplacer10)", loc1.getPhysicalLocation().getRegion().getStatement());
    }


    // Ensure plugin does not crash or throw on empty SARIF files.
    @Test
    void handlesEmptySarifFile() throws Exception {
        File sarifFile = new File("src/test/resources/sarif/empty-sarif.json");
        Gson gson = new Gson();
        SarifDeserialization.ViolationReport report =
                gson.fromJson(new FileReader(sarifFile), SarifDeserialization.ViolationReport.class);
        assertNotNull(report);
        assertTrue(report.getRuns() == null || report.getRuns().isEmpty(), "Runs should be empty or null");
    }



    // Simulates SARIF files from other tools or plugin bugs that leave out fields.
    @Test
    void handlesMissingFieldsGracefully() throws Exception {
        File sarifFile = new File("src/test/resources/sarif/missing-field.json");
        Gson gson = new Gson();
        SarifDeserialization.ViolationReport report =
                gson.fromJson(new FileReader(sarifFile), SarifDeserialization.ViolationReport.class);
        List<SarifDeserialization.Result> results = report.getRuns().get(0).getResults();
        assertEquals(1, results.size());
        SarifDeserialization.Result res = results.get(0);
        assertNull(res.getViolatedRule());
        assertNull(res.getErrorType());
        assertNotNull(res.getLocations());
        assertTrue(res.getLocations().isEmpty());
        assertNotNull(res.getMessage());
        assertNull(res.getMessage().getText());
        assertNull(res.getMessage().getRichText());
    }

    // The code must fail fast if the SARIF is not even valid JSON (prevents silent corruption).
    @Test
    void throwsOnMalformedSarif() {
        File sarifFile = new File("src/test/resources/sarif/malformed-sarif.json");
        Gson gson = new Gson();
        assertThrows(Exception.class, () -> {
            gson.fromJson(new FileReader(sarifFile), SarifDeserialization.ViolationReport.class);
        });
    }

    // Users may have multiple findings for a single source line; plugin must show all, not overwrite or lose any.
    @Test
    void handlesMultipleViolationsOnSameLine() throws Exception {
        File sarifFile = new File("src/test/resources/sarif/same-lines.json");
        Gson gson = new Gson();
        SarifDeserialization.ViolationReport report =
                gson.fromJson(new FileReader(sarifFile), SarifDeserialization.ViolationReport.class);
        List<SarifDeserialization.Result> results = report.getRuns().get(0).getResults();
        assertEquals(2, results.size());
        assertEquals("10", results.get(0).getLocations().get(0).get(0).getPhysicalLocation().getRegion().getStartLine());
        assertEquals("10", results.get(1).getLocations().get(0).get(0).getPhysicalLocation().getRegion().getStartLine());
    }


    // Some SARIF-producing tools (or future versions) might introduce new error types
    // that our plugin doesn't explicitly know about. We want to make sure such cases
    // don't crash the plugin, and the violation is still surfaced (perhaps marked "unknown" or similar).
    @Test
    void handlesUnknownErrorTypeGracefully() throws Exception {
        File sarifFile = new File("src/test/resources/sarif/unknown-error-type.json");
        Gson gson = new Gson();

        SarifDeserialization.ViolationReport report =
                gson.fromJson(new FileReader(sarifFile), SarifDeserialization.ViolationReport.class);

        assertNotNull(report, "Report should not be null");
        assertNotNull(report.getRuns(), "Runs should not be null");
        assertFalse(report.getRuns().isEmpty(), "Runs list should not be empty");

        List<SarifDeserialization.Result> results = report.getRuns().get(0).getResults();
        assertEquals(1, results.size(), "Should have one result even for unknown error type");

        SarifDeserialization.Result violation = results.get(0);
        assertEquals("javax.crypto.SecretKeyFactory", violation.getViolatedRule());
        assertEquals("AlienErrorType", violation.getErrorType()); // Unusual/unknown type

        assertEquals("org/example/Alien.java", violation.getLocations().get(0).get(0).getPhysicalLocation().getFileLocation().getUri());
        assertEquals("99", violation.getLocations().get(0).get(0).getPhysicalLocation().getRegion().getStartLine());
        assertEquals("This is a totally unknown error type.", violation.getMessage().getText());
    }



    
     @Test
    void throwsOnFileNotFound() {
        // If the SARIF file does not exist, plugin must throw FileNotFoundException and not crash unexpectedly.
        File sarifFile = new File("src/test/resources/sarif/nonexistent-file.json");
        assertThrows(FileNotFoundException.class, () -> {
            new FileReader(sarifFile);
        });
    }



}
