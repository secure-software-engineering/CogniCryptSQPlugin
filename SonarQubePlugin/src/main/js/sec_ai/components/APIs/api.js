import { getJSON } from "sonar-request";
import { SERVER_IP } from "../../utils/settings";

export const fetchIssues = async (projectKey) => {
    try {
        const response = await fetch(`/api/issues/search?componentKeys=${projectKey}`);
        const data = await response.json();
        const cognicryptIssues = data.issues.filter(_issue => {
            return _issue.rule.startsWith("cognicrypt") && _issue.line
        });
        return cognicryptIssues;
    } catch (err) {
        console.error("Failed to fetch issues:", err);
        return [];
    }
}

export const fetchSourceCode = async (componentKey, line, contextLines) => {
    try {
        const response = await fetch(`/api/sources/raw?key=${encodeURIComponent(componentKey)}`);
        const codeText = await response.text();
        const lines = codeText.split('\n');

        const startLine = Math.max(0, line - contextLines - 1);
        const endLine = Math.min(lines.length - 1, line + contextLines - 1);

        // Get lines before the target line
        const before = lines.slice(startLine, line - 1);

        // Get the target line
        const highlight = lines[line - 1] || '';

        // Get lines after the target line
        const after = lines.slice(line, endLine + 1);
        return { before, highlight, after };
    } catch (err) {
        console.error("Failed to fetch source code:", err);
        return null;
    }
};

export const sendToExternalApi = async (codeSnippet, _rule, _fullpathfromroottobottom, _selectednode, _sourcecodeanalysis, _message, model, itr) => {
    try {
        const response = await fetch(`http://${SERVER_IP}/newfix`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                llm_model: model, //rahega
                iterations: itr, //rahega

                code: codeSnippet, //chudega
                rule: _rule, //chudega
                msg: _message, //chudega

                selectedNode: _selectednode, //Nahi chudega
                fullPathFromRootToBottom: _fullpathfromroottobottom, //Nahi chudega
                sourceCodeAnalysis: _sourcecodeanalysis //Nahi chudega
            }),
        });

        const result = await response.json();
        return result;
    } catch (err) {
        console.error("Error sending to external API:", err);
        return err;
    }
};

export const findProjects = (project) => {
    return getJSON("/api/projects/search").then(function (response) {
        return (response.components);
    });
}


export const getActiveproject = (id, projects) => {
    return projects.find((prj) => prj.key === id);
}

export const getAnalysisReport = async (uri) => {
    try {
        const res = await fetch(uri);
        const sarifData = await res.json();
        return sarifData;
    } catch (err) {
        console.error("Failed to fetch SARIF report.", err);
    }

}

export const getRuleDescriptor = async (queryParam) => {
    try {
        const response = await fetch(`/api/rules/search?rule_key=${encodeURIComponent(queryParam)}`);
        const data = await response.json();
        return data;
    } catch (err) {
        console.error("Failed to fetch rule descriptor:", err);
        return null;
    }
};


export const getPullRequestDetails = async (projectKey) => {
    try {
        const response = await fetch(`/api/settings/values?component=${encodeURIComponent(projectKey)}&available=true`);
        const data = await response.json();
        const secaiSettings = {};
        data.settings.forEach((s) => {
            if (s.key.startsWith('sonar.secai.pullrequest')) {
                secaiSettings[s.key] = s.value;
            }
        });

        return {
            githubUsername: secaiSettings['sonar.secai.pullrequest.githubusername'] || '',
            githubRepoUrl: secaiSettings['sonar.secai.pullrequest.githubrepourl'] || '',
            githubPatToken: secaiSettings['sonar.secai.pullrequest.githubpattoken'] || ''
        };
    } catch (err) {
        console.error("Failed to fetch pull request details:", err);
        return null;
    }
};
