import { useState } from "react";

function HoverTip({ children, text }) {
  const [show, setShow] = useState(false);
  return (
    <div
      onMouseEnter={() => setShow(true)}
      onMouseLeave={() => setShow(false)}
      style={{ position: 'relative', display: 'inline-block' }}
    >
      {children}
      {show && (
        <div style={{
          position: 'absolute',
          bottom: '120%',
          left: '50%',
          transform: 'translateX(-50%)',
          background: '#fff',
          border: '1px solid #ccc',
          borderRadius: '6px',
          padding: '8px 10px',
          whiteSpace: 'pre-line',
          fontSize: '12px',
          zIndex: 1000,
          boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
          minWidth: '220px',
          maxWidth: '280px'
        }}>
          {text}
        </div>
      )}
    </div>
  );
}

export default HoverTip;