import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import axios from 'axios'

// Mock axios
vi.mock('axios', () => ({
  default: {
    create: vi.fn().mockReturnThis(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() }
    }
  }
}))

import { api, branchApi } from '../api'

describe('api.ts', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('debe tener instancia de axios configurada', () => {
    expect(api).toBeDefined()
    expect(api.interceptors.request).toBeDefined()
    expect(api.interceptors.response).toBeDefined()
  })

  describe('branchApi', () => {
    it('debe listar sucursales activas', async () => {
      const mockData = [{ id: 1, nombre: 'Sucursal 1', activa: true }]
      // @ts-ignore
      vi.mocked(api.get).mockResolvedValueOnce({ data: mockData })

      const result = await branchApi.listActive()

      expect(result).toEqual(mockData)
      // @ts-ignore
      expect(api.get).toHaveBeenCalledWith('/branches')
    })

    it('debe crear una sucursal', async () => {
      const mockData = { id: 1, nombre: 'Nueva Sucursal', activa: true }
      const requestData = { nombre: 'Nueva Sucursal', direccion: 'Calle 123' }
      // @ts-ignore
      vi.mocked(api.post).mockResolvedValueOnce({ data: mockData })

      const result = await branchApi.create(requestData)

      expect(result).toEqual(mockData)
      // @ts-ignore
      expect(api.post).toHaveBeenCalledWith('/branches', requestData)
    })

    it('debe actualizar una sucursal', async () => {
      const mockData = { id: 1, nombre: 'Sucursal Actualizada', activa: true }
      const requestData = { nombre: 'Sucursal Actualizada' }
      // @ts-ignore
      vi.mocked(api.put).mockResolvedValueOnce({ data: mockData })

      const result = await branchApi.update(1, requestData)

      expect(result).toEqual(mockData)
      // @ts-ignore
      expect(api.put).toHaveBeenCalledWith('/branches/1', requestData)
    })

    it('debe eliminar una sucursal', async () => {
      // @ts-ignore
      vi.mocked(api.delete).mockResolvedValueOnce({})

      await branchApi.delete(1)

      // @ts-ignore
      expect(api.delete).toHaveBeenCalledWith('/branches/1')
    })
  })
})
