import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import '@testing-library/jest-dom';
import DashboardPage from './DashboardPage';
import { authApi } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';

// Mock useAuth context
vi.mock('../../context/AuthContext', () => ({
  useAuth: vi.fn()
}));

// Mock authApi
vi.mock('../../api/authApi', () => ({
  authApi: {
    getPermissions: vi.fn(),
    getCurrentUser: vi.fn()
  }
}));

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    (useAuth as any).mockReturnValue({
      isProductionUser: false
    });

    localStorage.setItem('token', 'valid-token');

    // Reset navigator.onLine mock to true by default
    Object.defineProperty(navigator, 'onLine', {
      writable: true,
      value: true,
      configurable: true
    });
  });

  it('GivenOfflineNetwork_WhenDashboardLoads_ThenDisplaysConnectivityErrorMessage', async () => {
    // 1. Mock offline state
    Object.defineProperty(navigator, 'onLine', {
      writable: true,
      value: false,
      configurable: true
    });

    // 2. Mock API to reject with a "Failed to fetch" network error
    (authApi.getPermissions as any).mockRejectedValue(new Error('Failed to fetch'));
    (authApi.getCurrentUser as any).mockRejectedValue(new Error('Failed to fetch'));

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    // 3. Assert the exact error message is displayed
    await waitFor(() => {
      expect(
        screen.getByText('No internet connection. Please check your network settings.')
      ).toBeInTheDocument();
    });
  });
});
