import type { CartItem } from '../../types';

interface Props {
  items: CartItem[];
  onUpdateQuantity: (productId: number, quantity: number) => void;
  onRemoveItem: (productId: number) => void;
  onClearCart: () => void;
  subtotal: number;
  discount: number;
  total: number;
  itemCount: number;
}

export default function CartView({
  items,
  onUpdateQuantity,
  onRemoveItem,
  onClearCart,
  subtotal,
  discount,
  total,
  itemCount,
}: Props) {
  if (items.length === 0) {
    return (
      <div
        className="flex flex-1 items-center justify-center rounded-lg bg-white p-8 text-gray-400 shadow-sm"
        role="status"
      >
        Carrito vacío — busque productos con F2
      </div>
    );
  }

  return (
    <section className="flex flex-1 flex-col rounded-lg bg-white shadow-sm" aria-label="Carrito de compras">
      {/* Encabezado */}
      <div className="flex items-center justify-between border-b px-4 py-3">
        <h2 className="font-semibold text-gray-700">
          Carrito ({itemCount} ítems)
        </h2>
        <button
          onClick={() => {
            if (window.confirm('¿Limpiar carrito?')) onClearCart();
          }}
          className="text-sm text-red-600 hover:text-red-800"
        >
          Limpiar
        </button>
      </div>

      {/* Lista de items */}
      <div className="flex-1 overflow-auto">
        <table className="w-full text-sm">
          <caption className="sr-only">Productos en el carrito</caption>
          <thead>
            <tr className="border-b bg-gray-50 text-left text-gray-600">
              <th className="px-4 py-2" scope="col">Producto</th>
              <th className="px-2 py-2 text-right" scope="col">Precio</th>
              <th className="px-2 py-2 text-right" scope="col">Cantidad</th>
              <th className="px-2 py-2 text-right" scope="col">Subtotal</th>
              <th className="px-2 py-2" scope="col"><span className="sr-only">Acción</span></th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.product.id} className="border-b last:border-0 hover:bg-gray-50">
                <td className="px-4 py-3">
                  <div className="font-medium text-gray-800">{item.product.name}</div>
                  <div className="text-xs text-gray-500">{item.product.sku}</div>
                </td>
                <td className="px-2 py-3 text-right text-gray-700">
                  ${item.product.price.toLocaleString('es-CL')}
                </td>
                <td className="px-2 py-3 text-right">
                  <label htmlFor={`qty-${item.product.id}`} className="sr-only">
                    Cantidad de {item.product.name}
                  </label>
                  <input
                    id={`qty-${item.product.id}`}
                    type="number"
                    min={1}
                    value={item.quantity}
                    onChange={(e) => onUpdateQuantity(item.product.id, Number(e.target.value))}
                    className="w-16 rounded border px-2 py-1 text-right"
                  />
                </td>
                <td className="px-2 py-3 text-right font-semibold text-gray-800">
                  ${(item.product.price * item.quantity).toLocaleString('es-CL')}
                  {item.discount > 0 && (
                    <span className="ml-1 inline-block rounded bg-red-100 px-1.5 py-0.5 text-xs font-medium text-red-700">
                      -${item.discount.toLocaleString('es-CL')}
                    </span>
                  )}
                </td>
                <td className="px-2 py-3 text-right">
                  <button
                    onClick={() => onRemoveItem(item.product.id)}
                    aria-label={`Eliminar ${item.product.name} del carrito`}
                    className="text-red-500 hover:text-red-700"
                  >
                    ✕
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Totales */}
      <div className="border-t px-4 py-3">
        <div className="flex justify-between text-sm text-gray-600">
          <span>Subtotal</span>
          <span>${subtotal.toLocaleString('es-CL')}</span>
        </div>
        {discount > 0 && (
          <div className="flex justify-between text-sm text-red-600">
            <span>Descuento</span>
            <span>-${discount.toLocaleString('es-CL')}</span>
          </div>
        )}
        <div className="flex justify-between text-lg font-bold text-gray-800">
          <span>Total</span>
          <span>${total.toLocaleString('es-CL')}</span>
        </div>
      </div>
    </section>
  );
}
