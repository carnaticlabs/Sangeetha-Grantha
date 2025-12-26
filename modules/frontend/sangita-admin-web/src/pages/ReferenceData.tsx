import React, { useState } from 'react';

// --- Types & Mocks ---

type EntityType = 'Ragas' | 'Talas' | 'Composers' | 'Temples' | 'Languages' | 'Musical Forms' | null;
type ViewMode = 'HOME' | 'LIST' | 'FORM';

interface BaseEntity {
  id: string;
  name: string;
  normalizedName: string;
  updatedAt: string;
  updatedBy: string;
  status: 'Active' | 'Archived' | 'Draft';
}

interface Composer extends BaseEntity {
  type: 'Composers';
  birthYear?: string;
  deathYear?: string;
  place?: string;
  notes?: string;
}

interface Raga extends BaseEntity {
  type: 'Ragas';
  melakartaNumber?: string;
  parentRaga?: string;
  arohanam: string;
  avarohanam: string;
  notes?: string;
}

interface Tala extends BaseEntity {
  type: 'Talas';
  angaStructure: string;
  beatCount: number;
  notes?: string;
}

interface Temple extends BaseEntity {
  type: 'Temples';
  location: string;
  primaryDeity: string;
  coordinates?: string;
  aliases?: { lang: string; script: string; name: string }[];
  notes?: string;
}

// Union type for selected item
type ReferenceItem = Composer | Raga | Tala | Temple;

// Mock Data
const MOCK_COMPOSERS: Composer[] = [
    { id: 'c1', type: 'Composers', name: 'Tyagaraja', normalizedName: 'Tyāgarāja', birthYear: '1767', deathYear: '1847', place: 'Tiruvarur', updatedAt: '2023-10-01', updatedBy: 'JD', status: 'Active' },
    { id: 'c2', type: 'Composers', name: 'Muthuswami Dikshitar', normalizedName: 'Muthusvāmi Dīkṣitar', birthYear: '1775', deathYear: '1835', place: 'Tiruvarur', updatedAt: '2023-09-15', updatedBy: 'JD', status: 'Active' },
];

const MOCK_RAGAS: Raga[] = [
    { id: 'r1', type: 'Ragas', name: 'Mayamalavagowla', normalizedName: 'Māyāmāḷavagauḷa', melakartaNumber: '15', arohanam: 'S R1 G3 M1 P D1 N3 S', avarohanam: 'S N3 D1 P M1 G3 R1 S', updatedAt: '2023-10-10', updatedBy: 'System', status: 'Active' },
    { id: 'r2', type: 'Ragas', name: 'Kalyani', normalizedName: 'Kalyāṇi', melakartaNumber: '65', arohanam: 'S R2 G3 M2 P D2 N3 S', avarohanam: 'S N3 D2 P M2 G3 R2 S', updatedAt: '2023-10-12', updatedBy: 'System', status: 'Active' },
];

const MOCK_TALAS: Tala[] = [
    { id: 't1', type: 'Talas', name: 'Adi', normalizedName: 'Ādi', angaStructure: 'I4 0 0', beatCount: 8, updatedAt: '2023-08-20', updatedBy: 'JD', status: 'Active' },
];

const MOCK_TEMPLES: Temple[] = [
    { id: 'tm1', type: 'Temples', name: 'Srirangam', normalizedName: 'Śrīraṅgam', location: 'Trichy, Tamil Nadu', primaryDeity: 'Ranganatha Swamy', updatedAt: '2023-10-22', updatedBy: 'JD', status: 'Active', aliases: [{ lang: 'Tamil', script: 'Tamil', name: 'திருவரங்கம்' }] },
];

// --- Sub-Components ---

// Reusable styled input components matching the reference design
const FormInput = ({ label, placeholder, defaultValue, required = false, type = "text", help, className = "" }: any) => (
    <div className={`space-y-2 ${className}`}>
        <label className="block text-sm font-semibold text-ink-900">
            {label} {required && <span className="text-red-500">*</span>}
        </label>
        <div className="relative">
            <input 
                type={type} 
                defaultValue={defaultValue} 
                placeholder={placeholder} 
                className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 placeholder-ink-400 focus:ring-2 focus:ring-primary focus:border-transparent transition-all" 
            />
            {type === 'number' && (
                <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none text-ink-400">
                    <span className="material-symbols-outlined text-lg">tag</span>
                </div>
            )}
        </div>
        {help && <p className="text-xs text-ink-500">{help}</p>}
    </div>
);

