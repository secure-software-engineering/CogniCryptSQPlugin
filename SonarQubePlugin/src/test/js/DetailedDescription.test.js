import React from 'react';
import { render, screen } from '@testing-library/react';
import DetailedDescription from '../../main/js/sec_ai/components/Description/DetailedDescription';

describe('DetailedDescription', () => {
  test('shows loading state when description is falsy', () => {
    const { rerender } = render(<DetailedDescription description="" />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();

    rerender(<DetailedDescription description={null} />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();

    rerender(<DetailedDescription description={undefined} />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  test('renders HTML when description is provided', () => {
    const html = `<h3>Root cause</h3><p><strong>Something</strong> went wrong.</p>`;
    render(<DetailedDescription description={html} />);

    // Text content is rendered (not literal tags)
    expect(screen.getByText('Root cause')).toBeInTheDocument();
    expect(screen.getByText('Something')).toBeInTheDocument();

    // Ensure <strong> actually rendered
    const strong = screen.getByText('Something').closest('strong');
    expect(strong).not.toBeNull();
  });

  test('updates when description prop changes', () => {
    const { rerender } = render(<DetailedDescription description="<p>First</p>" />);
    expect(screen.getByText('First')).toBeInTheDocument();

    rerender(<DetailedDescription description="<p>Second</p>" />);
    expect(screen.getByText('Second')).toBeInTheDocument();
  });

  test('does not render "Loading..." when description has whitespace and HTML', () => {
    render(<DetailedDescription description="   <p>Trim?</p>   " />);
    expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    expect(screen.getByText('Trim?')).toBeInTheDocument();
  });
});
