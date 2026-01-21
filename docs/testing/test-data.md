# Test Data Management

Guidelines for managing test data, fixtures, and resources in the SecAI SonarQube Plugin.

## Test Data Structure

### SARIF Test Files

Located in `src/test/resources/sarif/`, these files simulate CogniCrypt analysis output:

#### Valid SARIF Files
- `cognicrypt-testing-sarif-version.json` - Standard CogniCrypt output
- `same-lines.json` - Multiple issues on same line

#### Invalid SARIF Files
- `empty-sarif.json` - Empty SARIF structure
- `malformed-sarif.json` - Invalid JSON syntax
- `missing-field.json` - Missing required SARIF fields
- `unknown-error-type.json` - Unrecognized error types

### Test Project

The `src/test/resources/test_project/` contains a complete Java project for integration testing:

```
test_project/
├── pom.xml                           # Maven configuration
├── TestProject-1.0.jar              # Pre-built JAR for testing
├── expected-errors.json             # Expected analysis results
└── src/main/java/com/example/
    ├── Main.java                    # Entry point
    └── violations/                  # Classes with crypto violations
        ├── ConstraintViolations.java
        ├── OrderViolations.java
        ├── OtherClass.java
        ├── OtherViolations.java
        └── RequiredPredicateViolations.java
```

## SARIF Format Specification

### Standard SARIF Structure

Following OASIS SARIF v2.1.0 specification:

```json
{
  "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
  "version": "2.1.0",
  "runs": [{
    "tool": {
      "driver": {
        "name": "CogniCrypt",
        "version": "3.0.0",
        "informationUri": "https://www.eclipse.org/cognicrypt/"
      }
    },
    "results": [{
      "ruleId": "RequiredPredicateError",
      "level": "error",
      "message": {
        "text": "First parameter was not properly generated as randomized."
      },
      "locations": [{
        "physicalLocation": {
          "artifactLocation": {
            "uri": "src/main/java/com/example/violations/RequiredPredicateViolations.java"
          },
          "region": {
            "startLine": 15,
            "startColumn": 1,
            "endLine": 15,
            "endColumn": 50
          }
        }
      }]
    }]
  }]
}
```

### CogniCrypt-Specific Fields

CogniCrypt extends SARIF with custom properties:

```json
{
  "ruleId": "ConstraintError",
  "properties": {
    "cognicrypt": {
      "errorType": "ConstraintError",
      "ruleName": "SecureRandom",
      "className": "javax.crypto.SecureRandom",
      "methodName": "nextBytes"
    }
  }
}
```

## Creating Test Data

### Adding New SARIF Files

1. Create SARIF file in `src/test/resources/sarif/`
2. Validate against SARIF schema
3. Add corresponding test case

Example test:
```java
@Test
void shouldParseCustomSarifFile() throws IOException {
    String sarifPath = "src/test/resources/sarif/custom-test.json";
    SarifReport report = SarifParser.parse(sarifPath);
    
    assertThat(report.getRuns()).hasSize(1);
    assertThat(report.getRuns().get(0).getResults()).hasSize(2);
}
```

### Test Project Modifications

When modifying the test project:

1. Update Java source files in `test_project/src/`
2. Rebuild JAR: `mvn clean package` in test_project directory
3. Update `expected-errors.json` with new expected results
4. Run integration tests to verify changes

### Expected Results Format

The `expected-errors.json` file defines expected analysis outcomes:

```json
{
  "expectedIssues": [
    {
      "file": "src/main/java/com/example/violations/RequiredPredicateViolations.java",
      "line": 15,
      "ruleId": "RequiredPredicateError",
      "message": "First parameter was not properly generated as randomized."
    }
  ],
  "expectedMetrics": {
    "totalIssues": 5,
    "errorCount": 3,
    "warningCount": 2
  }
}
```

## Test Data Validation

### SARIF Schema Validation

Validate SARIF files against official schema:

```bash
# Using ajv-cli
npm install -g ajv-cli
ajv validate -s sarif-2.1.0.json -d test-sarif-file.json
```

### Automated Validation

Include validation in test suite:

```java
@Test
void allSarifFilesShouldBeValid() throws IOException {
    Path sarifDir = Paths.get("src/test/resources/sarif");
    
    Files.walk(sarifDir)
        .filter(path -> path.toString().endsWith(".json"))
        .forEach(this::validateSarifFile);
}

private void validateSarifFile(Path sarifFile) {
    // Validation logic using JSON schema
    assertTrue(SarifValidator.isValid(sarifFile), 
        "Invalid SARIF file: " + sarifFile);
}
```

## Data Generation Tools

### SARIF Generator Utility

Create utility for generating test SARIF files:

```java
public class SarifTestDataGenerator {
    public static SarifReport createBasicReport(String toolName, 
                                               List<TestIssue> issues) {
        SarifReport report = new SarifReport();
        report.setVersion("2.1.0");
        
        Run run = new Run();
        run.setTool(createTool(toolName));
        run.setResults(convertIssues(issues));
        
        report.setRuns(List.of(run));
        return report;
    }
}
```

### Mock Data Builders

Use builder pattern for complex test data:

```java
public class IssueBuilder {
    private String ruleId = "DefaultRule";
    private String message = "Default message";
    private int line = 1;
    
    public IssueBuilder withRuleId(String ruleId) {
        this.ruleId = ruleId;
        return this;
    }
    
    public IssueBuilder withMessage(String message) {
        this.message = message;
        return this;
    }
    
    public Issue build() {
        return new Issue(ruleId, message, line);
    }
}
```

## Best Practices

### Data Maintenance

- Keep test data minimal but representative
- Version control all test data files
- Document the purpose of each test data file
- Regularly review and update test data

### Performance Considerations

- Use small test files for unit tests
- Cache parsed test data when possible
- Avoid loading large files in every test

### Security

- Never include real credentials or sensitive data
- Use placeholder values for sensitive fields
- Sanitize any real-world data before adding to tests