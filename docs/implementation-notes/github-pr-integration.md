# GitHub Pull Request Integration

The SecAI SonarQube plugin includes GitHub integration that allows users to automatically create pull requests with code fixes directly from the plugin interface. This feature is available in both the [*AIFix*](../user-guide/aifix.md) and [*Quick Fix*](../user-guide/vulnerabilities-list.md#quick-fixes) components.

For details on the setup, configuration and usage refer to the [user guide](../user-guide/github-pr-integration.md).

---

## Technical Implementation

### Core Components

#### GitHub API Integration
Both components use the Octokit library for GitHub API interactions:

```javascript
import { Octokit } from "@octokit/rest";

const octokit = new Octokit({
    auth: GITHUB_TOKEN,
});
```

#### Pull Request Creation Flow

1. **Branch Creation**: Creates a new branch with a unique name based on the issue
2. **File Update**: Updates the target file with the fixed code
3. **Pull Request**: Creates a PR with descriptive title and body
4. **User Feedback**: Displays success message with PR link

### API Endpoints Used

- `GET /repos/{owner}/{repo}/contents/{path}` - Fetch file content
- `PUT /repos/{owner}/{repo}/contents/{path}` - Update file content
- `POST /repos/{owner}/{repo}/pulls` - Create pull request
