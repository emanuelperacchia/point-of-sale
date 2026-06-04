import { useState, useEffect } from 'react';
import { useCart } from '../hooks/useCart';
import Layout from '../components/common/Layout';
import ProductSearch from '../components/pos/ProductSearch';
import CartView from '../components/pos/CartView';
import ClientSelector from '../components/pos/ClientSelector';
import InvoiceTypeSelector from '../components/pos/InvoiceTypeSelector';
import ReceptorForm from '../components/pos/ReceptorForm';
import ShiftPanel from '../components/pos/ShiftPanel';
import ReturnModal from '../components/pos/ReturnModal';
import CouponInput from '../components/pos/CouponInput';
import LoyaltyWidget from '../components/pos/LoyaltyWidget';
import PaymentModal from '../components/pos/PaymentModal';
import { saleApi } from '../services/api';
import type { ProductSearchResult, ClientSearchResult, PaymentMethod, SaleItemRequest, CouponValidationResponse } from '../types';

export default function PosPage() {
  const { items, addItem, updateQuantity, removeItem, clearCart, total, subtotal, discount, itemCount } = useCart();

  const [client, setClient] = useState<ClientSearchResult | null>(null);
  const [showPayment, setShowPayment] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState('');
  const [successId, setSuccessId] = useState<number | null>(null);
  const [successTotal, setSuccessTotal] = useState(0);
  const [showReturns, setShowReturns] = useState(false);
  const [appliedCoupon, setAppliedCoupon] = useState<CouponValidationResponse | null>(null);
  const [redemptionPuntos, setRedemptionPuntos] = useState(0);

  // Shortcut F2 para buscar
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'F2') {
        e.preventDefault();
        document.getElementById('product-search-input')?.focus();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  const handleSelectProduct = (product: ProductSearchResult) => {
    addItem(product);
    setTimeout(() => document.getElementById('product-search-input')?.focus(), 50);
  };

  const handlePaymentConfirm = async (payment: { method: PaymentMethod; amount: number; reference?: string }) => {
    setProcessing(true);
    setError('');

    try {
      const saleItems: SaleItemRequest[] = items.map((i) => ({
        productId: i.product.id,
        quantity: i.quantity,
        discount: i.discount || 0,
      }));

      const { data } = await saleApi.create({
        clientId: client?.id,
        items: saleItems,
        payments: [
          {
            paymentMethod: payment.method,
            amount: payment.amount,
            reference: payment.reference,
          },
        ],
        couponCode: appliedCoupon?.valido ? appliedCoupon.codigo : undefined,
        puntosCanje: redemptionPuntos > 0 ? redemptionPuntos : undefined,
      });

      const saleTotal = total;
      setSuccessId(data.id);
      clearCart();
      setClient(null);
      setAppliedCoupon(null);
      setRedemptionPuntos(0);
      setSuccessTotal(saleTotal);
      setShowPayment(false);
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg || 'Error al procesar la venta. Intente nuevamente.');
    } finally {
      setProcessing(false);
    }
  };

  const handleNewSale = () => {
    setSuccessId(null);
    setSuccessTotal(0);
    setError('');
    setAppliedCoupon(null);
    setRedemptionPuntos(0);
    setTimeout(() => document.getElementById('product-search-input')?.focus(), 50);
  };

  // ── Pantalla de éxito ──────────────────────────────────────────────
  if (successId) {
    return (
      <Layout title="Caja">
        <div className="flex h-full items-center justify-center">
          <div className="text-center">
            <div className="mb-4 text-5xl" aria-hidden="true">✅</div>
            <h2 className="mb-2 text-xl font-bold text-gray-800">Venta registrada</h2>
            <p className="mb-1 text-sm text-gray-600">
              Venta N° <span className="font-semibold">{successId}</span>
            </p>
            <p className="mb-6 text-sm text-gray-500">
              Total: <strong>${successTotal.toLocaleString('es-CL')}</strong>
            </p>
            <button
              onClick={handleNewSale}
              className="rounded-lg bg-blue-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-blue-700"
              autoFocus
            >
              Nueva venta
            </button>
          </div>
        </div>
      </Layout>
    );
  }

  // ── Pantalla principal de caja ─────────────────────────────────────
  const canCharge = items.length > 0 && !processing;

  return (
    <Layout title="Caja">
      <div className="flex h-full gap-6">
        {/* Panel izquierdo: búsqueda + carrito */}
        <section className="flex flex-1 flex-col gap-4" aria-label="Productos y carrito">
          <ProductSearch onSelect={handleSelectProduct} />

          <CartView
            items={items}
            onUpdateQuantity={updateQuantity}
            onRemoveItem={removeItem}
            onClearCart={clearCart}
            subtotal={subtotal}
            discount={discount}
            total={total}
            itemCount={itemCount}
          />

          {/* Botón cobrar */}
          {items.length > 0 && (
            <button
              onClick={() => setShowPayment(true)}
              disabled={!canCharge}
              className="w-full rounded-lg bg-green-600 py-3 text-lg font-bold text-white shadow-sm hover:bg-green-700 disabled:opacity-50"
            >
              {processing ? 'Procesando…' : `Cobrar $${total.toLocaleString('es-CL')}`}
            </button>
          )}

          {error && (
            <div className="rounded border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
              {error}
            </div>
          )}
        </section>

        {/* Panel derecho: turno → cliente → comprobante → pago */}
        <aside className="w-80 space-y-4" aria-label="Resumen de venta">
          <ShiftPanel />
          <button
            onClick={() => setShowReturns(true)}
            className="flex w-full items-center gap-2 rounded-lg border border-orange-200 bg-white px-4 py-3 text-sm font-medium text-orange-700 shadow-sm hover:bg-orange-50 transition-colors"
          >
            <span aria-hidden="true">↩️</span>
            Devoluciones
          </button>
          <ClientSelector client={client} onChange={(c) => { setClient(c); setRedemptionPuntos(0); }} />
          {client && (
            <LoyaltyWidget
              clientId={client.id}
              onRedeem={(pts) => setRedemptionPuntos(pts)}
              disabled={processing || items.length === 0}
            />
          )}
          <CouponInput
            onCouponApplied={(coupon) => setAppliedCoupon(coupon)}
            disabled={processing || items.length === 0}
          />

          {client && (
            <>
              <InvoiceTypeSelector condicionIva={client.condicionIva} />
              <ReceptorForm client={client} onUpdate={(updated) => setClient(updated)} />
            </>
          )}

          <section className="rounded-lg bg-white p-4 shadow-sm" aria-label="Pago">
            <h2 className="mb-1 font-semibold text-gray-700">Pago</h2>
            <p className="text-xs text-gray-400">
              {items.length === 0
                ? 'Agregue productos al carrito'
                : `Se cobrarán $${total.toLocaleString('es-CL')}`}
            </p>
            {appliedCoupon && (
              <p className="mt-1 text-xs text-green-600">
                Cupón {appliedCoupon.codigo} aplicado
              </p>
            )}
            {redemptionPuntos > 0 && (
              <p className="mt-1 text-xs text-purple-600">
                {redemptionPuntos} pts canjeados
              </p>
            )}
          </section>
        </aside>
      </div>

      {/* Modal de pago */}
      <PaymentModal
        open={showPayment}
        total={total}
        onConfirm={handlePaymentConfirm}
        onCancel={() => {
          setShowPayment(false);
          setError('');
        }}
      />

      {/* Modal de devoluciones */}
      <ReturnModal
        open={showReturns}
        onClose={() => setShowReturns(false)}
      />
    </Layout>
  );
}
