import React from 'react';
import { NavLink } from 'react-router-dom';
import { ViewState } from '../types';

interface SidebarProps {
  currentView?: ViewState; // Optional/Deprecated in favor of router state
}

const Sidebar: React.FC<SidebarProps> = () => {
  const navItems = [
    { id: ViewState.DASHBOARD, label: 'Dashboard', icon: 'dashboard' },
    { id: ViewState.KRITHIS, label: 'Kritis', icon: 'music_note' }, // Icon updated to match reference
    { id: ViewState.REFERENCE, label: 'Reference Data', icon: 'library_books' }, // Icon updated
    { id: ViewState.IMPORTS, label: 'Imports', icon: 'upload_file' },
    { id: ViewState.TAGS, label: 'Tags', icon: 'label' },
  ];



  return (
    <aside className="w-64 bg-surface-light border-r border-border-light flex-shrink-0 hidden md:flex flex-col z-20">
      {/* Brand Header */}
      <div className="p-6">
        <div className="flex flex-col gap-1">
          <h1 className="text-ink-900 text-lg font-display font-bold leading-normal tracking-tight">Sangita Grantha</h1>
          <p className="text-ink-500 text-xs font-medium uppercase tracking-wide">Admin Portal</p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-4 space-y-2 overflow-y-auto">
        {navItems.map((item) => {
          let path = '/';
          switch (item.id) {
            case ViewState.DASHBOARD: path = '/'; break;
            case ViewState.KRITHIS: path = '/krithis'; break;
            case ViewState.REFERENCE: path = '/reference'; break;
            case ViewState.IMPORTS: path = '/imports'; break;
            case ViewState.TAGS: path = '/tags'; break;
          }

          return (
            <NavLink
              key={item.id}
              to={path}
              className={({ isActive }) => `w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors duration-200 group ${isActive
                ? 'bg-primary-light text-primary'
                : 'text-ink-500 hover:bg-slate-100 hover:text-ink-900'
                }`}
            >
              {({ isActive }) => (
                <>
                  <span
                    className={`material-symbols-outlined text-2xl ${isActive ? 'text-primary fill-1' : 'text-ink-500 group-hover:text-primary transition-colors'
                      }`}
                  >
                    {item.icon}
                  </span>
                  {item.label}
                </>
              )}
            </NavLink>
          );
        })}

        <div className="mt-8 px-3 py-2 text-xs font-bold text-ink-400 uppercase tracking-wider">
          System
        </div>
        <button className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-ink-500 hover:bg-slate-100 hover:text-ink-900 transition-colors group">
          <span className="material-symbols-outlined text-2xl group-hover:text-primary transition-colors">settings</span>
          Settings
        </button>
        <button className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-ink-500 hover:bg-slate-100 hover:text-ink-900 transition-colors group">
          <span className="material-symbols-outlined text-2xl group-hover:text-primary transition-colors">group</span>
          Users
        </button>
      </nav>

      {/* Footer / Logout */}
      <div className="p-4 border-t border-border-light">
        <button className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-ink-500 hover:text-red-600 hover:bg-red-50 transition-colors">
          <span className="material-symbols-outlined text-xl">logout</span>
          <span className="text-sm font-medium">Sign Out</span>
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;