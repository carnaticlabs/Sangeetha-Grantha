import React from 'react';
import { useSearchParams } from 'react-router-dom';
import MobilePreviewApp from './mobile-preview/MobilePreviewApp';

/** Standalone route: the whole phone, optionally opened on one krithi (`/mobile-preview?krithi=<id>`). */
const MobilePreview: React.FC = () => {
    const [params] = useSearchParams();
    return <MobilePreviewApp krithiId={params.get('krithi')} />;
};

export default MobilePreview;
