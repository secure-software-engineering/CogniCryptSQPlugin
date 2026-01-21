import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import DetailedFix from '../../main/js/sec_ai/components/Navigations/DetailedFix';
import issuesReducer from '../../main/js/sec_ai/store/issuesReducer';
import * as api from '../../main/js/sec_ai/components/APIs/api';

// --- API mocks ---
jest.mock('../../main/js/sec_ai/components/APIs/api', () => ({
  fetchIssues: jest.fn(),
  fetchSourceCode: jest.fn(),
  getRuleDescriptor: jest.fn(),
  findProjects: jest.fn(),
  getActiveproject: jest.fn(),
}));

// --- Child mocks ---
jest.mock('../../main/js/sec_ai/components/Description/IssueList', () => {
  return function MockIssueList({ issue, handleIssueClick }) {
    // NOTE: renders data-testid="issue-issue-1"
    return (
      <button
        data-testid={`issue-${issue.key}`}
        onClick={() => handleIssueClick(issue)}
      >
        {issue.message}
      </button>
    );
  };
});

jest.mock('../../main/js/sec_ai/components/Description/AiFix', () => {
  return function MockAiFix({ sourceSnippet }) {
    return <div data-testid="mock-ai-fix">{sourceSnippet || 'no-snippet'}</div>;
  };
});

jest.mock('../../main/js/sec_ai/components/Description/DetailedDescription', () => {
  return function MockDetailedDescription({ description }) {
    return <div data-testid="mock-detailed-description">{description || 'no-description'}</div>;
  };
});

// --- Store helpers ---
const makeStore = (preloadedState) =>
  configureStore({
    reducer: { issues: issuesReducer },
    preloadedState,
  });

const renderWithStore = (ui) =>
  render(<Provider store={makeStore()}>{ui}</Provider>);

// --- Shared data ---
const MOCK_ISSUES = [
  {
    key: 'issue-1',
    message: 'Test Issue',
    component: 'src/Main.java',
    line: 42,
    rule: 'java:S1234',
  },
];

beforeEach(() => {
  jest.clearAllMocks();

  // Provide ?id=... so the component doesn't early-return
  window.history.pushState({}, 'Test', '/?id=demo');

  api.findProjects.mockResolvedValue([{ key: 'demo', name: 'Demo' }]);
  api.getActiveproject.mockReturnValue({ key: 'demo', name: 'Demo' });

  // DetailedFix expects an ARRAY
  api.fetchIssues.mockResolvedValue(MOCK_ISSUES);
  api.fetchSourceCode.mockResolvedValue('Code snippet');
  api.getRuleDescriptor.mockResolvedValue({
    rules: [
      {
        descriptionSections: [
          { key: 'root_cause', content: 'Root cause description' },
          { key: 'how_to_fix', content: 'How to fix text' },
          { key: 'resources', content: 'Resource links' },
        ],
      },
    ],
  });

  // Silence the harmless warn during initial render
  jest.spyOn(console, 'warn').mockImplementation((msg) => {
    if (String(msg).includes('No rule selected')) return;
    // eslint-disable-next-line no-console
    console.warn(msg);
  });
});

afterEach(() => {
  // restore warn spy
  jest.restoreAllMocks();
});

describe('DetailedFix', () => {
  it('renders loading then shows fetched issues', async () => {
    renderWithStore(<DetailedFix jumpTarget={null} clearJumpTarget={() => {}} />);

    expect(screen.getByText('Loading issues...')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('Test Issue')).toBeInTheDocument());
  });

  it('handles issue click and loads source code (snippet visible in AiFix tab)', async () => {
    renderWithStore(<DetailedFix jumpTarget={null} clearJumpTarget={() => {}} />);

    await waitFor(() => expect(screen.getByText('Test Issue')).toBeInTheDocument());

    // NOTE: our IssueList mock renders data-testid="issue-issue-1"
    fireEvent.click(screen.getByTestId('issue-issue-1'));

    await waitFor(() => expect(api.fetchSourceCode).toHaveBeenCalledWith('src/Main.java', 42));

    // switch to "AI Fix" to see snippet
    fireEvent.click(screen.getByText('AI Fix'));
    await waitFor(() => expect(screen.getByTestId('mock-ai-fix')).toHaveTextContent('Code snippet'));
  });

  it('switches between tabs and shows description/resources', async () => {
    renderWithStore(<DetailedFix jumpTarget={null} clearJumpTarget={() => {}} />);

    await waitFor(() => expect(screen.getByText('Test Issue')).toBeInTheDocument());

    // select the issue so description populates
    fireEvent.click(screen.getByTestId('issue-issue-1'));

    // default tab is "Information"
    await waitFor(() =>
      expect(screen.getByTestId('mock-detailed-description')).toHaveTextContent('Root cause description')
    );

    // go to More Info
    fireEvent.click(screen.getByText('More Info'));
    expect(screen.getByTestId('mock-detailed-description')).toHaveTextContent('Resource links');

    // go to AI Fix
    fireEvent.click(screen.getByText('AI Fix'));
    expect(screen.getByTestId('mock-ai-fix')).toBeInTheDocument();
  });

  it('respects jumpTarget and clears it after selecting', async () => {
    const clearJumpTarget = jest.fn();

    renderWithStore(<DetailedFix jumpTarget={{ key: 'issue-1' }} clearJumpTarget={clearJumpTarget} />);

    // wait for auto-selection triggered by jumpTarget
    await waitFor(() => expect(api.fetchSourceCode).toHaveBeenCalledWith('src/Main.java', 42));

    // ensure description loaded (unique selector avoids ambiguous getByText on title/button)
    await waitFor(() =>
      expect(screen.getByTestId('mock-detailed-description')).toHaveTextContent('Root cause description')
    );

    expect(clearJumpTarget).toHaveBeenCalled();
  });
});
