import { useEffect, useRef, useState } from 'react';
import { api } from '../services/api';
import type { ScanSummary } from '../types';

interface Props {
  scanId: number;
  onFinished: (scanId: number) => void;
}

export default function ScanProgressView({ scanId, onFinished }: Props) {
  const [scan, setScan] = useState<ScanSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [detecting, setDetecting] = useState(false);
  const finishedRef = useRef(false);

  useEffect(() => {
    let cancelled = false;

    const tick = async () => {
      try {
        const current = await api.getScan(scanId);
        if (cancelled) {
          return;
        }
        setScan(current);

        if (current.status === 'COMPLETED' && !finishedRef.current) {
          finishedRef.current = true;
          setDetecting(true);
          await api.detectScan(scanId);
          if (!cancelled) {
            onFinished(scanId);
          }
        } else if (current.status === 'FAILED' || current.status === 'CANCELLED') {
          finishedRef.current = true;
        } else if (!finishedRef.current) {
          window.setTimeout(tick, 1000);
        }
      } catch (err) {
        if (!cancelled) {
          setError((err as Error).message);
        }
      }
    };

    void tick();
    return () => {
      cancelled = true;
    };
  }, [scanId, onFinished]);

  const handleCancel = async () => {
    try {
      await api.cancelScan(scanId);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <section>
      <h2>Análisis en curso</h2>
      {scan && (
        <div className="card">
          <p className="scan-path" title={scan.rootPath}>
            {scan.rootPath}
          </p>
          <p>
            Estado: <span className={`status status-${scan.status.toLowerCase()}`}>{scan.status}</span>
          </p>
          {scan.status === 'RUNNING' && (
            <div className="progress-area">
              <div className="spinner" aria-label="Analizando" />
              <p className="muted">Buscando imágenes en la carpeta…</p>
              <button type="button" className="danger" onClick={handleCancel}>
                Cancelar
              </button>
            </div>
          )}
          {detecting && (
            <div className="progress-area">
              <div className="spinner" aria-label="Detectando duplicados" />
              <p className="muted">Detectando duplicados exactos y visuales…</p>
            </div>
          )}
          {scan.status === 'FAILED' && (
            <p className="error">{scan.errorMessage ?? 'El análisis ha fallado.'}</p>
          )}
          {scan.status === 'CANCELLED' && <p className="muted">Análisis cancelado.</p>}
        </div>
      )}
      {error && <p className="error">{error}</p>}
    </section>
  );
}
