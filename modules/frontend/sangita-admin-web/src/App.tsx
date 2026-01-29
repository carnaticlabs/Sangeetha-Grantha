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
import Login from './pages/Login';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      staleTime: 60 * 1000, // 1 minute
      retry: 1,
    },
  },
});

const App: React.FC = () => {


  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <div className="flex h-screen overflow-hidden bg-slate-50 font-sans text-ink-900">
          {/* Sidebar */}
          <Sidebar />

          {/* Main Content Area */}
          <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
            <TopBar />
            <main className="flex-1 overflow-y-auto p-6 md:p-8 lg:px-12 scroll-smooth">
              <Routes>
                <Route path="/login" element={<Login />} />
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
    </QueryClientProvider>
  );
};

export default App;