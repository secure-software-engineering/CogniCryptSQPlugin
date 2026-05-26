import { useState } from 'react';
import { useDispatch } from "react-redux";

function CheckBoxFilter({ label = "Filter", options = []}) {
    const [checkedOptions, setCheckedOptions] = useState([]);
    const [dropdownOpen, setDropdownOpen] = useState(false);
    const dispatch = useDispatch();

    const handleCheckboxChange = (option) => {
        if (option === "select all") {
            if (checkedOptions.includes(option)) {
                setCheckedOptions([]);
            } else {
                setCheckedOptions(options);
            }
        } else if (checkedOptions.includes(option)) {
            setCheckedOptions(checkedOptions.filter(item => item !== option));
        } else {
            setCheckedOptions([...checkedOptions, option]);
        }
        dispatch(updateCheckedOptions(checkedOptions));
    };

    const toggleDropdown = () => {
        setDropdownOpen(!dropdownOpen);
    };

    return (
        <div>
            <h2>Filter Options</h2>
            <button onClick={toggleDropdown}>☰ Select {label}</button>
            <ul style={{ display: dropdownOpen ? 'block' : 'none' }}>
                <li>
                    <input
                        type="checkbox"
                        id={`checkbox-all`}
                        value={"Select/Deselect All"}
                        checked={checkedOptions.length === options.length}
                        onChange={() => handleCheckboxChange("select all")}
                        />
                </li>
                {options.map((item, i) => (
                    <li>
                        <input
                            type="checkbox"
                            id={item.id || item.key || i}
                            value={item.value}
                            checked={checkedOptions.includes(item.value)}
                            onChange={() => handleCheckboxChange(item.value)}
                        />
                        {item.value}
                    </li>))
                }
            </ul>
        </div>
    );
}

export default CheckBoxFilter;
