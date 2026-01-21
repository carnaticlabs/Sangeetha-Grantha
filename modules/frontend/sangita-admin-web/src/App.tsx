import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import TopBar from './components/TopBar';
import Dashboard from './pages/Dashboard';
import KrithiList from './pages/KrithiList';
import KrithiEditor from './pages/KrithiEditor';
import ReferenceData from './pages/ReferenceData';
import ImportsPage from './pages/ImportsPage';
import TagsPage from './pages/TagsPage';
import UsersPage from './pages/UsersPage';
import RolesPage from './pages/RolesPage';
import BulkImportPage from './pages/BulkImport';
import ImportReviewPage from './pages/ImportReview';

// Default admin token for development (matches backend default)
const DEFAULT_ADMIN_TOKEN = 'dev-admin-token';

const App: React.FC = () => {
  // Automatically authenticate as admin on app load
  useEffect(() => {
    // Only set token if not already present (allows manual override if needed)
    if (!localStorage.getItem('authToken')) {
      localStorage.setItem('authToken', DEFAULT_ADMIN_TOKEN);
    }
  }, []);

  return (
    <Router>
      <div className="flex h-screen overflow-hidden bg-slate-50 font-sans text-ink-900">
        {/* Sidebar */}
        <Sidebar />

        {/* Main Content Area */}
        <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
          <TopBar />
          <main className="flex-1 overflow-y-auto p-6 md:p-8 lg:px-12 scroll-smooth">
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/krithis" element={<KrithiList />} />
              <Route path="/krithis/new" element={<KrithiEditor />} />
              <Route path="/krithis/:id" element={<KrithiEditor />} />
              <Route path="/reference" element={<ReferenceData />} />
              <Route path="/imports" element={<ImportsPage />} />
              <Route path="/bulk-import" element={<BulkImportPage />} />
              <Route path="/bulk-import/review" element={<ImportReviewPage />} />
              <Route path="/tags" element={<TagsPage />} />
              <Route path="/users" element={<UsersPage />} />
              <Route path="/roles" element={<RolesPage />} />
              {/* Fallback for other routes */}
              <Route path="*" element={
                <div className="flex flex-col items-center justify-center h-full text-ink-500">
                  <span className="material-symbols-outlined text-6xl mb-4 text-ink-200">construction</span>
                  <p className="font-serif text-xl">Module Under Development</p>
                  <button onClick={() => window.history.back()} className="mt-4 text-primary hover:underline">Go Back</button>
                </div>
              } />
            </Routes>
          </main>
        </div>
      </div>
    </Router>
  );
};

export default App;