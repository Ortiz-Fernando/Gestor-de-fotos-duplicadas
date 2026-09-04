export function formatBytes(bytes: number): string {
  if (!bytes || bytes < 0) {
    return '0 B';
  }
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }
  return `${value.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`;
}

export function formatDate(value: string | null): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  return date.toLocaleString('es-ES', {
    dateStyle: 'short',
    timeStyle: 'short',
  });
}

export function categoryLabel(category: string): string {
  return category === 'EXACT' ? 'Duplicado exacto' : 'Posible duplicado visual';
}
