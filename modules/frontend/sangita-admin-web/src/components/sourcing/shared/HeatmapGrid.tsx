import React from 'react';
import type { ColorScale } from '../../../types/sourcing';

interface HeatmapGridProps {
  rows: string[];
  columns: string[];
  data: number[][]; // data[rowIndex][colIndex] = percentage 0–100
  colorScale?: ColorScale;
}

function getCellColor(value: number, scale: ColorScale): string {
  const v = Math.max(0, Math.min(100, value));
  if (scale === 'heat') {
    if (v === 0) return 'bg-slate-50';
    if (v < 25) return 'bg-red-100';
    if (v < 50) return 'bg-amber-100';
    if (v < 75) return 'bg-yellow-100';
    return 'bg-emerald-100';
  }
  if (scale === 'blue') {
    if (v === 0) return 'bg-slate-50';
    if (v < 25) return 'bg-blue-50';
    if (v < 50) return 'bg-blue-100';
    if (v < 75) return 'bg-blue-200';
    return 'bg-blue-300';
  }
  // Default: green scale
  if (v === 0) return 'bg-slate-50';
  if (v < 25) return 'bg-emerald-50';
  if (v < 50) return 'bg-emerald-100';
  if (v < 75) return 'bg-emerald-200';
  return 'bg-emerald-300';
}

function getCellTextColor(value: number): string {
  return value >= 75 ? 'text-ink-700' : value > 0 ? 'text-ink-600' : 'text-ink-300';
}

const HeatmapGrid: React.FC<HeatmapGridProps> = ({
  rows,
  columns,
  data,
  colorScale = 'green',
}) => {
  return (
    <div className="overflow-x-auto" role="table" aria-label="Coverage heatmap">
      <table className="text-xs border-collapse w-full">
        <thead>
          <tr>
            <th className="px-2 py-1.5 text-left font-semibold text-ink-500 sticky left-0 bg-white z-10" />
            {columns.map((col) => (
              <th
                key={col}
                className="px-2 py-1.5 text-center font-semibold text-ink-500 whitespace-nowrap"
                title={col}
              >
                {col.length > 8 ? col.slice(0, 6) + '..' : col}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, ri) => (
            <tr key={row}>
              <td className="px-2 py-1 font-medium text-ink-700 whitespace-nowrap sticky left-0 bg-white z-10 border-r border-border-light">
                {row}
              </td>
              {columns.map((col, ci) => {
                const value = data[ri]?.[ci] ?? 0;
                return (
                  <td
                    key={col}
                    className={`px-2 py-1 text-center tabular-nums font-medium border border-white/50 rounded-sm ${getCellColor(value, colorScale)} ${getCellTextColor(value)}`}
                    title={`${row} × ${col}: ${value}%`}
                  >
                    {value > 0 ? `${value}` : '—'}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default HeatmapGrid;
