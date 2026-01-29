import React from 'react';

interface FormCheckboxProps extends React.InputHTMLAttributes<HTMLInputElement> {
    label: string;
    name: string;
    error?: string;
    help?: string;
    className?: string;
}

export const FormCheckbox: React.FC<FormCheckboxProps> = ({
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
            <div className="flex items-start">
                <div className="flex items-center h-5">
                    <input
                        id={inputId}
                        name={name}
                        type="checkbox"
                        className={`h-5 w-5 text-primary focus:ring-primary border-border-light rounded ${error ? 'border-red-300' : ''
                            } ${props.disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'}`}
                        {...props}
                    />
                </div>
                <div className="ml-3 text-sm">
                    <label
                        htmlFor={inputId}
                        className={`font-semibold text-ink-900 ${props.disabled ? 'cursor-not-allowed opacity-75' : 'cursor-pointer'
                            }`}
                    >
                        {label}
                        {props.required && <span className="text-primary ml-1">*</span>}
                    </label>
                    {help && <p className="text-slate-500">{help}</p>}
                    {error && <p className="text-rose-600">{error}</p>}
                </div>
            </div>
        </div>
    );
};
