import { WEIGHT } from "./modelOptions";

/** Safe JSON.parse with fallback */
const safeJSONParse = (text, fallback) => { try { return JSON.parse(text); } catch { return fallback; } };

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
    _raw: node,
    _descriptorSections: null // This will now be fetched on click
  };
};

/** Fetch new metric-based issues from the Sonar 'error tree' measure */
export async function fetchMetricIssues(projectKey) {
  const res = await fetch(
    `/api/measures/component?component=${encodeURIComponent(projectKey)}&metricKeys=secai.cognicrypt.error.tree`
  );
  if (!res.ok) return [];

  const json = await res.json();
  const rawValue = json?.component?.measures?.[0]?.value ?? null;
  if (!rawValue) return [];

  const flatList = safeJSONParse(rawValue, []);
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
  return mapped;
}

export const getPriorityFromScore = (p, s) => {
  const severityWeight = WEIGHT[s] ?? 0;
  const probability = typeof p === "number" ? p : 0;
  return (severityWeight + (1 - probability)) / 2;
};