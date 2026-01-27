// Pagination parameters
export interface PageParams {
    page?: number;
    size?: number;
    sort?: string;
    direction?: 'asc' | 'desc';
}

// Common filter parameters
export interface DateRangeParams {
    startDate?: string;
    endDate?: string;
}

// Search parameters
export interface SearchParams extends PageParams {
    query?: string;
    field?: string;
}

// Generic API Response wrapper (if your API uses one)
export interface ApiResponse<T> {
    data: T;
    status: number;
    message?: string;
}

// Paginated Response wrapper
export interface PaginatedResponse<T> {
    items: T[];
    total: number;
    page: number;
    size: number;
    totalPages: number;
}
