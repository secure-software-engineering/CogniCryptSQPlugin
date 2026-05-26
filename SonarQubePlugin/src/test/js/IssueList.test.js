// src/test/js/IssueList.test.js
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import IssueListItem from '../../main/js/sec_ai/components/Description/IssueListItem';

const renderWithStore = (ui, { selectedIssue = null } = {}) => {
  const preloadedState = { issues: { selectedIssue } };
  const store = configureStore({
    reducer: (state = preloadedState, _action) => state,
    preloadedState,
  });
  return render(<Provider store={store}>{ui}</Provider>);
};

const makeIssue = (overrides = {}) => ({
  key: 'issue-1',
  message: 'Test Issue',
  component: 'src/Main.java',
  ...overrides,
});

describe('IssueList', () => {
  test('renders issue message and file name', () => {
    const issue = makeIssue();
    const handleIssueClick = jest.fn();

    renderWithStore(
      <IssueListItem issue={issue} handleIssueClick={handleIssueClick} />
    );

    // Message shown
    expect(screen.getByText('Test Issue')).toBeInTheDocument();
    // File name derived from component path (“Main.java”)
    expect(screen.getByText('Main.java')).toBeInTheDocument();

    // Data-testid hook exists for clicking in other tests
    expect(screen.getByTestId('issue-issue-1')).toBeInTheDocument();
  });

  test('calls handleIssueClick on mouse click', () => {
    const issue = makeIssue();
    const handleIssueClick = jest.fn();

    renderWithStore(
      <IssueListItem issue={issue} handleIssueClick={handleIssueClick} />
    );

    fireEvent.click(screen.getByTestId('issue-issue-1'));
    expect(handleIssueClick).toHaveBeenCalledTimes(1);
    expect(handleIssueClick).toHaveBeenCalledWith(issue);
  });

  test('calls handleIssueClick on Enter/Space key press (accessibility)', () => {
    const issue = makeIssue();
    const handleIssueClick = jest.fn();

    renderWithStore(
      <IssueListItem issue={issue} handleIssueClick={handleIssueClick} />
    );

    const item = screen.getByTestId('issue-issue-1');

    fireEvent.keyDown(item, { key: 'Enter' });
    fireEvent.keyDown(item, { key: ' ' }); // space
    expect(handleIssueClick).toHaveBeenCalledTimes(2);
  });

  test('applies selected style when this issue is selected in Redux', () => {
    const issue = makeIssue();
    const handleIssueClick = jest.fn();

    const { getByTestId } = renderWithStore(
      <IssueListItem issue={issue} handleIssueClick={handleIssueClick} />,
      { selectedIssue: issue } // mark it selected
    );

    const li = getByTestId('issue-issue-1');

    // Because styles are inline in the component, we can check style attributes directly.
    // selectedCard uses backgroundColor #E5DFDF and borderColor #000000.
    expect(li).toHaveStyle({ backgroundColor: '#E5DFDF' });
    expect(li).toHaveStyle({ borderColor: '#000000' });
  });

  test('does not crash if component is missing; file name becomes empty', () => {
    const issue = makeIssue({ component: undefined });
    const handleIssueClick = jest.fn();

    renderWithStore(
      <IssueListItem issue={issue} handleIssueClick={handleIssueClick} />
    );

    // Message is there
    expect(screen.getByText('Test Issue')).toBeInTheDocument();

    // Subtitle div exists, but empty (no filename)
    const subtitles = screen.getAllByText((_, node) => {
      return node?.tagName.toLowerCase() === 'div' && node.textContent === '';
    });
    expect(subtitles.length).toBeGreaterThan(0);
  });
});
