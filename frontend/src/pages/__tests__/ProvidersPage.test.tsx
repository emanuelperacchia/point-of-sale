import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ProvidersPage } from '../ProvidersPage'

// Mock api services
vi.mock('../services/api', () => ({
  supplierApi: {
    getAll: vi.fn(() => Promise.resolve([])),
    create: vi.fn(() => Promise.resolve({})),
    update: vi.fn(() => Promise.resolve({})),
    delete: vi.fn(() => Promise.resolve())
  }
}))

describe('ProvidersPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('renders providers page correctly', () => {
    render(<ProvidersPage />)
    
    // Verify page title exists
    expect(screen.getByText(/proveedores/i)).toBeInTheDocument()
    
    // Verify empty state message
    expect(screen.getByText(/sin proveedores/i)).toBeInTheDocument()
  })

  it('displays error when API fails', async () => {
    // Mock API error
    // @ts-ignore
    vi.mocked(require('../services/api').supplierApi.getAll).mockRejectedValueOnce(new Error('Error de conexión'))

    render(<ProvidersPage />)

    // Wait for error to appear
    await new Promise(resolve => setTimeout(resolve, 100))

    // Verify error message
    expect(screen.getByText(/error al cargar proveedores/i)).toBeInTheDocument()
  })
})
