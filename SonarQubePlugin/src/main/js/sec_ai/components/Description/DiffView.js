import React from 'react';
import ReactDiffViewer from 'react-diff-viewer-continued';
// import 'react-diff-viewer-continued/dist/style.css';

const DiffView = ({ oldCode, newCode }) => {
    if (!oldCode || !newCode) {
        return <p>Not enough data to display the diff.</p>;
    }

    return (
        <ReactDiffViewer
            oldValue={oldCode}
            newValue={newCode}
            splitView={true}
            leftTitle="Original Code"
            rightTitle="AI Generated Fix"
            disableWordDiff={true}
        />
    );
};

export default DiffView;