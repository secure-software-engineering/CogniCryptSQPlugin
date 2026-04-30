import React, { useState, useRef } from 'react';
import ReactDOM from 'react-dom';
import { Handle, Position } from 'reactflow';
import { useDispatch, useSelector } from 'react-redux';
import { setAiSolution } from '../../store/issuesReducer';
import AiFix from '../Description/AiFix';

export default function TooltipNode({ data }) {
  const dispatch = useDispatch();
  const [showTooltip, setShowTooltip] = useState(false);
  const [showAiFix, setShowAiFix] = useState(false);
  const [sonarRuleKey, setSonarRuleKey] = useState(null);
  const nodeRef = useRef(null);

  // This is the matched, detailed issue passed down from ErrorVis
  const matchedIssue = data.full;

  const handleViewMoreClick = () => {
    // The matched issue is already here, so just call the jump function
    if (matchedIssue && data.onJumpToIssue) {
      data.onJumpToIssue(matchedIssue);
    } else {
      console.warn("Could not jump to issue, matched issue not found.", data);
    }
  };

  const handleOpenAiFix = () => {
    try {
      let errorType = matchedIssue.errorType;
      if (errorType === 'AlternativeReqPredicateError') {
        errorType = 'RequiredPredicateError';
      } else if (errorType === 'IncompleteOperationError' || errorType === 'TypestateError') {
        errorType = 'OrderError';
      }

      const rule = matchedIssue.rule;
      if (errorType && rule) {
        const ruleSuffix = rule.substring(rule.lastIndexOf('.') + 1);
        const key = `cognicrypt:${errorType}_${ruleSuffix}`;
        setSonarRuleKey(key);
      }
    } catch (e) {
      console.error('[Descriptor] request failed:', e);
    }
    dispatch(setAiSolution(null)); // Clear previous AI solution
    setShowAiFix(true);
  };

  const handleMouseLeave = () => {
    setShowTooltip(false);
    // Do not hide the AI fix modal on mouse leave, only on explicit close
  };

  return (
    <div
      onMouseEnter={() => setShowTooltip(true)}
      onMouseLeave={handleMouseLeave}
      style={{ display: 'inline-block', position: 'relative' }}
    >
      {/* The visible node in the graph */}
      <div
        ref={nodeRef}
        style={{
          padding: 10,
          background: '#fff',
          borderRadius: 8,
          border: '1px solid #ccc',
          fontSize: 12,
          textAlign: 'center',
          zIndex: 1,
        }}
      >
        <Handle type="target" position={Position.Top} id="in" />
        {data.label}
        <Handle type="source" position={Position.Bottom} id="out" />
      </div>

      {/* The hover tooltip */}
      {showTooltip && (
        <div
          style={{
            position: 'absolute',
            top: '85%',
            left: '50%',
            transform: 'translateX(-50%)',
            background: '#fff',
            border: '1px solid #ccc',
            borderRadius: '4px',
            padding: '6px 10px',
            fontSize: '14px',
            boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
            marginTop: '8px',
            zIndex: 10000,
            pointerEvents: 'auto',
            width: '150%',
            textAlign: 'left'
          }}
        >
          <strong>Class: {matchedIssue.class}</strong><br />
          <code>{matchedIssue.codeSnippet}</code><br />
          <strong>{matchedIssue.message}</strong>
          <br />
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: '10px' }}>
            <button
              onClick={handleViewMoreClick}
              style={btnStyle('#2b4c7e')}
            >
              View More
            </button>
            <button
              onClick={handleOpenAiFix}
              style={btnStyle('#51c9a6')}
            >
              AIFix
            </button>
          </div>
        </div>
      )}

      {/* AI Fix Popup using a Portal */}
      {showAiFix && ReactDOM.createPortal(
        <>
          {/* Backdrop */}
          <div
            onClick={() => setShowAiFix(false)}
            style={{
              position: 'fixed',
              top: 0,
              left: 0,
              width: '100vw',
              height: '100vh',
              background: 'rgba(0,0,0,0.4)',
              backdropFilter: 'blur(2px)',
              zIndex: 9998,
            }}
          />

          {/* Centered Popup */}
          <div style={{
            position: 'fixed',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            background: 'white',
            border: '1px solid #aaa',
            borderRadius: '10px',
            padding: '15px',
            zIndex: 9999,
            boxShadow: '0px 4px 12px rgba(0,0,0,0.2)',
            minWidth: '400px',
            maxWidth: '500px',
            animation: 'scaleFade 0.25s ease'
          }}>
            <div style={{ textAlign: 'right' }}>
              <button onClick={() => setShowAiFix(false)} style={{ background: 'transparent', border: 'none', fontSize: '16px', cursor: 'pointer' }}>
                ✖
              </button>
            </div>
            {/* Pass the snippet from the matched issue and the _oldRule from data */}
            <AiFix sourceSnippet={matchedIssue.codeSnippet} customIssue={matchedIssue} _oldRule={sonarRuleKey} />
          </div>
        </>,
        document.body
      )}
    </div>
  );
}

const btnStyle = (bg) => ({
  marginTop: '8px',
  padding: '5px 10px',
  fontSize: '12px',
  borderRadius: '5px',
  border: `1px solid ${bg}`,
  backgroundColor: bg,
  color: 'white',
  cursor: 'pointer'
});