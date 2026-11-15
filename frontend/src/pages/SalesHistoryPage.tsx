import { useState, useEffect } from 'react';
import Layout from '../components/common/Layout';
import { saleApi } from '../services/api';
import type { SaleResponse } from '../types';

export default function SalesHistoryPage() {
  const [sales, setSales] = useState<SaleResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [viewing, setViewing] = useState(false);
  const [currentSale, setCurrentSale] = useState<SaleResponse | null>(null);

  useEffect(() => {
    loadSales();
  }, [page]);

  const loadSales = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await saleApi.getMySales(page, 20);
      setSales(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Error al cargar ventas';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleViewSale = (sale: SaleResponse) => {
    setViewing(true);
    setCurrentSale(sale);
  };

  const handlePrevPage = () => {
    if (page > 0) setPage(page - 1);
  };

  const handleNextPage = () => {
    if (page < totalPages - 1) setPage(page + 1);
  };

  if (loading) {
    return (
      <Layout title="Ventas">
        <div className="flex h-full items-center justify-center text-ink-faint">
          Cargando ventas…
        </div>
      </Layout>
    );
  }

  return (
    <Layout title="Historial de Ventas">
      <div className="space-y-6">
        {/* Encabezado */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-ink">Historial de Ventas</h1>
            <p className="text-sm text-ink-faint">
              Consulta todas las ventas registradas en el punto de venta.
            </p>
          </div>
        </div>

        {error && (
          <div className="rounded-md border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
            {error}
          </div>
        )}

        {/* Tabla de ventas */}
        <div className="rounded-xl border border-surface-raised bg-surface/80 ">
          <table className="w-full text-sm text-left border-collapse">
            <thead>
              <tr className="border-b border-surface-raised bg-surface-raised/50 text-ink-soft">
                <th className="p-4 font-medium text-ink">N° Venta</th>
                <th className="p-4 font-medium text-ink">Fecha</th>
                <th className="p-4 font-medium text-ink-soft">Cliente</th>
                <th className="p-4 font-medium text-ink-soft text-right">Total</th>
                <th className="p-4 font-medium text-ink-soft text-center">Estado</th>
                <th className="p-4 font-medium text-ink-soft text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {sales.map((s) => (
                <tr key={s.id} className="border-b border-surface-raised/50 hover:bg-surface-raised/30 transition-colors">
                  <td className="p-4 text-ink font-medium">#{s.id}</td>
                  <td className="p-4 text-ink-soft">
                    {new Date(s.createdAt).toLocaleDateString('es-AR')}
                    {new Date(s.createdAt).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })}
                  </td>
                  <td className="p-4 text-ink-soft">
                    {s.client ? `${s.client.name} (${s.client.documentNumber})` : 'Consumidor Final'}
                  </td>
                  <td className="p-4 text-right text-ink font-medium">
                    ${s.total.toFixed(2)}
                  </td>
                  <td className="p-4 text-center">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-success/10 text-success border border-success/20`}
                    >
                      <span className="w-1.5 h-1.5 rounded-full bg-success" aria-hidden="true" />
                      Completada
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => handleViewSale(s)}
                      className="text-accent hover:text-primary-soft transition-colors"
                      title="Ver detalles"
                    >
                      Ver
                    </button>
                  </td>
                </tr>
              ))}
              {sales.length === 0 && (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-ink-faint">
                    No hay ventas registradas todavía.
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {/* Paginación */}
          {totalPages > 1 && (
            <div className="px-4 py-3 flex items-center justify-between">
              <button
                onClick={handlePrevPage}
                disabled={page === 0}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-ink-soft bg-surface border border-surface-raised disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-raised transition-colors"
              >
                Anterior
              </button>
              <span className="text-sm text-ink-faint">
                Página {page + 1} de {totalPages}
              </span>
              <button
                onClick={handleNextPage}
                disabled={page >= totalPages - 1}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-ink-soft bg-surface border border-surface-raised disabled:opacity-50 disabled:cursor-not-allowed hover:bg-surface-raised transition-colors"
              >
                Siguiente
              </button>
            </div>
          )}
        </div>

        {/* Modal detalles de venta */}
        {viewing && currentSale && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              className="absolute inset-0 bg-black/40 backdrop-blur-sm"
              onClick={() => setViewing(false)}
              aria-hidden="true"
            />
            <div className="relative w-full max-w-lg rounded-xl bg-surface p-6  border border-surface-raised">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-ink">Venta #{currentSale.id}</h2>
                <button
                  onClick={() => setViewing(false)}
                  className="rounded-md p-2 text-ink-faint hover:text-ink hover:bg-surface-raised transition-colors"
                  aria-label="Cerrar"
                >
                  <span aria-hidden="true">✕</span>
                </button>
              </div>

              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Fecha
                    </label>
                    <p className="text-ink">
                      {new Date(currentSale.createdAt).toLocaleString('es-AR')}
                    </p>
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Cliente
                    </label>
                    <p className="text-ink">
                      {currentSale.client ? currentSale.client.name : 'Consumidor Final'}
                    </p>
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Vendedor
                    </label>
                    <p className="text-ink">{currentSale.user.fullName}</p>
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-ink-soft">
                      Estado
                    </label>
                    <p className="text-ink">{currentSale.status}</p>
                  </div>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Items
                  </label>
                  <table className="w-full text-sm text-left border-collapse mt-2">
                    <thead>
                      <tr className="border-b border-surface-raised bg-surface-raised/50 text-ink-soft">
                        <th className="p-2">Producto</th>
                        <th className="p-2 text-right">Cant.</th>
                        <th className="p-2 text-right">Precio Unit.</th>
                        <th className="p-2 text-right">Subtotal</th>
                      </tr>
                    </thead>
                    <tbody>
                      {currentSale.items.map((item) => (
                        <tr key={item.id} className="border-b border-surface-raised/50">
                          <td className="p-2 text-ink">{item.productName}</td>
                          <td className="p-2 text-right text-ink-soft">{item.quantity}</td>
                          <td className="p-2 text-right text-ink-soft">${item.unitPrice.toFixed(2)}</td>
                          <td className="p-2 text-right text-ink">${item.subtotal.toFixed(2)}</td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr className="border-t border-surface-raised font-bold text-ink">
                        <td className="p-2 text-right" colSpan={3}>Total:</td>
                        <td className="p-2 text-right">${currentSale.total.toFixed(2)}</td>
                      </tr>
                    </tfoot>
                  </table>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-ink-soft">
                    Pagos
                  </label>
                  <ul className="mt-1 space-y-1">
                    {currentSale.payments.map((p) => (
                      <li key={p.id} className="text-sm text-ink-soft flex justify-between">
                        <span>{p.paymentMethod}</span>
                        <span>$${p.amount.toFixed(2)}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>

              <div className="flex justify-end pt-4">
                <button
                  onClick={() => setViewing(false)}
                  className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-ink-on-dark transition-colors hover:bg-primary-soft"
                >
                  Cerrar
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}

