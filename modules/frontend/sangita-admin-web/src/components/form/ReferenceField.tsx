import React from 'react';

interface ReferenceFieldProps {
    label: string;
    value: string | undefined; // The display label of the selected item
    placeholder?: string;
    onModify: () => void;
    error?: string;
    required?: boolean;
    disabled?: boolean;
    className?: string;
}

export const ReferenceField: React.FC<ReferenceFieldProps> = ({
    label,
    value,
    placeholder = 'Not selected',
    onModify,
    error,
    required,
    disabled,
    className = ''
}) => {
    return (
        <div className={`mb-4 ${className}`}>
            <label className="block text-sm font-semibold text-ink-900 mb-2">
                {label}
                {required && <span className="text-primary ml-1">*</span>}
            </label>
            <div className="flex gap-2">
                <div className={`flex-1 flex items-center px-4 h-12 rounded-lg border ${error ? 'border-red-300 bg-red-50' : 'border-border-light bg-slate-50'} ${disabled ? 'opacity-60 cursor-not-allowed' : ''}`}>
                    <span className={`truncate ${value ? 'text-ink-900' : 'text-slate-400 italic'}`}>
                        {value || placeholder}
                    </span>
                </div>
                <button
                    type="button"
                    onClick={onModify}
                    disabled={disabled}
                    className="px-4 h-12 bg-white border border-border-light text-ink-900 font-medium rounded-lg hover:bg-slate-50 hover:border-slate-300 transition-colors disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                >
                    Modify
                </button>
            </div>
            {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}
        </div>
    );
};
