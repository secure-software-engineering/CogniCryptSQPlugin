import { WEIGHT } from "./modelOptions";
import {SERVER_IP} from "./settings";

/** Safe JSON.parse with fallback */
const safeJSONParse = (text, fallback) => {
    try {
        return JSON.parse(text);
    } catch {
        return fallback;
    }
};

/** Normalize file paths for stable matching */
const normalizePath = (p) => {
    if (!p) return '';
    const np = p.replace(/\\/g, '/').replace(/\/{2,}/g, '/');
    return (np.includes('/') ? np : np.replace(/\./g, '/'));
};

/** Convert flat metric node -> our issue shape */
export const mapTreeNodeToIssue = (node, idx = 0) => {
    const file =
        node?.reportLocation?.filePath ||
        node?.file ||
        node?.path ||
        node?.class ||
        'unknown/unknown';

    const component = normalizePath(file);
    const line = node?.reportLocation?.start?.[0] ?? 0;
    const rule = node?.rule || null;
    const message = node?.message || node?.errorType || 'Issue';
    const key = node?.key || node?.hashcode || `${component}:${line}:${rule ?? 'rule'}:${idx}`;

    return {
        key,
        message,
        rule,
        component,
        line,
        fp_score: -1, // This will be set after fetching the issues
        _raw: node,
        _descriptorSections: null // This will now be fetched on click
    };
};

/** Fetch new metric-based issues from the Sonar 'error tree' measure */
export async function fetchMetricIssues(projectKey) {
    const branch = new URLSearchParams(window.location.search).get('branch')
    const branchArg = branch ? `&branch=${encodeURIComponent(branch)}` : "";
    const res = await fetch(
        `/api/measures/component?component=${encodeURIComponent(projectKey)}${branchArg}&metricKeys=secai.cognicrypt.error.tree`
    );
    if (!res.ok) return { lastAnalysis: null , metricIssues: []};

    const json = await res.json();
    const rawValue = json?.component?.measures?.[0]?.value ?? null;
    if (!rawValue) return [];


    const parsed = safeJSONParse(rawValue, []);
    const lastAnalysis = parsed.timestamp ?? new Date().getTime();
    const flatList = parsed.issues ?? parsed;
    if (!Array.isArray(flatList) || flatList.length === 0) return [];

    // Dedup-ish: sig by component|line|rule|message
    const seen = new Set();
    const mapped = [];
    flatList.forEach((node, i) => {
        const issue = mapTreeNodeToIssue(node, i);
        const sig = `${issue.component}|${issue.line}|${issue.rule}|${issue.message}`;
        if (!seen.has(sig)) {
            seen.add(sig);
            mapped.push(issue);
        }
    });

    return { lastAnalysis, metricIssues: mapped };
}

/** Fetch the false positive scores for the given issues. Returns the issues after the fp scores were added to the objects */
export async function fetchFPScores(lastAnalysis, metricIssues, projectKey, branchName) {
    if (!metricIssues) return [];

    const fp_data = [];
    metricIssues.forEach((issue) => {
        fp_data.push({hashcode: issue.key || issue._raw?.hashcode, dot_graph: issue._raw?.cpgBase64Gz});
    });
    const res = await fetch(`http://${SERVER_IP}/fpall`, {
        method: 'POST',
        mode: 'cors',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({
            last_analysis: lastAnalysis,
            project: projectKey,
            branch: branchName,
            errors: fp_data })
    });

    // Parse results
    const text = await res.text();
    let json = {};
    try { json = text ? JSON.parse(text) : {}; } catch (_) { }

    // Save generated fp scores
    // Results are in the same order as the issues
    metricIssues.forEach((issue, i) => {
        issue.fp_score = json.fp_scores[i].probability_score;
    });

    return metricIssues;
}

/**
 * Send DOT to the external FP service.
 * POST http://SERVER_IP/fp
 * Body: { hashcode: string, dot_graph: string }
 */
export async function fetchSingleFPScore(hashcode, dotGraph, projectKey, branchName) {
    if (!hashcode) throw new Error('sendDotForPrediction: hashcode is required');
    if (!dotGraph) throw new Error('sendDotForPrediction: dotGraph is required');

    const endpoint = `http://${SERVER_IP}/fp`;

    const res = await fetch(endpoint, {
        method: 'POST',
        mode: 'cors',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({
            hashcode: String(hashcode),
            dot_graph: dotGraph,
            project: projectKey,
            branch: branchName
        })
    });

    const text = await res.text();
    let json = {};
    try { json = text ? JSON.parse(text) : {}; } catch { /* leave {} */ }

    if (!res.ok) {
        const msg = json?.error || `HTTP ${res.status}: ${text?.slice?.(0, 300) ?? ''}`;
        throw new Error(msg);
    }

    return json; // { hashcode, prediction, probability_score }
}

export const getPriorityFromScore = (p, s) => {
    const severityWeight = WEIGHT[s] ?? 0;
    const probability = typeof p === "number" ? p : 0;
    return (severityWeight + (1 - probability)) / 2;
};