import { createSlice } from '@reduxjs/toolkit';
import {useSelector} from "react-redux";

const initialState = {
    projectKey: null,
    issues: [],
    selectedIssue: null,
    visibleIssues: [],
    sourceCode: null,
    aiSolution: null,
    AiModel: "OPENAI:gpt-4.1",
    Iteration: 2,
    sourceCodeResults: null,
    fileList: [],
    filter: {},
    sortBy: ["alph-asc", "priority-desc", "severity-desc", "fp-desc"]
};

export const defaultSort = ["alph-asc", "priority-desc", "severity-desc", "fp-desc"]

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
            state.visibleIssues = sortIssues(filterIssues(state.issues, state.filter), state.sortBy); // ??? Use {} instead to reset filter?
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
            state.filter = action.payload.filter;
            state.sortBy = action.payload.sortBy || state.sortBy;
            state.visibleIssues = sortIssues(filterIssues(state.issues, state.filter), state.sortBy);
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
export const selectSeverityFromFilter = (state) => state.issues.filter.severity || [];
export const selectSortBy = (state) => state.issues.sortBy;
export const getIssueStatus = (state) => {
    const issuesLoaded = state.issues.issues.length > 0;
    let fpLoaded = true;
    let priorityLoaded = true;
    if (issuesLoaded) {
        for (let i of state.issues.issues) {
            if (i.fp_score === -1) fpLoaded = false;
            if (i.priority === -1) priorityLoaded = false;
        }
    } else {
        // no issues -> no scores
        fpLoaded = false;
        priorityLoaded = false;
    }
    return {issuesLoaded, fpLoaded, priorityLoaded};
};

function filterIssues(original, filter) {
    let res = [];

    // If the filter is empty select all issues
    if (!filter || Object.keys(filter).length === 0) {
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
            // severity filter
            if (filter.severity) {
                const severity = issue.severity || issue._raw.severity;

                // If the severity isn't part of the filter, skip this issue
                if (severity && filter.severity.indexOf(severity) === -1) {
                    continue;
                }
            }

            // Confidence filter, skip issue if too low
            if (filter.confidence && (1 - issue.fp_score) < filter.confidence) {
                continue;
            }

            // Priority filter, skip issue if too low
            if (filter.priority && issue.priority < filter.priority) {
                continue;
            }

            // If we're still going then the issue matches all filters
            res.push(issue);
        }
    }

    return res;
}

function sortIssues(original, sortBy) {
    // Decide order descending/ascending
    const desc = (a, b) => {
        if (a > b) return -1;
        if (a < b) return 1;
        return 0;
    };
    const asc = (a, b) => {
        if (a < b) return -1;
        if (a > b) return 1;
        return 0;
    };

    // Pick attribute to compare
    const pick = (issue, attr) => {
        switch (attr) {
            case "priority":
                return issue.priority;
            case "severity":
                let sev = issue.severity || issue._raw.severity;
                return sev == "HIGH" ? 3 : (sev == "MEDIUM" ? 2 : (sev == "LOW" ? 1 : (sev == "INFO" ? 0 : 4)));
            case "fp":
                return 1 - issue.fp_score;
            case "alph":
                return issue.class || issue.reportLocation?.className || issue._raw.class || issue._raw.reportLocation.className;
        }
    }

    const createSorter = (sortBy) => (a, b) => {
        for (const rule of sortBy) {
            const [metric, dir] = rule.split("-");
            const cmp = dir === "asc" ? asc : desc;
            const av = pick(a, metric);
            const bv = pick(b, metric);
            const res = cmp(av, bv);
            //console.log(metric, ": ", av, cmp.name, bv, " => ", res);
            if (res !== 0) return res;
        }
        return 0;
    };

    let sorted = [...original].sort(createSorter(sortBy));
    /*let reduceLogs = (issue) => {
        let reducedIssue = {};
        reducedIssue.key = issue.key;
        reducedIssue.class = issue._raw.class;
        reducedIssue.fp = issue.fp_score;
        reducedIssue.priority = issue.priority;
        reducedIssue.severity = issue._raw.severity;
        return reducedIssue;
    }
    console.log(sorted.map(reduceLogs), original.map(reduceLogs));//*/
    return sorted;
}
