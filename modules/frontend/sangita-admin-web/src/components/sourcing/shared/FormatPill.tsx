import React from 'react';

interface FormatPillProps {
  format: string;
}

const formatColors: Record<string, string> = {
  PDF: 'bg-red-50 text-red-700 border-red-200',
  HTML: 'bg-blue-50 text-blue-700 border-blue-200',
  DOCX: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  API: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  MANUAL: 'bg-purple-50 text-purple-700 border-purple-200',
  IMAGE: 'bg-amber-50 text-amber-700 border-amber-200',
};

const defaultColors = 'bg-slate-50 text-slate-600 border-slate-200';

const FormatPill: React.FC<FormatPillProps> = ({ format }) => {
  const colors = formatColors[format.toUpperCase()] || defaultColors;

  return (
    <span
      className={`inline-flex items-center text-[10px] font-semibold uppercase tracking-wide px-2 py-0.5 rounded-full border ${colors} leading-none`}
    >
      {format}
    </span>
  );
};

export default FormatPill;
