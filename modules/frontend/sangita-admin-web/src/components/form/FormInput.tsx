import React from 'react';

interface FormInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
    label: string;
    name: string;
    error?: string;
    help?: string;
    className?: string;
}

export const FormInput: React.FC<FormInputProps> = ({
    label,
    name,
    error,
    help,
    className = '',
    id,
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
            <input
                id={inputId}
                name={name}
                className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-amber-500 transition-colors ${error ? 'border-rose-300 bg-rose-50' : 'border-slate-300'
                    } ${props.disabled ? 'bg-slate-100 text-slate-500 cursor-not-allowed' : ''}`}
                {...props}
            />
            {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}
            {help && !error && <p className="mt-1 text-xs text-slate-500">{help}</p>}
        </div>
    );
};
