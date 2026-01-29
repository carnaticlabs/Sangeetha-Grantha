import React from 'react';

interface Option {
    value: string | number;
    label: string;
}

interface FormSelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
    label: string;
    name: string;
    options: Option[] | string[];
    error?: string;
    help?: string;
    className?: string;
    placeholder?: string;
}

export const FormSelect: React.FC<FormSelectProps> = ({
    label,
    name,
    options,
    error,
    help,
    className = '',
    placeholder = 'Select an option',
    id,
    ...props
}) => {
    const inputId = id || name;

    const validOptions: Option[] = options.map((opt) => {
        if (typeof opt === 'string') {
            return { value: opt, label: opt };
        }
        return opt;
    });

    return (
        <div className={`mb-4 ${className}`}>
            <label
                htmlFor={inputId}
                className="block text-sm font-semibold text-ink-900 mb-2"
            >
                {label}
                {props.required && <span className="text-primary ml-1">*</span>}
            </label>
            <select
                id={inputId}
                name={name}
                className={`w-full h-12 px-4 border rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all bg-slate-50 ${error ? 'border-red-300 bg-red-50' : 'border-border-light'
                    } ${props.disabled ? 'opacity-60 cursor-not-allowed' : ''}`}
                {...props}
            >
                <option value="" disabled>
                    {placeholder}
                </option>
                {validOptions.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                        {opt.label}
                    </option>
                ))}
            </select>
            {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}
            {help && !error && <p className="mt-1 text-xs text-slate-500">{help}</p>}
        </div>
    );
};
