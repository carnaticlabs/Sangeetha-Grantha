import React from 'react';
import type { SourceFilterParams, SourceTier, SourceFormat } from '../../types/sourcing';

interface SourceFilterBarProps {
  filters: SourceFilterParams;
  onFiltersChange: (filters: SourceFilterParams) => void;
}

const tiers: SourceTier[] = [1, 2, 3, 4, 5];
const formats: SourceFormat[] = ['HTML', 'PDF', 'DOCX', 'API', 'MANUAL'];

const SourceFilterBar: React.FC<SourceFilterBarProps> = ({ filters, onFiltersChange }) => {
  const toggleTier = (tier: SourceTier) => {
    const current = filters.tier || [];
    const next = current.includes(tier) ? current.filter((t) => t !== tier) : [...current, tier];
    onFiltersChange({ ...filters, tier: next.length > 0 ? next : undefined });
  };

  const toggleFormat = (format: SourceFormat) => {
    const current = filters.format || [];
    const next = current.includes(format) ? current.filter((f) => f !== format) : [...current, format];
    onFiltersChange({ ...filters, format: next.length > 0 ? next : undefined });
  };

  return (
    <div className="bg-white rounded-xl border border-border-light p-4 mb-6">
      <div className="flex flex-wrap items-center gap-3">
        {/* Tier Filter */}
        <div className="flex items-center gap-1.5">
          <span className="text-xs font-semibold text-ink-500 uppercase mr-1">Tier</span>
          {tiers.map((tier) => {
            const isSelected = filters.tier?.includes(tier);
            return (
              <button
                key={tier}
                onClick={() => toggleTier(tier)}
                className={`px-2 py-1 text-xs font-semibold rounded-full border transition-colors ${
                  isSelected
                    ? 'bg-primary text-white border-primary'
                    : 'bg-white text-ink-500 border-border-light hover:border-primary/30'
                }`}
              >
                T{tier}
              </button>
            );
          })}
        </div>

        <div className="w-px h-6 bg-border-light" />

        {/* Format Filter */}
        <div className="flex items-center gap-1.5">
          <span className="text-xs font-semibold text-ink-500 uppercase mr-1">Format</span>
          {formats.map((format) => {
            const isSelected = filters.format?.includes(format);
            return (
              <button
                key={format}
                onClick={() => toggleFormat(format)}
                className={`px-2 py-1 text-[10px] font-semibold uppercase rounded-full border transition-colors ${
                  isSelected
                    ? 'bg-primary text-white border-primary'
                    : 'bg-white text-ink-500 border-border-light hover:border-primary/30'
                }`}
              >
                {format}
              </button>
            );
          })}
        </div>

        <div className="w-px h-6 bg-border-light" />

        {/* Search */}
        <div className="flex-1 min-w-[200px]">
          <div className="relative">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-400 text-lg">search</span>
            <input
              type="text"
              placeholder="Search by name or URL..."
              value={filters.search || ''}
              onChange={(e) => onFiltersChange({ ...filters, search: e.target.value || undefined })}
              className="w-full pl-9 pr-3 py-2 text-sm border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
          </div>
        </div>

        {/* Clear Filters */}
        {(filters.tier || filters.format || filters.search) && (
          <button
            onClick={() => onFiltersChange({})}
            className="text-xs text-ink-400 hover:text-primary transition-colors"
          >
            Clear all
          </button>
        )}
      </div>
    </div>
  );
};

export default SourceFilterBar;
