import React from 'react'
import { useState, useEffect } from 'react';
import ErrorVis from '../errorTree/ErrorVis';
import { ReactFlowProvider } from 'reactflow';
import DetailedFix from './DetailedFix';
import CodeGen  from '../codeGeneration/codeGen';
import { useDispatch } from 'react-redux';
import { setSelectedTab } from '../../store/issuesReducer';

function NavigationBar() {
    const dispatch = useDispatch();
    const [activeTab, setActiveTab] = useState('vulnerabilities');
    const [collapsed, setCollapsed] = useState(false);

    const [jumpTarget, setJumpTarget] = useState(null);


    const navItems = [
        // { key: 'report', icon: '📊', label: 'Report' },
        { key: 'vulnerabilities', icon: '🔍', label: 'Vulnerabilities' },
        { key: 'errorTree', icon: '🌳', label: 'Error Tree' },
        { key: 'codeGen', icon: '⚙️', label: 'Code Gen' },
    ];


    // This function is passed to the ErrorVis component.
    // When an issue is clicked there, this function is called.
    const handleJumpToIssue = (issue) => {
        setJumpTarget(issue); // Set the issue that we want to jump to
        setActiveTab('vulnerabilities'); // Switch the view to the Detailed Fix tab
    };

    // This function is passed to the DetailedFix component.
    // It is called after the jump has been handled to reset the state.
    const clearJumpTarget = () => {
        setJumpTarget(null);
    };

    return (
        <div style={styles.container}>
            <div style={{ ...styles.sidebar, width: collapsed ? '60px' : '220px' }}>
                <div style={styles.sidebarHeader}>
                    {!collapsed && <span style={{ flexGrow: 1 }}>SecAI</span>}
                    <span
                        style={styles.toggleIcon}
                        onClick={() => setCollapsed(!collapsed)}
                        title={collapsed ? 'Expand' : 'Collapse'}
                    >
                        {collapsed ? '➡️' : '⬅️'}
                    </span>
                </div>
                <ul style={styles.sidebarNav}>
                    {navItems.map(({ key, icon, label }) => (
                        <li
                            key={key}
                            title={collapsed ? label : ''}
                            style={{
                                ...styles.sidebarItem,
                                ...(activeTab === key ? styles.activeItem : {}),
                                justifyContent: collapsed ? 'center' : 'flex-start'
                            }}
                            onClick={() => setActiveTab(key)}
                        >
                            <span>{icon}</span>
                            {!collapsed && <span>{label}</span>}
                        </li>
                    ))}
                </ul>
            </div>

            <div style={styles.chatPane}>
                {activeTab === 'vulnerabilities' && <DetailedFix jumpTarget={jumpTarget} clearJumpTarget={() => setJumpTarget(null)} />}
                <ReactFlowProvider>
                    {activeTab === 'errorTree' && <ErrorVis onJumpToIssue={handleJumpToIssue} />}
                </ReactFlowProvider>
                {/* {activeTab === 'codeGen' && <CodeGenerationPage />} */}
                {/* Add other tab views here */}
                {activeTab === 'codeGen' && <CodeGen />}
            </div>
        </div>
    );
}

const styles = {
    container: {
        display: 'flex',
        height: 'calc(100vh - 300px)',
        fontFamily: 'Arial, sans-serif'
    },
    sidebar: {
        background: 'rgb(149 180 223)',
        color: '#fff',
        padding: '10px',
        display: 'flex',
        flexDirection: 'column',
        transition: 'width 0.3s ease'
    },
    sidebarHeader: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '20px',
        fontWeight: 'bold',
        marginBottom: '20px',
    },
    toggleIcon: {
        cursor: 'pointer',
        fontSize: '18px',
        padding: '4px'
    },
    sidebarNav: {
        listStyle: 'none',
        padding: 0,
        margin: 0,
        flexGrow: 1
    },
    sidebarItem: {
        padding: '10px 15px',
        margin: '5px 0',
        borderRadius: '6px',
        cursor: 'pointer',
        fontWeight: 'bold',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        fontSize: '15px',
        transition: 'all 0.2s ease'
    },
    activeItem: {
        backgroundColor: '#7C98D3'
    },
    chatPane: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        padding: '10px',
        height: 'inherit',
    },
};


export default NavigationBar