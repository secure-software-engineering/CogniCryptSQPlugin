# Contributing to the SecAI SonarQube Plugin: A Comprehensive Development Guide

## Abstract

This document establishes the comprehensive contribution guidelines for the SecAI (Security-focused Artificial Intelligence) SonarQube plugin development project. The SecAI plugin integrates CogniCrypt static analysis with AI-powered code generation and visualization capabilities, extending SonarQube's security analysis capabilities through advanced cryptographic API misuse detection and intelligent remediation suggestions.

## 1. Project Overview

### 1.1 Architecture

The SecAI plugin is a hybrid Java-React application that extends SonarQube Server 10.x with the following core components:

- **Backend (Java 17)**: SonarQube plugin implementation with CogniCrypt integration
- **Frontend (React 17)**: Interactive visualization and AI-powered code generation interface
- **Analysis Engine**: CogniCrypt-based cryptographic vulnerability detection
- **AI Integration**: LangChain4j-powered code generation with OpenAI and Google Gemini support
- **Build System**: Maven with frontend integration via frontend-maven-plugin

### 1.2 Key Technologies

- **Core Framework**: SonarQube Plugin API 10.11.0.2468
- **Static Analysis**: CogniCrypt HeadlessJavaScanner 5.0.1, SootUp 2.0.0
- **AI/ML**: LangChain4j 1.0.0, OpenAI API, Google Gemini API
- **Frontend**: React 17, Redux Toolkit, ReactFlow, D3.js
- **Build Tools**: Maven 3.8+, Yarn 4.5.0, Webpack 5
- **Testing**: JUnit 5, Mockito, Jest, React Testing Library

## 2. Development Environment Setup

### 2.1 Prerequisites

- **Java Development Kit**: OpenJDK 17 or Oracle JDK 17
- **Build System**: Apache Maven 3.8.0 or higher
- **Node.js**: Version 18+ (for frontend development)
- **Docker**: For SonarQube deployment and testing
- **IDE**: IntelliJ IDEA (recommended) or Eclipse with SonarLint plugin

### 2.2 Repository Setup

```bash
git clone https://github.com/secure-software-engineering/SonarQubePlugin.git
cd SonarQubePlugin
```

### 2.3 Build Configuration

#### 2.3.1 Full Build (Backend + Frontend)
```bash
mvn clean package
```

#### 2.3.2 Backend Only
```bash
mvn clean package -DskipTests
```

#### 2.3.3 Frontend Development
```bash
yarn install
yarn start  # Development server
yarn build  # Production build
yarn test   # Run tests
```

### 2.4 SonarQube Development Environment

#### 2.4.1 Docker Deployment
```bash
docker-compose -f dockerfiles/docker-compose.yml up -d
```

#### 2.4.2 Plugin Deployment
```bash
chmod +x deploy-sonarqube-plugin.sh
./deploy-sonarqube-plugin.sh
```

## 3. Project Structure and Component Guidelines

### 3.1 Backend Architecture

```
src/main/java/org/sonarsource/plugins/secai/
├── analysis/          # Core analysis components
│   ├── cognicrypt/    # CogniCrypt integration
│   └── Analyzer.java  # Main analysis orchestrator
├── api/               # Web service endpoints
├── code_generation/   # AI-powered code generation
├── languages/         # Quality profiles
├── reporting/         # Issue reporting and SARIF processing
├── sensor/            # SonarQube sensor implementation
├── settings/          # Plugin configuration
├── utils/             # Utility classes and helpers
└── web/               # Web extension points
```

### 3.2 Frontend Architecture

```
src/main/js/sec_ai/
├── components/
│   ├── analysisComponent/  # Analysis result display
│   ├── APIs/              # Backend communication
│   ├── codeGeneration/    # AI code generation UI
│   ├── Description/       # Issue descriptions and fixes
│   ├── errorTree/         # Interactive error visualization
│   └── Navigations/       # Navigation components
├── store/                 # Redux state management
└── styles/               # CSS styling
```

### 3.3 Component-Specific Contribution Guidelines

