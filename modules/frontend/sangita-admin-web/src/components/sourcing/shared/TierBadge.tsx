import React from 'react';
import type { SourceTier } from '../../../types/sourcing';

interface TierBadgeProps {
  tier: SourceTier;
  size?: 'sm' | 'md' | 'lg';
  showTooltip?: boolean;
}

const tierConfig: Record<SourceTier, { label: string; colors: string; definition: string }> = {
  1: {
    label: 'T1',
    colors: 'bg-amber-100 text-amber-800 border-amber-300',
    definition: 'Tier 1 — Primary Authority: Official publications, direct composer manuscripts, authenticated institutional sources',
  },
  2: {
    label: 'T2',
    colors: 'bg-slate-200 text-slate-700 border-slate-300',
    definition: 'Tier 2 — Scholarly: Peer-reviewed academic works, established musicological references',
  },
  3: {
    label: 'T3',
    colors: 'bg-orange-100 text-orange-800 border-orange-300',
    definition: 'Tier 3 — Curated: Well-maintained community databases, curated web portals with editorial oversight',
  },
  4: {
    label: 'T4',
    colors: 'bg-blue-100 text-blue-700 border-blue-300',
    definition: 'Tier 4 — Community: Community-contributed content, blogs, personal collections, fan sites',
  },
  5: {
    label: 'T5',
    colors: 'bg-slate-100 text-slate-500 border-slate-200',
    definition: 'Tier 5 — Unverified: Automated scrapes, unverified bulk imports, machine-generated content',
  },
};

const sizeClasses: Record<string, string> = {
  sm: 'text-[10px] px-1.5 py-0.5',
  md: 'text-xs px-2 py-0.5',
  lg: 'text-sm px-2.5 py-1',
};

const TierBadge: React.FC<TierBadgeProps> = ({ tier, size = 'md', showTooltip = true }) => {
  const config = tierConfig[tier];

  return (
    <span
      className={`inline-flex items-center font-semibold rounded-full border ${config.colors} ${sizeClasses[size]} leading-none`}
      title={showTooltip ? config.definition : undefined}
      role="img"
      aria-label={config.definition}
    >
      {config.label}
    </span>
  );
};

export default TierBadge;
