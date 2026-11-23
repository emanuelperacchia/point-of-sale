import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { EmployeesPage } from '../EmployeesPage'

// Mock api services
vi.mock('../services/api', () => ({
  employeeApi: {
    getAll: vi.fn(() => Promise.resolve([])),
    create: vi.fn(() => Promise.resolve({})),
    update: vi.fn(() => Promise.resolve({})),
    deactivate: vi.fn(() => Promise.resolve())
  }
}))

describe('EmployeesPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('renders employees page correctly', () => {
    render(<EmployeesPage />)
    
    // Verify page title exists
    expect(screen.getByText(/empleados/i)).toBeInTheDocument()
    
    // Verify empty state message
    expect(screen.getByText(/sin empleados registrados/i)).toBeInTheDocument()
  })

  it('displays error when API fails', async () => {
    // Mock API error
    // @ts-ignore
    vi.mocked(require('../services/api').employeeApi.getAll).mockRejectedValueOnce(new Error('Error al cargar empleados'))

    render(<EmployeesPage />)

    // Wait for error to appear
    await new Promise(resolve => setTimeout(resolve, 100))

    // Verify error message
    expect(screen.getByText(/error al cargar empleados/i)).toBeInTheDocument()
  })
})