#### 3.3.1 Analysis Components (`analysis/`)
- Implement `AnalysisTool` interface for new analysis engines
- Follow CogniCrypt integration patterns for cryptographic analysis
- Ensure SARIF compliance for result reporting
- Add comprehensive unit tests with mock CogniCrypt scenarios

#### 3.3.2 Code Generation (`code_generation/`)
- Use LangChain4j abstractions for LLM integration
- Implement proper API key management via environment variables
- Add rate limiting and error handling for external API calls
- Follow prompt engineering best practices

#### 3.3.3 Frontend Components (`js/sec_ai/`)
- Use React functional components with hooks
- Implement Redux for state management
- Follow accessibility guidelines (WCAG 2.1)
- Add comprehensive Jest/React Testing Library tests

## 4. Coding Standards and Best Practices

### 4.1 Java Backend Standards

#### 4.1.1 Code Style
- Follow Google Java Style Guide
- Use meaningful variable and method names
- Maximum line length: 120 characters
- Use proper JavaDoc for public APIs

#### 4.1.2 SonarQube Plugin Patterns
```java
// Sensor implementation example
@Override
public void execute(SensorContext context) {
    // Validate prerequisites
    if (!context.fileSystem().hasFiles(context.fileSystem().predicates().hasLanguage("java"))) {
        return;
    }
    
    // Execute analysis
    analyzeProject(context);
}
```

#### 4.1.3 Error Handling
- Use specific exception types from `utils/exceptions/`
- Log errors appropriately using SLF4J
- Provide meaningful error messages for users

### 4.2 React Frontend Standards

#### 4.2.1 Component Structure
```javascript
// Functional component with hooks
const ComponentName = ({ prop1, prop2 }) => {
    const [state, setState] = useState(initialState);
    const dispatch = useDispatch();
    
    useEffect(() => {
        // Side effects
    }, [dependencies]);
    
    return (
        <div className="component-name">
            {/* JSX content */}
        </div>
    );
};
```

#### 4.2.2 State Management
- Use Redux Toolkit for complex state
- Implement proper action creators and reducers
- Use selectors for derived state

## 5. Testing Strategy

### 5.1 Backend Testing

#### 5.1.1 Unit Tests (JUnit 5)
```java
@ExtendWith(MockitoExtension.class)
class CogniCryptSensorTest {
    @Mock
    private SensorContext context;
    
    @Test
    void shouldExecuteAnalysisWhenJavaFilesPresent() {
        // Test implementation
    }
}
```

#### 5.1.2 Integration Tests
- Use SonarQube testing harness
- Test with real project samples in `src/test/resources/test_project/`
- Validate SARIF output format

### 5.2 Frontend Testing

#### 5.2.1 Component Tests (Jest + React Testing Library)
```javascript
test('renders error tree visualization', () => {
    render(<ErrorVis data={mockData} />);
    expect(screen.getByRole('button', { name: /expand/i })).toBeInTheDocument();
});
```

#### 5.2.2 Integration Tests
- Test Redux store interactions
- Mock API calls using MSW or similar
- Test user workflows end-to-end

### 5.3 Test Coverage Requirements

- **Backend**: Minimum 80% line coverage, 90% for critical security components
- **Frontend**: Minimum 75% line coverage for components
- **Integration**: All major user workflows must be covered

## 6. Continuous Integration and Deployment

### 6.1 GitHub Actions Workflow

The project uses a comprehensive CI/CD pipeline (`.github/workflows/ci-cd.yml`) that includes:

- **Setup**: JDK 17, Node.js 18, Maven caching
- **Backend Build**: Maven compilation and testing
- **Frontend Build**: Yarn installation and build
- **Security Scanning**: OWASP Dependency Check
- **SonarQube Analysis**: Code quality assessment
- **Plugin Packaging**: JAR artifact generation

### 6.2 Quality Gates

All pull requests must pass:
- Maven build without errors
- All unit and integration tests
- SonarQube quality gate (zero critical issues)
- OWASP dependency check (no high-severity vulnerabilities)
- Code review by at least one maintainer

## 7. Commit Message and Branching Strategy

### 7.1 Conventional Commits

