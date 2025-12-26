import React, { useState } from 'react';
import { GoogleGenAI } from "@google/genai";
import { LyricVariant, AuditEvent, TagCategory, ViewState } from '../types';

interface KrithiEditorProps {
  krithiId: string | null;
  onBack: () => void;
}

// --- MOCK DATA ---

const MOCK_VARIANTS: LyricVariant[] = [
  {
    id: 'v1',
    language: 'Telugu',
    script: 'Telugu',
    isPrimary: true,
    label: 'Standard Pathanthara',
    sampradaya: 'Walajapet',
    source: 'T.K. Govinda Rao Book',
    pallavi: 'ఎందరో మహానుభావులు అందరికీ వందనములు',
    anupallavi: 'చంద్రవర్ణుని అంద చందమును హృదయార విందమున జూచి బ్రహ్మానందం అనుభవించు వా',
    charanams: ['సామ గాన లోల మనసిజ లావణ్య ధన్య మూర్ధన్యుల్']
  },
  {
    id: 'v2',
    language: 'English',
    script: 'ISO-15919 (Diacritics)',
    isPrimary: false,
    label: 'Academic Transliteration',
    source: 'Music Academy Journal',
    pallavi: 'Endarō mahānubhāvulu andariki vandanamulu',
    anupallavi: 'Chandravarnuni anda chandamunu hrudayāra vindamuna jūchi brahmānandam anubhavinchu vā',
    charanams: ['Sāma gāna lōla manasija lāvanya dhanya mūrdhanyul']
  }
];

const MOCK_TAGS: TagCategory[] = [
  {
    category: 'Bhava',
    tags: [{ label: 'Bhakti', confidence: 'High' }, { label: 'Vairagya', confidence: 'Medium' }]
  },
  {
    category: 'Kshetra',
    tags: [{ label: 'Srirangam', confidence: 'High' }]
  },
  {
    category: 'Festival',
    tags: [{ label: 'Rama Navami', confidence: 'High' }]
  }
];

const MOCK_AUDIT: AuditEvent[] = [
  {
    id: 'e1',
    timestamp: '2023-10-24 10:30 AM',
    user: 'John Doe',
    action: 'Workflow',
    changes: [{ field: 'Status', before: 'Review', after: 'Published' }]
  },
  {
    id: 'e2',
    timestamp: '2023-10-23 04:15 PM',
    user: 'Jane Smith',
    action: 'Update',
    changes: [{ field: 'Raga', before: 'Kapi', after: 'Sri' }]
  },
  {
    id: 'e3',
    timestamp: '2023-10-22 09:00 AM',
    user: 'System',
    action: 'Create',
    changes: []
  }
];

// --- HELPER COMPONENTS (Defined outside to prevent re-render focus loss) ---

const SectionHeader: React.FC<{ title: string; action?: React.ReactNode }> = ({ title, action }) => (
    <div className="flex items-center justify-between mb-6 pb-2 border-b border-border-light">
        <h3 className="font-display text-lg font-bold text-ink-900">{title}</h3>
        {action}
    </div>
);

const InputField = ({ label, field, value, onChange, placeholder = '', highlight = false }: any) => (
  <div>
    <label className="block text-sm font-semibold text-ink-900 mb-2 flex justify-between">
        {label}
        {highlight && (
            <span className="text-[10px] text-purple-600 font-bold flex items-center gap-1 animate-pulse">
                <span className="material-symbols-outlined text-[12px]">auto_awesome</span>
                UPDATED
            </span>
        )}
    </label>
    <div className="relative">
        <input 
            type="text" 
            value={value} 
            onChange={(e) => onChange(e.target.value)}
            className={`w-full h-12 px-4 border rounded-lg text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent transition-all ${
                highlight ? 'border-purple-400 bg-purple-50' : 'border-border-light bg-slate-50'
            }`}
            placeholder={placeholder}
        />
    </div>
  </div>
);