const FormTextarea = ({ label, placeholder, defaultValue, rows = 4, className = "" }: any) => (
    <div className={`space-y-2 ${className}`}>
        <label className="block text-sm font-semibold text-ink-900">{label}</label>
        <textarea 
            rows={rows} 
            defaultValue={defaultValue} 
            placeholder={placeholder} 
            className="w-full p-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 placeholder-ink-400 focus:ring-2 focus:ring-primary focus:border-transparent transition-all resize-none"
        ></textarea>
    </div>
);

const FormSelect = ({ label, options, defaultValue, className = "" }: any) => (
    <div className={`space-y-2 ${className}`}>
        <label className="block text-sm font-semibold text-ink-900">{label}</label>
        <div className="relative">
             <select 
                defaultValue={defaultValue} 
                className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent appearance-none"
             >
                {options.map((opt: string) => <option key={opt}>{opt}</option>)}
             </select>
             <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-ink-500 pointer-events-none">expand_more</span>
        </div>
    </div>
);

const EntityForm: React.FC<{ 
    entityType: EntityType; 
    initialData?: ReferenceItem | null; 
    onSave: () => void; 
    onCancel: () => void; 
}> = ({ entityType, initialData, onSave, onCancel }) => {
    
    // Layout Structure
    return (
        <div className="max-w-5xl mx-auto space-y-8 animate-fadeIn">
             {/* Header */}
             <div className="space-y-4">
                <nav className="flex items-center gap-2 text-sm text-ink-500">
                    <span className="hover:text-primary transition-colors cursor-pointer" onClick={onCancel}>Reference Data</span>
                    <span className="material-symbols-outlined text-base">chevron_right</span>
                    <span className="hover:text-primary transition-colors cursor-pointer" onClick={onCancel}>{entityType}</span>
                    <span className="material-symbols-outlined text-base">chevron_right</span>
                    <span className="text-ink-900 font-medium">{initialData ? 'Edit' : 'Create'} {entityType?.slice(0, -1)}</span>
                </nav>
                <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
                    <div>
                        <h2 className="text-3xl md:text-4xl font-display font-bold text-ink-900 tracking-tight">
                            {initialData ? 'Edit' : 'Create'} {entityType?.slice(0, -1)}
                        </h2>
                        <p className="text-ink-500 mt-2 text-base">Define canonical details for this entity.</p>
                    </div>
                    {/* Status Badge */}
                    <div className="hidden sm:block">
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-blue-50 text-blue-700 border border-blue-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-blue-600"></span>
                            Draft
                        </span>
                    </div>
                </div>
            </div>

            {/* Form Card */}
            <div className="bg-surface-light rounded-xl shadow-sm border border-border-light overflow-hidden">
                <div className="p-6 md:p-8 space-y-8">
                    {/* Render Form Fields based on Type */}
                    {entityType === 'Talas' && (
                        <div className="space-y-8">
                             <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                                <FormInput label="Name" placeholder="e.g. Adi Tala" defaultValue={(initialData as Tala)?.name} required />
                                <div className="space-y-2">
                                    <div className="flex items-center justify-between">
                                        <label className="block text-sm font-semibold text-ink-900">Normalized Name</label>
                                        <span className="text-xs text-ink-400">Auto-generated</span>
                                    </div>
                                    <input className="w-full h-12 px-4 rounded-lg bg-slate-100 border border-border-light text-ink-500 focus:outline-none cursor-not-allowed" readOnly placeholder="e.g. adi_tala" defaultValue={(initialData as Tala)?.normalizedName} />
                                </div>
                             </div>
                             <div className="grid grid-cols-1 md:grid-cols-12 gap-6 md:gap-8">
                                <FormInput label="Beat Count (Aksharas)" placeholder="e.g. 8" type="number" defaultValue={(initialData as Tala)?.beatCount} className="md:col-span-4" />
                                <FormInput label="Anga Structure" placeholder="e.g. I4 O O" help="Example: I4 O O for Adi Tala" defaultValue={(initialData as Tala)?.angaStructure} className="md:col-span-8" />
                             </div>
                             <FormTextarea label="Notes" placeholder="Add any additional context, historical details, or usage notes..." defaultValue={(initialData as Tala)?.notes} />
                        </div>
                    )}

                    {entityType === 'Ragas' && (
                        <div className="space-y-8">
                             <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                                <FormInput label="Name" placeholder="e.g. Mayamalavagowla" defaultValue={(initialData as Raga)?.name} required />
                                <div className="space-y-2">
                                    <label className="block text-sm font-semibold text-ink-900">Parent Raga</label>
                                    <select className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent">
                                        <option>Select Parent...</option>
                                        <option>Melakarta</option>
                                        <option>Janya</option>
                                    </select>
                                </div>
                                <FormInput label="Melakarta Number" placeholder="1-72" defaultValue={(initialData as Raga)?.melakartaNumber} />
                                <FormInput label="Normalized Name" placeholder="e.g. mayamulavagowla" defaultValue={(initialData as Raga)?.normalizedName} />
                             </div>
                             <div className="space-y-6">
                                <FormInput label="Arohanam" placeholder="Format: S R1 G3 M1 P D1 N3 S" defaultValue={(initialData as Raga)?.arohanam} />
                                <FormInput label="Avarohanam" placeholder="Format: S N3 D1 P M1 G3 R1 S" defaultValue={(initialData as Raga)?.avarohanam} />
                             </div>
                             <FormTextarea label="Musicological Notes" defaultValue={(initialData as Raga)?.notes} />
                        </div>
                    )}

                    {entityType === 'Composers' && (
                         <div className="space-y-8">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                               <FormInput label="Name" placeholder="e.g. Tyagaraja" defaultValue={(initialData as Composer)?.name} required />
                               <FormInput label="Normalized Name" placeholder="e.g. Tyāgarāja" defaultValue={(initialData as Composer)?.normalizedName} />
                               <FormInput label="Birth Year" placeholder="YYYY" defaultValue={(initialData as Composer)?.birthYear} />
                               <FormInput label="Death Year" placeholder="YYYY" defaultValue={(initialData as Composer)?.deathYear} />
                               <FormInput label="Place of Origin" placeholder="City/Village" defaultValue={(initialData as Composer)?.place} className="md:col-span-2" />
                            </div>
                            <FormTextarea label="Biographical Notes" defaultValue={(initialData as Composer)?.notes} />
                       </div>
                    )}

                    {/* Footer Actions */}
                    <div className="pt-6 border-t border-border-light flex flex-col-reverse sm:flex-row items-center justify-between gap-4">
                        <button className="w-full sm:w-auto px-4 py-2.5 rounded-lg text-red-600 hover:bg-red-50 text-sm font-medium transition-colors flex items-center justify-center gap-2 group">
                             <span className="material-symbols-outlined text-lg group-hover:fill-1">archive</span>
                             Archive {entityType?.slice(0, -1)}
                        </button>
                        <div className="flex items-center gap-3 w-full sm:w-auto">
                             <button onClick={onCancel} className="flex-1 sm:flex-none px-6 py-2.5 rounded-lg border border-border-light text-ink-700 bg-white hover:bg-slate-50 text-sm font-medium transition-colors">
                                Cancel
                             </button>
                             <button onClick={onSave} className="flex-1 sm:flex-none px-6 py-2.5 rounded-lg bg-primary hover:bg-primary-dark text-white text-sm font-medium shadow-md shadow-blue-500/20 transition-all flex items-center justify-center gap-2">
                                <span className="material-symbols-outlined text-lg">save</span>
                                Save Changes
                             </button>
                        </div>
                    </div>
                </div>
            </div>
             
             {/* Helper Cards */}
             <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="p-4 rounded-lg border border-blue-100 bg-blue-50/50 flex gap-3">
                    <span className="material-symbols-outlined text-blue-600 shrink-0">info</span>
                    <div className="text-sm">
                        <p className="font-medium text-blue-900">Tip for Curators</p>
                        <p className="text-blue-700 mt-1">Ensure the normalized name follows the IAST standard for proper indexing.</p>
                    </div>
                </div>
             </div>
        </div>
    );
};

