import { FormEvent, useEffect, useState } from 'react';
import { api } from '../services/api';
import { formatDate } from '../services/format';
import type { ScanSummary } from '../types';

interface Props {
  onScanCreated: (scanId: number) => void;
  onOpenResults: (scanId: number) => void;
}

export default function HomePage({ onScanCreated, onOpenResults }: Props) {
  const [rootPath, setRootPath] = useState('');
  const [scans, setScans] = useState<ScanSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);

  const loadScans = () => {
    api
      .listScans()
      .then(setScans)
      .catch((err: Error) => setError(err.message));
  };

  useEffect(() => {
    loadScans();
  }, []);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!rootPath.trim()) {
      setError('Introduce la ruta de la carpeta a analizar.');
      return;
    }
    setStarting(true);
    setError(null);
    try {
      const created = await api.startScan(rootPath.trim());
      onScanCreated(created.id);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setStarting(false);
    }
  };

  return (
    <section>
      <h2>Analizar una carpeta</h2>
      <form className="scan-form" onSubmit={handleSubmit}>
        <label htmlFor="rootPath">Ruta de la carpeta o unidad (p. ej. D:\Fotos)</label>
        <div className="scan-form-row">
          <input
            id="rootPath"
            type="text"
            value={rootPath}
            onChange={(event) => setRootPath(event.target.value)}
            placeholder="Escribe aquí la ruta de la carpeta…"
          />
          <button type="submit" className="primary" disabled={starting}>
            {starting ? 'Analizando…' : 'ANALIZAR'}
          </button>
        </div>
      </form>
      {error && <p className="error">{error}</p>}

      <h2>Análisis anteriores</h2>
      {scans.length === 0 ? (
        <p className="muted">Todavía no hay análisis guardados.</p>
      ) : (
        <ul className="scan-list">
          {scans.map((scan) => (
            <li key={scan.id} className="scan-row">
              <span className="scan-path" title={scan.rootPath}>
                {scan.rootPath}
              </span>
              <span className="muted">{formatDate(scan.startedAt)}</span>
              <span className={`status status-${scan.status.toLowerCase()}`}>{scan.status}</span>
              <button type="button" onClick={() => onOpenResults(scan.id)}>
                Ver resultados
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
