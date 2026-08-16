export interface PaginationMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiSuccess<T> {
  success: true;
  message?: string;
  data: T;
  pagination?: PaginationMeta;
  timestamp: string;
}

export interface ApiError {
  success: false;
  message: string;
  errorCode: string;
  errors?: Record<string, string>;
  data?: unknown;
  timestamp: string;
}

export type ApiEnvelope<T> = ApiSuccess<T> | ApiError;

export interface Paged<T> {
  items: T[];
  pagination: PaginationMeta;
}

export interface UserSummary {
  id: number;
  name: string;
  email: string;
}

export interface ListParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
}

export class ApiRequestError extends Error {
  errorCode: string;
  errors?: Record<string, string>;
  status: number;
  data?: unknown;

  constructor(message: string, errorCode: string, status: number, errors?: Record<string, string>, data?: unknown) {
    super(message);
    this.name = "ApiRequestError";
    this.errorCode = errorCode;
    this.status = status;
    this.errors = errors;
    this.data = data;
  }
}
