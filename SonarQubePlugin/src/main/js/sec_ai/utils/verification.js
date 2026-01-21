export const VerificationBadge = ({ verified }) => (
  <span style={{
    display: 'inline-flex',
    alignItems: 'center',
    gap: '6px',
    padding: '4px 8px',
    borderRadius: '999px',
    fontSize: '12px',
    fontWeight: 700,
    background: verified ? '#e6f7ed' : '#fdecec',
    color: verified ? '#1f7a4d' : '#a11b1b',
    border: `1px solid ${verified ? '#a6e2c6' : '#f3b0b0'}`
  }}>
    <span aria-hidden="true">{verified ? '✔️' : '❌'}</span>
    {verified ? 'CogniCrypt Verified' : 'CogniCrypt Not Verified'}
  </span>
);