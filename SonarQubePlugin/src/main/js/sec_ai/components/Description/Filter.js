import React, {useRef, useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import {selectFileList} from "../../store/issuesReducer";
import HoverTip from "../errorTree/HoverTip";
import {FiHelpCircle} from "react-icons/fi";
import {FaChevronDown, FaChevronUp} from "react-icons/fa";

export function Filter() {
    const fileList = useSelector(selectFileList);
    const [showFilter, setShowFilter] = useState(false);

    return (<div>
        <button>
            onClick={() => setShowFilter(true)}
            style={{
                marginTop: '8px',
                padding: '5px 10px',
                fontSize: '12px',
                borderRadius: '5px',
                border: `1px solid #2b4c7e`,
                backgroundColor: '#2b4c7e',
                color: 'white',
                cursor: 'pointer'
            }}
        </button>

        {showFilter && ReactDOM.createPortal(
            <>
                {/* Backdrop */}
                <div
                    onClick={() => setShowFilter(false)}
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
                        {/* Exit button in the corner */}
                        <button
                            onClick={() => setShowFilter(false)}
                            style={{ background: 'transparent', border: 'none', fontSize: '16px', cursor: 'pointer' }}>
                            ✖
                        </button>
                    </div>
                    {/* Create filters (in a table for easier alignment) */}
                    <h4 style="margin:0 0 8px 0; font-size:18px; font-weight:600;">Filter Issues</h4>
                    <table style="width:100%; border-collapse:collapse; margin:16px 0; font-size:14px;">
                        <tbody>
                            {/* File filter */}
                            <tr>
                                <td>Location:</td>
                                <td>
                                    <ClassSelector fileList={fileList} selector={} setter={}/>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </>,
            document.body
        )}
        </div>
    );
}

/* Java Class Multi-Select
* @param fileList list of classes that can be selected
* @param selector React selector to get selected classes
* @param setter React reducer to dispatch update to selected classes
* @param hoverText text to show when hovering over the question mark. Default: "Select/Deselect Java Classes"
*/
export function ClassSelector(fileList, selector, setter, hoverText = "Select/Deselect Java Classes") {
    const [open, setOpen] = useState(false);
    const dropdownRef = useRef();
    const dispatch = useDispatch();

    // Get the current setting
    const current = useSelector(selector);

    const handleSelect = (file) => {
        if (file === 'all') {
            dispatch(setter(['all'])); // Deselect all?
        } else {
            const newSelection = current.includes(file)
                ? current.filter(f => f !== file)
                : [...current.filter(f => f !== 'all'), file];
            dispatch(setter(newSelection));
        }
    };

    const handleReset = () => {
        dispatch(setter(['all']));
    };

    const isAllSelected = !current || current.length === 0 || current.includes('all');
    const labelText = isAllSelected
        ? 'Select classes...'
        : `${current.length} selected`;

    return <div style={{ position: 'relative' }} ref={dropdownRef}>
        <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#555', display: 'flex', alignItems: 'center' }}>
            Java Class
            <HoverTip text={hoverText}>
                <FiHelpCircle size={14} style={{ marginLeft: '6px', cursor: 'help' }} />
            </HoverTip>
        </label>
        <div
            onClick={() => setOpen(prev => !prev)}
            style={{
                padding: '6px 10px',
                borderRadius: '6px',
                border: '1px solid #ccc',
                fontSize: '12px',
                minWidth: '180px',
                background: '#fff',
                cursor: 'pointer',
                marginTop: '4px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
            }}
        >
            <span>{labelText}</span>
            {open ? <FaChevronUp size={12} /> : <FaChevronDown size={12} />}
        </div>

        {open && (
            <div style={{
                position: 'absolute',
                bottom: '70%',
                left: 0,
                background: '#fff',
                border: '1px solid #ccc',
                borderRadius: '6px',
                padding: '8px',
                marginTop: '4px',
                maxHeight: '200px',
                overflowY: 'auto',
                zIndex: 20,
                boxShadow: '0 2px 5px rgba(0,0,0,0.15)',
                minWidth: '200px'
            }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', marginBottom: '6px' }}>
                    <input
                        type="checkbox"
                        checked={current.includes('all')}
                        onChange={() => handleSelect('all')}
                    />
                    All Classes
                </label>

                {fileList.map(file => (
                    <label key={file} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', marginBottom: '4px' }}>
                        <input
                            type="checkbox"
                            checked={current.includes(file)}
                            onChange={() => handleSelect(file)}
                        />
                        {file.split('.').pop()} ({file})
                    </label>
                ))}

                <div style={{ marginTop: '8px', textAlign: 'right' }}>
                    <button
                        onClick={handleReset} // Instead make first item select/deselect?
                        style={{
                            fontSize: '12px',
                            padding: '4px 8px',
                            background: '#f44336',
                            color: '#fff',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer'
                        }}
                    >
                        Reset All
                    </button>
                </div>
            </div>
        )}
    </div>
}