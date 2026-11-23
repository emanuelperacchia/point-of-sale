import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PosPage } from '../PosPage'

// Mock api services
vi.mock('../services/api', () => ({
  productApi: {
    searchForPos: vi.fn(() => Promise.resolve([])),
    getAll: vi.fn(() => Promise.resolve([]))
  },
  saleApi: {
    create: vi.fn(() => Promise.resolve({ id: 1 })),
    validateCart: vi.fn(() => Promise.resolve({ valid: true, items: [] }))
  }
}))

// Mock AuthContext
vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn()
}))

describe('PosPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    // Mock useAuth to return authenticated user
    // @ts-ignore
    vi.mocked(require('../context/AuthContext').useAuth).mockReturnValue({
      login: vi.fn(),
      logout: vi.fn(),
      isAuthenticated: true,
      isLoading: false,
      branchId: 1,
      branches: []
    })
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('renders POS page correctly', () => {
    render(<PosPage />)
    
    // Verify search input exists
    expect(screen.getByPlaceholderText(/buscar producto/i)).toBeInTheDocument()
    
    // Verify cart section exists initially empty
    expect(screen.getByText(/buscar productos/i)).toBeInTheDocument()
  })

  it('displays search results when searching', async () => {
    // Mock product search results
    // @ts-ignore
    vi.mocked(require('../services/api').productApi.searchForPos).mockResolvedValueOnce([
      { id: 1, name: 'Coca Cola', sku: 'COC001', price: 15.0, stock: 100, categoryName: 'Bebidas' }
    ])

    render(<PosPage />)

    // Type in search box
    const searchInput = screen.getByPlaceholderText(/buscar producto/i)
    fireEvent.change(searchInput, { target: { value: 'coca' } })
    
    // Wait for search results
    await new Promise(resolve => setTimeout(resolve, 100))
    
    // Verify product appears
    expect(screen.getByText('Coca Cola')).toBeInTheDocument()
  })

  it('adds item to cart', async () => {
    // Mock product search
    // @ts-ignore
    vi.mocked(require('../services/api').productApi.searchForPos).mockResolvedValueOnce([
      { id: 1, name: 'Coca Cola', sku: 'COC001', price: 15.0, stock: 100, categoryName: 'Bebidas' }
    ])

    render(<PosPage />)

    // Search for product
    const searchInput = screen.getByPlaceholderText(/buscar producto/i)
    fireEvent.change(searchInput, { target: { value: 'coca' } })
    
    // Wait for results
    await new Promise(resolve => setTimeout(resolve, 100))
    
    // Click on product to add to cart
    fireEvent.click(screen.getByText('Coca Cola'))
    
    // Verify item appears in cart
    expect(screen.getByText('Coca Cola')).toBeInTheDocument()
  })
})
