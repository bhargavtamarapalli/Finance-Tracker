export type Role = 'user' | 'admin' | 'superAdmin';

export interface JwtPayload {
  userId: string;
  role: Role;
  email?: string;
  sessionVersion?: number;
}

export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  meta?: {
    page?: number;
    limit?: number;
    total?: number;
  };
  error?: {
    code: string;
    message: string;
    meta?: Record<string, unknown>;
  };
}

export interface PaginatedResponse<T> {
  items: T[];
  meta: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
}

export interface User {
  id: string;
  email: string;
  role: Role;
  status: 'ACTIVE' | 'SUSPENDED';
  createdAt: Date;
  lastLoginAt?: Date;
}

export interface AdminAction {
  id: string;
  type: string;
  targetUserId?: string;
  performedBy: string;
  timestamp: number;
  description: string;
}

export interface Announcement {
  id: string;
  title: string;
  content: string;
  category: string;
  timestamp: number;
  authorId: string;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
}
