import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { Octokit } from "@octokit/rest";
import { getPullRequestDetails } from '../APIs/api';
import { selectProjectKey, selectSelectedIssue } from '../../store/issuesReducer';
import { useDispatch } from 'react-redux';
import { setGithubPATtoken, setGithubRepourl, setGithubUsername } from '../../store/pullRequestReducer';

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

const QuickFixCard = ({ fixes = {} }) => {
    const dispatch = useDispatch();
    const issue = useSelector(selectSelectedIssue);
    const projectKey = useSelector(selectProjectKey);


    const [idx, setIdx] = useState(0);
    const [isCopied, setIsCopied] = useState(false);
    const [isApplied, setIsApplied] = useState(false);
    const [prSettingsValid, setPrSettingsValid] = useState(true);
    const [prSuccessMessage, setPrSuccessMessage] = useState('');
    const [prValidationMessage, setPrValidationMessage] = useState('');

    const { edits = [], location = {}, message = 'Change value to:' } = fixes;
    const fx = edits[idx] || {};

    const stripQuotes = (s) =>
        typeof s === 'string' ? s.replace(/^"+|"+$/g, '') : s;

    const filePath = location?.filePath ?? '—';
    const start = location?.start ? `${location.start[0]}:${location.start[1]}` : '—';
    const end = location?.end ? `${location.end[0]}:${location.end[1]}` : '—';

    const handleCopy = () => {
        const codeToCopy = fx?.edit ?? '';
        if (navigator.clipboard && codeToCopy) {
            navigator.clipboard.writeText(codeToCopy).then(() => {
                setIsCopied(true);
                setTimeout(() => setIsCopied(false), 2000);
            });
        }
    };

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


    const handleApplyFix = async () => {
        if (!fx?.edit || !location?.filePath && !issue?.key) {
            alert("Missing fix data or file path.");
            return;
        }

        setIsApplied(true);
        const prSettings = await getPullRequestDetails(projectKey);

        const githubUsername = prSettings.githubUsername || 'none';
        const githubRepoUrl = prSettings.githubRepoUrl || 'none';
        const githubPatToken = prSettings.githubPatToken || 'none';

        const repoUrlParts = githubRepoUrl.replace('.git', '').split('/');
        const repoOwner = repoUrlParts[repoUrlParts.length - 2];
        const repoName = repoUrlParts[repoUrlParts.length - 1];
        const branchName = `ai-fix/${issue.key}`;
        const baseBranch = "master";

        const filePath = location.filePath;

        try {
            const octokit = new Octokit({ auth: githubPatToken });

            // 1. Get latest SHA of base branch
            const refRes = await octokit.rest.git.getRef({
                owner: repoOwner,
                repo: repoName,
                ref: `heads/${baseBranch}`
            });

            const baseSha = refRes.data.object.sha;

            // 2. Create new branch
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

            // Replace the specific line
            const lineIndex = (location.start?.[0] || 1) - 1;
            lines[lineIndex] = fx.edit;

            const modifiedContent = lines.join('\n');

            // 4. Update file in new branch
            await octokit.rest.repos.createOrUpdateFileContents({
                owner: repoOwner,
                repo: repoName,
                path: filePath,
                message: `fix: apply quick fix to ${filePath}`,
                content: btoa(modifiedContent),
                branch: branchName,
                sha: fileRes.data.sha
            });

            // 5. Create Pull Request
            const prRes = await octokit.rest.pulls.create({
                owner: repoOwner,
                repo: repoName,
                title: `Quick Fix: ${message}`,
                head: branchName,
                base: baseBranch,
                body: `### Quick Fix Applied\n\n${message}\n\n_This fix was applied via the SecAI plugin._`
            });

            setPrSuccessMessage(`✅ Pull Request created: ${prRes.data.html_url}`);
            window.open(prRes.data.html_url, "_blank");

        } catch (err) {
            console.error("PR creation failed:", err);
            alert("❌ PR creation failed. See console for details.");
        } finally {
            setTimeout(() => setIsApplied(false), 3000);
        }
    };

    if (!edits.length) {
        return (
            <div style={styles.card}>
                <p style={styles.noFixes}>No quick fixes available.</p>
            </div>
        );
    }

    return (
        <div style={styles.card}>
            {/* Dropdown */}
            <div style={styles.dropdownRow}>
                <div style={styles.dropdownLabel}>{message}</div>
                <select
                    value={idx}
                    onChange={(e) => setIdx(parseInt(e.target.value, 10))}
                    style={styles.dropdown}
                >
                    {edits.map((opt, i) => (
                        <option key={i} value={i}>
                            {stripQuotes(opt?.value ?? `Fix #${i + 1}`)}
                        </option>
                    ))}
                </select>
            </div>

            {/* Location */}
            <div style={styles.location}>
                <strong>Location:</strong> {filePath} ({start} → {end})
            </div>

            {/* Replacement preview */}
            <div>
                <div style={styles.replaceLabel}><strong>Replace line with:</strong></div>
                <div style={{ position: 'relative' }}>
          <pre style={styles.codeBlock}>
            <code>{fx?.edit ?? ''}</code>
          </pre>
                    <button onClick={handleCopy} style={styles.copyButton} title="Copy to clipboard">
                        {isCopied ? <CheckIcon /> : <CopyIcon />}
                    </button>
                </div>
            </div>

            {/* Apply Fix Button */}
            <div style={styles.buttonRow}>
                <button
                    onClick={handleApplyFix}
                    style={{
                        ...styles.applyButton,
                        ...(isApplied ? styles.appliedButton : {}),

                        cursor: prSettingsValid || isApplied ? 'pointer' : 'not-allowed',
                        opacity: !prSettingsValid && !isApplied ? 0.7 : 1,
                        background: prSettingsValid || isApplied ? '#6f42c1' : '#ccc'
                    }}

                    disabled={!prSettingsValid || !isApplied}
                >
                    {isApplied ? '✓ GitHub PR applied' : 'Create GitHub PR'}
                </button>
            </div>

            {(prSuccessMessage || !prSettingsValid) && (
                <div style={{ marginTop: '8px', fontWeight: 'bold', color: prSuccessMessage ? 'green' : 'red' }}>
                    {prSuccessMessage || prValidationMessage}
                </div>
            )}
        </div>
    );
};

