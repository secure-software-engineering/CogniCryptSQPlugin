# Issue Management and Navigation

## Overview

The SecAI plugin provides a comprehensive issue management system that allows users to efficiently navigate, analyze, and manage security vulnerabilities detected by CogniCrypt analysis. The issue management interface integrates seamlessly with SonarQube's existing issue tracking while providing enhanced AI-powered remediation capabilities.

## Issue List Interface

### Accessing Issues

1. **Navigate to Project**: Open your analyzed project in SonarQube
2. **Access SecAI Plugin**: Click on the SecAI tab in the project dashboard
3. **View Vulnerabilities**: Select the "Vulnerabilities" tab in the navigation sidebar

### Issue Display Components

#### Issue Cards
Each security issue is displayed as an interactive card containing:

- **Issue Message**: Primary description of the security vulnerability
- **File Name**: Source file where the issue was detected
- **Visual Indicators**: Color-coded severity and selection states

#### Selection States
- **Default State**: Light gray background (`#f0eeee`)
- **Selected State**: Darker background (`#E5DFDF`) with bold border
- **Hover Effects**: Smooth transitions for better user experience

## Navigation and Interaction

### Selecting Issues

**Mouse Interaction:**
```
Click on any issue card to select it
```

**Keyboard Accessibility:**
```
Use Tab to navigate between issues
Press Enter or Space to select an issue
```

### Issue Information Display

When an issue is selected, the following information becomes available:

- **Detailed Description**: Comprehensive explanation of the vulnerability
- **Source Code Context**: Relevant code snippets with highlighting
- **File Location**: Exact file path and line numbers
- **Severity Level**: Risk assessment and priority classification

## Advanced Features

### File Path Resolution

The system automatically extracts and displays clean file names from SonarQube component paths:

```javascript
// Example: "project:src/main/java/Example.java" → "Example.java"
const fileName = issue.component.split('/').pop()
```

### Integration with AI Features

Selected issues can be processed through:

1. **AI Fix Generation**: Generate automated security fixes
2. **Error Tree Visualization**: View issue relationships and dependencies
3. **Code Generation**: Create secure code alternatives

## Issue Management Workflow

### Step 1: Issue Discovery
1. Run CogniCrypt analysis through SecAI plugin
2. Review generated security issues in the Vulnerabilities tab
3. Use filtering and sorting to prioritize critical issues

### Step 2: Issue Analysis
1. Select an issue from the list
2. Review detailed description and code context
3. Examine CWE references and security implications

### Step 3: Remediation
1. Generate AI-powered fix suggestions
2. Review proposed solutions
3. Apply fixes through IDE integration or GitHub PR creation

### Step 4: Verification
1. Re-run analysis to verify fix effectiveness
2. Update issue status in SonarQube
3. Document remediation actions for audit trails

## Best Practices

### Efficient Issue Navigation

- **Prioritize by Severity**: Focus on critical and high-severity issues first
- **Group by File**: Address multiple issues in the same file together
- **Use Keyboard Shortcuts**: Leverage accessibility features for faster navigation

### Issue Triage Process

1. **Quick Assessment**: Review issue message and file context
2. **Impact Analysis**: Evaluate potential security implications
3. **Effort Estimation**: Assess complexity of required fixes
4. **Priority Assignment**: Rank issues based on risk and effort

### Documentation Standards

- **Issue Tracking**: Maintain detailed records of remediation actions
- **Fix Validation**: Document testing and verification procedures
- **Knowledge Sharing**: Create team guidelines for common vulnerability patterns

## Troubleshooting

### Common Issues

**Issue List Not Loading:**
- Verify SonarQube analysis has completed successfully
- Check that CogniCrypt sensor executed without errors
- Ensure project has Java files for analysis

**Selection Not Working:**
- Clear browser cache and reload the page
- Check JavaScript console for error messages
- Verify Redux store state is properly initialized

**Missing File Information:**
- Confirm source files are accessible to SonarQube
- Check file path resolution in component mapping
- Verify project structure matches analysis configuration

### Performance Optimization

**Large Issue Lists:**
- Implement pagination for projects with many issues
- Use filtering to reduce displayed items
- Consider lazy loading for improved performance

**Memory Management:**
- Clear selected issue state when switching projects
- Optimize Redux store updates for large datasets
- Monitor browser memory usage during extended sessions

## Integration Points

### SonarQube Integration
- **Issue Synchronization**: Automatic sync with SonarQube issue database
- **Quality Gate Integration**: Issues contribute to overall quality metrics
- **Reporting**: Export issue data for external reporting tools

### IDE Integration
- **Direct Navigation**: Open issues directly in configured IDE
- **Context Preservation**: Maintain file and line number context
- **Real-time Updates**: Sync changes between IDE and SonarQube

### CI/CD Pipeline Integration
- **Automated Analysis**: Trigger issue detection in build pipelines
- **Quality Gates**: Block deployments based on critical issues
- **Notification Systems**: Alert teams about new security vulnerabilities

## API Reference

### Issue Data Structure
```javascript
{
  key: "issue-unique-identifier",
  message: "Security vulnerability description",
  component: "project:src/main/java/Example.java",
  line: 42,
  column: 15,
  severity: "CRITICAL",
  rule: "cognicrypt-rule-id"
}
```

### Redux Actions
- `selectIssue(issue)`: Set currently selected issue
- `clearSelection()`: Clear issue selection
- `updateIssueList(issues)`: Refresh issue list data

### Event Handlers
- `handleIssueClick(issue)`: Process issue selection
- `handleKeyboardNavigation(event)`: Handle accessibility interactions

---

*For additional support or feature requests, please refer to the [Contributing Guide](../contributing.md) or create an issue in the project repository.*