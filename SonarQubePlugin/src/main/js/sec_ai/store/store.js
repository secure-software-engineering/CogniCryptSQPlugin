import { combineReducers, createStore } from 'redux';
import issuesReducer from './issuesReducer';
import errortreeReducer from './errorTreeReducer';
import pullRequestReducer from './pullRequestReducer';
import {configureStore} from "@reduxjs/toolkit";

const store = configureStore({
    reducer: {
        issues: issuesReducer,
        errortree: errortreeReducer,
        pullRequest: pullRequestReducer,
        // Add other reducers here as needed
        // e.g., userReducer, settingsReducer, etc.
    }
})

export default store;