// Styles centralized here
const styles = {
    card: {
        border: '1px solid #eee',
        borderRadius: 10,
        padding: 16
    },
    header: {
        marginTop: 0,
        marginBottom: 10
    },
    noFixes: {
        margin: 0,
        fontStyle: 'italic',
        color: '#888'
    },
    dropdownRow: {
        marginBottom: 12,
        display: 'flex',
        gap: 8,
        alignItems: 'center',
        flexWrap: 'wrap'
    },
    dropdownLabel: {
        fontSize: 13,
        color: '#333',
        fontWeight: 600
    },
    dropdown: {
        padding: '6px 10px',
        borderRadius: 8,
        border: '1px solid #ccc',
        background: '#fff',
        fontWeight: 600
    },
    location: {
        fontSize: 13,
        color: '#555',
        marginBottom: 8
    },
    replaceLabel: {
        fontSize: 13,
        color: '#333',
        marginBottom: 6
    },
    codeBlock: {
        background: '#0b1021',
        color: '#e6e6e6',
        padding: 10,
        borderRadius: 6,
        overflow: 'auto',
        paddingRight: 40 // Make space for the button
    },
    copyButton: {
        position: 'absolute',
        top: 8,
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
    },
    buttonRow: {
        marginTop: 12,
        display: 'flex',
        justifyContent: 'flex-end'
    },
    applyButton: {
        padding: '8px 16px',
        borderRadius: 6,
        border: 'none',
        background: '#10b981',
        color: '#fff',
        fontSize: 14,
        fontWeight: 600,
        cursor: 'pointer',
        transition: 'all 0.2s',
        width: '100%'
    },
    appliedButton: {
        background: '#059669',
        borderColor: '#059669',
        cursor: 'default'
    }
};

export default QuickFixCard;
