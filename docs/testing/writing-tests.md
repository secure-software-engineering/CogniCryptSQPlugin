# Writing Tests

Guidelines for creating effective tests for the SecAI SonarQube Plugin.

## Backend Test Guidelines

### JUnit 5 Test Structure

Follow the AAA (Arrange, Act, Assert) pattern as seen in `IssueReporterTest`:

```java
@Test
void reportsSingleValidIssue() throws IOException, CentralSootUp.NoCentralSootUpInstanceException {
    // Arrange
    NewCCIssue ccIssue = mockValidCCIssue("MyClass", "void myMethod()", "src/MyClass.java",
            "repo", "rule", Collections.emptyList(), Collections.emptyList(), new int[2][2]);
    IssueReporter reporter = spy(new IssueReporter(context));
    
    // Act
    reporter.parseAndReportIssues(Map.of("1", ccIssue), "irrelevantPath");
    
    // Assert
    verify(context, times(1)).newIssue();
    verify(mockIssue, times(1)).save();
}
```

### Test Naming Conventions

Use descriptive test method names as shown in existing tests:
- `reportsSingleValidIssue()` - describes expected behavior
- `skipsNullRuleKey()` - describes edge case handling
- `handlesEmptyIssueListGracefully()` - describes error handling

### Mocking with Mockito

Mock SonarQube dependencies extensively as shown in `IssueReporterTest`:

```java
@BeforeEach
void setUp() {
    context = mock(SensorContext.class);
    fileSystem = mock(FileSystem.class);
    predicates = mock(FilePredicates.class);
    mockIssue = mock(NewIssue.class);
    inputFile = mock(InputFile.class);
    config = mock(Configuration.class);
    
    when(context.fileSystem()).thenReturn(fileSystem);
    when(context.newIssue()).thenReturn(mockIssue);
    when(mockIssue.forRule(any(RuleKey.class))).thenReturn(mockIssue);
}
```

### Configuration Mocking

Mock all SecAI configuration keys:

```java
when(config.get(anyString())).thenAnswer(invocation -> {
    String key = invocation.getArgument(0);
    switch (key) {
        case "sonar.secai.build.system":
            return Optional.of("AUTO");
        case "sonar.secai.ai.model":
            return Optional.of("DefaultModel");
        case "sonar.secai.ai.iterations":
            return Optional.of("1");
        case "sonar.projectKey":
            return Optional.of("test_project");
        case "sonar.secai.cognicrypt.messages":
            return Optional.of("Shortened");
        default:
            return Optional.of("test_value");
    }
});
```

### Test Data Management

Use test resources for complex data as seen in project structure:

```java
@Test
void shouldParseSarifReport() throws IOException {
    String sarifContent = Files.readString(
        Paths.get("src/test/resources/sarif/cognicrypt-testing-sarif-version.json"));
    
    SarifReport report = parser.parse(sarifContent);
    
    assertThat(report.getRuns()).hasSize(1);
}
```

## Frontend Test Guidelines

### React Component Testing

Use React Testing Library as shown in `DetailedDescription.test.js`:

```javascript
import React from 'react';
import { render, screen } from '@testing-library/react';
import DetailedDescription from '../../main/js/sec_ai/components/Description/DetailedDescription';

describe('DetailedDescription', () => {
  test('shows loading state when description is falsy', () => {
    const { rerender } = render(<DetailedDescription description="" />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();

    rerender(<DetailedDescription description={null} />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });
});
```

### Jest Configuration

The project uses custom Jest configuration in `package.json`:

```javascript
"jest": {
  "testEnvironment": "jest-environment-jsdom",
  "setupFiles": ["<rootDir>/conf/jest/SetupTestEnvironment.js"],
  "setupFilesAfterEnv": ["@testing-library/jest-dom/extend-expect"],
  "coverageDirectory": "<rootDir>/target/coverage",
  "coveragePathIgnorePatterns": [
    "<rootDir>/node_modules",
    "<rootDir>/tests"
  ],
  "testPathIgnorePatterns": [
    "<rootDir>/node_modules",
    "<rootDir>/scripts",
    "<rootDir>/conf"
  ]
}
```

## Test Data and Fixtures

### SARIF Test Files

The project includes specific SARIF test files in `src/test/resources/sarif/`:

- `cognicrypt-testing-sarif-version.json` - Valid CogniCrypt output
- `empty-sarif.json` - Empty SARIF structure
- `malformed-sarif.json` - Invalid JSON syntax
- `missing-field.json` - Missing required SARIF fields
- `same-lines.json` - Multiple issues on same line
- `unknown-error-type.json` - Unrecognized error types

### Test Project Structure

The `test_project` contains:

```
src/test/resources/test_project/
├── pom.xml                    # Maven configuration
├── TestProject-1.0.jar       # Pre-built JAR
├── expected-errors.json      # Expected analysis results
└── src/main/java/com/example/
    ├── Main.java             # Entry point
    └── violations/           # Classes with violations
```

## Mock Helper Methods

Create reusable mock helpers as shown in `IssueReporterTest`:

```java
static NewCCIssue mockValidCCIssue(String className, String methodCall, String filePath, 
                                   String repo, String rule, List<String> preceding, 
                                   List<String> subsequent, int[][] location) {
    NewCCIssue ccIssue = mock(NewCCIssue.class);
    when(ccIssue.getClassName()).thenReturn(className);
    when(ccIssue.getMethodCall()).thenReturn(methodCall);
    when(ccIssue.getFilePath()).thenReturn(filePath);
    when(ccIssue.getRuleKey()).thenReturn(RuleKey.of(repo, rule));
    when(ccIssue.getErrorType()).thenReturn(CCErrorType.IMPRECISE_VALUE_EXTRACTION);
    return ccIssue;
}
```

## Coverage Requirements

### Current Coverage Setup

- **Backend**: No coverage measurement (JaCoCo not configured)
- **Frontend**: Automatic coverage via Jest (reports in `target/coverage/`)
- **Integration**: SonarQube analysis combines available coverage data

## Best Practices

### Test Independence

- Each test should be independent as shown in existing tests
- Use `@BeforeEach` for setup as demonstrated in `IssueReporterTest`
- Mock all external dependencies extensively

### Error Handling Tests

Include defensive tests for edge cases:

```java
@Test
void handlesAssembleIssueExceptionGracefully() {
    doThrow(new RuntimeException("Assemble failed!"))
            .when(ccIssue).assembleIssue(any(), any());
    
    assertDoesNotThrow(() -> reporter.parseAndReportIssues(Map.of("1", ccIssue), "path"));
}
```

### Maintainability

- Use descriptive test names that explain the scenario
- Group related tests in the same test class
- Use helper methods for common mock setups
- Follow the existing project patterns for consistency