// --- Main Page Component ---

const ReferenceData: React.FC = () => {
    const [viewMode, setViewMode] = useState<ViewMode>('HOME');
    const [activeEntity, setActiveEntity] = useState<EntityType>(null);
    const [selectedItem, setSelectedItem] = useState<ReferenceItem | null>(null);

    const handleEntityClick = (type: EntityType) => {
        setActiveEntity(type);
        setViewMode('LIST');
        setSelectedItem(null);
    };

    const handleBack = () => {
        if (viewMode === 'FORM') {
            setViewMode('LIST');
        } else {
            setViewMode('HOME');
            setActiveEntity(null);
            setSelectedItem(null);
        }
    };

    const handleCreate = () => {
        setSelectedItem(null);
        setViewMode('FORM');
    };

    const handleEdit = (item: ReferenceItem) => {
        setSelectedItem(item);
        setViewMode('FORM');
    };

    // --- RENDERERS ---

    const renderCard = (title: string, count: number, description: string, colorClass: string, type: EntityType) => (
        <div 
            onClick={() => handleEntityClick(type)}
            className="bg-surface-light border border-border-light rounded-xl p-6 hover:shadow-md transition-all cursor-pointer group relative overflow-hidden hover:-translate-y-1"
        >
            <div className={`absolute top-0 left-0 w-1 h-full ${colorClass}`}></div>
            <div className="flex justify-between items-start mb-4">
                <h3 className="font-display text-xl font-bold text-ink-900">{title}</h3>
                <span className="bg-slate-100 text-ink-600 text-xs font-bold px-2.5 py-1 rounded-full">{count}</span>
            </div>
            <p className="text-sm text-ink-500 mb-6 leading-relaxed">{description}</p>
            <div className="flex items-center text-sm font-medium text-ink-700 group-hover:text-primary transition-colors">
                Manage
                <span className="material-symbols-outlined text-[18px] ml-1 group-hover:translate-x-1 transition-transform">arrow_forward</span>
            </div>
        </div>
    );

    const renderList = () => {
        let data: ReferenceItem[] = [];
        if (activeEntity === 'Composers') data = MOCK_COMPOSERS;
        else if (activeEntity === 'Ragas') data = MOCK_RAGAS;
        else if (activeEntity === 'Talas') data = MOCK_TALAS;
        else if (activeEntity === 'Temples') data = MOCK_TEMPLES;

        return (
            <div className="h-full flex flex-col max-w-7xl mx-auto space-y-6">
                {/* List Header */}
                 <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <nav className="flex items-center gap-2 text-sm text-ink-500 mb-1">
                        <span className="hover:text-primary transition-colors cursor-pointer" onClick={handleBack}>Reference Data</span>
                        <span className="material-symbols-outlined text-base">chevron_right</span>
                        <span className="text-ink-900 font-medium">{activeEntity}</span>
                    </nav>
                 </div>
                 
                <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
                    <div>
                        <h1 className="font-display text-3xl md:text-4xl font-bold text-ink-900 tracking-tight">{activeEntity}</h1>
                        <p className="text-ink-500 mt-2">Manage the collection of {activeEntity?.toLowerCase()}.</p>
                    </div>
                    <button 
                        onClick={handleCreate}
                        className="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg text-sm font-medium shadow-md shadow-blue-500/20 transition-all"
                    >
                        <span className="material-symbols-outlined text-[20px]">add</span>
                        Create New
                    </button>
                </div>

                {/* Filters & Table Container */}
                <div className="bg-surface-light border border-border-light rounded-xl shadow-sm overflow-hidden flex flex-col">
                     {/* Toolbar */}
                    <div className="p-4 border-b border-border-light bg-slate-50/50 flex gap-4">
                        <div className="relative flex-1 max-w-md">
                            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-400 text-[20px]">search</span>
                            <input 
                                type="text" 
                                placeholder={`Search ${activeEntity}...`}
                                className="w-full pl-10 pr-4 py-2 bg-white border border-border-light rounded-lg text-sm focus:ring-2 focus:ring-primary focus:border-transparent transition-shadow"
                            />
                        </div>
                        <select className="px-3 py-2 bg-white border border-border-light rounded-lg text-sm text-ink-700">
                            <option>All Status</option>
                            <option>Active</option>
                            <option>Archived</option>
                        </select>
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-left border-collapse">
                            <thead className="bg-slate-50/50 border-b border-border-light text-xs font-semibold uppercase tracking-wider text-ink-500">
                                <tr>
                                    <th className="px-6 py-4">Name</th>
                                    <th className="px-6 py-4">Normalized</th>
                                    {activeEntity === 'Composers' && <th className="px-6 py-4">Period</th>}
                                    {activeEntity === 'Ragas' && <th className="px-6 py-4">Structure</th>}
                                    {activeEntity === 'Talas' && <th className="px-6 py-4">Beats</th>}
                                    <th className="px-6 py-4">Status</th>
                                    <th className="px-6 py-4 text-right">Updated</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100 bg-white">
                                {data.map(item => (
                                    <tr 
                                        key={item.id}
                                        onClick={() => handleEdit(item)}
                                        className="cursor-pointer hover:bg-slate-50 transition-colors group"
                                    >
                                        <td className="px-6 py-4">
                                            <span className="font-display font-medium text-ink-900">{item.name}</span>
                                        </td>
                                        <td className="px-6 py-4 text-sm text-ink-500 font-mono">{item.normalizedName}</td>
                                        
                                        {item.type === 'Composers' && <td className="px-6 py-4 text-sm text-ink-500">{(item as Composer).birthYear} – {(item as Composer).deathYear}</td>}
                                        {item.type === 'Ragas' && <td className="px-6 py-4 text-sm text-ink-500">{(item as Raga).arohanam?.substring(0, 15)}...</td>}
                                        {item.type === 'Talas' && <td className="px-6 py-4 text-sm text-ink-500">{(item as Tala).beatCount}</td>}
                                        {item.type === 'Temples' && <td className="px-6 py-4 text-sm text-ink-500">{(item as Temple).location}</td>}

                                        <td className="px-6 py-4">
                                            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${
                                                item.status === 'Active' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-slate-100 text-slate-600 border-slate-200'
                                            }`}>
                                                {item.status}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 text-right text-xs text-ink-500">
                                            {item.updatedAt}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        );
    };

    // --- MAIN RENDER ---

    if (viewMode === 'FORM' && activeEntity) {
        return <EntityForm entityType={activeEntity} initialData={selectedItem} onSave={() => setViewMode('LIST')} onCancel={handleBack} />;
    }

    if (viewMode === 'LIST') {
        return renderList();
    }

    // Default: Dashboard View
    return (
        <div className="max-w-7xl mx-auto space-y-8 animate-fadeIn">
            <div>
                <h1 className="font-display text-3xl font-bold text-ink-900 tracking-tight">Reference Data</h1>
                <p className="text-ink-500 mt-2 text-lg">Controlled vocabularies and taxonomies used across the archive.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {renderCard('Ragas', 342, 'Melakarta and Janya ragas with Arohana/Avarohana scales.', 'bg-blue-500', 'Ragas')}
                {renderCard('Talas', 35, 'Suladi Sapta talas, Chapu talas, and rhythmic cycles.', 'bg-teal-500', 'Talas')}
                {renderCard('Composers', 86, 'Biographical data, periods, and lineages of vaggeyakaras.', 'bg-purple-500', 'Composers')}
                {renderCard('Temples', 120, 'Geographical locations associated with specific compositions.', 'bg-orange-500', 'Temples')}
                {renderCard('Languages', 12, 'Supported languages and script mappings for lyrics.', 'bg-pink-500', 'Languages')}
                {renderCard('Musical Forms', 18, 'Types of compositions (Varnam, Krithi, Padam, etc.).', 'bg-indigo-500', 'Musical Forms')}
            </div>

             <div className="p-6 rounded-xl border border-blue-100 bg-blue-50/50 flex gap-4">
                 <span className="material-symbols-outlined text-3xl text-blue-600">sync</span>
                 <div>
                    <h3 className="font-bold text-blue-900">External Sync</h3>
                    <p className="text-sm text-blue-700 mt-1">Last synced with Musicological Ontology Service on Oct 24, 2023.</p>
                 </div>
            </div>
        </div>
    );
};

export default ReferenceData;