import React, {useRef, useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import {
    defaultSort,
    getIssueStatus,
    selectClassesFromFilter,
    selectFileList,
    selectFilter,
    selectIssues, selectSeverityFromFilter, selectSortBy,
    setVisibleIssues
} from "../../store/issuesReducer";
import HoverTip from "../errorTree/HoverTip";
import {FiHelpCircle} from "react-icons/fi";
import {FaChevronDown, FaChevronUp} from "react-icons/fa";

export function Filter() {
    const fileList = useSelector(selectFileList);
    const severityList = ["HIGH", "MEDIUM", "LOW", "INFO"];
    let currentFilter = useSelector(selectFilter);
    let currentSortBy = useSelector(selectSortBy);
    const [showFilter, setShowFilter] = useState(false);
    const [confidence, setConfidence] = useState(currentFilter.confidence || 0);
    const [priority, setPriority] = useState(currentFilter.priority || 0);

    const dispatch = useDispatch();

    // Check which data is available
    const issueStatus = useSelector(getIssueStatus);

    // Button handlers
    const toggleFilter = () => setShowFilter(!showFilter);
    const resetFilterAndApply = () => setVisibleIssues({filter: {}});
    const resetFilter = () => {
        const filterForm = document.forms["filter"];

        // Can't figure out how to reset the selection for the classes dropdown
        const classesAll = filterForm["classes"].children[0].children[0];
        console.log(classesAll);
        if (!classesAll.checked) {
            classesAll.checked = true;
            const event = new Event("change", {bubbles: true});
            event.simulated = true;
            classesAll.dispatchEvent(event);
        }

        // Reset confidence and priority
        setConfidence(0);
        setPriority(0);
    }
    const resetSort = () => {
        let sortForm = document.forms["sort"];
        for (let i = 1; i <= defaultSort.length; i++) {
            const metric = defaultSort[i - 1];
            const [sort, dir] = metric.split("-");
            sortForm["sort" + i].value = sort;
            sortForm["dir" + i].value = dir;
        }
    };
    const applyFilter = () => {
        let filter = {};
        const filterForm = document.forms["filter"];

        // Get selected files
        const fileSet = filterForm["classes"].children;
        let files = [];
        for (let f of fileSet) {
            const file = f.children[0];
            if (file.id && file.checked) {
                files.push(file.id);
            }
        }
        if (files.length > 0 && files.length < fileList.length) filter.files = files;

        // Get selected severities
        const severitySet = filterForm["severity"].children;
        let severity = [];
        for (let s of severitySet) {
            const sev = s.children[0];
            if (sev.id && sev.checked) {
                severity.push(sev.id);
            }
        }
        if (severity.length > 0 && severity.length < severityList.length) filter.severity = severity;

        // Get confidence threshold (/100 because it's percent)
        const confidence = filterForm["confidence"].value / 100;
        if (confidence > 0) filter.confidence = confidence;

        // Get priority threshold
        const priority = filterForm["priority"].value;
        if (priority > 0) filter.priority = priority;

        let sortBy = [];
        // Get sort order
        let sortForm = document.forms["sort"];
        sortBy.push(sortForm["sort1"].value + "-" + sortForm["dir1"].value);
        sortBy.push(sortForm["sort2"].value + "-" + sortForm["dir2"].value);
        sortBy.push(sortForm["sort3"].value + "-" + sortForm["dir3"].value);
        sortBy.push(sortForm["sort4"].value + "-" + sortForm["dir4"].value);

        setShowFilter(false);
        dispatch(setVisibleIssues({filter, sortBy}));
    };

    return (
        <div>
            <div style={{display: 'grid', gridTemplateColumns: '80% 20%'}}>
        <button
            onClick={toggleFilter}
            style={{...btnStyle('#2b4c7e', '0px'),

                cursor: issueStatus.issuesLoaded ? 'pointer' : 'not-allowed',
                opacity: !issueStatus.issuesLoaded ? 0.7 : 1,
                background: issueStatus.issuesLoaded ? '#2b4c7e' : '#ccc'}}
            disabled={!issueStatus.issuesLoaded}
            >
            Filter
        </button>
        <HoverTip text={"Reset Filter"}>
            <button
                onClick={resetFilterAndApply}
                style={{ ...btnStyle('#2b4c7e', '0px'),

                    cursor: issueStatus.issuesLoaded ? 'pointer' : 'not-allowed',
                    opacity: !issueStatus.issuesLoaded ? 0.7 : 1,
                    background: issueStatus.issuesLoaded ? '#2b4c7e' : '#ccc' }}>
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
                    maxWidth: '600px',
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
                    <form name={'filter'}>
                    <table style={{width: '100%', borderSpacing: '10px', margin: '16px 0', fontSize: '14px'}}>
                        <tbody>
                            {/* File filter */}
                            <tr>
                                <td>Location:</td>
                                <td>
                                    <Selector id={"classes"} list={fileList} selector={selectClassesFromFilter}/>
                                </td>
                            </tr>
                            {/* Severity Score */}
                            <tr>
                                <td>Severity:</td>
                                <td><Selector id={"severity"} type={"Severities"} list={severityList} selector={selectSeverityFromFilter} /></td>
                            </tr>
                            {/* Confidence Threshold */}
                            <tr>
                                <td>Confidence Score Threshold:</td>
                                <td><input type={"range"}
                                    id={"confidence"} name={"confidence"}
                                    value={confidence}
                                    onChange={(e) => setConfidence(e.target.value)}
                                    disabled={!issueStatus.fpLoaded}
                                /> {confidence}%</td>
                            </tr>
                            {/* Priority Threshold */}
                            <tr>
                                <td>Priority Score Threshold:</td>
                                <td><input type={"range"}
                                    id={"priority"} name={"priority"}
                                    min={"0"} max={"1"} step={"0.01"}
                                    value={priority}
                                    onChange={(e) => setPriority(e.target.value)}
                                    disabled={!issueStatus.priorityLoaded}
                                /> {priority}</td>
                            </tr>
                            {/* Reset Button */}
                            <tr>
                                <td><button
                                    style={btnStyle('#b22222')}
                                    type="button"
                                    onClick={resetFilter}
                                    >Reset Filter
                                </button></td>
                                <td></td>
                            </tr>
                        </tbody>
                    </table>
                    </form>
                    {/* Sorting (in a table for easier alignment) */}
                    <h4 style={{margin: '0 0 8px 0', fontSize: '18px', fontWeight: 600}}>Sort By</h4>
                    <form name={'sort'}>
                    <table style={{width: '100%', borderSpacing: '10px', margin: '16px 0', fontSize: '14px'}}>
                        <tbody>
                        {/* Sort 1 */}
                        <tr>
                            <td>1.</td>
                            <td><select name={"sort1"} id={"sort1"} defaultValue={currentSortBy[0].split("-")[0]} style={dropdownStyleClosed}>
                                <option value={"alph"} style={optionStyle}>Alphabetically (by fully qualified class name)</option>
                                <option value={"fp"} style={optionStyle}>Confidence Score</option>
                                <option value={"priority"} style={optionStyle}>Priority Score</option>
                                <option value={"severity"} style={optionStyle}>Severity Score</option>
                            </select></td>
                            <td><select name={"dir1"} id={"dir1"} defaultValue={currentSortBy[0].split("-")[1]} style={dropdownStyleClosed}>
                                <option value={"asc"} style={optionStyle}>ascending</option>
                                <option value={"desc"} style={optionStyle}>descending</option>
                            </select></td>
                        </tr>
                        {/* Sort 2 */}
                        <tr>
                            <td>2.</td>
                            <td><select name={"sort2"} id={"sort2"} defaultValue={currentSortBy[1].split("-")[0]} style={dropdownStyleClosed}>
                                <option value={"alph"} style={optionStyle}>Alphabetically (by fully qualified class name)</option>
                                <option value={"fp"} style={optionStyle}>Confidence Score</option>
                                <option value={"priority"} style={optionStyle}>Priority Score</option>
                                <option value={"severity"} style={optionStyle}>Severity Score</option>
                            </select></td>
                            <td><select name={"dir2"} id={"dir2"} defaultValue={currentSortBy[1].split("-")[1]} style={dropdownStyleClosed}>
                                <option value={"asc"} style={optionStyle}>ascending</option>
                                <option value={"desc"} style={optionStyle}>descending</option>
                            </select></td>
                        </tr>
                        {/* Sort 3 */}
                        <tr>
                            <td>3.</td>
                            <td><select name={"sort3"} id={"sort3"} defaultValue={currentSortBy[2].split("-")[0]} style={dropdownStyleClosed}>
                                <option value={"alph"} style={optionStyle}>Alphabetically (by fully qualified class name)</option>
                                <option value={"fp"} style={optionStyle}>Confidence Score</option>
                                <option value={"priority"} style={optionStyle}>Priority Score</option>
                                <option value={"severity"} style={optionStyle}>Severity Score</option>
                            </select></td>
                            <td><select name={"dir3"} id={"dir3"} defaultValue={currentSortBy[2].split("-")[1]} style={dropdownStyleClosed}>
                                <option value={"asc"} style={optionStyle}>ascending</option>
                                <option value={"desc"} style={optionStyle}>descending</option>
                            </select></td>
                        </tr>
                        {/* Sort 4 */}
                        <tr>
                            <td>4.</td>
                            <td><select name={"sort4"} id={"sort4"} defaultValue={currentSortBy[3].split("-")[0]} style={dropdownStyleClosed}>
                                <option value={"alph"} style={optionStyle}>Alphabetically (by fully qualified class name)</option>
                                <option value={"fp"} style={optionStyle}>Confidence Score</option>
                                <option value={"priority"} style={optionStyle}>Priority Score</option>
                                <option value={"severity"} style={optionStyle}>Severity Score</option>
                            </select></td>
                            <td><select name={"dir4"} id={"dir4"} defaultValue={currentSortBy[3].split("-")[1]} style={dropdownStyleClosed}>
                                <option value={"asc"} style={optionStyle}>ascending</option>
                                <option value={"desc"} style={optionStyle}>descending</option>
                            </select></td>
                        </tr>
                        {/* Reset Button */}
                        <tr>
                            <td><button
                                style={btnStyle('#b22222')}
                                type="button"
                                onClick={resetSort}
                                >Reset Sort
                            </button></td>
                            <td></td>
                        </tr>
                        </tbody>
                    </table>
                    </form>
                    <button
                        style={btnStyle('#51c9a6')}
                        onClick={applyFilter}
                        >Apply Filter and Sort
                    </button>
                </div>
            </>,
            document.body
        )}
        </div>
    );
}

