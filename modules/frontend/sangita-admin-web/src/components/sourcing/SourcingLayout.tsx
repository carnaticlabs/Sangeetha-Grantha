import React from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';

const sourcingNavItems = [
  { path: '/admin/sourcing', label: 'Dashboard', icon: 'dashboard', end: true },
  { path: '/admin/sourcing/sources', label: 'Source Registry', icon: 'source' },
  { path: '/admin/sourcing/extractions', label: 'Extraction Queue', icon: 'manufacturing' },
  { path: '/admin/sourcing/evidence', label: 'Source Evidence', icon: 'fact_check' },
  { path: '/admin/sourcing/voting', label: 'Structural Voting', icon: 'how_to_vote' },
  { path: '/admin/sourcing/quality', label: 'Quality Dashboard', icon: 'analytics' },
];

const SourcingLayout: React.FC = () => {
  const location = useLocation();

  // Determine current page title from nav items
  const currentItem = sourcingNavItems.find((item) =>
    item.end
      ? location.pathname === item.path
      : location.pathname.startsWith(item.path)
  ) || sourcingNavItems[0];

  // Build breadcrumbs
  const breadcrumbs = [
    { label: 'Sourcing & Quality', path: '/admin/sourcing' },
  ];

  if (currentItem && !currentItem.end) {
    breadcrumbs.push({ label: currentItem.label, path: currentItem.path });
  }

  // Add detail breadcrumbs based on path segments
  const pathSegments = location.pathname.split('/').filter(Boolean);
  if (pathSegments.length > 3) {
    // We're on a detail page (e.g., /admin/sourcing/sources/:id)
    const detailId = pathSegments[3];
    if (detailId && detailId !== 'new') {
      breadcrumbs.push({ label: `Detail`, path: location.pathname });
    }
  }

  return (
    <div className="max-w-7xl mx-auto">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-1.5 text-sm text-ink-400 mb-4" aria-label="Breadcrumb">
        <NavLink to="/" className="hover:text-primary transition-colors">Home</NavLink>
        {breadcrumbs.map((crumb, i) => (
          <React.Fragment key={crumb.path}>
            <span className="material-symbols-outlined text-base">chevron_right</span>
            {i === breadcrumbs.length - 1 ? (
              <span className="text-ink-700 font-medium">{crumb.label}</span>
            ) : (
              <NavLink to={crumb.path} className="hover:text-primary transition-colors">
                {crumb.label}
              </NavLink>
            )}
          </React.Fragment>
        ))}
      </nav>

      {/* Sub-navigation tabs */}
      <div className="border-b border-border-light mb-6">
        <nav className="flex gap-1 -mb-px overflow-x-auto" aria-label="Sourcing navigation">
          {sourcingNavItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.end}
              className={({ isActive }) =>
                `flex items-center gap-2 px-4 py-2.5 text-sm font-medium whitespace-nowrap border-b-2 transition-colors ${
                  isActive
                    ? 'border-primary text-primary'
                    : 'border-transparent text-ink-500 hover:text-ink-700 hover:border-ink-200'
                }`
              }
            >
              <span className="material-symbols-outlined text-lg">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>

      {/* Page content */}
      <Outlet />
    </div>
  );
};

export default SourcingLayout;
