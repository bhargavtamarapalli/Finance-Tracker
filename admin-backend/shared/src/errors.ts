export enum ErrorCode {
  OTP_RATE_LIMIT_MOBILE = 'OTP_RATE_LIMIT_MOBILE',
  OTP_RATE_LIMIT_IP = 'OTP_RATE_LIMIT_IP',
  OTP_INVALID = 'OTP_INVALID',
  OTP_EXPIRED = 'OTP_EXPIRED',
  JWT_INVALID = 'JWT_INVALID',
  JWT_EXPIRED = 'JWT_EXPIRED',
  UNAUTHORIZED = 'UNAUTHORIZED',
  INSUFFICIENT_ROLE = 'INSUFFICIENT_ROLE',
  ACCOUNT_DELETED = 'ACCOUNT_DELETED',
  NOT_FOUND = 'NOT_FOUND',
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  RATE_LIMIT_EXCEEDED = 'RATE_LIMIT_EXCEEDED',
  BULK_UPLOAD_LIMIT = 'BULK_UPLOAD_LIMIT',
  INVALID_CSV = 'INVALID_CSV',
  INTERNAL_ERROR = 'INTERNAL_ERROR',
  EXTERNAL_SERVICE_ERROR = 'EXTERNAL_SERVICE_ERROR'
}

export class AppError extends Error {
  public code: ErrorCode;
  public meta?: Record<string, unknown>;
  public statusCode: number;
  public isOperational: boolean = true;

  constructor(code: ErrorCode, message?: string, meta?: Record<string, unknown>) {
    super(message || code);
    this.name = 'AppError';
    this.code = code;
    this.meta = meta;
    this.statusCode = AppError.getStatusCode(code);
  }

  private static getStatusCode(code: ErrorCode): number {
    switch (code) {
      case ErrorCode.VALIDATION_ERROR:
      case ErrorCode.INVALID_CSV:
      case ErrorCode.BULK_UPLOAD_LIMIT:
        return 400;
      case ErrorCode.OTP_INVALID:
      case ErrorCode.OTP_EXPIRED:
      case ErrorCode.JWT_INVALID:
      case ErrorCode.JWT_EXPIRED:
      case ErrorCode.UNAUTHORIZED:
        return 401;
      case ErrorCode.INSUFFICIENT_ROLE:
      case ErrorCode.ACCOUNT_DELETED:
        return 403;
      case ErrorCode.NOT_FOUND:
        return 404;
      case ErrorCode.OTP_RATE_LIMIT_MOBILE:
      case ErrorCode.OTP_RATE_LIMIT_IP:
      case ErrorCode.RATE_LIMIT_EXCEEDED:
        return 429;
      case ErrorCode.EXTERNAL_SERVICE_ERROR:
        return 502;
      case ErrorCode.INTERNAL_ERROR:
      default:
        return 500;
    }
  }
}
