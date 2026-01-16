/**
 * Enum mapping utilities for consistent formatting between database and UI
 * Maps database enum values to user-friendly display labels
 */

// Workflow State Mapping
export const WORKFLOW_STATE_LABELS: Record<string, string> = {
  'draft': 'Draft',
  'in_review': 'In Review',
  'published': 'Published',
  'archived': 'Archived',
  // UI uppercase variants
  'DRAFT': 'Draft',
  'IN_REVIEW': 'In Review',
  'PUBLISHED': 'Published',
  'ARCHIVED': 'Archived',
};

export const WORKFLOW_STATE_COLORS: Record<string, string> = {
  'draft': 'bg-slate-100 text-slate-600 border-slate-200',
  'in_review': 'bg-amber-50 text-amber-700 border-amber-200',
  'published': 'bg-green-50 text-green-700 border-green-200',
  'archived': 'bg-gray-100 text-gray-800 border-gray-200',
  'DRAFT': 'bg-slate-100 text-slate-600 border-slate-200',
  'IN_REVIEW': 'bg-amber-50 text-amber-700 border-amber-200',
  'PUBLISHED': 'bg-green-50 text-green-700 border-green-200',
  'ARCHIVED': 'bg-gray-100 text-gray-800 border-gray-200',
};

// Language Code Mapping
export const LANGUAGE_CODE_LABELS: Record<string, string> = {
  'sa': 'Sanskrit',
  'ta': 'Tamil',
  'te': 'Telugu',
  'kn': 'Kannada',
  'ml': 'Malayalam',
  'hi': 'Hindi',
  'en': 'English',
};

export const LANGUAGE_CODE_OPTIONS = [
  { value: 'sa', label: 'Sanskrit' },
  { value: 'ta', label: 'Tamil' },
  { value: 'te', label: 'Telugu' },
  { value: 'kn', label: 'Kannada' },
  { value: 'ml', label: 'Malayalam' },
  { value: 'hi', label: 'Hindi' },
  { value: 'en', label: 'English' },
];

// Script Code Mapping
export const SCRIPT_CODE_LABELS: Record<string, string> = {
  'devanagari': 'Devanagari',
  'tamil': 'Tamil',
  'telugu': 'Telugu',
  'kannada': 'Kannada',
  'malayalam': 'Malayalam',
  'latin': 'Latin',
};

export const SCRIPT_CODE_OPTIONS = [
  { value: 'devanagari', label: 'Devanagari' },
  { value: 'tamil', label: 'Tamil' },
  { value: 'telugu', label: 'Telugu' },
  { value: 'kannada', label: 'Kannada' },
  { value: 'malayalam', label: 'Malayalam' },
  { value: 'latin', label: 'Latin' },
];

// Musical Form Mapping
export const MUSICAL_FORM_LABELS: Record<string, string> = {
  'KRITHI': 'Krithi',
  'VARNAM': 'Varnam',
  'SWARAJATHI': 'Swarajathi',
};

// Section Type Mapping
export const SECTION_TYPE_LABELS: Record<string, string> = {
  'PALLAVI': 'Pallavi',
  'ANUPALLAVI': 'Anupallavi',
  'CHARANAM': 'Charanam',
  'SAMASHTI_CHARANAM': 'Samashti Charanam',
  'CHITTASWARAM': 'Chittaswaram',
  'SWARA_SAHITYA': 'Swara Sahitya',
  'MADHYAMA_KALA': 'Madhyama Kala',
  'SOLKATTU_SWARA': 'Solkattu Swara',
  'ANUBANDHA': 'Anubandha',
  'MUKTAYI_SWARA': 'Muktayi Swara',
  'ETTUGADA_SWARA': 'Ettugada Swara',
  'ETTUGADA_SAHITYA': 'Ettugada Sahitya',
  'VILOMA_CHITTASWARAM': 'Viloma Chittaswaram',
  'OTHER': 'Other',
};

// Tag Category Mapping
export const TAG_CATEGORY_LABELS: Record<string, string> = {
  'BHAVA': 'Bhava',
  'FESTIVAL': 'Festival',
  'PHILOSOPHY': 'Philosophy',
  'KSHETRA': 'Kshetra',
  'STOTRA_STYLE': 'Stotra Style',
  'NAYIKA_BHAVA': 'Nayika Bhava',
  'OTHER': 'Other',
};

/**
 * Format workflow state for display
 */
export function formatWorkflowState(state: string): string {
  return WORKFLOW_STATE_LABELS[state] || state;
}

/**
 * Get workflow state color classes
 */
export function getWorkflowStateColor(state: string): string {
  return WORKFLOW_STATE_COLORS[state] || 'bg-slate-100 text-slate-600 border-slate-200';
}

/**
 * Format language code for display
 */
export function formatLanguageCode(code: string): string {
  return LANGUAGE_CODE_LABELS[code] || code.toUpperCase();
}

/**
 * Format script code for display
 */
export function formatScriptCode(code: string): string {
  return SCRIPT_CODE_LABELS[code] || code;
}

/**
 * Format musical form for display
 */
export function formatMusicalForm(form: string): string {
  return MUSICAL_FORM_LABELS[form] || form;
}

/**
 * Format section type for display
 */
export function formatSectionType(type: string): string {
  return SECTION_TYPE_LABELS[type] || type;
}

/**
 * Format tag category for display
 */
export function formatTagCategory(category: string): string {
  return TAG_CATEGORY_LABELS[category] || category;
}


