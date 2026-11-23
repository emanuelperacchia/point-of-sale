import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { CategoriesPage } from '../CategoriesPage'

// Mock api services
vi.mock('../services/api', () => ({
  categoryApi: {
    getAll: vi.fn(() => Promise.resolve([])),
    create: vi.fn(() => Promise.resolve({})),
    update: vi.fn(() => Promise.resolve({})),
    delete: vi.fn(() => Promise.resolve())
  }
}))

describe('CategoriesPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('renders categories page correctly', () => {
    render(<CategoriesPage />)
    
    // Verify page title exists
    expect(screen.getByText(/categorías/i)).toBeInTheDocument()
    
    // Verify empty state message
    expect(screen.getByText(/sin categorías/i)).toBeInTheDocument()
  })

  it('displays error when API fails', async () => {
    // Mock API error
    // @ts-ignore
    vi.mocked(require('../services/api').categoryApi.getAll).mockRejectedValueOnce(new Error('Error al cargar categorías'))

    render(<CategoriesPage />)

    // Wait for error to appear
    await new Promise(resolve => setTimeout(resolve, 100))

    // Verify error message
    expect(screen.getByText(/error al cargar categorías/i)).toBeInTheDocument()
  })
})
