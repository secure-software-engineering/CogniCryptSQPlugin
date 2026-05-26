// src/utils/htmlBuilders.js

import { fetchSourceCode } from "../components/APIs/api";

// ---------- helpers ----------
export const esc = (s) =>
  String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

const pickSection = (sections = [], key) =>
  sections.find((s) => s.key === key)?.content || '';

/* ... other functions like buildCodeSnippetHTML are unchanged ... */
export function buildCodeSnippetHTML(issue, sourceSnippet = '') {
  const snippet = issue?._raw?.codeSnippet ?? sourceSnippet ?? '';
  const filePath = esc(issue?._raw?.reportLocation?.filePath || issue?.component || '—');
  const start = issue?._raw?.reportLocation?.start ? `${issue._raw.reportLocation.start[0]}:${issue._raw.reportLocation.start[1]}` : '—';
  const end = issue?._raw?.reportLocation?.end ? `${issue._raw.reportLocation.end[0]}:${issue._raw.reportLocation.end[1]}` : '—';

  if (!snippet) return '';

  return `
    <h4 style="margin:24px 0 8px 0; font-size:18px; font-weight:600; border-bottom:1px solid #e0e0e0; padding-bottom:8px;">Code Snippet</h4>
    <div style="font-size:13px; color:#666; margin-bottom:8px; font-family:'Fira Code', Menlo, monospace;">
      <strong>Location:</strong> ${filePath} (${start} → ${end})
    </div>
    <pre style="background:#282c34; color:#abb2bf; padding:16px; border-radius:8px; overflow-x:auto; font-family:'Fira Code', Menlo, monospace; font-size:14px;"><code>${esc(snippet)}</code></pre>
    <hr style="border:none; border-top:1px solid #e0e0e0; margin:24px 0;" />
  `;
}


/**
 * Builds a highlighted HTML code snippet with line numbers and a precise underline.
 * * CHANGE #1: This function now accepts the full `issue` object to access column numbers.
 */
function buildHighlightedCodeSnippetHTML(sourceCode, issue) {
    if (!sourceCode || typeof sourceCode.highlight !== 'string') return '';
    
    // Define styles as constants for readability
    const wrapperStyle = `display:flex; align-items-flex-start; padding:2px 0;`;
    const lineNumStyle = `min-width:40px; padding-right:16px; color:#636d83; text-align:right; user-select:none;`;
    const highlightWrapperStyle = `background-color:rgba(224, 208, 117, 0.15); border-left:3px solid #e0c855;`;
    const underlineStyle = `text-decoration: underline wavy rgba(215, 90, 90, 1) 2px; text-underline-offset: 3px;`;

    const { before = [], highlight, after = [] } = sourceCode;
    const highlightLineNumber = issue._raw.reportLocation.start[0];
    let currentLine = highlightLineNumber - before.length;
    const linesHtml = [];

    // --- Create the content for the highlighted line ---
    let highlightedContent;
    const loc = issue?._raw?.reportLocation;
    
    // Check if we have precise start and end columns
    if (loc && loc.start && loc.end && loc.start[0] === highlightLineNumber && loc.end[0] === highlightLineNumber) {
        const startCol = loc.start[1]; // Convert to 0-based index
        const endCol = loc.end[1] + 1; // End column is exclusive in slice
        
        const beforeUnderline = highlight.substring(0, startCol);
        const toUnderline = highlight.substring(startCol, endCol);
        const afterUnderline = highlight.substring(endCol);

        highlightedContent = `
            ${esc(beforeUnderline)}<span style="${underlineStyle}">${esc(toUnderline)}</span>${esc(afterUnderline)}
        `;
    } else {
        // Fallback if there's no column info: just escape the whole line
        highlightedContent = esc(highlight);
    }
    // ---------------------------------------------------


    // Lines before
    before.forEach(line => {
        linesHtml.push(`<div style="${wrapperStyle}"><span style="${lineNumStyle}">${currentLine++}</span><span>${esc(line)}</span></div>`);
    });

    // The highlighted line with the new underlined content
    linesHtml.push(`<div style="${highlightWrapperStyle}"><div style="${wrapperStyle} padding-left:13px;"><span style="${lineNumStyle}">${currentLine++}</span><span>${highlightedContent}</span></div></div>`);

    // Lines after
    after.forEach(line => {
        linesHtml.push(`<div style="${wrapperStyle}"><span style="${lineNumStyle}">${currentLine++}</span><span>${esc(line)}</span></div>`);
    });

    return `
        <div style="background:#282c34; color:#abb2bf; padding:16px; border-radius:8px; overflow-x:auto; font-family:'Fira Code', Menlo, monospace; font-size:14px; max-width:100%;">
            <pre style="margin:0; padding:0; background:none; white-space:pre; max-width:100%;">
                <code>${linesHtml.join('\n')}</code>
            </pre>
        </div>`;
}

