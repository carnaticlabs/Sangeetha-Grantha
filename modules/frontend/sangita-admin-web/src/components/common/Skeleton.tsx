import React from 'react';

interface SkeletonProps {
    className?: string;
    count?: number;
    height?: string | number;
    width?: string | number;
    circle?: boolean;
}

export const Skeleton: React.FC<SkeletonProps> = ({
    className = '',
    count = 1,
    height,
    width,
    circle = false,
}) => {
    const style: React.CSSProperties = {};
    if (height) style.height = height;
    if (width) style.width = width;
    if (circle) style.borderRadius = '50%';

    const items = Array.from({ length: count });

    return (
        <>
            {items.map((_, index) => (
                <div
                    key={index}
                    className={`animate-pulse bg-slate-200 rounded ${className} ${index < items.length - 1 ? 'mb-2' : ''
                        }`}
                    style={style}
                    aria-hidden="true"
                />
            ))}
            <span className="sr-only">Loading...</span>
        </>
    );
};