/* Multi-Select
* @param id the id used to identify the different inputs in the list
* @param type what the user is selecting (plural). Used for label text
* @param fileList list of classes that can be selected
* @param selector React selector to get current selection
* @param setter React reducer to dispatch update to selection and rerender
* @param hoverText text to show when hovering over the question mark next to the newly added label. Default doesn't include a label
*/
export function Selector({id = "f", type = "Classes", list, selector, setter = null, hoverText = null}) {
    const [open, setOpen] = useState(false);
    const dropdownRef = useRef();
    const dispatch = useDispatch();

    // Get the current setting
    const current = useSelector(selector);
    let initialChecked = {};
    // get checked state of each element
    for (let e of list) {
        initialChecked[e] = current.length === 0 || current.indexOf('all') !== -1 || current.indexOf(e) !== -1;
    }
    // all selector
    initialChecked.all = current.length === 0 || current.indexOf('all') !== -1;
    const [checkboxes, setCheckboxes] = useState(initialChecked);

    /* @param check boolean that the checkboxes should be set to */
    const selectAll = (check) => {
        let newChecked = {};
        for (let e in checkboxes) {
            newChecked[e] = check;
        }
        console.log(newChecked);
        setCheckboxes(newChecked);
    }

    /* @param check boolean that the checkbox should be set to
    *  @param element checkbox element to be selected/deselected */
    const selectOne = (check, element) => {
        let res = [];
        let newChecked = {};

        // Get all checkboxes marked as true but only include element if check is true
        for (let e in checkboxes) {
            if (e === element) {
                newChecked[e] = check;
                if (check) res.push(e);
            } else {
                newChecked[e] = checkboxes[e];
                if (checkboxes[e]) res.push(e);
            }
        }

        if (res.length === list.length) {
            // Either all elements are selected and 'all' isn't
            if (res.indexOf('all') === -1) {
                newChecked.all = true;
                res.push('all');
            } else {
                // Or 'all' was selected and one element was deselected
                newChecked.all = false;
                res = res.filter((f) => f !== 'all');
            }
        }
        setCheckboxes(newChecked);
        return res;
    }

    const handleSelect = (element) => {
        if (element === 'all') {
            checkboxes.all ? selectAll(false) : selectAll(true);
            if (setter) {
                dispatch(setter(['all'])); // Deselect all?
            }
        } else {
            const newSelection = checkboxes[element] ? selectOne(false, element) : selectOne(true, element);
            if (setter) {
                dispatch(setter(newSelection));
            }
        }
    };

    const isAllSelected = !current || current.length === 0 || current.includes('all');
    const labelText = isAllSelected
        ? `Select ${type}...`
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

        <fieldset id={id} name={id} style={{
            display: open ? 'block' : 'none',
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
                    key={`${id}All`}
                    checked={checkboxes.all}
                    onChange={() => handleSelect('all')}
                />
                Select/Deselect All {type}
            </label>

            {list.map(element => (
                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', marginBottom: '4px' }}>
                    <input
                        id={element}
                        type="checkbox"
                        checked={checkboxes[element]}
                        onChange={() => handleSelect(element)}
                    />
                    {element.split('.').pop()} {element.indexOf('.') !== -1 ? "(" + element + ")" : ""}
                </label>
            ))}
        </fieldset>
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

const dropdownStyleClosed = {
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
};
const optionStyle = {
    padding: '6px 10px',
    borderRadius: '6px',
    fontSize: '13px',
    background: '#fff',
    marginBottom: '4px',
};