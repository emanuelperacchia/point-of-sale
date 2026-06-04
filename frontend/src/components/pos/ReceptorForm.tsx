import { useState, type FormEvent } from 'react';
import { clientApi } from '../../services/api';
import {
  CONDICION_IVA_LABELS,
  type ClientSearchResult,
  type CondicionIva,
} from '../../types';

interface Props {
  client: ClientSearchResult;
  onUpdate: (updated: ClientSearchResult) => void;
}

const IVA_OPTIONS: CondicionIva[] = [
  'RESPONSABLE_INSCRIPTO',
  'MONOTRIBUTISTA',
  'EXENTO',
  'CONSUMIDOR_FINAL',
];

export default function ReceptorForm({ client, onUpdate }: Props) {
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  const [businessName, setBusinessName] = useState(client.businessName ?? '');
  const [condicionIva, setCondicionIva] = useState<CondicionIva>(
    client.condicionIva ?? 'CONSUMIDOR_FINAL',
  );
  const [taxAddress, setTaxAddress] = useState(client.taxAddress ?? '');
  const [documentType, setDocumentType] = useState(client.documentType ?? 'DNI');
  const [documentNumber, setDocumentNumber] = useState(client.documentNumber ?? '');
  const [error, setError] = useState('');

  const needsFiscalData = condicionIva !== 'CONSUMIDOR_FINAL' && !businessName;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');

    try {
      const { data } = await clientApi.update(client.id, {
        name: client.name,
        documentType: documentType || undefined,
        documentNumber: documentNumber || undefined,
        email: client.email ?? undefined,
        phone: client.phone ?? undefined,
        address: client.address ?? undefined,
        businessName: businessName || undefined,
        condicionIva,
        taxAddress: taxAddress || undefined,
      });

      onUpdate(data);
      setEditing(false);
    } catch {
      setError('Error al guardar. Intente nuevamente.');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setBusinessName(client.businessName ?? '');
    setCondicionIva(client.condicionIva ?? 'CONSUMIDOR_FINAL');
    setTaxAddress(client.taxAddress ?? '');
    setDocumentType(client.documentType ?? 'DNI');
    setDocumentNumber(client.documentNumber ?? '');
    setError('');
    setEditing(false);
  };

  // ── Vista de solo lectura ────────────────────────────────────────
  if (!editing) {
    return (
      <div className="rounded-lg bg-white p-4 shadow-sm">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-gray-700">Datos fiscales</h3>
          <button
            onClick={() => setEditing(true)}
            className="text-xs text-blue-600 hover:text-blue-800"
          >
            Editar
          </button>
        </div>

        {needsFiscalData && (
          <p className="mb-2 mt-1 rounded bg-amber-50 px-2 py-1 text-xs text-amber-700">
            Complete los datos fiscales para emitir comprobante
          </p>
        )}

        <div className="mt-1 space-y-0.5 text-xs text-gray-600">
          {businessName && <p><span className="text-gray-400">Razón social:</span> {businessName}</p>}
          <p>
            <span className="text-gray-400">Cond. IVA:</span>{' '}
            {CONDICION_IVA_LABELS[condicionIva]}
          </p>
          {taxAddress && <p><span className="text-gray-400">Domicilio fiscal:</span> {taxAddress}</p>}
          <p>
            <span className="text-gray-400">{documentType}:</span> {documentNumber || '—'}
          </p>
        </div>
      </div>
    );
  }

  // ── Formulario de edición ────────────────────────────────────────
  return (
    <div className="rounded-lg bg-white p-4 shadow-sm">
      <h3 className="mb-3 text-sm font-semibold text-gray-700">
        Editar datos fiscales
      </h3>

      <form onSubmit={handleSubmit} className="space-y-3">
        {/* Razón social */}
        <div>
          <label htmlFor="receptor-business-name" className="mb-0.5 block text-xs font-medium text-gray-600">
            Razón social
          </label>
          <input
            id="receptor-business-name"
            type="text"
            value={businessName}
            onChange={(e) => setBusinessName(e.target.value)}
            placeholder="Nombre o razón social"
            className="w-full rounded border border-gray-300 px-2.5 py-1.5 text-sm focus:border-blue-500 focus:outline-none"
          />
        </div>

        {/* Condición IVA */}
        <div>
          <label htmlFor="receptor-iva" className="mb-0.5 block text-xs font-medium text-gray-600">
            Condición IVA
          </label>
          <select
            id="receptor-iva"
            value={condicionIva}
            onChange={(e) => setCondicionIva(e.target.value as CondicionIva)}
            className="w-full rounded border border-gray-300 px-2.5 py-1.5 text-sm focus:border-blue-500 focus:outline-none"
          >
            {IVA_OPTIONS.map((opt) => (
              <option key={opt} value={opt}>
                {CONDICION_IVA_LABELS[opt]}
              </option>
            ))}
          </select>
        </div>

        {/* Tipo y número de documento */}
        <div className="flex gap-2">
          <div className="flex-1">
            <label htmlFor="receptor-doc-type" className="mb-0.5 block text-xs font-medium text-gray-600">
              Tipo doc.
            </label>
            <select
              id="receptor-doc-type"
              value={documentType}
              onChange={(e) => setDocumentType(e.target.value)}
              className="w-full rounded border border-gray-300 px-2.5 py-1.5 text-sm focus:border-blue-500 focus:outline-none"
            >
              <option value="DNI">DNI</option>
              <option value="CUIT">CUIT</option>
              <option value="CUIL">CUIL</option>
              <option value="Pasaporte">Pasaporte</option>
            </select>
          </div>
          <div className="flex-[2]">
            <label htmlFor="receptor-doc-number" className="mb-0.5 block text-xs font-medium text-gray-600">
              N° documento
            </label>
            <input
              id="receptor-doc-number"
              type="text"
              value={documentNumber}
              onChange={(e) => setDocumentNumber(e.target.value)}
              placeholder="Número"
              className="w-full rounded border border-gray-300 px-2.5 py-1.5 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
        </div>

        {/* Domicilio fiscal */}
        <div>
          <label htmlFor="receptor-tax-address" className="mb-0.5 block text-xs font-medium text-gray-600">
            Domicilio fiscal
          </label>
          <input
            id="receptor-tax-address"
            type="text"
            value={taxAddress}
            onChange={(e) => setTaxAddress(e.target.value)}
            placeholder="Calle y número"
            className="w-full rounded border border-gray-300 px-2.5 py-1.5 text-sm focus:border-blue-500 focus:outline-none"
          />
        </div>

        {error && (
          <p className="text-xs text-red-600" role="alert">{error}</p>
        )}

        <div className="flex gap-2">
          <button
            type="button"
            onClick={handleCancel}
            className="flex-1 rounded border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={saving}
            className="flex-1 rounded bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {saving ? 'Guardando…' : 'Guardar'}
          </button>
        </div>
      </form>
    </div>
  );
}
