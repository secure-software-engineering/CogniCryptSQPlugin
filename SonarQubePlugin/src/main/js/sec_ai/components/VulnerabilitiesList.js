import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
    findProjects,
    getActiveproject,
    getRuleDescriptor,
    getDotFromIssueRaw
} from './APIs/api';

import IssueList from './Description/IssueList';
import AiFix from './Description/AiFix';
import DetailedDescription from './Description/DetailedDescription';
import {
    buildRootCauseHTML,
    buildHowToFixHTML,
    buildMoreInfoHTML
} from '../utils/htmlBuilders';
import { fetchMetricIssues, getPriorityFromScore } from '../utils/issuesService';

import {
    selectAiSolution,
    selectIssues,
    selectProjectKey,
    selectSelectedIssue,
    selectSourceCode,
    selectSourceCodeResults,
    setAiSolution,
    setIssues,
    setProjectKey,
    setSelectedIssue,
    setSourceCode
} from '../store/issuesReducer';
import QuickFixCard from './Description/QuickFixCard';
import DiffView from './Description/DiffView';
import { SERVER_IP } from '../utils/settings';

const BASE_NAVS = ['Root Cause', 'How to Fix', 'AI Fix', 'More Info'];

const Spinner = ({ size = 14 }) => (
    <svg width={size} height={size} viewBox="0 0 50 50" style={{ marginLeft: 6, verticalAlign: 'middle' }}>
        <circle cx="25" cy="25" r="20" fill="none" stroke="currentColor" strokeWidth="6" strokeLinecap="round" strokeDasharray="31.4 188.4">
            <animateTransform attributeName="transform" type="rotate" from="0 25 25" to="360 25 25" dur="0.9s" repeatCount="indefinite" />
        </circle>
    </svg>
);

const formatPercent = (val) => {
    if (val == null || val === '' || Number.isNaN(Number(val))) return '-';
    const n = typeof val === 'string' ? parseFloat(val) : val;
    const inverted = 1 - n;
    const pct = inverted <= 1 ? inverted * 100 : inverted;
    if (pct >= 99.995) {
        return '99.99%';
    }
    if (pct <= 0.005 && pct > 0) {
        return '0.01%';
    }
    if (pct === 0) {
        return '0.01%';
    }

    return `${pct.toFixed(2)}%`;
};

