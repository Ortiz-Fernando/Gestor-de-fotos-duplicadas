export type ScanStatus = 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
export type GroupCategory = 'EXACT' | 'POSSIBLE_VISUAL';
export type ImageStatus = 'ACTIVE' | 'IN_TRASH' | 'DELETED';

export interface ScanSummary {
  id: number;
  rootPath: string;
  status: ScanStatus;
  startedAt: string | null;
  finishedAt: string | null;
  fileCount: number;
  errorCount: number;
  errorMessage: string | null;
}

export interface CreateScanResponse {
  id: number;
  status: ScanStatus;
}

export interface GroupSummary {
  id: number;
  scanId: number;
  category: GroupCategory;
  recommendedImageId: number;
  memberCount: number;
  reclaimableBytes: number;
}

export interface ImageMeta {
  id: number;
  absolutePath: string;
  name: string;
  folder: string | null;
  extension: string | null;
  sizeBytes: number;
  lastModified: string;
  sha256: string | null;
  phash: number | null;
  width: number | null;
  height: number | null;
  exifOrientation: number | null;
  status: ImageStatus;
}

export interface GroupDetail extends GroupSummary {
  members: ImageMeta[];
}

export interface DetectionResult {
  scanId: number;
  exactGroups: number;
  exactImages: number;
  visualGroups: number;
  visualImages: number;
  similarReviewPairs: number;
  errors: number;
  reclaimableBytes: number;
}
