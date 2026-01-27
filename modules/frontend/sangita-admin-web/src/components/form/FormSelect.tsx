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
                className="block text-sm font-medium text-slate-700 mb-1"
            >
                {label}
                {props.required && <span className="text-rose-500 ml-1">*</span>}
            </label>
            <select
                id={inputId}
                name={name}
                className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-amber-500 transition-colors bg-white ${error ? 'border-rose-300 bg-rose-50' : 'border-slate-300'
                    } ${props.disabled ? 'bg-slate-100 text-slate-500 cursor-not-allowed' : ''}`}
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
