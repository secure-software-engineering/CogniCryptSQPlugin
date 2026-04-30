import React, { useEffect, useState } from 'react'
import { getPullRequestDetails, sendToExternalApi } from '../APIs/api';
import Skeleton from 'react-loading-skeleton';
import 'react-loading-skeleton/dist/skeleton.css';
import { selectAiModel, selectAiSolution, selectIteration, selectProjectKey, selectSelectedIssue, setAiModel, setAiSolution, setIteration, setSourceCodeResults } from '../../store/issuesReducer';
import { useDispatch, useSelector } from 'react-redux';
import { Octokit } from "@octokit/rest";
import { setGithubUsername, setGithubRepourl, setGithubPATtoken, selectGithubRepourl, selectGithubPATtoken, selectGithubUsername } from '../../store/pullRequestReducer';
import { MODEL_OPTIONS } from '../../utils/modelOptions';
import { VerificationBadge } from '../../utils/verification';
import filterCpgField from '../../utils/filterFields';
import {fetchMetricIssues} from "../../utils/issuesService";

// Build full error path including preceding and subsequent errors
async function buildFullErrorPath(selectedNode, projectKey) {
    try {
        // Fetch the error tree data
        let fetchedIssues = await fetchMetricIssues(projectKey);
        const flatList = fetchedIssues.metricIssues;
        
        // Build graph map
        const graphMap = {};
        flatList.forEach(n => {
            graphMap[n.hashcode] = n.subsequentErrors || [];
        });
        
        // Collect path to root
        const pathToRoot = collectPathToRoot(selectedNode.hashcode, graphMap, flatList);
        
        // Collect subtree
        const subtree = collectSubtree(selectedNode.hashcode, graphMap, flatList);
        
        // Build full path from root to bottom
        const fullPath = [];
        
        // Add path from root to current node
        pathToRoot.forEach(nodeId => {
            const fullNode = flatList.find(n => n.hashcode === nodeId);
            if (fullNode) fullPath.push(fullNode);
        });
        
        // Add subtree nodes (from current to bottom)
        subtree.forEach(nodeId => {
            if (!pathToRoot.includes(nodeId)) {
                const fullNode = flatList.find(n => n.hashcode === nodeId);
                if (fullNode) fullPath.push(fullNode);
            }
        });
        
        return fullPath;
    } catch (error) {
        console.error('Failed to build full error path:', error);
        return [selectedNode];
    }
}

// Collect path to root for AiFix
function collectPathToRoot(startId, graphMap, flatList) {
    const visited = new Set();
    const resultNodes = [];
    
    const reverseGraph = {};
    for (const [parent, children] of Object.entries(graphMap)) {
        for (const child of children) {
            if (!reverseGraph[child]) reverseGraph[child] = [];
            reverseGraph[child].push(parent);
        }
    }
    
    function dfs(nodeId) {
        if (visited.has(nodeId)) return;
        visited.add(nodeId);
        resultNodes.push(nodeId);
        const parents = reverseGraph[nodeId] || [];
        for (const parent of parents) {
            dfs(parent);
        }
    }
    
    dfs(startId);
    return resultNodes.reverse(); // Return from root to current
}

// Collect subtree for AiFix
function collectSubtree(startId, graphMap, flatList) {
    const visited = new Set();
    const resultNodes = [];
    
    function dfs(nodeId) {
        if (visited.has(nodeId)) return;
        visited.add(nodeId);
        resultNodes.push(nodeId);
        const children = graphMap[nodeId] || [];
        for (const child of children) {
            dfs(child);
        }
    }
    
    dfs(startId);
    return resultNodes;
}

const CopyIcon = ({ size = 16 }) => (
    <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
    </svg>
);

const CheckIcon = ({ size = 16 }) => (
    <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="20 6 9 17 4 12"></polyline>
    </svg>
);

