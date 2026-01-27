import { EntityType } from '../../hooks/useEntityCrud';

export interface EntityFormProps {
    initialData?: any;
    onSave: (data: any) => Promise<void>;
    onCancel: () => void;
    saving?: boolean;
}

export interface EntityPageProps {
    onBack: () => void;
}
