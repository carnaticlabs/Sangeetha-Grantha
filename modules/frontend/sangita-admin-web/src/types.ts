export enum ViewState {
  DASHBOARD = 'DASHBOARD',
  KRITHIS = 'KRITHIS',
  KRITHI_DETAIL = 'KRITHI_DETAIL',
  REFERENCE = 'REFERENCE',
  IMPORTS = 'IMPORTS',
  TAGS = 'TAGS',
}

export interface Krithi {
  id: string;
  title: string;
  composer: string;
  raga: string;
  tala: string;
  status: 'Draft' | 'Review' | 'Published' | 'Archived';
  lastModified: string;
}

export interface LyricVariant {
  id: string;
  language: string;
  script: string;
  isPrimary: boolean;
  label: string; 
  sampradaya?: string;
  source: string;
  pallavi: string;
  anupallavi: string;
  charanams: string[];
}

export interface TagCategory {
  category: string;
  tags: { label: string; confidence?: 'Low' | 'Medium' | 'High'; source?: string }[];
}

export interface AuditEvent {
  id: string;
  timestamp: string;
  user: string;
  action: 'Create' | 'Update' | 'Workflow';
  changes?: { field: string; before: string; after: string }[];
}

export interface StatCardProps {
  label: string;
  value: string;
  trend?: string;
  trendUp?: boolean;
}