Use the following format for commit messages:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

#### 7.1.1 Types
- `feat`: New features (analysis, UI, AI integration)
- `fix`: Bug fixes
- `docs`: Documentation updates
- `style`: Code formatting
- `refactor`: Code restructuring
- `test`: Test additions/modifications
- `ci`: CI/CD changes
- `perf`: Performance improvements

#### 7.1.2 Scopes
- `cognicrypt`: CogniCrypt analysis integration
- `ai`: AI/LLM code generation
- `ui`: React frontend components
- `api`: Backend web services
- `sensor`: SonarQube sensor implementation
- `build`: Build system changes

#### 7.1.3 Examples
```
feat(cognicrypt): add support for custom CrySL rules

Implement dynamic CrySL rule loading from user-defined
rule files, enabling custom cryptographic API analysis.

Closes #45
```

### 7.2 Branching Strategy

- **main**: Production-ready releases
- **develop**: Integration branch for ongoing development
- **feature/***: New feature development
- **fix/***: Bug fixes
- **hotfix/***: Critical production fixes

## 8. Documentation Requirements

### 8.1 Code Documentation

- **JavaDoc**: All public APIs must have comprehensive JavaDoc
- **JSDoc**: React components should include prop documentation
- **README Updates**: Update relevant README sections for new features

### 8.2 MkDocs Documentation

The project uses MkDocs for comprehensive documentation:

```bash
# Install dependencies
pip install -r requirements.txt

# Serve documentation locally
mkdocs serve

# Build documentation
mkdocs build
```

### 8.3 API Documentation

Document all web service endpoints in `api/` with:
- Request/response formats
- Authentication requirements
- Error handling
- Usage examples

## 9. Security and AI Ethics Guidelines

### 9.1 API Key Management

- Store API keys in environment variables
- Use `.env` files for local development (never commit)
- Implement proper key rotation strategies
- Add rate limiting for external API calls

### 9.2 AI Code Generation Ethics

- Validate generated code for security vulnerabilities
- Provide clear attribution for AI-generated content
- Implement user consent mechanisms
- Ensure compliance with AI usage policies

### 9.3 Data Privacy

- Minimize data collection and retention
- Implement proper data anonymization
- Follow GDPR compliance guidelines
- Document data processing activities

## 10. Release Process

### 10.1 Version Management

- Follow Semantic Versioning (SemVer)
- Update version in `pom.xml` and `package.json`
- Create release tags with detailed changelogs

### 10.2 Plugin Distribution

- Build final JAR: `mvn clean package`
- Test with multiple SonarQube versions
- Update plugin marketplace metadata
- Provide migration guides for breaking changes

## References

[1] SonarSource. (2024). "SonarQube Plugin Development Guide." Retrieved from https://docs.sonarqube.org/latest/extend/developing-plugin/

[2] Krüger, S., et al. (2018). "CogniCrypt: Supporting developers in using cryptography." In *Proceedings of the 33rd ACM/IEEE International Conference on Automated Software Engineering*.

[3] LangChain4j Contributors. (2024). "LangChain4j Documentation." Retrieved from https://docs.langchain4j.dev/

[4] Facebook Inc. (2024). "React Documentation." Retrieved from https://react.dev/

[5] Redux Toolkit Team. (2024). "Redux Toolkit Documentation." Retrieved from https://redux-toolkit.js.org/

[6] OWASP Foundation. (2024). "OWASP Dependency Check." Retrieved from https://owasp.org/www-project-dependency-check/

[7] Conventional Commits Contributors. (2024). "Conventional Commits 1.0.0." Retrieved from https://www.conventionalcommits.org/

[8] Google. (2024). "Google Java Style Guide." Retrieved from https://google.github.io/styleguide/javaguide.html

[9] W3C. (2024). "Web Content Accessibility Guidelines (WCAG) 2.1." Retrieved from https://www.w3.org/WAI/WCAG21/quickref/

---

*This document is maintained as part of the SecAI research project at the Secure Software Engineering Department, Paderborn University, and is subject to continuous improvement based on empirical findings and community feedback.*
