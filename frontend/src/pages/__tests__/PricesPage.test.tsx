import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PricesPage } from '../PricesPage'

// Mock api services
vi.mock('../services/api', () => ({
  branchApi: {
    listActive: vi.fn(() => Promise.resolve([]))
  },
  productApi: {
    getAll: vi.fn(() => Promise.resolve([]))
  },
  productPriceApi: {
    resolvePrice: vi.fn(() => Promise.resolve({}))
  },
  branchPriceApi: {
    listByBranch: vi.fn(() => Promise.resolve([])),
    create: vi.fn(() => Promise.resolve({})),
    update: vi.fn(() => Promise.resolve({})),
    delete: vi.fn(() => Promise.resolve()),
    syncGlobal: vi.fn(() => Promise.resolve())
  }
}))

describe('PricesPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('renders prices page correctly', () => {
    render(<PricesPage />)
    
    // Verify page title exists
    expect(screen.getByText(/precios por sucursal/i)).toBeInTheDocument()
    
    // Verify branch selector exists
    expect(screen.getByText(/seleccionar sucursal/i)).toBeInTheDocument()
  })

  it('displays error when API fails', async () => {
    // Mock API error
    // @ts-ignore
    vi.mocked(require('../services/api').branchApi.listActive).mockRejectedValueOnce(new Error('Error al cargar sucursales'))

    render(<PricesPage />)

    // Wait for error to appear
    await new Promise(resolve => setTimeout(resolve, 100))

    // Verify error message
    expect(screen.getByText(/error al cargar sucursales/i)).toBeInTheDocument()
  })
})
