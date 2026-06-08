import { useState, useEffect, useRef, type KeyboardEvent } from 'react';
import { productApi } from '../../services/api';
import type { ProductSearchResult } from '../../types';

interface Props {
  onSelect: (product: ProductSearchResult) => void;
}

const DEBOUNCE_MS = 200;

export default function ProductSearch({ onSelect }: Props) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<ProductSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);

  // Debounced search
  useEffect(() => {
    if (!query.trim()) {
      return; // results already initialized as []
    }
    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const { data } = await productApi.search(query);
        setResults(data);
        setSelectedIndex(-1);
      } catch {
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [query]);

  // Keyboard navigation
  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => Math.min(prev + 1, results.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => Math.max(prev - 1, -1));
    } else if (e.key === 'Enter' && selectedIndex >= 0 && results[selectedIndex]) {
      e.preventDefault();
      onSelect(results[selectedIndex]);
      setQuery('');
      setResults([]);
    }
  };

  const handleSelect = (product: ProductSearchResult) => {
    onSelect(product);
    setQuery('');
    setResults([]);
    inputRef.current?.focus();
  };

  return (
    <div className="relative">
      <label htmlFor="product-search-input" className="sr-only">
        Buscar producto
      </label>
      <input
        ref={inputRef}
        id="product-search-input"
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Buscar producto (F2)…"
        role="combobox"
        aria-expanded={results.length > 0}
        aria-controls="product-search-results"
        aria-activedescendant={
          selectedIndex >= 0 ? `product-option-${results[selectedIndex]?.id}` : undefined
        }
        aria-autocomplete="list"
        className="w-full rounded-lg border border-gray-300 px-4 py-3 text-lg focus:border-blue-500 focus:outline-none"
      />

      {loading && (
        <div className="absolute right-3 top-3" aria-hidden="true">
          <div className="h-5 w-5 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
        </div>
      )}

      {results.length > 0 && (
        <ul
          id="product-search-results"
          role="listbox"
          aria-label="Resultados de búsqueda"
          className="absolute z-10 mt-1 max-h-64 w-full overflow-auto rounded-lg border border-gray-200 bg-white shadow-lg"
        >
          {results.map((p, i) => (
            <li
              key={p.id}
              id={`product-option-${p.id}`}
              role="option"
              aria-selected={i === selectedIndex}
              onClick={() => handleSelect(p)}
              className={`flex cursor-pointer items-center justify-between px-4 py-3 ${
                i === selectedIndex ? 'bg-blue-100' : 'hover:bg-gray-50'
              }`}
            >
              <div>
                <div className="font-medium text-gray-800">{p.name}</div>
                <div className="text-xs text-gray-500">
                  {p.sku} {p.categoryName && `· ${p.categoryName}`}
                </div>
              </div>
              <div className="text-right">
                <div className="font-semibold text-gray-800">
                  ${p.price.toLocaleString('es-CL')}
                </div>
                <div className="text-xs text-gray-500">Stock: {p.stock}</div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