/**
 * Builds the complete HTML block for the "Root Cause" tab with inline styles.
 */
export async function buildRootCauseHTML(issue, projectKey, contextLines = 5) {
  try {
    const component = `${projectKey}:${issue.component}`;
    const sourceCode = await fetchSourceCode(component, issue._raw.reportLocation.start[0], contextLines);
    
    const sections = issue?._descriptorSections || [];
    const rootCauseContent = pickSection(sections, 'root_cause');

    // CHANGE #2: Pass the entire `issue` object to the snippet builder.
    const snippetBlock = buildHighlightedCodeSnippetHTML(sourceCode, issue);

    const rootCauseBlock = rootCauseContent ? `<div style="line-height:1.6;">${rootCauseContent}</div>` : '';
    const divider = snippetBlock && rootCauseBlock ? `<hr style="border:none; border-top:1px solid #e0e0e0; margin:24px 0;" />` : '';
    
    return `${snippetBlock}${divider}${rootCauseBlock}`;
  } catch (error)
  {
    console.error("Failed to build root cause HTML:", error);
    const rootCause = pickSection(issue?._descriptorSections || [], 'root_cause');
    return `<p>Could not load source code.</p><div style="line-height:1.6;">${rootCause || ''}</div>`;
  }
}

/* ... other functions like buildHowToFixHTML and buildMoreInfoHTML are unchanged ... */
export function buildHowToFixHTML(issue) {
  const sections = issue?._descriptorSections || [];
  const content = pickSection(sections, 'how_to_fix');
  return `<div style="line-height:1.6;" class="list-items">${content}</div>`;
}

export function buildMoreInfoHTML(issue) {
  const sections = issue?._descriptorSections || [];
  const n = issue?._raw ?? {};

  const preceding = n?.precedingErrors?.length ?? 0;
  const subsequent = n?.subsequentErrors?.length ?? 0;
  const filePath = n?.reportLocation?.filePath || issue?.component || '—';
  const start = n?.reportLocation?.start ? `${n.reportLocation.start[0]}:${n.reportLocation.start[1]}` : '—';
  const end = n?.reportLocation?.end ? `${n.reportLocation.end[0]}:${n.reportLocation.end[1]}` : '—';
  const resources = pickSection(sections, 'resources');

  // Define styles for table cells since pseudo-selectors aren't possible
  const tdKeyStyle = `padding:8px 4px; border-bottom:1px solid #e0e0e0; width:100px; font-weight:600; color:#666;`;
  const tdValueStyle = `padding:8px 4px; border-bottom:1px solid #e0e0e0;`;

  return `
    <div>
      <h4 style="margin:0 0 8px 0; font-size:18px; font-weight:600;">Context</h4>
      <table style="width:100%; border-collapse:collapse; margin:16px 0; font-size:14px;">
        <tbody>
          <tr>
            <td style="${tdKeyStyle}"><strong>Graph</strong></td>
            <td style="${tdValueStyle}">${preceding} preceding • ${subsequent} subsequent</td>
          </tr>
          <tr>
            <td style="${tdKeyStyle}"><strong>Location</strong></td>
            <td style="${tdValueStyle}">${esc(filePath)} (${esc(start)} → ${esc(end)})</td>
          </tr>
        </tbody>
      </table>
      ${resources ? `
        <h4 style="margin:24px 0 8px 0; font-size:18px; font-weight:600;">Resources</h4>
        <div style="line-height:1.6; " class="list-items">${resources}</div>
      ` : ''}
    </div>
  `;
}