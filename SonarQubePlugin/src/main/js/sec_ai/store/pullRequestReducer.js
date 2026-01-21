import { createSlice } from '@reduxjs/toolkit';

const initialState = {
    githubUsername: '',
    githubRepourl: '',
    githubPATtoken: '',
};

const pullRequestSlice = createSlice({
  name: 'pullRequest',
  initialState,
  reducers: {
    setGithubUsername(state, action) {
      state.githubUsername = action.payload;
    },
    setGithubRepourl(state, action) {
      state.githubRepourl = action.payload;
    },
    setGithubPATtoken(state, action) {
      state.githubPATtoken = action.payload;
    },
  },
});

export const { setGithubUsername, setGithubRepourl, setGithubPATtoken } = pullRequestSlice.actions;

export default pullRequestSlice.reducer;

// Selectors
export const selectGithubUsername = (state) => state.pullRequest.githubUsername;
export const selectGithubRepourl = (state) => state.pullRequest.githubRepourl;
export const selectGithubPATtoken = (state) => state.pullRequest.githubPATtoken;