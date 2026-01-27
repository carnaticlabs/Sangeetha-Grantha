// A generic interface for simple form state
// useful when not using a form library like react-hook-form
export interface FormState<T> {
    data: T;
    errors: Partial<Record<keyof T, string>>;
    isSubmitting: boolean;
    isDirty: boolean;
}

export type ChangeHandler = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
) => void;

export interface FormAction<T> {
    type: 'SET_FIELD' | 'SET_ERROR' | 'RESET' | 'SUBMIT_START' | 'SUBMIT_END' | 'SET_DATA';
    field?: keyof T;
    value?: unknown;
    error?: string;
    data?: T;
}
