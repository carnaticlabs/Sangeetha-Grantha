import React from 'react';
import type { SectionSummary } from '../../../types/sourcing';

interface StructureVisualiserProps {
  sections: SectionSummary[];
  compact?: boolean;
}

const sectionColors: Record<string, { bg: string; text: string; abbr: string }> = {
  PALLAVI: { bg: 'bg-blue-500', text: 'text-white', abbr: 'P' },
  ANUPALLAVI: { bg: 'bg-emerald-500', text: 'text-white', abbr: 'A' },
  CHARANAM: { bg: 'bg-amber-500', text: 'text-white', abbr: 'C' },
  MADHYAMA_KALA_SAHITYA: { bg: 'bg-teal-400', text: 'text-white', abbr: 'MK' },
  CHITTASWARAM: { bg: 'bg-purple-500', text: 'text-white', abbr: 'CS' },
};

const defaultSection = { bg: 'bg-slate-400', text: 'text-white', abbr: '?' };

const StructureVisualiser: React.FC<StructureVisualiserProps> = ({ sections, compact = false }) => {
  const sorted = [...sections].sort((a, b) => a.orderIndex - b.orderIndex);

  if (compact) {
    // Compact: horizontal sequence of small coloured blocks
    return (
      <div className="flex items-center gap-0.5" role="img" aria-label={`Structure: ${sorted.map((s) => s.sectionType).join(', ')}`}>
        {sorted.map((section, i) => {
          const config = sectionColors[section.sectionType] || defaultSection;
          return (
            <div
              key={i}
              className={`${config.bg} ${config.text} rounded-sm text-[9px] font-bold px-1 py-0.5 leading-none`}
              title={`${section.sectionType}${section.label ? ` (${section.label})` : ''}`}
            >
              {config.abbr}
            </div>
          );
        })}
      </div>
    );
  }

  // Full: labelled blocks with section names
  return (
    <div className="flex flex-wrap items-center gap-1.5" role="img" aria-label={`Structure: ${sorted.map((s) => s.sectionType).join(', ')}`}>
      {sorted.map((section, i) => {
        const config = sectionColors[section.sectionType] || defaultSection;
        return (
          <div
            key={i}
            className={`${config.bg} ${config.text} rounded-md text-xs font-semibold px-2.5 py-1 leading-none`}
            title={section.label || section.sectionType}
          >
            {config.abbr}
            {section.label ? ` ${section.label}` : ''}
          </div>
        );
      })}
    </div>
  );
};

export default StructureVisualiser;
