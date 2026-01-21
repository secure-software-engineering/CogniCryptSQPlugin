import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  viewMode: ['all'],         // Default to full view
  colorTheme: 'colored',     // Default to color-coded view
  highlightMode: 'chain'
};

const errorTreeSlice = createSlice({
  name: 'errortree',
  initialState,
  reducers: {
    setViewMode(state, action) {
      state.viewMode = action.payload;
    },
    setColorTheme(state, action) {
      state.colorTheme = action.payload;
    },
    resetViewSettings(state) {
      state.viewMode = ['all'];
      state.colorTheme = 'colored';
    },
    setHighlightMode(state, action) {
      state.highlightMode = action.payload;
    }
  },
});

export const {
  setViewMode,
  setColorTheme,
  resetViewSettings,
  setHighlightMode
} = errorTreeSlice.actions;

export default errorTreeSlice.reducer;

// Selectors
export const selectViewMode = (state) => state.errortree.viewMode;
export const selectColorTheme = (state) => state.errortree.colorTheme;
export const selectHighlightMode = (state) => state.errortree.highlightMode;