function DetailedFix({ jumpTarget, clearJumpTarget }) {
    const dispatch = useDispatch();
    const issues = useSelector(selectIssues);
    const selectedIssue = useSelector(selectSelectedIssue);
    const sourceSnippet = useSelector(selectSourceCode);
    const projectKey = useSelector(selectProjectKey);
    const aiSolution = useSelector(selectAiSolution);
    const fullSourceCode = useSelector(selectSourceCodeResults);

    const [loading, setLoading] = useState(true);
    const [selectedTab, setSelectedTab] = useState('Root Cause');

    const [descriptorSections, setDescriptorSections] = useState(null);
    const [sonarRuleKey, setSonarRuleKey] = useState(null);

    const [rootCauseHTML, setRootCauseHTML] = useState('');
    const [howToFixHTML, setHowToFixHTML] = useState('');
    const [moreInfoHTML, setMoreInfoHTML] = useState('');

    const [fpLoading, setFpLoading] = useState(false);
    const [fpScore, setFpScore] = useState(null);
    const [priority, setPriority] = useState(null);
    const [fpError, setFpError] = useState(null);

    const getIssueHashcode = (issue) => issue?._raw?.hashcode || issue?.key || null;

    const handleIssueClick = async (issue) => {
        if (!issue) return;
        dispatch(setAiSolution(null));
        dispatch(setSelectedIssue(issue));

        const snippet = issue?._raw?.codeSnippet || null;
        dispatch(setSourceCode(snippet));

        setDescriptorSections(null);
        setFpLoading(true);
        setFpScore(null);
        setFpError(null);

        try {
            let errorType = issue?._raw?.errorType;
            if (errorType === 'AlternativeReqPredicateError') {
                errorType = 'RequiredPredicateError';
            } else if (errorType === 'IncompleteOperationError' || errorType === 'TypestateError') {
                errorType = 'OrderError';
            }

            const rule = issue?._raw?.rule;
            if (errorType && rule) {
                const ruleSuffix = rule.substring(rule.lastIndexOf('.') + 1);
                const key = `cognicrypt:${errorType}_${ruleSuffix}`;
                setSonarRuleKey(key);

                const resp = await getRuleDescriptor(key);
                const sections = resp?.rules?.[0]?.descriptionSections || null;
                setDescriptorSections(sections);
            }
        } catch (e) {
            console.error('[Descriptor] request failed:', e);
        }

        try {
            const rawDot = await getDotFromIssueRaw(issue?._raw);
            if (!rawDot) {
                setFpError('No CPG found for this issue');
                setFpLoading(false);
                console.warn('[FP] No DOT/CPG present in _raw (expecting cpgBase64Gz or dotGraph)');
                return;
            }


            const dotGraph = String(rawDot).replace(/\r\n/g, '\n');


            const hashcode = String(getIssueHashcode(issue) || '');
            const res = await fetch(`http://${SERVER_IP}/fp`, {
                method: 'POST',
                mode: 'cors',
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                body: JSON.stringify({ hashcode, dot_graph: dotGraph })
            });

            const text = await res.text();
            let json = {};
            try { json = text ? JSON.parse(text) : {}; } catch (_) { }

            if (!res.ok) {
                const msg = json?.error || `HTTP ${res.status}: ${text?.slice?.(0, 300) ?? ''}`;
                throw new Error(msg);
            }

            // 4) display probability_score as percentage
            const p = json?.probability_score ?? json?.probability ?? json?.score ?? null;
            setFpScore(typeof p === 'number' ? p : (p ? Number(p) : null));
            const priority = getPriorityFromScore(p, issue?._raw?.severity);
            setPriority(priority);
            setFpError(null);
        } catch (e) {
            console.error('[FP] request failed:', e);
            setFpError(e?.message || String(e));
            setFpScore(null);
        } finally {
            setFpLoading(false);
        }
    };

    useEffect(() => {
        let isMounted = true;
        const run = async () => {
            setLoading(true);
            try {
                const projects = await findProjects();
                const _id = new URLSearchParams(window.location.search).get('id');
                if (!projects || !_id) { setLoading(false); return; }

                const activeProject = getActiveproject(_id, projects);
                if (!activeProject?.key) { setLoading(false); return; }

                dispatch(setProjectKey(activeProject.key));
                const metricIssues = await fetchMetricIssues(activeProject.key);

                if (!isMounted) return;
                dispatch(setIssues(metricIssues));
                setLoading(false);

                if (jumpTarget && metricIssues.length > 0) {
                    const match = metricIssues.find(i => i._raw.hashcode === jumpTarget.hashcode);
                    if (match) {
                        handleIssueClick(match);
                        clearJumpTarget?.();
                    }
                }
            } catch (e) {
                console.error('DetailedFix initial fetch error:', e);
                setLoading(false);
            }
        };
        run();
        return () => { isMounted = false; };
    }, [jumpTarget]);

    useEffect(() => {

        const run = async () => {
            if (!selectedIssue) {
                setRootCauseHTML('');
                setHowToFixHTML('');
                setMoreInfoHTML('');
                return;
            }

            const issueWithDescriptors = { ...selectedIssue, _descriptorSections: descriptorSections };
            setRootCauseHTML(await buildRootCauseHTML(issueWithDescriptors, projectKey, 5));
            setHowToFixHTML(buildHowToFixHTML(issueWithDescriptors));
            setMoreInfoHTML(buildMoreInfoHTML(issueWithDescriptors));
        };
        run();
    }, [selectedIssue, sourceSnippet, descriptorSections]);

    const navs = React.useMemo(() => {
        const hasQuickFix = Array.isArray(selectedIssue?._raw?.quickFixes?.edits) && selectedIssue._raw.quickFixes?.edits.length > 0;
        let list = ['Root Cause', 'How to Fix'];
        if (hasQuickFix) {
            list.push('Quick Fix');
        }
        list.push('AI Fix');
        if (aiSolution && aiSolution.Final_Secure_Code_Snippet) {
            list.push('Diff View');
        }
        list.push('More Info');
        return list;
    }, [selectedIssue, aiSolution]);

    // useEffect(() => {
    //     if (aiSolution && aiSolution.Final_Secure_Code_Snippet && fullSourceCode) {
    //         console.log("Full source code available for Diff View.", fullSourceCode);
    //         setSelectedTab('Diff View');
    //     }
    // }, [aiSolution]);

    useEffect(() => {
        if (!navs.includes(selectedTab)) {
            setSelectedTab('Root Cause');
        }
    }, [navs, selectedTab]);


    return (
        <div style={styles.container}>
            <div style={styles.sidebar}>
                {loading ? (
                    <p style={styles.loadingText}>Loading issues...</p>
                ) : (
                    <ul style={styles.issueList}>
                        {(issues || []).length === 0 ? (
                            <p style={styles.loadingText}>No issues found.</p>
                        ) : (
                            issues.map(issue => (
                                <IssueList
                                    key={issue.key}
                                    issue={issue}
                                    handleIssueClick={() => handleIssueClick(issue)}
                                />
                            ))
                        )}
                    </ul>
                )}
            </div>

            <div style={styles.descriptionPane}>
                {selectedIssue ? (
                    <div style={styles.detailWrapper}>
                        <div style={styles.titleRow}>
                            <div style={{ flex: 2 }}>
                                <h2 style={styles.mainTitle}>
                                    {selectedIssue.message || 'Issue'}
                                </h2>
                                <p style={styles.errorType}><strong>Type:</strong> {selectedIssue?._raw?.errorType || '—'}</p>
                            </div>
                            <div style={styles.titleRight}>
                                <span
                                    style={styles.confidence}
                                    title="True positive likelihood"
                                >
                                    Confidence: {' '}
                                    {fpLoading ? <Spinner /> : <strong>{formatPercent(fpScore)}</strong>}
                                    {/* <span
                                        title="True positive likelihood: the higher the percentage, the more likely this issue is a true positive."
                                        style={{
                                            cursor: 'help',
                                            marginLeft: '5px',
                                            borderBottom: '1px dotted #333'
                                        }}
                                    >
                                        ?
                                    </span> */}
                                </span>
                                {fpError && (
                                    <span style={{ background: '#fde8e8', padding: '4px 8px', borderRadius: 8, color: '#b91c1c' }}>
                                        {fpError}
                                    </span>
                                )}
                                {selectedIssue?._raw?.severity && (
                                    <span style={{
                                        ...styles.severityBadge,
                                        backgroundColor: (
                                            selectedIssue._raw.severity === 'HIGH' ? '#f44336' :
                                                selectedIssue._raw.severity === 'MEDIUM' ? '#ff9800' :
                                                    selectedIssue._raw.severity === 'LOW' ? '#4caf50' :
                                                        '#9e9e9e'
                                        )
                                    }}>
                                        Severity: {selectedIssue._raw.severity}
                                    </span>
                                )}
                                {priority && (
                                    
                                        <span style={{
                                            ...styles.severityBadge,
                                            backgroundColor: (
                                                selectedIssue._raw.severity === 'HIGH' ? '#f44336' :
                                                    selectedIssue._raw.severity === 'MEDIUM' ? '#ff9800' :
                                                        selectedIssue._raw.severity === 'LOW' ? '#4caf50' :
                                                            '#9e9e9e'
                                            )
                                        }}>
                                            Priority: {priority.toFixed(2)}
                                            {/* <span
                                                title="Priority: a combined score based on confidence and severity; higher means more urgent. Ranges from 0 to 1."
                                                style={{
                                                    cursor: 'help',
                                                    marginLeft: '5px',
                                                    borderBottom: '1px dotted #333'
                                                }}
                                            >
                                                ?
                                            </span> */}
                                        </span>
                                )}
                            </div>
                        </div>

                        <div style={styles.tabs}>
                            {navs.map(tab => (
                                <button
                                    key={tab}
                                    style={{ ...styles.tab, ...(selectedTab === tab ? styles.activeTab : {}) }}
                                    onClick={() => setSelectedTab(tab)}
                                >
                                    {tab}
                                </button>
                            ))}
                        </div>

                        <div style={styles.tabContent}>
                            {selectedTab === "AI Fix" && <AiFix sourceSnippet={sourceSnippet} _oldRule={sonarRuleKey} />}
                            {selectedTab === "Root Cause" && <DetailedDescription description={rootCauseHTML} />}
                            {selectedTab === "How to Fix" && <DetailedDescription description={howToFixHTML} />}
                            {selectedTab === "Quick Fix" && <QuickFixCard fixes={selectedIssue?._raw?.quickFixes || []} />}
                            {selectedTab === "More Info" && <DetailedDescription description={moreInfoHTML} />}
                            {selectedTab === "Diff View" && (
                                <DiffView
                                    oldCode={fullSourceCode[0].fullSourceCode}
                                    newCode={aiSolution?.Final_Secure_Code_Snippet}
                                />
                            )}
                        </div>
                    </div>
                ) : (
                    <p style={styles.placeholderText}>Click an issue to view details and compute the FP confidence.</p>
                )}
            </div>
        </div>
    );
}

