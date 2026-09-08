import React from 'react';

const TopBar: React.FC = () => {
  return (
    <header className="h-16 bg-cream-deep border-b border-border-light flex items-center justify-between px-6 md:px-8 sticky top-0 z-10">
      {/* Left: Context/Search */}
      <div className="flex items-center flex-1 gap-6">
        <div className="relative w-full max-w-md hidden md:block">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-500 text-[20px]">
            search
          </span>
          <input
            type="text"
            placeholder="Search kritis, ragas, or composers..."
            className="w-full pl-10 pr-4 py-2 bg-surface-light border border-border-light rounded-lg text-sm text-ink-900 caret-primary focus:ring-2 focus:ring-primary/30 focus:border-primary/40 placeholder:text-ink-400 transition-shadow"
          />
        </div>
      </div>

      {/* Right: Environment & Actions */}
      <div className="flex items-center gap-4">
        {/* Environment Badge */}
        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-surface-light border border-border-light">
          <div className="w-1.5 h-1.5 rounded-full bg-accent animate-pulse"></div>
          <span className="text-xs font-semibold text-accent tracking-[0.12em] uppercase">Staging</span>
        </div>

        <div className="h-6 w-px bg-border-light mx-2 hidden sm:block"></div>
        
        <button className="p-2 text-ink-500 hover:text-ink-900 hover:bg-slate-100 rounded-full transition-colors md:hidden">
            <span className="material-symbols-outlined text-[22px]">menu</span>
        </button>
      </div>
    </header>
  );
};

export default TopBar;