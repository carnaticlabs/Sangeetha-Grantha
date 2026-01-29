import React from 'react';

interface SectionHeaderProps {
    title: string;
    action?: React.ReactNode;
    className?: string;
}

export const SectionHeader: React.FC<SectionHeaderProps> = ({ title, action, className = '' }) => {
    return (
        <div className={`flex items-center justify-between mb-6 pb-2 border-b border-border-light ${className}`}>
            <h3 className="text-lg font-bold text-ink-900">{title}</h3>
            {action}
        </div>
    );
};
