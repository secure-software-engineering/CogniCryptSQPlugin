import React from 'react';

jest.mock('reactflow', () => ({
  ReactFlowProvider: ({ children }) => <div data-testid="react-flow-provider">{children}</div>
}));

jest.mock('../../main/js/sec_ai/components/codeGeneration/codeGen', () => {
  return function MockCodeGen() {
    return <div data-testid="mock-codegen">Mock CodeGen</div>;
  };
});

jest.mock('../../main/js/sec_ai/components/errorTree/ErrorVis', () => {
  return function MockErrorVis({ onJumpToIssue }) {
    return (
      <div data-testid="mock-errorvis">
        Mock ErrorVis
        <button onClick={() => onJumpToIssue('test-issue')}>Jump to Issue</button>
      </div>
    );
  };
});

jest.mock('../../main/js/sec_ai/components/Navigations/DetailedFix', () => {
  return function MockDetailedFix({ jumpTarget, clearJumpTarget }) {
    return (
      <div data-testid="mock-detailedfix">
        Mock DetailedFix
        {jumpTarget && <span>Jump Target: {jumpTarget}</span>}
        <button onClick={clearJumpTarget}>Clear Jump</button>
      </div>
    );
  };
});

import { render, screen, fireEvent } from '@testing-library/react';
import NavigationBar from '../../main/js/sec_ai/components/Navigations/NavigationBar';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';

// Create a mock store for testing
const createMockStore = (initialState = {}) => {
  return configureStore({
    reducer: {
      // Add your actual reducers here
      app: (state = {}) => state,
    },
    preloadedState: initialState,
  });
};

const renderWithProvider = (ui, { store = createMockStore() } = {}) => {
  return render(<Provider store={store}>{ui}</Provider>);
};

describe('NavigationBar', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders sidebar and all nav items', () => {
    renderWithProvider(<NavigationBar />);
    
    expect(screen.getByText('SecAI')).toBeInTheDocument();
    expect(screen.getByText('Vulnerabilities')).toBeInTheDocument();
    expect(screen.getByText('Error Tree')).toBeInTheDocument();
    expect(screen.getByText('Code Gen')).toBeInTheDocument();
  });

  it('renders DetailedFix component by default', () => {
    renderWithProvider(<NavigationBar />);
    expect(screen.getByTestId('mock-detailedfix')).toBeInTheDocument();
  });

  it('switches to Error Tree tab and shows ErrorVis', () => {
    renderWithProvider(<NavigationBar />);
    fireEvent.click(screen.getByText('Error Tree'));
    expect(screen.getByTestId('mock-errorvis')).toBeInTheDocument();
    expect(screen.getByTestId('react-flow-provider')).toBeInTheDocument();
  });

  it('switches to Code Gen tab and shows CodeGen', () => {
    renderWithProvider(<NavigationBar />);
    fireEvent.click(screen.getByText('Code Gen'));
    expect(screen.getByTestId('mock-codegen')).toBeInTheDocument();
  });

  it('toggles collapse state of sidebar', () => {
    renderWithProvider(<NavigationBar />);
    const toggleIcon = screen.getByTitle('Collapse');
    
    expect(screen.getByText('SecAI')).toBeInTheDocument();
    
    fireEvent.click(toggleIcon);
    
    expect(screen.queryByText('SecAI')).not.toBeInTheDocument();
    expect(screen.getByTitle('Expand')).toBeInTheDocument();
  });

  it('handles jump from ErrorVis to DetailedFix', () => {
    renderWithProvider(<NavigationBar />);
    
    // Switch to Error Tree
    fireEvent.click(screen.getByText('Error Tree'));
    expect(screen.getByTestId('mock-errorvis')).toBeInTheDocument();
    
    // Trigger jump to issue
    fireEvent.click(screen.getByText('Jump to Issue'));
    
    // Should switch back to vulnerabilities tab
    expect(screen.getByTestId('mock-detailedfix')).toBeInTheDocument();
    expect(screen.getByText('Jump Target: test-issue')).toBeInTheDocument();
  });
});
