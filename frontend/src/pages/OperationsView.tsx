import { useCallback, useEffect, useState } from 'react';
import { api } from '../services/api';
import { formatDate } from '../services/format';
import type { Operation } from '../types';

export default function OperationsView() {
  const [operations, setOperations] = useState<Operation[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(() => {
    api
      .listOperations()
      .then(setOperations)
      .catch((err: Error) => setError(err.message));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleUndo = async (operation: Operation) => {
    if (
      !window.confirm(
        '¿Deshacer esta operación? El archivo volverá a su ubicación anterior.',
      )
    ) {
      return;
    }
    try {
      await api.undoOperation(operation.id);
      setNotice('Operación deshecha correctamente.');
      setError(null);
      load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const label = (type: Operation['type']): string => {
    switch (type) {
      case 'RENAME':
        return 'Renombrar';
      case 'TRASH':
        return 'Papelera';
      case 'UNDO':
        return 'Deshacer';
      default:
        return type;
    }
  };

  return (
    <section>
      <h2>Historial de operaciones</h2>
      {notice && <p className="ok-message">{notice}</p>}
      {error && <p className="error">{error}</p>}
      {operations.length === 0 ? (
        <p className="muted">Todavía no hay operaciones registradas.</p>
      ) : (
        <ul className="scan-list">
          {operations.map((operation) => {
            const canUndo =
              operation.type === 'RENAME' &&
              operation.reversible &&
              operation.undoneAt === null;
            return (
              <li key={operation.id} className="scan-row">
                <span className={`badge badge-${operation.type.toLowerCase()}`}>
                  {label(operation.type)}
                </span>
                <span className="scan-path muted" title={operation.sourcePath ?? ''}>
                  {operation.sourcePath ?? '—'} → {operation.destinationPath ?? 'Papelera'}
                </span>
                <span className="muted">{formatDate(operation.operationTime)}</span>
                {operation.undoneAt ? (
                  <span className="status status-completed">Deshecha</span>
                ) : (
                  <button
                    type="button"
                    disabled={!canUndo}
                    title={canUndo ? 'Restaurar el archivo' : 'No se puede deshacer automáticamente'}
                    onClick={() => handleUndo(operation)}
                  >
                    Deshacer
                  </button>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