export default function AiFix({ sourceSnippet, customIssue = null, _oldRule = null }) {


    const dispatch = useDispatch();

    // Redux selectors
    const selectedIssue = useSelector(selectSelectedIssue);
    const projectKey = useSelector(selectProjectKey);

    const githubUsername = useSelector(selectGithubUsername);
    const githubRepoUrl = useSelector(selectGithubRepourl);
    const githubPATtoken = useSelector(selectGithubPATtoken);

    const aiFix = useSelector(selectAiSolution);
    const selectedModel = useSelector(selectAiModel);
    const selectedIteration = useSelector(selectIteration);


    // state variables
    const issue = customIssue || selectedIssue._raw;

    const [apiLoading, setApiLoading] = useState(false);
    const [prSettingsValid, setPrSettingsValid] = useState(true);
    const [prValidationMessage, setPrValidationMessage] = useState('');

    const [prLoading, setPrLoading] = useState(false);
    const [prSuccessMessage, setPrSuccessMessage] = useState('');
    const [isCopied, setIsCopied] = useState(false);


    const handleCopy = () => {
        if (navigator.clipboard && aiFix?.Final_Secure_Code_Snippet) {
            navigator.clipboard.writeText(aiFix.Final_Secure_Code_Snippet).then(() => {
                setIsCopied(true);
                setTimeout(() => setIsCopied(false), 2000);
            });
        }
    };

    const handleCreatePr = async () => {
        if (!aiFix || !issue || !issue.component) {
            alert("Missing issue or fix. Generate the fix first.");
            return;
        }

        // Parse repoOwner and repoName from URL like: https://github.com/YashikKhunt/exampletest.git
        const repoUrlParts = githubRepoUrl.replace('.git', '').split('/');
        const repoOwner = repoUrlParts[repoUrlParts.length - 2];
        const repoName = repoUrlParts[repoUrlParts.length - 1];
        const branchName = `ai-fix/${issue.key}`;
        const baseBranch = "master"; // or "main"

        const filePath = issue.component.split(":").pop();

        try {
            const octokit = new Octokit({ auth: githubPATtoken });

            // 1. Get latest SHA of base branch
            const refRes = await octokit.rest.git.getRef({
                owner: repoOwner,
                repo: repoName,
                ref: `heads/${baseBranch}`
            });

            const baseSha = refRes.data.object.sha;

            // 2. Create new branch from base
            await octokit.rest.git.createRef({
                owner: repoOwner,
                repo: repoName,
                ref: `refs/heads/${branchName}`,
                sha: baseSha
            });

            // 3. Get original file content
            const fileRes = await octokit.rest.repos.getContent({
                owner: repoOwner,
                repo: repoName,
                path: filePath,
                ref: baseBranch
            });

            const originalContent = atob(fileRes.data.content);
            const lines = originalContent.split('\n');
            const fixedLines = (aiFix.Final_Secure_Code_Snippet || '').split('\n');

            if (!aiFix.Final_Secure_Code_Snippet) {
                alert("No fix snippet available to apply.");
                return;
            }

            // const issueLineIndex = (issue.line || 1) - 1;

            // const modifiedLines = [
            //     ...lines.slice(0, issueLineIndex), // Keep everything before the issue line
            //     ...fixedLines, // Replace the issue line with AI fix (multiple lines)
            //     ...lines.slice(issueLineIndex + 1) // Keep everything after the issue line
            // ];

            // const modifiedContent = modifiedLines.join('\n');

            // 4. Update file in new branch
            await octokit.rest.repos.createOrUpdateFileContents({
                owner: repoOwner,
                repo: repoName,
                path: filePath,
                message: `fix: secure issue in ${filePath} via AI`,
                content: btoa(aiFix.Final_Secure_Code_Snippet),
                branch: branchName,
                sha: fileRes.data.sha
            });

            // 5. Create Pull Request
            const prRes = await octokit.rest.pulls.create({
                owner: repoOwner,
                repo: repoName,
                title: `AI Fix: ${aiFix.Vulnerability_name}`,
                head: branchName,
                base: baseBranch,
                body: `### AI-Generated Fix\n\n${aiFix.Explanation}\n\n_This fix was generated by the SecAI plugin._`
            });

            setPrSuccessMessage(`✅ Pull Request created: ${prRes.data.html_url}`);
            window.open(prRes.data.html_url, "_blank");

        } catch (err) {
            console.error("PR creation failed:", err);
            alert("❌ PR creation failed. See console for details.");
        }
    };

    const handleAiFixClick = async () => {
        let fullPath = null;
        let sourceCodeResults = null;
        let cleanSelectedNodes = null;

        if (!issue) {
            alert("Please select an issue before requesting an AI fix.");
            return;
        }
        
        // Extract source code analysis before AI fix
        if (issue) {
            try {
                fullPath = await buildFullErrorPath(issue, projectKey);
                const { extractSourceCodeFromPath } = await import('../errorTree/helperCalls');
                sourceCodeResults = await extractSourceCodeFromPath(fullPath, projectKey);
                dispatch(setSourceCodeResults(sourceCodeResults));

                cleanSelectedNodes = filterCpgField(issue);
                fullPath = filterCpgField(fullPath);

            } catch (error) {
                console.error('ErrorTree Vis - Failed to extract source code:', error);
            }
        }
        
        dispatch(setAiSolution(null));
        setApiLoading(true);

        const codeSnippet = issue?.codeSnippet || sourceSnippet;
        if (codeSnippet && fullPath && sourceCodeResults && issue) {
            try {
                const _res = await sendToExternalApi(
                    codeSnippet,
                    _oldRule.toString().toLowerCase(),

                    fullPath,
                    cleanSelectedNodes,
                    sourceCodeResults,

                    issue.message,
                    selectedModel.toString().toLowerCase(),
                    selectedIteration
                );

                if (_res) {
                    dispatch(setAiSolution(_res));
                }
            } catch (error) {
                console.error("AI Fix request failed", error);
            } finally {
                setApiLoading(false);
            }
        } else {
            alert("No source code snippet found for this issue.");
            setApiLoading(false);
        }
    };


    const handleRequest = (issue) => {
        // Just get the relative path after the last colon (SonarQube convention)
        let filePath = issue.component || issue.reportLocation?.filePath;
        const parts = filePath.split(':');
        if (parts.length > 1) {
            filePath = parts[parts.length - 1];
        }

        // This still takes path of Sonarqube Project not the local one!.
        filePath = filePath.replace(/^file:/, '').replace(/^\/+/, ''); // Only strip file: and leading slashes if any

        // Now, filePath is relative, e.g., "src/main/java/org/example/Main.java"
        fetch('http://localhost:8081/open-file', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                fileUri: filePath,          // relative path!
                line: issue.reportLocation?.start[0] || issue.line || 1,
                column: issue.reportLocation?.start[1] || issue.column || 1
            })
        });

    }

    useEffect(() => {
        const fetchAllData = async () => {
            try {
                if (!projectKey) return;

                const prSettings = await getPullRequestDetails(projectKey);

                const username = prSettings.githubUsername || 'none';
                const repoUrl = prSettings.githubRepoUrl || 'none';
                const patToken = prSettings.githubPatToken || 'none';

                const isUnset = (val) => !val || val.toLowerCase() === 'none';

                if (isUnset(username) || isUnset(repoUrl) || isUnset(patToken)) {
                    setPrSettingsValid(false);
                    setPrValidationMessage("⚠️ Please configure GitHub Username, Repo URL, and PAT Token in the SonarQube project settings.");
                    return;
                }
                
                // If all values are valid
                dispatch(setGithubUsername(username));
                dispatch(setGithubRepourl(repoUrl));
                dispatch(setGithubPATtoken(patToken));
                setPrSettingsValid(true);
                setPrValidationMessage('');
            } catch (error) {
                console.error("Error fetching pull request settings:", error);
                setPrSettingsValid(false);
                setPrValidationMessage("❌ Failed to fetch GitHub PR settings. Please check your SonarQube server or plugin.");
            }
        };

        fetchAllData();
    }, [projectKey, dispatch]);


    return (
        <div style={styles.container}>

            <div style={styles.fixArea}>
                <h3>Code Fixes, Powered by AI</h3>
                <div style={styles.dropdownGroup}>
                    {/* Model Selection */}
                    <div style={styles.dropdownContainer}>
                        <label style={styles.dropdownLabel}>Select LLM Model</label>
                        <select
                            value={selectedModel}
                            onChange={(e) => dispatch(setAiModel(e.target.value))}
                            style={styles.selectBox}
                        >
                            {MODEL_OPTIONS.map((model) => (
                                <option key={model.value} value={model.value}>
                                    {model.label}
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* Iteration Count */}
                    <div style={styles.dropdownContainer}>
                        <label style={styles.dropdownLabel}>Iteration Count</label>
                        <select
                            value={selectedIteration}
                            onChange={(e) => dispatch(setIteration(Number(e.target.value)))}
                            style={styles.selectBox}
                        >
                            {[1, 2, 3, 4, 5].map(i => (
                                <option key={i} value={i}>{i}</option>
                            ))}
                        </select>
                    </div>
                </div>

                <button onClick={handleAiFixClick} style={styles.fixButton}>
                    Generate the fix <span style={{ marginLeft: '6px' }}>🤖</span>
                </button>
            </div>
            {apiLoading && (
                <div style={styles.apiCard}>
                    <Skeleton height={25} width={200} style={{ marginBottom: '10px' }} />
                    <Skeleton count={2} />
                    <Skeleton height={80} style={{ marginTop: '10px', borderRadius: '6px' }} />
                    <Skeleton count={3} width={`60%`} style={{ marginTop: '10px' }} />
                </div>
            )}
            {aiFix && (
                <div style={styles.apiCard}>
                    {aiFix.error ? (
                        <div style={{ color: 'red', fontWeight: 'bold' }}>
                            Error from AI service: {aiFix.error}
                        </div>
                    ) : (
                        <>
                            <div style={styles.apiTitle}>{aiFix.Vulnerability_name}</div>
                            <p><strong>Explanation:</strong> {aiFix.Explanation}</p>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
                                <p style={{ margin: 0 }}><strong>Fix:</strong></p>
                                {'CogniCrypt_Verified' in aiFix && (
                                    <VerificationBadge verified={Boolean(aiFix.CogniCrypt_Verified)} />
                                )}
                            </div>
                            <div style={{ position: 'relative' }}>
                                <pre style={styles.codeBlock}>{aiFix.Final_Secure_Code_Snippet}</pre>
                                <button onClick={handleCopy} style={styles.copyButton} title="Copy to clipboard">
                                    {isCopied ? <CheckIcon /> : <CopyIcon />}
                                </button>
                            </div>
                            {aiFix.CWE_references && (
                                <div>
                                    <p><strong>CWE References:</strong></p>
                                    <ul>
                                        {aiFix.CWE_references.map((cwe, idx) => (
                                            <li key={idx}>
                                                <a href={cwe.link} target="_blank" rel="noopener noreferrer">{cwe.cwe}</a>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </>
                    )} {/* End of !aiFix.error check */}
                </div>
            )}

            {aiFix && issue && (issue.component || issue.reportLocation?.filePath) && issue.line && (
                <button
                    style={{ marginTop: '10px', background: '#ccc', color: 'white', border: 'none', borderRadius: '6px', padding: '8px 14px', fontWeight: 'bold', cursor: 'not-allowed' }}
                    onClick={() =>
                        handleRequest(issue)
                    }
                    disabled={true}
                >
                    Open in IDE
                </button>
            )}

            {aiFix && issue && (issue.component || issue.reportLocation?.filePath) && issue.line && (
                <>
                    <button
                        style={{
                            marginTop: '10px',
                            background: prSettingsValid ? '#6f42c1' : '#ccc',
                            color: 'white',
                            border: 'none',
                            borderRadius: '6px',
                            padding: '8px 14px',
                            fontWeight: 'bold',
                            cursor: prSettingsValid && !prLoading ? 'pointer' : 'not-allowed',
                            position: 'relative',
                            overflow: 'hidden',
                            width: '100%',
                            opacity: prLoading ? 0.7 : 1,
                        }}
                        onClick={handleCreatePr}
                        disabled={!prSettingsValid || prLoading}
                    >

                        {prLoading && (
                            <span
                                style={{
                                    position: 'absolute',
                                    left: 0,
                                    top: 0,
                                    height: '100%',
                                    width: '100%',
                                    background: 'rgba(255, 255, 255, 0.2)',
                                    animation: 'fillProgress 2s linear forwards',
                                    zIndex: 0
                                }}
                            ></span>
                        )}
                        <span style={{ position: 'relative', zIndex: 1 }}>
                            {prLoading ? 'Creating PR...' : 'Create GitHub PR'}
                        </span>
                    </button>

                    {(prSuccessMessage || !prSettingsValid) && (
                        <div style={{ marginTop: '8px', fontWeight: 'bold', color: prSuccessMessage ? 'green' : 'red' }}>
                            {prSuccessMessage || prValidationMessage}
                        </div>
                    )}
                </>

            )}

        </div>
    )
}

const styles = {
    container: { display: 'flex', fontFamily: 'Arial, sans-serif', flex: '1', flexDirection: 'column' },
    sidebar: {
        width: '22%',
        background: 'rgb(245, 247, 250)',
        borderRight: '1px solid rgb(221, 221, 221)',
        overflowY: 'auto',
        borderRadius: '20px',
        scrollbarWidth: 'none' // Firefox only},
    },
    issueList: {
        listStyle: 'none',
        margin: 0,
        padding: '10px',
        display: 'flex',
        flexDirection: 'column',
        gap: '10px'
    },


    issueMessage: {
        fontWeight: 'bold',
        color: '#2b4c7e',
        fontSize: '13px'
    },
    issueSubtitle: {
        fontSize: '11px',
        color: '#555'
    },
    chatPane: { flex: 1, display: 'flex', flexDirection: 'column', padding: '10px' },
    chatWrapper: { display: 'flex', height: '100%', gap: '20px' },
    aiBox: { width: '50%', padding: '10px', backgroundColor: '#fff', borderRadius: '8px', border: '1px solid #ddd', overflowY: 'auto' },
    aiBoxLeft: { flex: 1, padding: '10px', backgroundColor: '#f8f8f8', borderRadius: '8px', border: '1px solid #ddd', overflowY: 'auto' },
    detailTitle: { fontSize: '16px', marginBottom: '10px' },
    loadingText: { fontStyle: 'italic', color: '#888' },
    placeholderText: { fontStyle: 'italic', color: '#aaa' },
    codeBlock: { backgroundColor: '#f6f8fa', fontFamily: 'monospace', fontSize: '13px', padding: '10px', borderRadius: '6px', marginTop: '10px', wordWrap: 'break-word', whiteSpace: 'pre-wrap' },
    highlightedLine: { backgroundColor: '#ffe9e9', fontWeight: 'bold' },
    fixButton: { marginTop: '10px', padding: '8px 12px', border: 'none', borderRadius: '5px', backgroundColor: '#4a90e2', color: 'white', cursor: 'pointer' },
    apiCard: { backgroundColor: '#e7f3ff', padding: '15px', borderRadius: '8px', border: '1px solid #b3d4fc', marginTop: '20px' },
    apiTitle: { fontSize: '16px', fontWeight: 'bold', marginBottom: '10px' },
    detailWrapper: { padding: '20px', width: '100%' },
    mainTitle: {
        background: '#dde3ec',
        padding: '10px 15px',
        borderRadius: '10px',
        fontSize: '20px',
        color: '#2b4c7e',
        marginBottom: '20px'
    },
    tabs: {
        display: 'flex',
        gap: '10px',
        marginBottom: '20px'
    },
    tab: {
        padding: '10px 15px',
        borderRadius: '8px',
        border: 'none',
        background: '#d3d3d3',
        cursor: 'pointer',
        fontWeight: 'bold'
    },
    activeTab: {
        backgroundColor: '#fff',
        border: '2px solid #888'
    },
    tabContent: {
        background: '#fff',
        borderRadius: '10px',
        padding: '20px',
        boxShadow: '0 0 5px rgba(0,0,0,0.1)'
    },
    fixArea: {
        textAlign: 'center'
    },
    fixButton: {
        marginTop: '20px',
        padding: '10px 20px',
        borderRadius: '10px',
        border: '1px solid #222',
        backgroundColor: '#fff',
        fontWeight: 'bold',
        cursor: 'pointer'
    },
    dropdownGroup: {
        display: 'flex',
        justifyContent: 'center',
        gap: '30px',
        marginTop: '20px',
        marginBottom: '10px',
        flexWrap: 'wrap'
    },
    dropdownContainer: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-start',
        gap: '5px',
        minWidth: '200px'
    },
    dropdownLabel: {
        fontWeight: 'bold',
        color: '#2b4c7e',
        fontSize: '14px'
    },
    selectBox: {
        padding: '8px 12px',
        borderRadius: '6px',
        border: '1px solid #ccc',
        fontSize: '14px',
        backgroundColor: '#fff',
        cursor: 'pointer',
        width: '100%'
    },
    copyButton: {
        position: 'absolute',
        top: 18,
        right: 8,
        background: '#333',
        border: '1px solid #555',
        color: '#e6e6e6',
        borderRadius: 4,
        cursor: 'pointer',
        padding: '4px 6px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
    }

};