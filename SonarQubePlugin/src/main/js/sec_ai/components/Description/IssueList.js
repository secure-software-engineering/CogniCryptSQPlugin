import React from 'react'
import { selectSelectedIssue } from '../../store/issuesReducer';
import { useSelector } from 'react-redux';

function IssueList({ key, issue, handleIssueClick }) {
    const selectedIssue = useSelector(selectSelectedIssue);

    // Safe filename extraction even if component is missing
    const fileName = (issue?.component || '').split('/').pop() || '';

    return (
        <li
            data-testid={`issue-${issue.key}`}
            key={key}
            onClick={() => handleIssueClick(issue)}
            onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') handleIssueClick(issue);
            }}
            style={{
                ...styles.issueCard,
                ...(selectedIssue && selectedIssue.key === issue.key ? styles.selectedCard : {}),
            }}
        >
            <div style={styles.issueMessage}>{issue.message}</div>
            <div style={styles.issueSubtitle}>{fileName}</div>
        </li>
    )
}

const styles = {
    issueCard: {
        backgroundColor: '#f0eeee',
        border: '1px solid #555',
        borderRadius: '10px',
        padding: '10px',
        cursor: 'pointer',
        transition: 'all 0.2s ease',
    },
    selectedCard: {
        backgroundColor: '#E5DFDF',
        borderColor: '#000000'
    },
}

export default IssueList