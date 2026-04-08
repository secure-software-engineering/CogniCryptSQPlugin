import React, {useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import styles from './QuickFixCard';

export function Filter({fileList = []}) {
    const [idx, setIdx] = useState(0);

    return <div style={{}}>
        <label htmlFor={"file-select"} style={styles.dropdownLabel}>File:</label>
        <select
            id={"file-select"}
            value={idx}
            onChange={(e) => setIdx(parseInt(e.target.value, 10))}
            style={styles.dropdown}
            multiple
        >
            {fileList.map((file, i) =>
                <option key={i} value={i}>
                    {file}
                </option>
            )}
        </select>

        <label htmlFor={"cwe-select"} style={styles.dropdownLabel}>File:</label>
        <select
            id={"cwe-select"}
            value={idx}
            onChange={(e) => setIdx(parseInt(e.target.value, 10))}
            style={styles.dropdown}
        >
            {fileList.map((file, i) =>
                <option key={i} value={i}>
                    {file}
                </option>
            )}
        </select>
    </div>
}