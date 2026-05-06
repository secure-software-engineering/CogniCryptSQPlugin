import { createSlice } from '@reduxjs/toolkit';

const initialState = {
    projectKey: null,
    issues: [],
    selectedIssue: null,
    visibleIssues: [],
    sourceCode: null,
    aiSolution: null,
    AiModel: "OPENAI:gpt-4.1",
    Iteration: 1,
    sourceCodeResults: null,
    fileList: [],
    filter: {}
};

const issuesSlice = createSlice({
    name: 'issues',
    initialState,
    reducers: {
        setProjectKey(state, action) {
            state.projectKey = action.payload;
        },
        setIssues(state, action) {
            state.issues = action.payload;

            // Create file list
            let files = new Set();
            for (let issue of state.issues) {
                const file = issue.reportLocation?.className || issue._raw.reportLocation.className;
                files.add(file);
            }
            state.fileList = new Array(...files).sort();

            // Update visible issues
            state.visibleIssues = filterIssues(state.issues, state.filter); // ??? Use {} instead to reset filter?
        },
        setSelectedIssue(state, action) {
            state.selectedIssue = action.payload;
        },
        setSourceCode(state, action) {
            state.sourceCode = action.payload;
        },
        setAiSolution(state, action) {
            state.aiSolution = action.payload;
        },
        clearSelectedIssue(state) {
            state.selectedIssue = null;
            state.sourceCode = null;
            state.aiSolution = null;
        },
        setAiModel(state, action) {
            state.AiModel = action.payload;
        },
        setIteration(state, action) {
            state.Iteration = action.payload;
        },
        setSourceCodeResults: (state, action) => {
            state.sourceCodeResults = action.payload;
        },
        setVisibleIssues(state, action) {
            state.filter = action.payload;
            state.visibleIssues = filterIssues(state.issues, state.filter);
        }
    },
});

export const {
    setProjectKey,
    setIssues,
    setVisibleIssues,
    setSelectedIssue,
    setSourceCode,
    setAiSolution,
    clearSelectedIssue,
    setAiModel,
    setIteration,
    setSourceCodeResults
} = issuesSlice.actions;

export default issuesSlice.reducer;

// Selectors
export const selectProjectKey = (state) => state.issues.projectKey;
export const selectIssues = (state) => state.issues.issues;
export const selectVisibleIssues = (state) => state.issues.visibleIssues;
export const selectSelectedIssue = (state) => state.issues.selectedIssue;
export const selectSourceCode = (state) => state.issues.sourceCode;
export const selectAiSolution = (state) => state.issues.aiSolution;
export const selectAiModel = (state) => state.issues.AiModel;
export const selectIteration = (state) => state.issues.Iteration;
export const selectSourceCodeResults = (state) => state.issues.sourceCodeResults;
export const selectFileList = (state) => state.issues.fileList;
export const selectFilter = (state) => state.issues.filter;
export const selectClassesFromFilter = (state) => state.issues.filter.files || [];

function filterIssues(original, filter) {
    let res = [];

    // If the filter is empty select all issues
    if (filter === {}) {
        res = original.slice();
    } else {
        for (let issue of original) {
            // file filter
            if (filter.files) {
                const file = issue.reportLocation?.className || issue._raw.reportLocation.className;

                // If the file isn't part of the filter, skip this issue
                if (file && filter.files.indexOf(file) === -1) {
                    continue;
                }
            }

            // Confidence filter, skip issue if too low
            if (filter.confidence && issue.fp_score < filter.confidence) {
                continue;
            }

            // If we're still going then the issue matches all filters
            res.push(issue);
        }
    }

    return res;
}