const KrithiEditor: React.FC<KrithiEditorProps> = ({ krithiId, onBack }) => {
  const [activeTab, setActiveTab] = useState<'Metadata' | 'Lyrics' | 'Tags' | 'Audit'>('Metadata');
  
  // Metadata State
  const [metadata, setMetadata] = useState({
    title: "Endaro Mahanubhavulu",
    incipit: "Endarō mahānubhāvulu andariki vandanamulu",
    composer: "Tyagaraja",
    raga: "Sri",
    tala: "Adi",
    deity: "Rama",
    temple: "Srirangam",
    language: "Telugu",
    summary: "",
    notes: ""
  });

  // Lyrics State
  const [variants, setVariants] = useState<LyricVariant[]>(MOCK_VARIANTS);
  const [isEditingLyric, setIsEditingLyric] = useState(false);
  const [currentLyric, setCurrentLyric] = useState<LyricVariant | null>(null);

  // Tags State
  const [tags, setTags] = useState<TagCategory[]>(MOCK_TAGS);
  const [tagInput, setTagInput] = useState('');
  
  // Audit State
  const [auditFilter, setAuditFilter] = useState({ user: '', type: '' });

  // AI State
  const [aiThinking, setAiThinking] = useState(false);
  const [aiHighlights, setAiHighlights] = useState<string[]>([]);

  // --- ACTIONS ---

  const handleEditLyric = (variant?: LyricVariant) => {
    if (variant) {
      setCurrentLyric({ ...variant });
    } else {
      setCurrentLyric({
        id: `new_${Date.now()}`,
        language: '',
        script: '',
        isPrimary: false,
        label: '',
        source: '',
        pallavi: '',
        anupallavi: '',
        charanams: ['']
      });
    }
    setIsEditingLyric(true);
  };

  const handleSaveLyric = () => {
    if (!currentLyric) return;
    setVariants(prev => {
        const exists = prev.find(v => v.id === currentLyric.id);
        if (exists) return prev.map(v => v.id === currentLyric.id ? currentLyric : v);
        return [...prev, currentLyric];
    });
    setIsEditingLyric(false);
    setCurrentLyric(null);
  };

  const handleAiTransliterate = async () => {
    if (!currentLyric || !currentLyric.language) {
         alert("Please specify a target Language in the Language field first.");
         return;
    }
    
    // Find Primary Variant
    const primaryVariant = variants.find(v => v.isPrimary);
    if (!primaryVariant) {
        alert("No Primary variant found to transliterate from. Please mark a variant as Primary first.");
        return;
    }

    if (primaryVariant.id === currentLyric.id) {
         alert("Cannot transliterate the Primary variant onto itself. Create a new variant.");
         return;
    }

    setAiThinking(true);
    
    try {
        const ai = new GoogleGenAI({ apiKey: process.env.API_KEY });
        
        const prompt = `Act as a linguistic expert in Indian languages and Carnatic music.
        Transliterate the following Carnatic music lyrics from ${primaryVariant.language} (${primaryVariant.script}) to ${currentLyric.language}.
        
        Source Lyrics (Primary):
        Pallavi: ${primaryVariant.pallavi}
        Anupallavi: ${primaryVariant.anupallavi}
        Charanams: ${JSON.stringify(primaryVariant.charanams)}

        Output strictly in JSON format with keys: 'pallavi' (string), 'anupallavi' (string), and 'charanams' (array of strings).
        Ensure accurate transliteration suited for singing.`;

        const response = await ai.models.generateContent({
            model: 'gemini-3-flash-preview',
            contents: prompt,
            config: {
                responseMimeType: 'application/json'
            }
        });
        
        const jsonText = response.text;
        if (jsonText) {
            const data = JSON.parse(jsonText);
            setCurrentLyric(prev => ({
                ...prev!,
                pallavi: data.pallavi || '',
                anupallavi: data.anupallavi || '',
                charanams: Array.isArray(data.charanams) ? data.charanams : [],
                label: `AI Transliterated (${currentLyric.language})`,
                source: `Transliterated from ${primaryVariant.language} Primary`
            }));
        }
    } catch (e) {
        console.error("Transliteration Error", e);
        alert("AI Transliteration failed. Please check your API configuration.");
    } finally {
        setAiThinking(false);
    }
  };

  const handleAiSuggestTags = async () => {
    setAiThinking(true);
    try {
        const ai = new GoogleGenAI({ apiKey: process.env.API_KEY });
        const prompt = `Analyze the Carnatic composition "${metadata.title}" by ${metadata.composer} in Raga ${metadata.raga}.
        Suggest appropriate tags for the following categories: Bhava (Emotion), Kshetra (Temple/Place), and Deity.
        
        Output strictly in JSON format:
        {
          "Bhava": [{"label": "string", "confidence": "High" | "Medium" | "Low"}],
          "Kshetra": [{"label": "string", "confidence": "High" | "Medium" | "Low"}],
          "Deity": [{"label": "string", "confidence": "High" | "Medium" | "Low"}]
        }`;

        const response = await ai.models.generateContent({
             model: 'gemini-3-flash-preview',
             contents: prompt,
             config: { responseMimeType: 'application/json' }
        });

        const jsonText = response.text;
        if (jsonText) {
             const data = JSON.parse(jsonText);
             const newCategories: TagCategory[] = [];
             
             Object.keys(data).forEach(cat => {
                 if (Array.isArray(data[cat])) {
                     newCategories.push({
                         category: cat,
                         tags: data[cat].map((t: any) => ({ ...t, source: 'Gemini AI' }))
                     });
                 }
             });
             
             // Merge with existing tags logic could go here, for now replacing/appending
             setTags(prev => [...prev, ...newCategories]);
        }

    } catch (e) {
         console.error(e);
         // Fallback mock
         setTimeout(() => {
            const newTags: TagCategory = {
                category: 'AI Suggestions',
                tags: [
                    { label: 'Pancharatna Krithi', confidence: 'High', source: 'Gemini' },
                    { label: 'Ghanaraga', confidence: 'Medium', source: 'Gemini' }
                ]
            };
            setTags(prev => [...prev, newTags]);
         }, 1000);
    } finally {
        setAiThinking(false);
    }
  };

  const renderWorkflowPill = (status: string) => {
    const styles = status === 'Published' ? 'bg-green-50 text-green-700 border-green-200' :
                   status === 'Review' ? 'bg-amber-50 text-amber-700 border-amber-200' :
                   'bg-slate-100 text-slate-600 border-slate-200';
    return (
      <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border tracking-wide uppercase ${styles}`}>
        {status}
      </span>
    );
  };

  return (
    <div className="max-w-7xl mx-auto animate-fadeIn pb-12 relative">
      {/* 1. Top Header */}
      <div className="mb-6">
        <nav className="flex items-center gap-2 text-sm text-ink-500 mb-4">
          <button onClick={onBack} className="hover:text-primary transition-colors">Kritis</button>
          <span className="material-symbols-outlined text-[14px]">chevron_right</span>
          <span className="text-ink-900 font-medium">Edit Composition</span>
        </nav>

        <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-6">
          <div>
            <div className="flex items-center gap-3 mb-2 flex-wrap">
              <h1 className="font-display text-3xl font-bold text-ink-900 tracking-tight">{metadata.title}</h1>
              {renderWorkflowPill('Published')}
            </div>
            <p className="text-ink-500 font-medium text-sm flex items-center gap-2">
              {metadata.composer} <span className="text-ink-300">•</span> 
              {metadata.raga} Raga <span className="text-ink-300">•</span> 
              {metadata.tala} Tala
            </p>
          </div>
          
          <div className="flex gap-2 flex-wrap">
             <button className="px-4 py-2.5 bg-white border border-border-light text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors flex items-center gap-2">
                <span className="material-symbols-outlined text-[18px]">save</span>
                Save Draft
            </button>
            <button className="px-4 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors shadow-sm shadow-blue-500/20">
                Publish Changes
            </button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-border-light mb-8">
        <nav className="flex gap-8">
          {['Metadata', 'Lyrics', 'Tags', 'Audit'].map((tab) => (
            <button
              key={tab}
              onClick={() => { setActiveTab(tab as any); setIsEditingLyric(false); }}
              className={`pb-4 text-sm font-bold border-b-2 transition-colors ${
                activeTab === tab
                  ? 'border-primary text-primary'
                  : 'border-transparent text-ink-500 hover:text-ink-900 hover:border-slate-300'
              }`}
            >
              {tab}
            </button>
          ))}
        </nav>
      </div>

      {/* --- TAB CONTENT --- */}

      {/* 1. METADATA TAB */}
      {activeTab === 'Metadata' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-8">
            {/* Identity */}
            <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6 relative overflow-hidden">
              <SectionHeader title="Identity" />
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="md:col-span-2">
                   <InputField label="Title (Transliterated)" field="title" value={metadata.title} onChange={(v: string) => setMetadata({...metadata, title: v})} highlight={aiHighlights.includes('title')} />
                </div>
                <div className="md:col-span-2">
                   <InputField label="Incipit (First Line)" field="incipit" value={metadata.incipit} onChange={(v: string) => setMetadata({...metadata, incipit: v})} highlight={aiHighlights.includes('incipit')} />
                </div>
              </div>
            </div>

            {/* Canonical Links */}
            <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6">
              <SectionHeader title="Canonical Links" />
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <InputField label="Composer" field="composer" value={metadata.composer} onChange={(v: string) => setMetadata({...metadata, composer: v})} highlight={aiHighlights.includes('composer')} />
                <InputField label="Raga" field="raga" value={metadata.raga} onChange={(v: string) => setMetadata({...metadata, raga: v})} highlight={aiHighlights.includes('raga')} />
                <InputField label="Tala" field="tala" value={metadata.tala} onChange={(v: string) => setMetadata({...metadata, tala: v})} highlight={aiHighlights.includes('tala')} />
                <InputField label="Deity" field="deity" value={metadata.deity} onChange={(v: string) => setMetadata({...metadata, deity: v})} highlight={aiHighlights.includes('deity')} />
              </div>
            </div>
          </div>
          
           {/* Right Column */}
           <div className="space-y-6">
                <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-5">
                    <h4 className="text-xs font-bold text-ink-500 uppercase tracking-wider mb-4">Status</h4>
                    <div className="flex items-center gap-2">
                         <span className="material-symbols-outlined text-green-600">check_circle</span>
                         <span className="font-bold text-ink-900">Published</span>
                    </div>
                </div>
           </div>
        </div>
      )}

      {/* 2. LYRICS TAB */}
      {activeTab === 'Lyrics' && (
          <div className="space-y-6">
              {!isEditingLyric ? (
                  // LIST VIEW
                  <>
                    <div className="flex justify-between items-center">
                        <h3 className="font-display text-xl font-bold text-ink-900">Lyric Variants</h3>
                        <button 
                            onClick={() => handleEditLyric()}
                            className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors shadow-sm"
                        >
                            <span className="material-symbols-outlined text-[18px]">add</span>
                            Add Variant
                        </button>
                    </div>
                    <div className="grid grid-cols-1 gap-4">
                        {variants.map(variant => (
                            <div key={variant.id} className="bg-surface-light border border-border-light rounded-xl p-6 hover:shadow-md transition-shadow">
                                <div className="flex justify-between items-start mb-4">
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 bg-blue-50 text-primary rounded-lg">
                                            <span className="material-symbols-outlined text-[24px]">description</span>
                                        </div>
                                        <div>
                                            <h4 className="font-bold text-ink-900 text-lg">{variant.label || variant.language}</h4>
                                            <p className="text-sm text-ink-500">{variant.script} • {variant.source}</p>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        {variant.isPrimary && (
                                            <span className="px-2 py-1 bg-green-50 text-green-700 text-xs font-bold rounded uppercase tracking-wide border border-green-100">Primary</span>
                                        )}
                                        <button 
                                            onClick={() => handleEditLyric(variant)}
                                            className="p-2 text-ink-500 hover:text-primary hover:bg-slate-50 rounded-lg transition-colors"
                                        >
                                            <span className="material-symbols-outlined">edit</span>
                                        </button>
                                    </div>
                                </div>
                                <div className="bg-slate-50 p-4 rounded-lg font-serif text-ink-700 leading-relaxed text-sm line-clamp-3">
                                    {variant.pallavi}
                                </div>
                            </div>
                        ))}
                    </div>
                  </>
              ) : (
                  // EDITOR VIEW
                  <div className="flex flex-col xl:flex-row gap-8 animate-fadeIn">
                      {/* Left: Editor Form */}
                      <div className="flex-1 space-y-8">
                          <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6">
                                <div className="flex justify-between items-center mb-6">
                                    <h3 className="font-display text-lg font-bold text-ink-900">Variant Metadata</h3>
                                    {aiThinking && <span className="text-xs font-bold text-purple-600 animate-pulse flex items-center gap-1"><span className="material-symbols-outlined text-[14px]">auto_awesome</span> Processing...</span>}
                                </div>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    <InputField 
                                        label="Language" 
                                        value={currentLyric?.language} 
                                        onChange={(v: string) => setCurrentLyric({...currentLyric!, language: v})} 
                                        placeholder="e.g., English, Sanskrit, Tamil"
                                    />
                                    <InputField 
                                        label="Script" 
                                        value={currentLyric?.script} 
                                        onChange={(v: string) => setCurrentLyric({...currentLyric!, script: v})} 
                                    />
                                    <InputField 
                                        label="Label" 
                                        value={currentLyric?.label} 
                                        onChange={(v: string) => setCurrentLyric({...currentLyric!, label: v})} 
                                    />
                                    <InputField 
                                        label="Source Reference" 
                                        value={currentLyric?.source} 
                                        onChange={(v: string) => setCurrentLyric({...currentLyric!, source: v})} 
                                    />
                                </div>
                          </div>

                          <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6 space-y-6">
                                <div className="flex justify-between items-center">
                                    <h3 className="font-display text-lg font-bold text-ink-900">Lyrics Editor</h3>
                                    <button 
                                        onClick={handleAiTransliterate}
                                        className="text-xs font-bold text-purple-600 bg-purple-50 hover:bg-purple-100 px-3 py-1.5 rounded-lg border border-purple-200 transition-colors flex items-center gap-1"
                                    >
                                        <span className="material-symbols-outlined text-[16px]">translate</span>
                                        AI Transliterate from Primary
                                    </button>
                                </div>
                                
                                <div className="space-y-4">
                                    <label className="block text-sm font-bold text-ink-900">Pallavi</label>
                                    <textarea 
                                        rows={3}
                                        value={currentLyric?.pallavi}
                                        onChange={(e) => setCurrentLyric({...currentLyric!, pallavi: e.target.value})}
                                        className="w-full p-3 bg-slate-50 border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent font-serif"
                                        placeholder="Enter Pallavi..."
                                    />
                                </div>
                                <div className="space-y-4">
                                    <label className="block text-sm font-bold text-ink-900">Anupallavi</label>
                                    <textarea 
                                        rows={3}
                                        value={currentLyric?.anupallavi}
                                        onChange={(e) => setCurrentLyric({...currentLyric!, anupallavi: e.target.value})}
                                        className="w-full p-3 bg-slate-50 border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent font-serif"
                                        placeholder="Enter Anupallavi..."
                                    />
                                </div>
                                <div className="space-y-4">
                                    <div className="flex justify-between items-center">
                                        <label className="block text-sm font-bold text-ink-900">Charanams</label>
                                        <button 
                                            onClick={() => setCurrentLyric({...currentLyric!, charanams: [...(currentLyric?.charanams || []), '']})}
                                            className="text-xs font-bold text-primary hover:bg-primary-light px-2 py-1 rounded transition-colors"
                                        >
                                            + Add Charanam
                                        </button>
                                    </div>
                                    {currentLyric?.charanams.map((charanam, idx) => (
                                        <div key={idx} className="relative group">
                                            <span className="absolute -left-6 top-3 text-xs text-ink-400 font-bold">{idx + 1}</span>
                                            <textarea 
                                                rows={4}
                                                value={charanam}
                                                onChange={(e) => {
                                                    const newC = [...currentLyric!.charanams];
                                                    newC[idx] = e.target.value;
                                                    setCurrentLyric({...currentLyric!, charanams: newC});
                                                }}
                                                className="w-full p-3 bg-slate-50 border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent font-serif"
                                                placeholder={`Enter Charanam ${idx + 1}...`}
                                            />
                                            <button 
                                                onClick={() => {
                                                    const newC = currentLyric!.charanams.filter((_, i) => i !== idx);
                                                    setCurrentLyric({...currentLyric!, charanams: newC});
                                                }}
                                                className="absolute top-2 right-2 p-1 text-ink-400 hover:text-red-600 bg-white/80 rounded opacity-0 group-hover:opacity-100 transition-opacity"
                                            >
                                                <span className="material-symbols-outlined text-[18px]">delete</span>
                                            </button>
                                        </div>
                                    ))}
                                </div>
                          </div>
                          
                          {/* Footer Actions */}
                          <div className="flex items-center gap-4 pt-4 border-t border-border-light">
                                <button 
                                    onClick={() => setIsEditingLyric(false)}
                                    className="px-6 py-2.5 border border-border-light text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors"
                                >
                                    Cancel
                                </button>
                                <button 
                                    onClick={handleSaveLyric}
                                    className="px-6 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors shadow-sm"
                                >
                                    Save Variant
                                </button>
                                <div className="flex-1"></div>
                                <div className="flex items-center gap-2">
                                    <input 
                                        type="checkbox" 
                                        id="isPrimary"
                                        checked={currentLyric?.isPrimary}
                                        onChange={(e) => setCurrentLyric({...currentLyric!, isPrimary: e.target.checked})}
                                        className="rounded border-gray-300 text-primary focus:ring-primary" 
                                    />
                                    <label htmlFor="isPrimary" className="text-sm text-ink-700 font-medium">Mark as Primary</label>
                                </div>
                          </div>
                      </div>

                      {/* Right: Live Preview */}
                      <div className="w-full xl:w-96 flex-shrink-0">
                          <div className="sticky top-24 bg-white border border-border-light rounded-xl shadow-lg overflow-hidden">
                                <div className="bg-slate-50 border-b border-border-light p-4 flex justify-between items-center">
                                    <h4 className="font-bold text-ink-900 text-sm uppercase tracking-wide">Live Preview</h4>
                                    <span className="text-xs font-medium text-ink-500 bg-white border px-2 py-0.5 rounded">User View</span>
                                </div>
                                <div className="p-6 space-y-6 font-serif text-ink-900 leading-relaxed max-h-[80vh] overflow-y-auto">
                                    {currentLyric?.pallavi ? (
                                        <div>
                                            <span className="block text-xs font-bold text-ink-400 uppercase tracking-widest mb-1">Pallavi</span>
                                            <p>{currentLyric.pallavi}</p>
                                        </div>
                                    ) : <p className="text-ink-300 italic">Pallavi will appear here...</p>}

                                    {currentLyric?.anupallavi && (
                                        <div>
                                            <span className="block text-xs font-bold text-ink-400 uppercase tracking-widest mb-1">Anupallavi</span>
                                            <p>{currentLyric.anupallavi}</p>
                                        </div>
                                    )}

                                    {currentLyric?.charanams.map((c, i) => c && (
                                        <div key={i}>
                                            <span className="block text-xs font-bold text-ink-400 uppercase tracking-widest mb-1">Charanam {i+1}</span>
                                            <p>{c}</p>
                                        </div>
                                    ))}
                                </div>
                          </div>
                      </div>
                  </div>
              )}
          </div>
      )}

      {/* 3. TAGS TAB */}
      {activeTab === 'Tags' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              <div className="lg:col-span-2 space-y-6">
                    <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6">
                        <div className="flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center mb-6">
                             <div>
                                <h3 className="font-display text-lg font-bold text-ink-900">Managed Tags</h3>
                                <p className="text-sm text-ink-500">Controlled taxonomy for faceted search.</p>
                             </div>
                             <button 
                                onClick={handleAiSuggestTags}
                                disabled={aiThinking}
                                className="flex items-center gap-2 px-4 py-2 bg-purple-50 text-purple-700 border border-purple-200 rounded-lg text-sm font-bold hover:bg-purple-100 transition-colors"
                             >
                                <span className={`material-symbols-outlined text-[18px] ${aiThinking ? 'animate-spin' : ''}`}>
                                    {aiThinking ? 'sync' : 'auto_awesome'}
                                </span>
                                {aiThinking ? 'Analyzing...' : 'AI Suggest Tags'}
                             </button>
                        </div>

                        {/* Tag Input */}
                        <div className="relative mb-8">
                            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-400">add_circle</span>
                            <input 
                                type="text"
                                value={tagInput}
                                onChange={(e) => setTagInput(e.target.value)}
                                placeholder="Add a tag..."
                                className="w-full pl-10 pr-4 py-3 bg-slate-50 border border-border-light rounded-lg text-sm focus:ring-2 focus:ring-primary focus:border-transparent"
                            />
                        </div>

                        {/* Tag Categories */}
                        <div className="space-y-6">
                            {tags.map((cat) => (
                                <div key={cat.category} className="animate-fadeIn">
                                    <h4 className="text-xs font-bold text-ink-500 uppercase tracking-wide mb-3">{cat.category}</h4>
                                    <div className="flex flex-wrap gap-2">
                                        {cat.tags.map((tag, i) => (
                                            <div 
                                                key={i} 
                                                className={`group flex items-center gap-2 pl-3 pr-2 py-1.5 rounded-full border text-sm transition-colors ${
                                                    tag.source === 'Gemini AI' || tag.source === 'Gemini' ? 'bg-purple-50 border-purple-200 text-purple-900' : 
                                                    'bg-slate-50 border-border-light text-ink-700 hover:border-slate-300'
                                                }`}
                                            >
                                                <span className="font-medium">{tag.label}</span>
                                                {/* Confidence Indicator */}
                                                {tag.confidence && (
                                                    <div className="flex items-center gap-1" title={`Confidence: ${tag.confidence}`}>
                                                        <div className={`w-1.5 h-1.5 rounded-full ${
                                                            tag.confidence === 'High' ? 'bg-green-500' : 
                                                            tag.confidence === 'Medium' ? 'bg-yellow-500' : 'bg-red-500'
                                                        }`}></div>
                                                    </div>
                                                )}
                                                <button className="text-ink-400 hover:text-red-600 transition-colors ml-1">
                                                    <span className="material-symbols-outlined text-[16px]">close</span>
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
              </div>

              {/* Sidebar Guide */}
              <div>
                  <div className="bg-blue-50 border border-blue-100 rounded-xl p-5 sticky top-24">
                      <div className="flex items-start gap-3 mb-3">
                          <span className="material-symbols-outlined text-blue-600">info</span>
                          <h4 className="font-bold text-blue-900 text-sm">Tagging Guidelines</h4>
                      </div>
                      <p className="text-sm text-blue-800 leading-relaxed mb-4">
                          Tags are used to power the "Explore" feature. Please ensure you select tags from the approved ontology.
                      </p>
                      <ul className="text-sm text-blue-800 space-y-2 list-disc pl-4">
                          <li>Use <b>Bhava</b> for emotional content.</li>
                          <li>Use <b>Kshetra</b> for geographical references.</li>
                          <li>Use <b>Deity</b> for the primary subject.</li>
                      </ul>
                  </div>
              </div>
          </div>
      )}

      {/* 4. AUDIT TAB */}
      {activeTab === 'Audit' && (
          <div className="bg-surface-light border border-border-light rounded-xl shadow-sm overflow-hidden">
              {/* Filters */}
              <div className="p-4 border-b border-border-light bg-slate-50/50 flex gap-4">
                  <select 
                    className="bg-white border border-border-light rounded-lg text-sm py-2 px-3 text-ink-700 focus:ring-primary focus:border-primary"
                    value={auditFilter.user}
                    onChange={(e) => setAuditFilter({...auditFilter, user: e.target.value})}
                  >
                      <option value="">All Users</option>
                      <option value="John Doe">John Doe</option>
                      <option value="System">System</option>
                  </select>
                  <select 
                    className="bg-white border border-border-light rounded-lg text-sm py-2 px-3 text-ink-700 focus:ring-primary focus:border-primary"
                    value={auditFilter.type}
                    onChange={(e) => setAuditFilter({...auditFilter, type: e.target.value})}
                  >
                      <option value="">All Actions</option>
                      <option value="Update">Update</option>
                      <option value="Workflow">Workflow</option>
                  </select>
              </div>

              {/* Timeline */}
              <div className="p-6">
                  <div className="relative border-l-2 border-slate-200 ml-3 space-y-8">
                      {MOCK_AUDIT
                        .filter(e => !auditFilter.user || e.user === auditFilter.user)
                        .filter(e => !auditFilter.type || e.action === auditFilter.type)
                        .map((event) => (
                          <div key={event.id} className="relative pl-8 group">
                              {/* Dot */}
                              <div className={`absolute -left-[9px] top-0 w-4 h-4 rounded-full border-2 border-white shadow-sm ${
                                  event.action === 'Workflow' ? 'bg-green-500' : 
                                  event.action === 'Create' ? 'bg-primary' : 'bg-slate-400'
                              }`}></div>
                              
                              <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-2 mb-2">
                                  <div>
                                      <span className="text-xs font-bold text-ink-500 uppercase tracking-wide">{event.action}</span>
                                      <h4 className="font-bold text-ink-900">
                                          {event.user} <span className="font-normal text-ink-500">performed an action</span>
                                      </h4>
                                  </div>
                                  <time className="text-xs text-ink-400 font-mono bg-slate-100 px-2 py-1 rounded">{event.timestamp}</time>
                              </div>

                              {/* Changes Diff */}
                              {event.changes && event.changes.length > 0 && (
                                  <div className="bg-slate-50 border border-border-light rounded-lg overflow-hidden mt-2">
                                      <table className="w-full text-sm text-left">
                                          <thead className="bg-slate-100 text-xs font-bold text-ink-500 uppercase">
                                              <tr>
                                                  <th className="px-4 py-2 w-1/4">Field</th>
                                                  <th className="px-4 py-2 w-1/3">Before</th>
                                                  <th className="px-4 py-2 w-1/3">After</th>
                                              </tr>
                                          </thead>
                                          <tbody className="divide-y divide-border-light">
                                              {event.changes.map((change, i) => (
                                                  <tr key={i}>
                                                      <td className="px-4 py-2 font-medium text-ink-900">{change.field}</td>
                                                      <td className="px-4 py-2 text-red-600 bg-red-50/50 line-through decoration-red-300">{change.before}</td>
                                                      <td className="px-4 py-2 text-green-700 bg-green-50/50">{change.after}</td>
                                                  </tr>
                                              ))}
                                          </tbody>
                                      </table>
                                  </div>
                              )}
                          </div>
                      ))}
                  </div>
              </div>
          </div>
      )}
    </div>
  );
};

export default KrithiEditor;