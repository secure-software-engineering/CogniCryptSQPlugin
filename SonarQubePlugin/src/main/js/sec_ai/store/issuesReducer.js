import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  projectKey: null,
  issues: [],
  selectedIssue: null,
  sourceCode: null,
  aiSolution: null,
  AiModel: "OPENAI:gpt-4.1",
  Iteration: 2,
  sourceCodeResults: null,
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
  },
});

export const {
  setProjectKey,
  setIssues,
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
export const selectSelectedIssue = (state) => state.issues.selectedIssue;
export const selectSourceCode = (state) => state.issues.sourceCode;
export const selectAiSolution = (state) => state.issues.aiSolution;
export const selectAiModel = (state) => state.issues.AiModel;
export const selectIteration = (state) => state.issues.Iteration; 
export const selectSourceCodeResults = (state) => state.issues.sourceCodeResults;
