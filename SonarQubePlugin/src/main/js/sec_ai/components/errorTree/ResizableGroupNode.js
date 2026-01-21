import React, { useEffect, useRef } from 'react';
import { useStoreApi } from 'reactflow';

export default function ResizableGroupNode({ id, data }) {
  const ref = useRef(null);
  const store = useStoreApi();

  useEffect(() => {
    const resizeObserver = new ResizeObserver(() => {
      const { updateNodeInternals } = store.getState();
      if (typeof updateNodeInternals === 'function') {
        updateNodeInternals(id);
      }
    });

    if (ref.current) {
      resizeObserver.observe(ref.current);
    }

    return () => resizeObserver.disconnect();
  }, [id, store]);

  return (
    <div
      ref={ref}
      style={{
        border: '2px solid #88f',
        background: '#f5f6ff',
        padding: '8px 12px',
        borderRadius: 6,
        minHeight: 100,
        minWidth: 240,
        position: 'relative',
        overflow: 'visible',
      }}
    >
      <div
        style={{
          position: 'absolute',
          top: -20,
          left: 0,
          fontWeight: 'bold',
          fontSize: '14px',
          background: '#f5f6ff',
          padding: '2px 8px',
          borderTopLeftRadius: 4,
          borderTopRightRadius: 4,
          border: '1px solid #88f',
          borderBottom: 'none',
        }}
      >
        {data.label}
      </div>
    </div>
  );
}
