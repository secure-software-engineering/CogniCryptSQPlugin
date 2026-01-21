# Testing Overview

The SecAI SonarQube Plugin employs a comprehensive testing strategy to ensure code quality, security, and reliability across both backend and frontend components.

## Testing Strategy

Our testing approach follows industry best practices for plugin development and continuous integration.

### Test Types

#### 1. Unit Tests
- **Backend**: JUnit 5 tests for Java components
- **Frontend**: Jest tests for React components
- **Coverage Target**: Minimum 80% code coverage

#### 2. Integration Tests
- SonarQube plugin integration testing
- API endpoint testing
- Database interaction testing

#### 3. Security Testing
- OWASP Dependency Check for vulnerability scanning
- Static Application Security Testing (SAST) via SonarQube analysis

## Test Structure

```
src/test/
├── java/                    # Backend tests
│   └── org/sonarsource/plugins/secai/
│       ├── reporting/       # Issue reporting tests
│       └── utils/          # Utility function tests
├── js/                     # Frontend tests
│   ├── DetailedDescription.test.js
│   ├── DetailedFix.test.js
│   ├── IssueList.test.js
│   └── NavigationBar.test.js
└── resources/              # Test data
    ├── sarif/             # SARIF test files
    └── test_project/      # Sample Java project
```

## Testing Frameworks

### Backend Testing
- **JUnit 5**: Primary testing framework
- **Mockito**: Mocking framework for isolated unit tests
- **SonarQube Testing Harness**: Plugin-specific testing utilities

### Frontend Testing
- **Jest**: JavaScript testing framework
- **React Testing Library**: Component testing utilities
- **JSDOM**: DOM simulation for headless testing

## Continuous Integration

Our CI/CD pipeline automatically runs all tests on every push and pull request:

1. **Backend Tests**: Maven Surefire plugin execution
2. **Frontend Tests**: Jest with coverage reporting
3. **Security Scans**: OWASP dependency vulnerability checks
4. **Code Quality**: SonarQube analysis with coverage integration