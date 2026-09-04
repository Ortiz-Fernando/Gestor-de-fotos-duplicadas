import { useEffect, useState } from 'react';
import { api } from '../services/api';
import { categoryLabel, formatBytes } from '../services/format';
import type { GroupDetail, ImageMeta } from '../types';

interface Props {
  scanId: number;
  groupId: number;
  onBack: () => void;
}

export default function GroupDetailView({ groupId, onBack }: Props) {
  const [group, setGroup] = useState<GroupDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const reload = async () => {
    try {
      setGroup(await api.getGroup(groupId));
    } catch (err) {
      setError((err as Error).message);
    }
  };

  useEffect(() => {
    api
      .getGroup(groupId)
      .then(setGroup)
      .catch((err: Error) => setError(err.message));
  }, [groupId]);

  const handleRename = async (member: ImageMeta) => {
    const requested = window.prompt(
      `Nuevo nombre para "${member.name}" (sin extensión):`,
      member.name,
    );
    if (!requested || !requested.trim()) {
      return;
    }
    const name = requested.trim();
    try {
      const preview = await api.renamePreview(member.id, name);
      if (!window.confirm(`¿Renombrar a "${preview.newName}"?`)) {
        return;
      }
      setBusyId(member.id);
      await api.renameImage(member.id, name);
      setNotice(`"${member.name}" se ha renombrado a "${preview.newName}".`);
      await reload();
    } catch (err) {
      setNotice((err as Error).message);
    } finally {
      setBusyId(null);
    }
  };

  const handleTrash = async (member: ImageMeta) => {
    const confirmed = window.confirm(
      `¿Enviar "${member.name}" a la Papelera de Windows?\n` +
        'Podrás recuperarlo desde la Papelera del sistema.',
    );
    if (!confirmed) {
      return;
    }
    setBusyId(member.id);
    try {
      await api.trashImage(member.id);
      setNotice(`"${member.name}" se ha enviado a la Papelera.`);
      await reload();
    } catch (err) {
      setNotice((err as Error).message);
    } finally {
      setBusyId(null);
    }
  };

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

      {notice && (
        <p className={notice.includes('se ha') ? 'ok-message' : 'error'}>{notice}</p>
      )}

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
              <button
                type="button"
                className="danger-outline"
                disabled={busyId === member.id}
                onClick={() => handleTrash(member)}
              >
                {busyId === member.id ? 'Enviando…' : 'Papelera'}
              </button>
              <button
                type="button"
                disabled={busyId === member.id}
                onClick={() => handleRename(member)}
              >
                {busyId === member.id ? 'Renombrando…' : 'Renombrar'}
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
