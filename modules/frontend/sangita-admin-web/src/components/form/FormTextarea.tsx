import React from 'react';

interface FormTextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
    label: string;
    name: string;
    error?: string;
    help?: string;
    className?: string;
}

export const FormTextarea: React.FC<FormTextareaProps> = ({
    label,
    name,
    error,
    help,
    className = '',
    id,
    rows = 3,
    ...props
}) => {
    const inputId = id || name;

    return (
        <div className={`mb-4 ${className}`}>
            <label
                htmlFor={inputId}
                className="block text-sm font-semibold text-ink-900 mb-2"
            >
                {label}
                {props.required && <span className="text-primary ml-1">*</span>}
            </label>
            <textarea
                id={inputId}
                name={name}
                rows={rows}
                className={`w-full px-4 py-3 border rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all ${error ? 'border-red-300 bg-red-50' : 'border-border-light bg-slate-50'
                    } ${props.disabled ? 'opacity-60 cursor-not-allowed' : ''}`}
                {...props}
            />
            {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}
            {help && !error && <p className="mt-1 text-xs text-slate-500">{help}</p>}
        </div>
    );
};
