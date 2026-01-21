// src/components/MoreInfo.js
import React from 'react';

const esc = (s) =>
  String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

export default function MoreInfo({ issue }) {
  if (!issue) return null;

  const sections = issue?._descriptorSections || [];
  const n = issue?._raw ?? {};
  const hashcode = n?.hashcode ?? '—';
  const preceding = Array.isArray(n?.precedingErrors) ? n.precedingErrors.length : 0;
  const subsequent = Array.isArray(n?.subsequentErrors) ? n.subsequentErrors.length : 0;
  const filePath = n?.reportLocation?.filePath || issue?.component || '—';
  const start = n?.reportLocation?.start ? `${n.reportLocation.start[0]}:${n.reportLocation.start[1]}` : '—';
  const end = n?.reportLocation?.end ? `${n.reportLocation.end[0]}:${n.reportLocation.end[1]}` : '—';

  const pick = (k) => sections.find(s => s.key === k)?.content || '';
  const resources = pick('resources');

  return (
    <div>
      <h4 style={{ marginTop: 0 }}>Context</h4>
      <table style={{ width: '100%', borderCollapse: 'collapse', margin: '10px 0' }}>
        <tbody>
          <tr><td><strong>Hash</strong></td><td>{esc(hashcode)}</td></tr>
          <tr><td><strong>Graph</strong></td><td>{preceding} preceding • {subsequent} subsequent</td></tr>
          <tr><td><strong>Location</strong></td><td>{esc(filePath)} ({esc(start)} → {esc(end)})</td></tr>
        </tbody>
      </table>

      {resources && (
        <>
          <h4>Resources</h4>
          <div dangerouslySetInnerHTML={{ __html: resources }} />
        </>
      )}
    </div>
  );
}
