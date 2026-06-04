import { useState, useEffect, useRef, type KeyboardEvent } from 'react';
import { clientApi } from '../../services/api';
import type { ClientSearchResult } from '../../types';

interface Props {
  client: ClientSearchResult | null;
  onChange: (client: ClientSearchResult | null) => void;
}

const DEBOUNCE_MS = 300;

export default function ClientSelector({ client, onChange }: Props) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<ClientSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const timerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const inputRef = useRef<HTMLInputElement>(null);

  // Debounced search
  useEffect(() => {
    if (!query.trim() || query.length < 2) {
      setResults([]);
      return;
    }
    setLoading(true);
    clearTimeout(timerRef.current);
    timerRef.current = setTimeout(async () => {
      try {
        const { data } = await clientApi.search(query);
        setResults(data);
        setSelectedIndex(-1);
      } catch {
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, DEBOUNCE_MS);
    return () => clearTimeout(timerRef.current);
  }, [query]);

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => Math.min(prev + 1, results.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => Math.max(prev - 1, -1));
    } else if (e.key === 'Enter' && selectedIndex >= 0 && results[selectedIndex]) {
      e.preventDefault();
      onChange(results[selectedIndex]);
      setQuery('');
      setResults([]);
    } else if (e.key === 'Escape') {
      setResults([]);
    }
  };

  const handleSelect = (c: ClientSearchResult) => {
    onChange(c);
    setQuery('');
    setResults([]);
  };

  const handleClear = () => {
    onChange(null);
    setQuery('');
    inputRef.current?.focus();
  };

  // Si ya hay un cliente seleccionado, mostramos ficha en vez del input
  if (client) {
    return (
      <div className="rounded-lg bg-white p-4 shadow-sm">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-gray-700">Cliente</h2>
          <button
            onClick={handleClear}
            className="text-xs text-blue-600 hover:text-blue-800"
            aria-label="Cambiar cliente"
          >
            Cambiar
          </button>
        </div>
        <p className="mt-1 text-sm font-medium text-gray-800">{client.name}</p>
        {client.documentNumber && (
          <p className="text-xs text-gray-500">
            {client.documentType ?? 'Doc'}: {client.documentNumber}
          </p>
        )}
        {client.email && <p className="text-xs text-gray-500">{client.email}</p>}
      </div>
    );
  }

  return (
    <div className="rounded-lg bg-white p-4 shadow-sm">
      <label htmlFor="client-search" className="mb-1 block font-semibold text-gray-700">
        Cliente
      </label>
      <input
        ref={inputRef}
        id="client-search"
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Buscar por nombre o documento…"
        role="combobox"
        aria-expanded={results.length > 0}
        aria-controls="client-search-results"
        aria-activedescendant={
          selectedIndex >= 0 ? `client-option-${results[selectedIndex]?.id}` : undefined
        }
        aria-autocomplete="list"
        className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
      />

      {loading && (
        <div className="mt-1 text-xs text-gray-400">Buscando…</div>
      )}

      {!query.trim() && !client && (
        <p className="mt-2 text-xs text-gray-400">
          Consumidor final — opcional
        </p>
      )}

      {results.length > 0 && (
        <ul
          id="client-search-results"
          role="listbox"
          aria-label="Clientes encontrados"
          className="mt-1 max-h-48 overflow-auto rounded border border-gray-200 bg-white shadow-sm"
        >
          {results.map((c, i) => (
            <li
              key={c.id}
              id={`client-option-${c.id}`}
              role="option"
              aria-selected={i === selectedIndex}
              onClick={() => handleSelect(c)}
              className={`cursor-pointer px-3 py-2 text-sm ${
                i === selectedIndex ? 'bg-blue-100' : 'hover:bg-gray-50'
              }`}
            >
              <div className="font-medium text-gray-800">{c.name}</div>
              {c.documentNumber && (
                <div className="text-xs text-gray-500">{c.documentNumber}</div>
              )}
            </li>
          ))}
        </ul>
      )}

      {results.length === 0 && query.trim().length >= 2 && !loading && (
        <p className="mt-1 text-xs text-gray-400">
          Sin resultados — se registrará como consumidor final
        </p>
      )}
    </div>
  );
}
