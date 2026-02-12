import React, { useState, useEffect } from 'react';
import type { CreateExtractionRequest, ExtractionIntent, SourceFormat } from '../../types/sourcing';
import { useSourceList, useExtractionList } from '../../hooks/useSourcingQueries';

interface ExtractionRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: CreateExtractionRequest) => void;
  isSubmitting?: boolean;
}

const formats: SourceFormat[] = ['HTML', 'PDF', 'DOCX', 'API', 'MANUAL', 'IMAGE'];
const languages = [
  { code: '', label: 'Auto-detect' },
  { code: 'en', label: 'English' },
  { code: 'sa', label: 'Sanskrit' },
  { code: 'ta', label: 'Tamil' },
  { code: 'te', label: 'Telugu' },
  { code: 'kn', label: 'Kannada' },
  { code: 'ml', label: 'Malayalam' },
  { code: 'hi', label: 'Hindi' },
];

const ExtractionRequestModal: React.FC<ExtractionRequestModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  isSubmitting = false,
}) => {
  const { data: sourcesData } = useSourceList();
  // Fetch completed extractions for the "related extraction" dropdown
  const { data: completedExtractions } = useExtractionList(
    { status: ['DONE', 'INGESTED'] },
    undefined,
  );

  const [sourceUrl, setSourceUrl] = useState('');
  const [sourceFormat, setSourceFormat] = useState<SourceFormat>('HTML');
  const [importSourceId, setImportSourceId] = useState('');
  const [composerHint, setComposerHint] = useState('');
  const [pageRange, setPageRange] = useState('');
  const [expectedKrithiCount, setExpectedKrithiCount] = useState('');
  const [contentLanguage, setContentLanguage] = useState('');
  const [extractionIntent, setExtractionIntent] = useState<ExtractionIntent>('PRIMARY');
  const [relatedExtractionId, setRelatedExtractionId] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isOpen) {
      setSourceUrl('');
      setSourceFormat('HTML');
      setImportSourceId('');
      setComposerHint('');
      setPageRange('');
      setExpectedKrithiCount('');
      setContentLanguage('');
      setExtractionIntent('PRIMARY');
      setRelatedExtractionId('');
      setErrors({});
    }
  }, [isOpen]);

  // Auto-select format when a registered source is chosen
  useEffect(() => {
    if (importSourceId && sourcesData?.items) {
      const source = sourcesData.items.find((s) => s.id === importSourceId);
      if (source?.supportedFormats?.length) {
        setSourceFormat(source.supportedFormats[0] as SourceFormat);
      }
      if (source?.baseUrl && !sourceUrl) {
        setSourceUrl(source.baseUrl);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [importSourceId]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};
    if (!sourceUrl.trim()) {
      newErrors.sourceUrl = 'Source URL is required';
    } else {
      try {
        new URL(sourceUrl);
      } catch {
        newErrors.sourceUrl = 'Please enter a valid URL';
      }
    }
    if (expectedKrithiCount && (isNaN(Number(expectedKrithiCount)) || Number(expectedKrithiCount) < 1)) {
      newErrors.expectedKrithiCount = 'Must be a positive number';
    }
    if (extractionIntent === 'ENRICH' && !relatedExtractionId) {
      newErrors.relatedExtractionId = 'A related extraction is required for enrichment';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    onSubmit({
      sourceUrl: sourceUrl.trim(),
      sourceFormat,
      importSourceId: importSourceId || null,
      composerHint: composerHint.trim() || null,
      pageRange: pageRange.trim() || null,
      expectedKrithiCount: expectedKrithiCount ? Number(expectedKrithiCount) : null,
      contentLanguage: contentLanguage || null,
      extractionIntent: extractionIntent,
      relatedExtractionId: extractionIntent === 'ENRICH' ? relatedExtractionId || null : null,
    });
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={onClose}>
      <div
        className="bg-white rounded-2xl shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between p-6 border-b border-border-light">
          <h2 className="text-lg font-display font-bold text-ink-900">New Extraction Request</h2>
          <button onClick={onClose} className="text-ink-400 hover:text-ink-600 transition-colors">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {/* Extraction Intent */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-2">Extraction Intent</label>
            <div className="flex gap-2">
              {(['PRIMARY', 'ENRICH'] as ExtractionIntent[]).map((intent) => (
                <button
                  key={intent}
                  type="button"
                  onClick={() => {
                    setExtractionIntent(intent);
                    if (intent === 'PRIMARY') setRelatedExtractionId('');
                  }}
                  className={`flex-1 px-3 py-2 text-sm font-semibold rounded-lg border transition-all ${extractionIntent === intent
                      ? intent === 'PRIMARY'
                        ? 'bg-primary text-white border-primary'
                        : 'bg-amber-500 text-white border-amber-500'
                      : 'bg-white text-ink-500 border-border-light hover:border-primary/30'
                    }`}
                >
                  <span className="material-symbols-outlined text-base align-middle mr-1">
                    {intent === 'PRIMARY' ? 'add_circle' : 'enhanced_encryption'}
                  </span>
                  {intent === 'PRIMARY' ? 'Primary' : 'Enrich (Variant)'}
                </button>
              ))}
            </div>
            <p className="text-xs text-ink-400 mt-1">
              {extractionIntent === 'PRIMARY'
                ? 'Create new Krithis from this source'
                : 'Add language variants to Krithis from an existing extraction'}
            </p>
          </div>

          {/* Related Extraction (only for ENRICH) */}
          {extractionIntent === 'ENRICH' && (
            <div>
              <label className="block text-sm font-medium text-ink-700 mb-1">
                Related Extraction <span className="text-red-500">*</span>
              </label>
              <select
                value={relatedExtractionId}
                onChange={(e) => setRelatedExtractionId(e.target.value)}
                className={`w-full px-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-500/20 ${errors.relatedExtractionId ? 'border-red-300' : 'border-border-light'
                  }`}
              >
                <option value="">— Select a completed extraction —</option>
                {completedExtractions?.items.map((ext) => (
                  <option key={ext.id} value={ext.id}>
                    {ext.sourceName || 'Unknown Source'} — {ext.resultCount ?? '?'} results ({ext.status})
                    {ext.contentLanguage ? ` [${ext.contentLanguage.toUpperCase()}]` : ''}
                  </option>
                ))}
              </select>
              {errors.relatedExtractionId && <p className="text-xs text-red-600 mt-1">{errors.relatedExtractionId}</p>}
              <p className="text-xs text-ink-400 mt-1">
                Select the primary extraction whose Krithis will be enriched with lyrics from this source
              </p>
            </div>
          )}

          {/* Content Language */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">Content Language</label>
            <select
              value={contentLanguage}
              onChange={(e) => setContentLanguage(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              {languages.map((lang) => (
                <option key={lang.code} value={lang.code}>
                  {lang.label}
                </option>
              ))}
            </select>
            <p className="text-xs text-ink-400 mt-1">Language of the source document</p>
          </div>

          {/* Source (optional) */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">Registered Source</label>
            <select
              value={importSourceId}
              onChange={(e) => setImportSourceId(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option value="">— None (ad-hoc URL) —</option>
              {sourcesData?.items.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} (Tier {s.sourceTier})
                </option>
              ))}
            </select>
            <p className="text-xs text-ink-400 mt-1">Optionally link this request to a registered source</p>
          </div>

          {/* Source URL */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">
              Source URL <span className="text-red-500">*</span>
            </label>
            <input
              type="url"
              value={sourceUrl}
              onChange={(e) => setSourceUrl(e.target.value)}
              placeholder="https://example.com/krithis/page"
              className={`w-full px-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 ${errors.sourceUrl ? 'border-red-300' : 'border-border-light'}`}
            />
            {errors.sourceUrl && <p className="text-xs text-red-600 mt-1">{errors.sourceUrl}</p>}
          </div>

          {/* Format */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-2">
              Format <span className="text-red-500">*</span>
            </label>
            <div className="flex flex-wrap gap-2">
              {formats.map((format) => (
                <button
                  key={format}
                  type="button"
                  onClick={() => setSourceFormat(format)}
                  className={`px-3 py-1.5 text-xs font-semibold rounded-full border transition-colors ${sourceFormat === format
                      ? 'bg-primary text-white border-primary'
                      : 'bg-white text-ink-500 border-border-light hover:border-primary/30'
                    }`}
                >
                  {format}
                </button>
              ))}
            </div>
          </div>

          {/* Composer Hint */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">Composer Hint</label>
            <input
              type="text"
              value={composerHint}
              onChange={(e) => setComposerHint(e.target.value)}
              placeholder="e.g., Muthuswami Dikshitar"
              className="w-full px-3 py-2 text-sm border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
            <p className="text-xs text-ink-400 mt-1">Helps the extractor narrow down attribution</p>
          </div>

          {/* Page Range (for PDFs) */}
          {sourceFormat === 'PDF' && (
            <div>
              <label className="block text-sm font-medium text-ink-700 mb-1">Page Range</label>
              <input
                type="text"
                value={pageRange}
                onChange={(e) => setPageRange(e.target.value)}
                placeholder="e.g., 1-10 or 5,8,12-15"
                className="w-full px-3 py-2 text-sm border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
          )}

          {/* Expected Krithi Count */}
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">Expected Krithi Count</label>
            <input
              type="number"
              min={1}
              value={expectedKrithiCount}
              onChange={(e) => setExpectedKrithiCount(e.target.value)}
              placeholder="Optional"
              className={`w-full px-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 ${errors.expectedKrithiCount ? 'border-red-300' : 'border-border-light'}`}
            />
            {errors.expectedKrithiCount && (
              <p className="text-xs text-red-600 mt-1">{errors.expectedKrithiCount}</p>
            )}
            <p className="text-xs text-ink-400 mt-1">If known, the extraction will flag discrepancies</p>
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
              className={`px-5 py-2 text-white text-sm font-medium rounded-lg disabled:opacity-50 disabled:cursor-not-allowed transition-colors ${extractionIntent === 'ENRICH'
                  ? 'bg-amber-500 hover:bg-amber-600'
                  : 'bg-primary hover:bg-primary/90'
                }`}
            >
              {isSubmitting
                ? 'Submitting...'
                : extractionIntent === 'ENRICH'
                  ? 'Submit Enrichment'
                  : 'Submit Extraction'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ExtractionRequestModal;
