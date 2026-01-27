// Interface for the Toast methods (to decouple from specific library if needed)
interface ToastContext {
    success: (message: string) => void;
    error: (message: string) => void;
}

export const formatErrorMessage = (error: unknown): string => {
    if (error instanceof Error) {
        return error.message;
    }
    if (typeof error === 'string') {
        return error;
    }
    if (typeof error === 'object' && error !== null && 'message' in error) {
        return String((error as { message: unknown }).message);
    }
    return 'An unexpected error occurred';
};

export const handleApiError = (error: unknown, toast?: ToastContext) => {
    const message = formatErrorMessage(error);

    // Log to console in development
    if (import.meta.env.DEV) {
        console.error('[API Error]:', error);
    }

    // Display toast if provided
    if (toast) {
        toast.error(message);
    } else {
        // Fallback if no toast provided (though we aim to avoid alert/console only)
        console.error(message);
    }

    return message;
};
