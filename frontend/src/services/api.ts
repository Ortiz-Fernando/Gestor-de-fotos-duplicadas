import type {
  CreateScanResponse,
  DetectionResult,
  GroupDetail,
  GroupSummary,
  ImageMeta,
  ScanSummary,
} from '../types';

const BASE = '/api';

async function parseError(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string };
    if (body.message) {
      return body.message;
    }
  } catch {
    // ignore parse errors
  }
  return `Error de comunicación con el servidor (${response.status}).`;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  if (!response.ok) {
    throw new Error(await parseError(response));
  }
  return (await response.json()) as T;
}

export const api = {
  health(): Promise<{ status: string }> {
    return request('/health');
  },

  startScan(rootPath: string): Promise<CreateScanResponse> {
    return request('/scans', { method: 'POST', body: JSON.stringify({ rootPath }) });
  },

  getScan(id: number): Promise<ScanSummary> {
    return request(`/scans/${id}`);
  },

  listScans(): Promise<ScanSummary[]> {
    return request('/scans');
  },

  cancelScan(id: number): Promise<ScanSummary> {
    return request(`/scans/${id}/cancel`, { method: 'POST' });
  },

  detectScan(id: number): Promise<DetectionResult> {
    return request(`/scans/${id}/detect`, { method: 'POST' });
  },

  listGroups(scanId: number): Promise<GroupSummary[]> {
    return request(`/scans/${scanId}/groups`);
  },

  getGroup(id: number): Promise<GroupDetail> {
    return request(`/groups/${id}`);
  },

  getImage(id: number): Promise<ImageMeta> {
    return request(`/images/${id}`);
  },

  imageContentUrl(id: number): string {
    return `${BASE}/images/${id}/content`;
  },
};
