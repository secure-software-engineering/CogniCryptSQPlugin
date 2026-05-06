import React, {useRef, useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import {
    selectClassesFromFilter,
    selectFileList,
    selectFilter,
    selectIssues,
    setVisibleIssues
} from "../../store/issuesReducer";
import HoverTip from "../errorTree/HoverTip";
import {FiHelpCircle} from "react-icons/fi";
import {FaChevronDown, FaChevronUp} from "react-icons/fa";

export function Filter() {
    const fileList = useSelector(selectFileList);
    console.log(fileList);
    let currentFilter = useSelector(selectFilter);
    const [showFilter, setShowFilter] = useState(false);

    // Check which data is available
    let issuesLoaded = useSelector(selectIssues).length > 0;
    // TODO: check if confidence scores are available -> disable confidence + priority

    // Button handlers
    const toggleFilter = () => setShowFilter(!showFilter);
    const resetFilter = () => setVisibleIssues({});
    const applyFilter = () => {
        let filter = {};

        // Get selected files

        // Get confidence threshold

        // Get priority threshold

        // Get selected severities

        setVisibleIssues(filter);
    };

    return (
        <div>
            <div style={{display: 'grid', gridTemplateColumns: '80% 20%'}}>
        <button
            onClick={toggleFilter}
            style={{...btnStyle('#2b4c7e', '0px'),

                cursor: issuesLoaded ? 'pointer' : 'not-allowed',
                opacity: !issuesLoaded ? 0.7 : 1,
                background: issuesLoaded ? '#2b4c7e' : '#ccc'}}
            disabled={!issuesLoaded}
            >
            Filter
        </button>
        <HoverTip text={"Reset Filter"}>
            <button
                onClick={resetFilter}
                style={{ ...btnStyle('#2b4c7e', '0px'),

                    cursor: issuesLoaded ? 'pointer' : 'not-allowed',
                    opacity: !issuesLoaded ? 0.7 : 1,
                    background: issuesLoaded ? '#2b4c7e' : '#ccc' }}>
                ✖
            </button>
        </HoverTip>
            </div>

        {showFilter && ReactDOM.createPortal(
            <>
                {/* Backdrop */}
                <div
                    onClick={toggleFilter}
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
                            onClick={toggleFilter}
                            style={{ background: 'transparent', border: 'none', fontSize: '16px', cursor: 'pointer' }}>
                            ✖
                        </button>
                    </div>
                    {/* Create filters (in a table for easier alignment) */}
                    <h4 style={{margin: '0 0 8px 0', fontSize: '18px', fontWeight: 600}}>Filter Issues</h4>
                    <table style={{width: '100%', borderCollapse: 'collapse', margin: '16px 0', fontSize: '14px'}}>
                        <tbody>
                            {/* File filter */}
                            <tr>
                                <td>Location:</td>
                                <td>
                                    <ClassSelector fileList={fileList} selector={selectClassesFromFilter}/>
                                </td>
                            </tr>
                            {/* Buttons */}
                            <tr>
                                <td><button
                                    style={btnStyle('#2b4c7e')}
                                    >Reset Filter
                                </button></td>
                                <td><button
                                    style={{...btnStyle('#51c9a6'), justifySelf: 'right' }}
                                    >Apply Filter
                                </button></td>
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
* @param hoverText text to show when hovering over the question mark next to the newly added label. Default: "Select/Deselect Java Classes"
*/
export function ClassSelector({fileList, selector, setter = null, hoverText = null}) {
    const [open, setOpen] = useState(false);
    const dropdownRef = useRef();
    const dispatch = useDispatch();

    // Get the current setting
    const current = useSelector(selector);
    let initialChecked = {};
    // get checked state of each file
    for (let f of fileList) {
        initialChecked[f] = current.length === 0 || current.indexOf('all') !== -1 || current.indexOf(f) !== -1;
    }
    // all selector
    initialChecked.all = current.length === 0 || current.indexOf('all') !== -1;
    const [checkboxes, setCheckboxes] = useState(initialChecked);

    const selectAll = (check) => {
        console.log(check ? "Checking" : "Unchecking", " all classes");
        let newChecked = {};
        for (let f in checkboxes) {
            newChecked[f] = check;
        }
        setCheckboxes(newChecked);
        console.log(newChecked);
    }

    const selectOne = (check, file) => {
        let res = [];
        let newChecked = {};
        console.log(check ? "Checking " : "Unchecking ", file);

        // Get all checkboxes marked as true but only include file if check is true
        for (let f in checkboxes) {
            if (f === file) {
                newChecked[f] = check;
                if (check) res.push(f);
            } else {
                newChecked[f] = checkboxes[f];
                if (checkboxes[f]) res.push(f);
            }
        }

        if (res.length === fileList.length) {
            // Either all files are selected and 'all' isn't
            if (res.indexOf('all') === -1) {
                newChecked.all = true;
                res.push('all');
            } else {
                // Or 'all' was selected and one file was deselected
                newChecked.all = false;
                res = res.filter((f) => f !== 'all');
            }
        }
        setCheckboxes(newChecked);
        console.log(newChecked);
        return res;
    }

    const handleSelect = (file) => {
        if (file === 'all') {
            checkboxes.all ? selectAll(false) : selectAll(true);
            if (setter) {
                dispatch(setter(['all'])); // Deselect all?
            }
        } else {
            const newSelection = checkboxes[file] ? selectOne(false, file) : selectOne(true, file);
            if (setter) {
                dispatch(setter(newSelection));
            }
        }
    };

    const isAllSelected = !current || current.length === 0 || current.includes('all');
    const labelText = isAllSelected
        ? 'Select classes...'
        : `${current.length} selected`;

    return <div style={{ position: 'relative' }} ref={dropdownRef}>
        { hoverText &&
        <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#555', display: 'flex', alignItems: 'center' }}>
            Java Class
            <HoverTip text={hoverText}>
                <FiHelpCircle size={14} style={{ marginLeft: '6px', cursor: 'help' }} />
            </HoverTip>
        </label> }
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
                <label key="all" style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', marginBottom: '6px' }}>
                    <input
                        type="checkbox"
                        checked={checkboxes.all}
                        onChange={() => handleSelect('all')}
                    />
                    Select/Deselect All Classes
                </label>

                {fileList.map(file => (
                    <label key={file} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', marginBottom: '4px' }}>
                        <input
                            type="checkbox"
                            checked={checkboxes[file]}
                            onChange={() => handleSelect(file)}
                        />
                        {file.split('.').pop()} ({file})
                    </label>
                ))}
            </div>
        )}
    </div>
}

const btnStyle = (bg, br = '5px') => ({
    marginTop: '8px',
    padding: '5px 10px',
    fontSize: '12px',
    borderRadius: br,
    border: `1px solid ${bg}`,
    backgroundColor: bg,
    color: 'white',
    cursor: 'pointer'
});