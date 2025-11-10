import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import ClassDirectory from './ClassDirectory';

const sampleClasses = [
  {
    fullyQualifiedName: 'com.demo.Controller',
    stereotype: 'CONTROLLER',
    sourceSet: 'MAIN'
  },
  {
    fullyQualifiedName: 'com.demo.Service',
    stereotype: 'SERVICE',
    sourceSet: 'TEST'
  }
];

describe('ClassDirectory', () => {
  it('shows friendly hint when no classes were captured', () => {
    render(<ClassDirectory classes={[]} searchQuery="" />);
    expect(screen.getByText(/No classes were captured/i)).toBeInTheDocument();
  });

  it('filters classes based on the search query', () => {
    render(<ClassDirectory classes={sampleClasses} searchQuery="controller" />);
    expect(screen.getByText('com.demo.Controller')).toBeInTheDocument();
    expect(screen.queryByText('com.demo.Service')).not.toBeInTheDocument();
  });
});
