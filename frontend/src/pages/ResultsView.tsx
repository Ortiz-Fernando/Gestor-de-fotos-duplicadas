import { useCallback, useEffect, useState } from 'react';
import { api } from '../services/api';
import { categoryLabel, formatBytes } from '../services/format';
import type { GroupSummary, ScanSummary } from '../types';

interface Props {
  scanId: number;
  onOpenGroup: (scanId: number, groupId: number) => void;
}

export default function ResultsView({ scanId, onOpenGroup }: Props) {
  const [scan, setScan] = useState<ScanSummary | null>(null);
  const [groups, setGroups] = useState<GroupSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const [scanData, groupData] = await Promise.all([
        api.getScan(scanId),
        api.listGroups(scanId),
      ]);
      setScan(scanData);
      setGroups(groupData);
    } catch (err) {
      setError((err as Error).message);
    }
  }, [scanId]);

  useEffect(() => {
    void load();
  }, [load]);

  const exactGroups = groups.filter((group) => group.category === 'EXACT');
  const visualGroups = groups.filter((group) => group.category === 'POSSIBLE_VISUAL');
  const reclaimable = groups.reduce((sum, group) => sum + group.reclaimableBytes, 0);

  const handleDetectAgain = async () => {
    setBusy(true);
    setError(null);
    try {
      await api.detectScan(scanId);
      await load();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section>
      <div className="results-header">
        <h2>Resultados</h2>
        <button type="button" disabled={busy} onClick={handleDetectAgain}>
          {busy ? 'Analizando…' : 'Volver a analizar duplicados'}
        </button>
      </div>
      {scan && (
        <p className="scan-path muted" title={scan.rootPath}>
          {scan.rootPath}
        </p>
      )}

      <div className="summary-cards">
        <div className="summary-card">
          <span className="summary-value">{exactGroups.length}</span>
          <span className="summary-label">Grupos de duplicados exactos</span>
        </div>
        <div className="summary-card">
          <span className="summary-value">{visualGroups.length}</span>
          <span className="summary-label">Grupos de posibles duplicados visuales</span>
        </div>
        <div className="summary-card">
          <span className="summary-value">{formatBytes(reclaimable)}</span>
          <span className="summary-label">Espacio recuperable</span>
        </div>
      </div>

      {error && <p className="error">{error}</p>}

      {groups.length === 0 ? (
        <p className="muted">
          No se han encontrado grupos de duplicados. Recuerda que el pHash solo sugiere
          candidatos: la decisión final siempre es tuya.
        </p>
      ) : (
        <ul className="group-list">
          {groups.map((group) => (
            <li key={group.id} className="group-card">
              <div className="group-info">
                <span className={`badge badge-${group.category.toLowerCase()}`}>
                  {categoryLabel(group.category)}
                </span>
                <span>{group.memberCount} archivos</span>
                <span className="muted">Ahorro: {formatBytes(group.reclaimableBytes)}</span>
              </div>
              <button type="button" onClick={() => onOpenGroup(scanId, group.id)}>
                Ver grupo
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
