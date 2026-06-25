import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import '@testing-library/jest-dom';
import LoginPage from './LoginPage';
import { useAuth } from '../../context/AuthContext';

// Mock logo asset
vi.mock('../../assets/Logo.png', () => ({
  default: 'logo-mock-path'
}));

// Mock useAuth context
vi.mock('../../context/AuthContext', () => ({
  useAuth: vi.fn()
}));

// Mock authApi
vi.mock('../../api/authApi', () => ({
  authApi: {
    login: vi.fn()
  }
}));

describe('LoginPage', () => {
  const mockEnsureGuestToken = vi.fn();
  const mockLoginWithToken = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    
    // Default useAuth mock values
    (useAuth as any).mockReturnValue({
      isMember: false,
      loading: false,
      ensureGuestToken: mockEnsureGuestToken,
      loginWithToken: mockLoginWithToken
    });

    // Reset navigator.onLine mock to true by default
    Object.defineProperty(navigator, 'onLine', {
      writable: true,
      value: true,
      configurable: true
    });
  });

  it('GivenOfflineNetwork_WhenUserAttemptsLogin_ThenDisplaysConnectivityErrorMessage', async () => {
    // 1. Mock offline state
    Object.defineProperty(navigator, 'onLine', {
      writable: true,
      value: false,
      configurable: true
    });

    // 2. Mock ensureGuestToken to throw a TypeError "Failed to fetch" which is typical for fetch offline.
    mockEnsureGuestToken.mockRejectedValue(new TypeError('Failed to fetch'));

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    // 3. Fill in Account ID and Access Key
    const accountInput = screen.getByPlaceholderText('johndoe123');
    const passwordInput = screen.getByPlaceholderText('••••••••');
    const submitButton = screen.getByRole('button', { name: /sign in/i });

    fireEvent.change(accountInput, { target: { value: 'testuser' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });

    // 4. Click Sign In
    fireEvent.click(submitButton);

    // 5. Assert the exact error message is displayed
    await waitFor(() => {
      expect(
        screen.getByText('No internet connection. Please check your Wi-Fi or mobile data and try again.')
      ).toBeInTheDocument();
    });
  });
});
