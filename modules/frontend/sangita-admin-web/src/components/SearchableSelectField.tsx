import React, { useState, useRef, useEffect, useCallback } from 'react';

interface SearchableSelectFieldProps {
    label: string;
    value: string;
    onChange: (value: string) => void;
    options?: Array<{ id: string; name?: string; canonicalName?: string; displayName?: string }>;
    placeholder?: string;
    onAddNew?: () => void;
    addNewLabel?: string;
}

export const SearchableSelectField: React.FC<SearchableSelectFieldProps> = ({
    label,
    value,
    onChange,
    options,
    placeholder = 'Select...',
    onAddNew,
    addNewLabel = 'Add New'
}) => {
    const [isOpen, setIsOpen] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [focusedIndex, setFocusedIndex] = useState(-1);
    const containerRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const addNewButtonRef = useRef<HTMLButtonElement>(null);
    const isOpeningModalRef = useRef(false);

    // Ensure options is an array and filter out any null/undefined entries
    const safeOptions = Array.isArray(options) 
        ? options.filter(opt => opt != null && opt.id != null)
        : [];
    
    const selectedOption = safeOptions.find(opt => opt.id === value);
    const displayValue = selectedOption 
        ? (selectedOption.name || selectedOption.canonicalName || selectedOption.displayName || '')
        : '';

    // Filter options based on search term
    const filteredOptions = safeOptions.filter(opt => {
        if (!opt) return false;
        const searchLower = searchTerm.toLowerCase();
        const name = (opt.name || opt.canonicalName || opt.displayName || '').toLowerCase();
        return name.includes(searchLower);
    });

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            // Don't close if we're opening a modal
            if (isOpeningModalRef.current) {
                return;
            }
            const target = event.target as Node;
            // Don't close if clicking on the Add New button or its container
            if (addNewButtonRef.current && (addNewButtonRef.current.contains(target) || addNewButtonRef.current === target)) {
                return;
            }
            // Don't close if clicking on a modal
            const targetElement = target as HTMLElement;
            if (targetElement.closest && (targetElement.closest('[class*="z-50"]') || targetElement.closest('[class*="z-[50"]'))) {
                return;
            }
            if (containerRef.current && !containerRef.current.contains(target)) {
                setIsOpen(false);
                setSearchTerm('');
                setFocusedIndex(-1);
            }
        };

        if (isOpen) {
            // Use mousedown but check the flag
            document.addEventListener('mousedown', handleClickOutside);
            // Focus input when dropdown opens
            setTimeout(() => inputRef.current?.focus(), 0);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isOpen]);

    // Handle keyboard navigation
    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (!isOpen) {
            if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
                e.preventDefault();
                setIsOpen(true);
            }
            return;
        }

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                setFocusedIndex(prev => 
                    prev < filteredOptions.length - 1 ? prev + 1 : prev
                );
                break;
            case 'ArrowUp':
                e.preventDefault();
                setFocusedIndex(prev => prev > 0 ? prev - 1 : -1);
                break;
            case 'Enter':
                e.preventDefault();
                if (focusedIndex >= 0 && focusedIndex < filteredOptions.length) {
                    onChange(filteredOptions[focusedIndex].id);
                    setIsOpen(false);
                    setSearchTerm('');
                    setFocusedIndex(-1);
                }
                break;
            case 'Escape':
                e.preventDefault();
                setIsOpen(false);
                setSearchTerm('');
                setFocusedIndex(-1);
                break;
        }
    };

    const handleSelect = (optionId: string) => {
        onChange(optionId);
        setIsOpen(false);
        setSearchTerm('');
        setFocusedIndex(-1);
    };

    return (
        <div className="relative" ref={containerRef}>
            <label className="block text-sm font-semibold text-ink-900 mb-2">{label}</label>
            <div className="relative">
                <div
                    className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus-within:ring-2 focus-within:ring-primary focus-within:border-transparent cursor-pointer flex items-center justify-between"
                    onClick={() => setIsOpen(!isOpen)}
                    onKeyDown={handleKeyDown}
                    tabIndex={0}
                    role="combobox"
                    aria-expanded={isOpen}
                    aria-haspopup="listbox"
                >
                    <span className={displayValue ? 'text-ink-900' : 'text-ink-500'}>
                        {displayValue || placeholder}
                    </span>
                    <span className={`material-symbols-outlined text-ink-500 transition-transform ${isOpen ? 'rotate-180' : ''}`}>
                        expand_more
                    </span>
                </div>

                {isOpen && (
                    <div className="absolute z-50 w-full mt-1 bg-white border border-border-light rounded-lg shadow-lg max-h-60 overflow-hidden flex flex-col">
                        {/* Search Input */}
                        <div className="p-2 border-b border-border-light">
                            <div className="relative">
                                <span className="material-symbols-outlined absolute left-2 top-1/2 -translate-y-1/2 text-ink-400 text-lg">
                                    search
                                </span>
                                <input
                                    ref={inputRef}
                                    type="text"
                                    value={searchTerm}
                                    onChange={(e) => {
                                        setSearchTerm(e.target.value);
                                        setFocusedIndex(-1);
                                    }}
                                    placeholder="Search..."
                                    className="w-full pl-8 pr-3 py-2 text-sm rounded border border-border-light focus:ring-2 focus:ring-primary focus:border-transparent"
                                    onClick={(e) => e.stopPropagation()}
                                />
                            </div>
                        </div>

                        {/* Options List */}
                        <div className="overflow-y-auto flex-1">
                            {filteredOptions.length === 0 ? (
                                <div className="p-4 text-center text-sm text-ink-500">
                                    {searchTerm ? 'No matches found' : 'No options available'}
                                </div>
                            ) : (
                                filteredOptions.map((opt, index) => {
                                    const optName = opt.name || opt.canonicalName || opt.displayName || '';
                                    const isSelected = opt.id === value;
                                    const isFocused = index === focusedIndex;
                                    
                                    return (
                                        <button
                                            key={opt.id}
                                            type="button"
                                            onClick={() => handleSelect(opt.id)}
                                            className={`w-full text-left px-4 py-2 text-sm transition-colors ${
                                                isSelected
                                                    ? 'bg-primary-light text-primary font-medium'
                                                    : isFocused
                                                    ? 'bg-slate-100 text-ink-900'
                                                    : 'text-ink-700 hover:bg-slate-50'
                                            }`}
                                            onMouseEnter={() => setFocusedIndex(index)}
                                        >
                                            {optName}
                                        </button>
                                    );
                                })
                            )}
                        </div>

                        {/* Add New Button */}
                        {onAddNew && (
                            <div 
                                className="border-t border-border-light p-2"
                                onMouseDown={(e) => {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    isOpeningModalRef.current = true;
                                }}
                            >
                                <button
                                    ref={addNewButtonRef}
                                    type="button"
                                    onMouseDown={(e) => {
                                        e.preventDefault();
                                        e.stopPropagation();
                                        isOpeningModalRef.current = true;
                                    }}
                                    onClick={(e) => {
                                        e.preventDefault();
                                        e.stopPropagation();
                                        // Set flag immediately to prevent click outside handler
                                        isOpeningModalRef.current = true;
                                        // Call onAddNew FIRST - this sets the modal state
                                        // This must happen before closing the dropdown
                                        onAddNew?.();
                                        // Use requestAnimationFrame to ensure state update is processed
                                        requestAnimationFrame(() => {
                                            // Close dropdown after modal state is set
                                            setIsOpen(false);
                                            setSearchTerm('');
                                            setFocusedIndex(-1);
                                        });
                                        // Reset flag after modal should have opened
                                        setTimeout(() => {
                                            isOpeningModalRef.current = false;
                                        }, 1000);
                                    }}
                                    className="w-full flex items-center gap-2 px-3 py-2 text-sm text-primary hover:bg-primary-light rounded transition-colors"
                                >
                                    <span className="material-symbols-outlined text-lg">add</span>
                                    {addNewLabel}
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};
