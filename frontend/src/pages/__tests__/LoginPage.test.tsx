import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { LoginPage } from '../LoginPage'

// Mock authApi
vi.mock('../services/api', () => ({
  authApi: {
    login: vi.fn()
  }
}))

// Mock useAuth para evitar dependencia de AuthContext
vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn()
}))

describe('LoginPage', () => {
  const mockLogin = vi.fn()
  const mockNavigate = vi.fn()

  beforeEach(() => {
    vi.resetAllMocks()
    // @ts-ignore
    vi.mocked(require('../context/AuthContext').useAuth).mockReturnValue({
      login: mockLogin,
      isLoading: false
    })
    // @ts-ignore
    vi.mocked(require('react-router-dom').useNavigate).mockReturnValue(mockNavigate)
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('renderiza el formulario de login correctamente', () => {
    render(<LoginPage />)

    // Verificar elementos del formulario
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/contraseña/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /ingresar/i })).toBeInTheDocument()
    expect(screen.getByText(/iniciar sesión/i)).toBeInTheDocument()
  })

  it('muestra error cuando las credenciales son inválidas', async () => {
    // @ts-ignore
    vi.mocked(mockLogin).mockRejectedValueOnce(new Error('Credenciales inválidas'))

    render(<LoginPage />)

    // Rellenar formulario y enviar
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'test@test.com' } })
    fireEvent.change(screen.getByLabelText(/contraseña/i), { target: { value: 'wrong' } })
    fireEvent.click(screen.getByRole('button', { name: /ingresar/i }))

    // Verificar mensaje de error
    expect(screen.getByText(/credenciales inválidas/i)).toBeInTheDocument()
  })

  it('redirige a /pos después de login exitoso', async () => {
    // @ts-ignore
    vi.mocked(mockLogin).mockResolvedValueOnce({})

    render(<LoginPage />)

    // Rellenar formulario y enviar
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'admin@pos.com' } })
    fireEvent.change(screen.getByLabelText(/contraseña/i), { target: { value: 'admin123' } })
    fireEvent.click(screen.getByRole('button', { name: /ingresar/i }))

    // Verificar que se llamó a navigate con /pos
    // @ts-ignore
    expect(vi.mocked(require('react-router-dom').useNavigate)).toHaveBeenCalledWith('/pos')
  })
})
