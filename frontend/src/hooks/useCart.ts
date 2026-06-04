import { useState, useCallback, useMemo } from 'react';
import type { CartItem, ProductSearchResult } from '../types';

export function useCart() {
  const [items, setItems] = useState<CartItem[]>([]);

  // Agregar producto (o incrementar si ya existe)
  const addItem = useCallback((product: ProductSearchResult) => {
    setItems((prev) => {
      const existing = prev.find((i) => i.product.id === product.id);
      if (existing) {
        return prev.map((i) =>
          i.product.id === product.id ? { ...i, quantity: i.quantity + 1 } : i,
        );
      }
      return [...prev, { product, quantity: 1, discount: 0 }];
    });
  }, []);

  // Cambiar cantidad
  const updateQuantity = useCallback((productId: number, quantity: number) => {
    setItems((prev) =>
      quantity <= 0
        ? prev.filter((i) => i.product.id !== productId)
        : prev.map((i) => (i.product.id === productId ? { ...i, quantity } : i)),
    );
  }, []);

  // Eliminar item
  const removeItem = useCallback((productId: number) => {
    setItems((prev) => prev.filter((i) => i.product.id !== productId));
  }, []);

  // Limpiar carrito
  const clearCart = useCallback(() => setItems([]), []);

  // Totales
  const subtotal = useMemo(
    () => items.reduce((acc, i) => acc + i.product.price * i.quantity, 0),
    [items],
  );

  const discount = useMemo(
    () => items.reduce((acc, i) => acc + (i.discount || 0), 0),
    [items],
  );

  const total = useMemo(() => subtotal - discount, [subtotal, discount]);

  const itemCount = useMemo(() => items.reduce((acc, i) => acc + i.quantity, 0), [items]);

  return {
    items,
    addItem,
    updateQuantity,
    removeItem,
    clearCart,
    subtotal,
    discount,
    total,
    itemCount,
  } as const;
}
