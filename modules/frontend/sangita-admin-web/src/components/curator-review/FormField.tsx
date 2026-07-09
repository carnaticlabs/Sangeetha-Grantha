import React, { useId } from 'react';

export const FormField: React.FC<{ label: string; value: string; onChange: (v: string) => void }> = ({ label, value, onChange }) => {
    const id = useId();
    return (
        <div>
            <label htmlFor={id} className="block text-xs font-semibold text-ink-600 mb-1">{label}</label>
            <input id={id} value={value} onChange={e => onChange(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-border-light rounded focus:ring-2 focus:ring-primary focus:border-transparent" />
        </div>
    );
};
