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
                className="block text-sm font-medium text-slate-700 mb-1"
            >
                {label}
                {props.required && <span className="text-rose-500 ml-1">*</span>}
            </label>
            <textarea
                id={inputId}
                name={name}
                rows={rows}
                className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-amber-500 transition-colors ${error ? 'border-rose-300 bg-rose-50' : 'border-slate-300'
                    } ${props.disabled ? 'bg-slate-100 text-slate-500 cursor-not-allowed' : ''}`}
                {...props}
            />
            {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}
            {help && !error && <p className="mt-1 text-xs text-slate-500">{help}</p>}
        </div>
    );
};
