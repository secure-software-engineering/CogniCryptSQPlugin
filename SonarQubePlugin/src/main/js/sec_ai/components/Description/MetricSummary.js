// src/components/MetricSummary.js
import React from 'react';

const esc = (s) =>
  String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

export default function MetricSummary({ issue }) {
  if (!issue) return null;

  const n = issue?._raw ?? {};
  const message = issue?.message || n?.message || 'Issue';
  const errorType = n?.errorType || '—';
  const rule = issue?.rule || '—';
  const method = n?.method || '—';
  const klass = n?.class || n?.reportLocation?.className || '—';
  const severity = n?.severity || '—';
  const conf = (n?.confidenceScore ?? '—');
  const statement = n?.statement || '—';
  const codeSnippet = n?.codeSnippet || '';
  const line = issue?.line ?? n?.reportLocation?.start?.[0] ?? '—';
  const filePath = n?.reportLocation?.filePath || issue?.component || '—';

  return (
    <div>
      <h3 style={{ marginTop: 0 }}>{esc(message)}</h3>
      <p><strong>Error Type:</strong> {esc(errorType)}</p>
      <table style={{ width: '100%', borderCollapse: 'collapse', margin: '10px 0' }}>
        <tbody>
          <tr><td><strong>Rule</strong></td><td>{esc(rule)}</td></tr>
          <tr><td><strong>Severity</strong></td><td>{esc(severity)}</td></tr>
          <tr><td><strong>Confidence</strong></td><td>{esc(conf)}</td></tr>
          <tr><td><strong>Class</strong></td><td>{esc(klass)}</td></tr>
          <tr><td><strong>Method</strong></td><td>{esc(method)}</td></tr>
          <tr><td><strong>File</strong></td><td>{esc(filePath)}</td></tr>
          <tr><td><strong>Line</strong></td><td>{esc(line)}</td></tr>
        </tbody>
      </table>

      <div style={{ margin: '10px 0' }}>
        <strong>Statement:</strong>
        <pre style={{ background: '#f6f8fa', padding: '10px', borderRadius: '6px', overflow: 'auto' }}>
          <code>{esc(statement)}</code>
        </pre>
      </div>

      {codeSnippet && (
        <div style={{ margin: '10px 0' }}>
          <strong>Code Snippet:</strong>
          <pre style={{ background: '#f6f8fa', padding: '10px', borderRadius: '6px', overflow: 'auto' }}>
            <code>{esc(codeSnippet)}</code>
          </pre>
        </div>
      )}
    </div>
  );
}
