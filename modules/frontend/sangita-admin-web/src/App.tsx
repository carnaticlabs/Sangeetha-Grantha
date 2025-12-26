import React, { useState } from 'react';
import { ViewState } from './types';
import Sidebar from './components/Sidebar';
import TopBar from './components/TopBar';
import Dashboard from './pages/Dashboard';
import KrithiList from './pages/KrithiList';
import KrithiEditor from './pages/KrithiEditor';
import ReferenceData from './pages/ReferenceData';

const App: React.FC = () => {
  const [currentView, setCurrentView] = useState<ViewState>(ViewState.DASHBOARD);
  const [selectedKrithiId, setSelectedKrithiId] = useState<string | null>(null);

  const handleNavigate = (view: ViewState, id?: string) => {
    setCurrentView(view);
    if (id) {
      setSelectedKrithiId(id);
    } else if (view !== ViewState.KRITHI_DETAIL) {
      setSelectedKrithiId(null);
    }
  };

  const renderContent = () => {
    switch (currentView) {
      case ViewState.DASHBOARD:
        return <Dashboard onNavigate={handleNavigate} />;
      case ViewState.KRITHIS:
        return <KrithiList onNavigate={handleNavigate} />;
      case ViewState.KRITHI_DETAIL:
        return <KrithiEditor krithiId={selectedKrithiId} onBack={() => handleNavigate(ViewState.KRITHIS)} />;
      case ViewState.REFERENCE:
        return <ReferenceData />;
      default:
        return (
          <div className="flex flex-col items-center justify-center h-full text-ink-500">
            <span className="material-symbols-outlined text-6xl mb-4 text-ink-200">construction</span>
            <p className="font-serif text-xl">Module Under Development</p>
          </div>
        );
    }
  };

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50 font-sans text-ink-900">
      {/* Sidebar */}
      <Sidebar currentView={currentView} onNavigate={handleNavigate} />

      {/* Main Content Area */}
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        <TopBar />
        <main className="flex-1 overflow-y-auto p-6 md:p-8 lg:px-12 scroll-smooth">
          {renderContent()}
        </main>
      </div>
    </div>
  );
};

export default App;