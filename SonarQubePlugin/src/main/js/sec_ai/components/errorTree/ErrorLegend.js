import React, { useState } from 'react';

const legendItems = [
    { color: '#fdd', label: 'Constraint Error' },
    { color: '#ddf', label: 'RequiredPredicateError' },
    { color: '#ffd', label: 'AlternativeRepPredicateError' },
    { color: '#dfd', label: 'IncompleteOperationError' },
    { color: '#fcb', label: 'ForbiddenMethodError' },
    { color: '#cfc', label: 'ImpreciseValueExtractionError' },
    { color: '#ffc', label: 'TypestateError' },
    { color: '#eee', label: 'Other' },
];

export default function ErrorLegend() {
    const [open, setOpen] = useState(false);

    return (
        <div style={{
            position: 'absolute',
            bottom: 0,
            right: 0,
            background: 'rgba(255,255,255,0.95)',
            border: '1px solid #ccc',
            borderRadius: 8,
            padding: open ? '10px 15px' : '6px 10px',
            boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
            zIndex: 100,
            transition: 'all 0.2s ease'
        }}>
            <div
                onClick={() => setOpen(!open)}
                style={{
                    cursor: 'pointer',
                    fontWeight: 'bold',
                    fontSize: '13px',
                    marginBottom: open ? '8px' : '0',
                    userSelect: 'none',
                }}
            >
                {open ? 'Hide Legend ✖' : 'Show Legend ℹ️'}
            </div>
            {open && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    {legendItems.map((item, idx) => (
                        <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <div style={{
                                width: '16px',
                                height: '16px',
                                backgroundColor: item.color,
                                border: '1px solid #888',
                                borderRadius: '4px',
                                flexShrink: 0
                            }} />
                            <div style={{ fontSize: '12px', color: '#333' }}>{item.label}</div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
