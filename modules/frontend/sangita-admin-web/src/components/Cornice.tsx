import React from 'react';

/**
 * Painted cornice — the Rasika signature header band ported from the mobile
 * RasikaOrnaments.PaintedCornice. Green · gold · scalloped teal · coral base.
 * Styling lives in src/index.css (.rasika-cornice); this is a thin wrapper so
 * pages can drop the temple-header band anywhere.
 */
const Cornice: React.FC<{ className?: string }> = ({ className = '' }) => (
  <div className={`rasika-cornice ${className}`} aria-hidden="true">
    <i className="band-green" />
    <i className="band-gold" />
    <i className="band-teal" />
    <i className="band-coral" />
  </div>
);

export default Cornice;
