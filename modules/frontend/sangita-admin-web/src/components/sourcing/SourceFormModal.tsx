import React, { useState, useEffect } from 'react';
import type { ImportSource, CreateSourceRequest, SourceTier, SourceFormat } from '../../types/sourcing';

interface SourceFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: CreateSourceRequest) => void;
  source?: ImportSource | null; // null = create mode, defined = edit mode
  isSubmitting?: boolean;
}

const tiers: { value: SourceTier; label: string }[] = [
  { value: 1, label: 'Tier 1 — Primary Authority' },
  { value: 2, label: 'Tier 2 — Scholarly' },
  { value: 3, label: 'Tier 3 — Curated' },
  { value: 4, label: 'Tier 4 — Community' },
  { value: 5, label: 'Tier 5 — Unverified' },
];

const formats: SourceFormat[] = ['HTML', 'PDF', 'DOCX', 'API', 'MANUAL'];

const SourceFormModal: React.FC<SourceFormModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  source,
  isSubmitting = false,
}) => {
  const isEdit = !!source;

  const [name, setName] = useState('');
  const [baseUrl, setBaseUrl] = useState('');
  const [description, setDescription] = useState('');
  const [sourceTier, setSourceTier] = useState<SourceTier>(5);
  const [selectedFormats, setSelectedFormats] = useState<SourceFormat[]>(['HTML']);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (source) {
      setName(source.name);
      setBaseUrl(source.baseUrl || '');
      setDescription(source.description || '');
      setSourceTier(source.sourceTier);
      setSelectedFormats(source.supportedFormats as SourceFormat[]);
    } else {
      setName('');
      setBaseUrl('');
      setDescription('');
      setSourceTier(5);
      setSelectedFormats(['HTML']);
    }
    setErrors({});
  }, [source, isOpen]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};
    if (!name.trim()) newErrors.name = 'Name is required';
    if (!baseUrl.trim()) newErrors.baseUrl = 'Base URL is required';
    else {
      try {
        new URL(baseUrl);
      } catch {
        newErrors.baseUrl = 'Please enter a valid URL';
      }
    }
    if (selectedFormats.length === 0) newErrors.formats = 'At least one format is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    onSubmit({
      name: name.trim(),
      baseUrl: baseUrl.trim(),
      description: description.trim() || undefined,
      sourceTier,
      supportedFormats: selectedFormats,
      composerAffinity: source?.composerAffinity,
    });
  };

  const toggleFormat = (format: SourceFormat) => {
    setSelectedFormats((prev) =>
      prev.includes(format) ? prev.filter((f) => f !== format) : [...prev, format]
    );
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between p-6 border-b border-border-light">
          <h2 className="text-lg font-display font-bold text-ink-900">
            {isEdit ? 'Edit Source' : 'Register New Source'}
          </h2>
          <button onClick={onClose} className="text-ink-400 hover:text-ink-600 transition-colors">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {/* Name */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">Name <span className="text-red-500">*</span></label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g., Guruguha.org"
              className={`w-full px-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 ${errors.name ? 'border-red-300' : 'border-border-light'}`}
            />
            {errors.name && <p className="text-xs text-red-600 mt-1">{errors.name}</p>}
          </div>

          {/* Base URL */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">Base URL <span className="text-red-500">*</span></label>
            <input
              type="url"
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
              placeholder="https://example.com"
              className={`w-full px-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 ${errors.baseUrl ? 'border-red-300' : 'border-border-light'}`}
            />
            {errors.baseUrl && <p className="text-xs text-red-600 mt-1">{errors.baseUrl}</p>}
          </div>

          {/* Tier */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">Authority Tier <span className="text-red-500">*</span></label>
            <select
              value={sourceTier}
              onChange={(e) => setSourceTier(Number(e.target.value) as SourceTier)}
              className="w-full px-3 py-2 text-sm border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              {tiers.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </div>

          {/* Formats */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-2">Supported Formats <span className="text-red-500">*</span></label>
            <div className="flex flex-wrap gap-2">
              {formats.map((format) => (
                <button
                  key={format}
                  type="button"
                  onClick={() => toggleFormat(format)}
                  className={`px-3 py-1.5 text-xs font-semibold rounded-full border transition-colors ${
                    selectedFormats.includes(format)
                      ? 'bg-primary text-white border-primary'
                      : 'bg-white text-ink-500 border-border-light hover:border-primary/30'
                  }`}
                >
                  {format}
                </button>
              ))}
            </div>
            {errors.formats && <p className="text-xs text-red-600 mt-1">{errors.formats}</p>}
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              maxLength={500}
              placeholder="Brief description of this source..."
              className="w-full px-3 py-2 text-sm border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 resize-none"
            />
            <p className="text-xs text-ink-400 mt-1 text-right">{description.length}/500</p>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-ink-600 hover:text-ink-800 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 bg-primary text-white text-sm font-medium rounded-lg hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isSubmitting ? 'Saving...' : isEdit ? 'Update Source' : 'Register Source'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SourceFormModal;
