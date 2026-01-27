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
                        className={`h-4 w-4 text-amber-600 focus:ring-amber-500 border-slate-300 rounded ${error ? 'border-rose-300' : ''
                            } ${props.disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'}`}
                        {...props}
                    />
                </div>
                <div className="ml-3 text-sm">
                    <label
                        htmlFor={inputId}
                        className={`font-medium text-slate-700 ${props.disabled ? 'cursor-not-allowed opacity-75' : 'cursor-pointer'
                            }`}
                    >
                        {label}
                        {props.required && <span className="text-rose-500 ml-1">*</span>}
                    </label>
                    {help && <p className="text-slate-500">{help}</p>}
                    {error && <p className="text-rose-600">{error}</p>}
                </div>
            </div>
        </div>
    );
};
