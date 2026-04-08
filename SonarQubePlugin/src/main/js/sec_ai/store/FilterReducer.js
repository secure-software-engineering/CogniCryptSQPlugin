const initialState = {
    checkedOptions: []
};

export function filterReducer(state = initialState, action) {
    switch (action.type) {
        case 'UPDATE_CHECKED_OPTIONS':
            return { ...state, checkedOptions: [...action.payload] };
        default:
            return state;
    }
}