// Styles
const styles = {
    container: {
        display: 'flex',
        height: 'inherit',
        fontFamily: 'Arial, sans-serif',
        flex: '1', width: '100%',
        maxWidth: '100%'
    },
    sidebar: {
        width: '22%',
        background: 'rgb(245, 247, 250)',
        borderRight: '1px solid rgb(221, 221, 221)',
        overflowY: 'auto',
        borderRadius: '20px',
        scrollbarWidth: '5px'
    },
    issueList: {
        listStyle: 'none',
        margin: 0,
        padding: '10px',
        display: 'flex',
        flexDirection: 'column',
        gap: '10px'
    },
    descriptionPane: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        padding: '10px',
        width: '78%',
        maxWidth: '78%',
        overflowY: 'auto'
    },
    loadingText: { fontStyle: 'italic', color: '#888', padding: '20px' },
    placeholderText: { fontStyle: 'italic', color: '#aaa', padding: '20px', textAlign: 'center' },
    detailWrapper: { padding: '20px' },
    mainTitle: {
        fontSize: '20px',
        margin: 0
    },
    tabs: { display: 'flex', gap: '10px', marginBottom: '20px' },
    tab: {
        padding: '10px 15px',
        borderRadius: '8px',
        border: '1px solid #ccc',
        backgroundColor: '#f0f0f0',
        cursor: 'pointer',
        fontWeight: 'bold'
    },
    activeTab: {
        padding: '10px 15px',
        borderRadius: '8px',
        border: '1px solid #007bff',
        backgroundColor: '#e7f3ff',
        color: '#0056b3',
        cursor: 'pointer',
        fontWeight: 'bold'
    },
    tabContent: {
        background: '#fff',
        borderRadius: '10px',
        padding: '20px',
        boxShadow: '0 0 10px rgba(0,0,0,0.05)'
    },
    titleRow: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        background: '#f8f9fa',
        padding: '10px 15px',
        borderRadius: '10px',
        color: '#333',
        marginBottom: '20px',
        border: '1px solid #dee2e6'
    },
    titleRight: {
        fontSize: '14px',
        fontWeight: 'bold',
        color: '#555',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '15px',
        flex: 0.5
    },
    confidence: {
        backgroundColor: '#e0e7ff',
        padding: '4px 10px',
        borderRadius: '8px',
        color: '#1e3a8a',
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
    },
    severityBadge: {
        padding: '4px 10px',
        color: 'white',
        borderRadius: '8px',
        fontSize: '13px',
        fontWeight: 'bold',
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
    },
    errorType: { fontSize: '12px', color: '#555', marginTop: '4px' }
};

export default DetailedFix;