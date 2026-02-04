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
    const response = await fetch(`http://${SERVER_IP}:8000/newfix`, {
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
// === DOT decoding helpers (supports cpgBase64Gz) ===
export function base64ToUint8Array(b64) {
  const bin = atob(b64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

export async function gunzipUint8(uint8) {
  // Modern browsers (Chromium, FF): DecompressionStream available
  if ('DecompressionStream' in window) {
    const ds = new DecompressionStream('gzip');
    const stream = new Blob([uint8]).stream().pipeThrough(ds);
    const ab = await new Response(stream).arrayBuffer();
    return new TextDecoder().decode(ab);
  }
  // Fallback (not truly gunzip, but avoids total break if not gzipped)
  return new TextDecoder().decode(uint8);
}

// Try to unescape common JSON-escaped content (if you ever store plain DOT)
function softUnescape(s) {
  if (typeof s !== 'string') return null;
  try {
    const maybe = s.startsWith('"') && s.endsWith('"') ? s : `"${s.replace(/"/g, '\\"')}"`;
    const parsed = JSON.parse(maybe);
    if (typeof parsed === 'string') return parsed;
  } catch (e) { /* ignore */ }
  return s
    .replaceAll('\\n', '\n')
    .replaceAll('\\r', '\r')
    .replaceAll('\\"', '"')
    .replaceAll('\\\\', '\\');
}

/**
 * Returns a plain DOT string from an issue's _raw.
 * Supports:
 *  - _raw.cpgBase64Gz  (your current format)
 *  - (aliases if you ever change naming): cpg_base64_gz, cpgB64Gz, dotGraphB64Gz, dot_graph_b64_gz
 *  - plain text variants: dotGraph, dot_graph, cpg, cpgDot
 */
export async function getDotFromIssueRaw(raw) {
  if (!raw) return null;

  // 1) Gzipped+Base64 variants (current)
  const b64 =
    raw.cpgBase64Gz ??
    raw.cpg_base64_gz ??
    raw.cpgB64Gz ??
    raw.dotGraphB64Gz ??
    raw.dot_graph_b64_gz ??
    null;

  if (typeof b64 === 'string' && b64.trim()) {
    const u8 = base64ToUint8Array(b64.trim());
    try {
      const dot = await gunzipUint8(u8);
      if (dot && dot.trim()) return dot.trim();
    } catch (e) {
      // if not gzipped for some reason, decode raw bytes to text
      const txt = new TextDecoder().decode(u8);
      if (txt && txt.trim()) return txt.trim();
    }
  }

  // 2) Plain text variants (if you ever emit them)
  const plain =
    raw.dotGraph ??
    raw.dot_graph ??
    raw.cpg ??
    raw.cpgDot ??
    null;

  if (typeof plain === 'string' && plain.trim()) {
    const un = softUnescape(plain.trim());
    return (un || plain).trim();
  }

  return null;
}

/**
 * Send DOT to the external FP service.
 * POST http://131.234.29.71/fp
 * Body: { hashcode: string, dot_graph: string }
 */
export async function sendDotForPrediction({ endpoint = `http://${SERVER_IP}/fp`, hashcode, dotGraph, timeoutMs = 20000 }) {
  if (!hashcode) throw new Error('sendDotForPrediction: hashcode is required');
  if (!dotGraph) throw new Error('sendDotForPrediction: dotGraph is required');

  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), timeoutMs);

  try {
    const res = await fetch(endpoint, {
      method: 'POST',
      mode: 'cors',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ hashcode: String(hashcode), dot_graph: dotGraph }),
      signal: ctrl.signal
    });

    const text = await res.text();
    let json = {};
    try { json = text ? JSON.parse(text) : {}; } catch { /* leave {} */ }

    if (!res.ok) {
      const msg = json?.error || `HTTP ${res.status}: ${text?.slice?.(0, 300) ?? ''}`;
      throw new Error(msg);
    }
    return json; // { hashcode, prediction, probability_score }
  } finally {
    clearTimeout(t);
  }
}
