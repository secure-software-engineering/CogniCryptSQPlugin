import { combineReducers, createStore } from 'redux';
import issuesReducer from './issuesReducer';
import errortreeReducer from './errorTreeReducer';
import pullRequestReducer from './pullRequestReducer';

const rootReducer = combineReducers({
    issues: issuesReducer,
    errortree: errortreeReducer,
    pullRequest: pullRequestReducer,
    // Add other reducers here as needed
    // e.g., userReducer, settingsReducer, etc.
});

const store = createStore(rootReducer);

export default store;
