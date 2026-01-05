import React, { useState, useEffect } from 'react';
import { getImports, scrapeContent, reviewImport } from '../api/client';
import { ImportedKrithi, ImportReviewRequest } from '../types';
import { useToast, ToastContainer } from '../components/Toast';
import { ReviewImportModal } from '../components/ReviewImportModal';

const ImportsPage: React.FC = () => {
    const { toasts, removeToast, success, error } = useToast();
    const toast = { success, error }; // Helper wrapper

    const [activeTab, setActiveTab] = useState<'SCRAPE' | 'LIST'>('SCRAPE');
    const [scrapeUrl, setScrapeUrl] = useState('');
    const [isScraping, setIsScraping] = useState(false);
    const [scrapeResult, setScrapeResult] = useState<ImportedKrithi | null>(null);

    const [imports, setImports] = useState<ImportedKrithi[]>([]);
    const [loadingImports, setLoadingImports] = useState(false);
    const [reviewModalOpen, setReviewModalOpen] = useState(false);
    const [selectedImport, setSelectedImport] = useState<ImportedKrithi | null>(null);

    useEffect(() => {
        if (activeTab === 'LIST') {
            loadImports();
        }
    }, [activeTab]);

    const loadImports = async () => {
        setLoadingImports(true);
        try {
            const data = await getImports();
            setImports(data);
        } catch (e) {
            toast.error('Failed to load imports');
        } finally {
            setLoadingImports(false);
        }
    };

    const handleScrape = async () => {
        if (!scrapeUrl) return;
        setIsScraping(true);
        setScrapeResult(null);
        try {
            const result = await scrapeContent(scrapeUrl);
            setScrapeResult(result);
            toast.success('Content scraped successfully!');
        } catch (e) {
            toast.error('Scraping failed. Check URL or server logs.');
        } finally {
            setIsScraping(false);
        }
    };

    const handleReviewClick = (importItem: ImportedKrithi) => {
        setSelectedImport(importItem);
        setReviewModalOpen(true);
    };

    const handleReviewSubmit = async (id: string, request: ImportReviewRequest) => {
        try {
            const updated = await reviewImport(id, request);
            // Update the import in the list
            setImports(prev => prev.map(imp => imp.id === id ? updated : imp));
            toast.success('Import reviewed successfully!');
            setReviewModalOpen(false);
            setSelectedImport(null);
        } catch (e) {
            const errorMessage = e instanceof Error ? e.message : 'Failed to review import. Please try again.';
            toast.error(errorMessage);
            throw e;
        }
    };

    return (
        <div className="max-w-7xl mx-auto space-y-6">
            <ToastContainer toasts={toasts} onRemove={removeToast} />
            <ReviewImportModal
                isOpen={reviewModalOpen}
                onClose={() => {
                    setReviewModalOpen(false);
                    setSelectedImport(null);
                }}
                importItem={selectedImport}
                onReview={handleReviewSubmit}
            />
            <div className="flex flex-col gap-1">
                <h1 className="text-2xl font-display font-bold text-ink-900">Imports & Ingestion</h1>
                <p className="text-sm text-ink-500">Scrape content from external sources or review pending imports.</p>
            </div>

            {/* Tabs */}
            <div className="flex border-b border-border-light">
                <button
                    onClick={() => setActiveTab('SCRAPE')}
                    className={`px-6 py-3 text-sm font-medium border-b-2 transition-colors ${activeTab === 'SCRAPE'
                        ? 'border-primary text-primary'
                        : 'border-transparent text-ink-500 hover:text-ink-700'
                        }`}
                >
                    Scrape New
                </button>
                <button
                    onClick={() => setActiveTab('LIST')}
                    className={`px-6 py-3 text-sm font-medium border-b-2 transition-colors ${activeTab === 'LIST'
                        ? 'border-primary text-primary'
                        : 'border-transparent text-ink-500 hover:text-ink-700'
                        }`}
                >
                    Import History
                </button>
            </div>

            {/* Tab Content */}
            <div className="bg-white rounded-xl shadow-sm border border-border-light min-h-[400px]">
                {activeTab === 'SCRAPE' && (
                    <div className="p-8 max-w-3xl mx-auto flex flex-col items-center justify-center space-y-8 mt-12">
                        <div className="text-center space-y-2">
                            <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                <span className="material-symbols-outlined text-3xl text-purple-600">travel_explore</span>
                            </div>
                            <h2 className="text-xl font-bold text-ink-900">Web Scraper Service</h2>
                            <p className="text-ink-500">Enter a valid URL to extract Krithi metadata using Gemini AI.</p>
                        </div>

                        <div className="w-full flex gap-3">
                            <input
                                type="text"
                                value={scrapeUrl}
                                onChange={e => setScrapeUrl(e.target.value)}
                                placeholder="https://shivkumar.org/music/..."
                                className="flex-1 px-4 py-3 rounded-lg border border-border-light focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
                            />
                            <button
                                onClick={handleScrape}
                                disabled={!scrapeUrl || isScraping}
                                className="px-6 py-3 bg-black text-white rounded-lg font-medium hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                            >
                                {isScraping ? (
                                    <>
                                        <span className="material-symbols-outlined animate-spin text-sm">autorenew</span>
                                        Scraping...
                                    </>
                                ) : (
                                    <>
                                        <span className="material-symbols-outlined text-sm">download</span>
                                        Scrape
                                    </>
                                )}
                            </button>
                        </div>

                        {scrapeResult && (
                            <div className="w-full p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3 animate-fadeIn">
                                <span className="material-symbols-outlined text-green-600 mt-1">check_circle</span>
                                <div className="flex-1">
                                    <h3 className="font-bold text-green-800">Scrape Successful</h3>
                                    <p className="text-sm text-green-700 mt-1">
                                        Found: <strong>{scrapeResult.rawTitle || 'Untitled'}</strong> by {scrapeResult.rawComposer || 'Unknown'}
                                    </p>
                                    <div className="mt-3 flex gap-2">
                                        <button onClick={() => setActiveTab('LIST')} className="text-sm font-medium text-green-800 hover:text-green-900 underline">
                                            View in History
                                        </button>
                                    </div>
                                </div>
                            </div>
                        )}

                        <div className="w-full text-xs text-ink-400 text-center">
                            Supported domains: shivkumar.org (more coming soon)
                        </div>
                    </div>
                )}

                {activeTab === 'LIST' && (
                    <div className="p-0">
                        {loadingImports ? (
                            <div className="flex flex-col items-center justify-center py-20 text-ink-400">
                                <span className="material-symbols-outlined text-4xl animate-spin mb-2">autorenew</span>
                                <p>Loading imports...</p>
                            </div>
                        ) : imports.length === 0 ? (
                            <div className="flex flex-col items-center justify-center py-20 text-ink-400">
                                <span className="material-symbols-outlined text-4xl mb-4 text-ink-200">inbox</span>
                                <p>No imports found</p>
                                <button onClick={() => setActiveTab('SCRAPE')} className="mt-4 text-primary font-medium hover:underline">
                                    Scrape a new Krithi
                                </button>
                            </div>
                        ) : (
                            <table className="w-full text-left text-sm">
                                <thead className="bg-slate-50 text-ink-500 font-medium border-b border-border-light">
                                    <tr>
                                        <th className="px-6 py-3">Source / Key</th>
                                        <th className="px-6 py-3">Raw Title</th>
                                        <th className="px-6 py-3">Raw Composer</th>
                                        <th className="px-6 py-3">Status</th>
                                        <th className="px-6 py-3">Created At</th>
                                        <th className="px-6 py-3 text-right">Actions</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-border-light">
                                    {imports.map(imp => (
                                        <tr key={imp.id} className="hover:bg-slate-50 group">
                                            <td className="px-6 py-4 max-w-[200px] truncate text-ink-500" title={imp.sourceKey || ''}>{imp.sourceKey}</td>
                                            <td className="px-6 py-4 font-medium text-ink-900">{imp.rawTitle || '-'}</td>
                                            <td className="px-6 py-4 text-ink-600">{imp.rawComposer || '-'}</td>
                                            <td className="px-6 py-4">
                                                <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-bold ring-1 ring-inset ${
                                                    imp.importStatus === 'APPROVED' ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20' :
                                                    imp.importStatus === 'MAPPED' ? 'bg-green-50 text-green-700 ring-green-600/20' :
                                                    imp.importStatus === 'REJECTED' ? 'bg-red-50 text-red-700 ring-red-600/20' :
                                                    imp.importStatus === 'DISCARDED' ? 'bg-gray-50 text-gray-700 ring-gray-600/20' :
                                                    imp.importStatus === 'IN_REVIEW' ? 'bg-yellow-50 text-yellow-700 ring-yellow-600/20' :
                                                    'bg-blue-50 text-blue-700 ring-blue-700/10'
                                                }`}>
                                                    {imp.importStatus.replace(/_/g, ' ')}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 text-ink-500 font-mono text-xs">
                                                {new Date(imp.createdAt).toLocaleDateString()}
                                            </td>
                                            <td className="px-6 py-4 text-right">
                                                <button
                                                    onClick={() => handleReviewClick(imp)}
                                                    className="text-primary hover:text-primary-dark font-medium opacity-0 group-hover:opacity-100 transition-opacity"
                                                >
                                                    Review
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default ImportsPage;
