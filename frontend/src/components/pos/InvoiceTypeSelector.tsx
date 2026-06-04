import { TIPO_COMPROBANTE_MAP, CONDICION_IVA_LABELS, type CondicionIva } from '../../types';

interface Props {
  condicionIva: CondicionIva | null;
}

const TYPE_INFO: Record<string, { icon: string; desc: string }> = {
  'Factura A': { icon: '📄', desc: 'Responsable Inscripto' },
  'Factura B': { icon: '📄', desc: 'Exento' },
  'Factura C': { icon: '📄', desc: 'Monotributista' },
  'Boleta':     { icon: '🧾', desc: 'Consumidor Final' },
};

export default function InvoiceTypeSelector({ condicionIva }: Props) {
  if (!condicionIva) {
    return (
      <div className="rounded-lg bg-white p-4 shadow-sm">
        <h3 className="mb-1 text-sm font-semibold text-gray-700">
          Comprobante
        </h3>
        <p className="text-xs text-gray-400">
          Seleccione un cliente para ver el tipo de comprobante
        </p>
      </div>
    );
  }

  const tipo = TIPO_COMPROBANTE_MAP[condicionIva];
  const info = TYPE_INFO[tipo];

  return (
    <div className="rounded-lg bg-white p-4 shadow-sm">
      <h3 className="mb-2 text-sm font-semibold text-gray-700">
        Comprobante
      </h3>
      <div className="flex items-center gap-3 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2.5">
        <span className="text-xl" aria-hidden="true">{info?.icon ?? '📄'}</span>
        <div>
          <p className="text-sm font-bold text-blue-800">{tipo}</p>
          <p className="text-xs text-blue-600">
            {condicionIva ? CONDICION_IVA_LABELS[condicionIva] : '—'}
          </p>
        </div>
      </div>
    </div>
  );
}
