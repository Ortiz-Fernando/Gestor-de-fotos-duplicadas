import { useEffect, useState } from 'react';
import { api } from '../services/api';
import { categoryLabel, formatBytes } from '../services/format';
import type { GroupDetail } from '../types';

interface Props {
  scanId: number;
  groupId: number;
  onBack: () => void;
}

export default function GroupDetailView({ groupId, onBack }: Props) {
  const [group, setGroup] = useState<GroupDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .getGroup(groupId)
      .then(setGroup)
      .catch((err: Error) => setError(err.message));
  }, [groupId]);

  if (error) {
    return <p className="error">{error}</p>;
  }
  if (!group) {
    return <p className="muted">Cargando grupo…</p>;
  }

  return (
    <section>
      <div className="results-header">
        <button type="button" onClick={onBack}>
          ← Volver a resultados
        </button>
        <h2>{categoryLabel(group.category)}</h2>
        <span className="muted">{group.memberCount} archivos</span>
      </div>

      <div className="member-grid">
        {group.members.map((member) => (
          <div
            key={member.id}
            className={`member-card ${member.id === group.recommendedImageId ? 'member-recommended' : ''}`}
          >
            <img
              className="member-preview"
              src={api.imageContentUrl(member.id)}
              alt={member.name}
              loading="lazy"
            />
            {member.id === group.recommendedImageId && (
              <span className="badge badge-recommended">Conservar (sugerido)</span>
            )}
            <div className="member-meta">
              <strong title={member.name}>{member.name}</strong>
              <span className="muted" title={member.absolutePath}>
                {member.folder}
              </span>
              <span>
                {formatBytes(member.sizeBytes)}
                {member.width && member.height ? ` · ${member.width}×${member.height}` : ''}
              </span>
              {member.sha256 && (
                <span className="hash" title={member.sha256}>
                  SHA-256: {member.sha256.slice(0, 16)}…
                </span>
              )}
            </div>
            <div className="member-actions">
              <button type="button" disabled>
                Conservar
              </button>
              <button type="button" disabled>
                Papelera
              </button>
              <button type="button" disabled>
                Renombrar
              </button>
            </div>
          </div>
        ))}
      </div>

      <p className="muted note">
        Las acciones (conservar, papelera, renombrar) se activarán en próximas fases. Ninguna
        imagen se elimina automáticamente.
      </p>
    </section>
  );
}
