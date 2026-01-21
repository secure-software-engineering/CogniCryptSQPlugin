import React, { useState, useRef, useEffect } from 'react';
import {
  useReactFlow,
  ControlButton
} from 'reactflow';
import { MdZoomIn, MdZoomOut, MdZoomOutMap } from 'react-icons/md';
import { FaChevronDown, FaChevronUp } from 'react-icons/fa';
import { FiHelpCircle } from 'react-icons/fi';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectColorTheme,
  selectViewMode,
  selectHighlightMode,
  setColorTheme,
  setViewMode,
  setHighlightMode
} from '../../store/errorTreeReducer';
import HoverTip from './HoverTip';



function HorizontalControls({ fileList }) {
  const { zoomIn, zoomOut, fitView } = useReactFlow();
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef();

  const dispatch = useDispatch();
  const viewMode = useSelector(selectViewMode);
  const colorTheme = useSelector(selectColorTheme);
  const highlightMode = useSelector(selectHighlightMode);

  const handleToggle = () => setOpen(prev => !prev);

  const handleSelect = (file) => {
    if (file === 'all') {
      dispatch(setViewMode(['all']));
    } else {
      const newSelection = viewMode.includes(file)
        ? viewMode.filter(f => f !== file)
        : [...viewMode.filter(f => f !== 'all'), file];
      dispatch(setViewMode(newSelection));
    }
  };

  const handleReset = () => {
    dispatch(setViewMode(['all']));
  };

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const isAllSelected = !viewMode || viewMode.length === 0 || viewMode.includes('all');
  const labelText = isAllSelected
    ? 'Select classes...'
    : `${viewMode.length} selected`;

  return (
    <div style={{
      position: 'absolute',
      bottom: '0',
      left: '50%',
      transform: 'translateX(-50%)',
      zIndex: 10,
      display: 'flex',
      alignItems: 'center',
      background: 'rgba(255, 255, 255, 0.95)',
      borderRadius: '8px',
      padding: '8px 12px',
      boxShadow: '0 2px 5px rgba(0,0,0,0.15)',
      gap: '16px'
    }}>
      {/* Zoom Controls */}
      <div style={{ display: 'flex', gap: '6px' }}>
        <ControlButton title="Zoom In" onClick={zoomIn}><MdZoomIn size={18} /></ControlButton>
        <ControlButton title="Zoom Out" onClick={zoomOut}><MdZoomOut size={18} /></ControlButton>
        <ControlButton title="Fit View" onClick={fitView}><MdZoomOutMap size={18} /></ControlButton>
      </div>

      {/* Divider */}
      <div style={{
        width: '1px',
        background: '#ccc',
        margin: '0 8px',
        height: '40px'
      }} />

      {/* Theme Toggle */}
      <div style={{ display: 'flex', flexDirection: 'column' }}>
        <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#555', display: 'flex', alignItems: 'center' }}>
          Theme
          <HoverTip text={"Switch between color-coded error types and neutral gray styling."}>
            <FiHelpCircle size={14} style={{ marginLeft: '6px', cursor: 'help' }} />
          </HoverTip>
        </label>
        <select
          value={colorTheme}
          onChange={(e) => dispatch(setColorTheme(e.target.value))}
          style={{
            padding: '6px 10px',
            borderRadius: '6px',
            border: '1px solid #ccc',
            fontSize: '12px',
            background: '#fff',
            marginTop: '4px'
          }}
        >
          <option value="colored">Error Colors</option>
          <option value="plain">Plain Theme</option>
        </select>
      </div>

      {/* Divider */}
      <div style={{
        width: '1px',
        background: '#ccc',
        margin: '0 8px',
        height: '40px'
      }} />

      {/* Highlight Mode */}
      <div style={{ display: 'flex', flexDirection: 'column' }}>
        <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#555', display: 'flex', alignItems: 'center' }}>
          Highlight
          <HoverTip text={
            "What to highlight when clicking an error node:\n" +
            "- Ancestor: Shows the path up to the root\n" +
            "- Descendants: Shows all resulting errors\n" +
            "- Chain: Shows both directions"
          }>
            <FiHelpCircle size={14} style={{ marginLeft: '6px', cursor: 'help' }} />
          </HoverTip>
        </label>
        <select
          value={highlightMode}
          onChange={(e) => dispatch(setHighlightMode(e.target.value))}
          style={{
            padding: '6px 10px',
            borderRadius: '6px',
            border: '1px solid #ccc',
            fontSize: '12px',
            background: '#fff',
            marginTop: '4px'
          }}
        >
          <option value="chain">Entire Chain</option>
          <option value="ancestor">Only Ancestors</option>
          <option value="children">Only Descendants</option>
        </select>
      </div>

      {/* Divider */}
      <div style={{
        width: '1px',
        background: '#ccc',
        margin: '0 8px',
        height: '40px'
      }} />

      {/* Java Class Multi-Select */}
      <div style={{ position: 'relative' }} ref={dropdownRef}>
        <label style={{ fontSize: '12px', fontWeight: 'bold', color: '#555', display: 'flex', alignItems: 'center' }}>
          Java Class
          <HoverTip text={"Filter the error graph by Java class. Only selected class paths will be shown."}>
            <FiHelpCircle size={14} style={{ marginLeft: '6px', cursor: 'help' }} />
          </HoverTip>
        </label>
        <div
          onClick={handleToggle}
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
                checked={viewMode.includes('all')}
                onChange={() => handleSelect('all')}
              />
              All Classes
            </label>

            {fileList.map(file => (
              <label key={file} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', marginBottom: '4px' }}>
                <input
                  type="checkbox"
                  checked={viewMode.includes(file)}
                  onChange={() => handleSelect(file)}
                />
                {file.split('.').pop()} ({file})
              </label>
            ))}

            <div style={{ marginTop: '8px', textAlign: 'right' }}>
              <button
                onClick={handleReset}
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
    </div>
  );
}

export default HorizontalControls;
