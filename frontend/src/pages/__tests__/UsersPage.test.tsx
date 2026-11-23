import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { UsersPage } from '../UsersPage'

// Mock api services
vi.mock('../services/api', () => ({
  userApi: {
    getAll: vi.fn(() => Promise.resolve([])),
    getMyPermissions: vi.fn(() => Promise.resolve([])),
    getUserById: vi.fn(() => Promise.resolve({}))
  }
}))

describe('UsersPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('renders users page correctly', () => {
    render(<UsersPage />)
    
    // Verify page title exists
    expect(screen.getByText(/usuarios/i)).toBeInTheDocument()
    
    // Verify empty state message
    expect(screen.getByText(/sin usuarios registrados/i)).toBeInTheDocument()
  })

  it('displays error when API fails', async () => {
    // Mock API error
    // @ts-ignore
    vi.mocked(require('../services/api').userApi.getAll).mockRejectedValueOnce(new Error('Error al cargar usuarios'))

    render(<UsersPage />)

    // Wait for error to appear
    await new Promise(resolve => setTimeout(resolve, 100))

    // Verify error message
    expect(screen.getByText(/error al cargar usuarios/i)).toBeInTheDocument()
  })
})
