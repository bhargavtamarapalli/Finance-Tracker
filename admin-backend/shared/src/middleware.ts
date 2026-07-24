import crypto from 'crypto';
import { Request, Response, NextFunction } from 'express';
import pino from 'pino';
import { z } from 'zod';
import jwt from 'jsonwebtoken';
import * as Sentry from '@sentry/node';
import { getDbPool } from './db';
import { Role, JwtPayload } from './types';
import { AppError, ErrorCode } from './errors';

const logger = pino();

// Initialize Sentry for backend services
const initializeSentry = () => {
  const sentryDsn = process.env.SENTRY_DSN;
  if (sentryDsn) {
    Sentry.init({
      dsn: sentryDsn,
      environment: process.env.APP_ENV || 'development',
      tracesSampleRate: process.env.APP_ENV === 'production' ? 0.1 : 1.0,
    });
  }
};

initializeSentry();

// Extend Express Request type to include the user property
declare global {
  namespace Express {
    interface Request {
      user?: {
        userId: string;
        role: Role;
        email?: string;
      };
    }
  }
}

export const requireAuth = (req: Request, _res: Response, next: NextFunction) => {
  if (!req.user || !req.user.userId) {
    return next(new AppError(ErrorCode.JWT_INVALID, 'Authentication required'));
  }
  next();
};

const extractJwtToken = (req: Request): string | null => {
  const authHeader = req.headers.authorization;
  if (authHeader?.startsWith('Bearer ')) {
    return authHeader.slice(7).trim();
  }

  const cookieToken = (req as any).cookies?.jwt;
  if (cookieToken) {
    return cookieToken as string;
  }

  return null;
};

export const attachAuthUser = async (req: Request, _res: Response, next: NextFunction) => {
  const token = extractJwtToken(req);
  if (!token) {
    return next();
  }

  try {
    const jwtSecret = process.env.APP_JWT_SECRET;
    if (!jwtSecret) {
      logger.error({ msg: 'APP_JWT_SECRET is not configured; skipping auth token verification' });
      return next();
    }

    const payload = jwt.verify(token, jwtSecret) as JwtPayload & { iat?: number };
    if (!payload?.userId || !payload?.role) {
      return next();
    }

    const { rows } = await getDbPool().query(
      `SELECT id, role, status FROM users WHERE id = $1`,
      [payload.userId]
    );

    const userRow = rows[0];
    if (!userRow || userRow.status === 'SUSPENDED') {
      return next();
    }

    req.user = {
      userId: payload.userId,
      role: userRow.role ?? payload.role,
      email: payload.email
    };
  } catch (error) {
    const isJwtError = error instanceof Error && 
      (error.name === 'JsonWebTokenError' || 
       error.name === 'TokenExpiredError' ||
       error.message.includes('jwt'));
    
    if (!isJwtError) {
      logger?.error({ error }, 'Unexpected error in JWT verification middleware');
    }
  }

  next();
};

export const requireRole = (...roles: Role[]) => {
  return (req: Request, _res: Response, next: NextFunction) => {
    if (!req.user || !req.user.role) {
      return next(new AppError(ErrorCode.JWT_INVALID, 'Authentication required'));
    }

    if (!roles.includes(req.user.role)) {
      return next(
        new AppError(ErrorCode.INSUFFICIENT_ROLE, 'You do not have permission to perform this action')
      );
    }
    next();
  };
};

export const catchAsync = (
  fn: (req: Request, res: Response, next: NextFunction) => Promise<any>
) => {
  return (req: Request, res: Response, next: NextFunction): void => {
    fn(req, res, next).catch(next);
  };
};

interface ErrorPayload {
  success: false;
  error: {
    code: ErrorCode;
    message: string;
    statusCode: number;
    details?: unknown;
    stack?: string;
  };
}

const isDevelopment = process.env.APP_ENV === 'development';

const buildErrorPayload = (
  errorCode: ErrorCode,
  message: string,
  statusCode: number,
  details?: unknown,
  stack?: string
): ErrorPayload => {
  const payload: ErrorPayload = {
    success: false,
    error: {
      code: errorCode,
      message,
      statusCode,
    },
  };

  if (isDevelopment && details !== undefined) {
    payload.error.details = details;
  }

  if (isDevelopment && stack) {
    payload.error.stack = stack;
  }

  return payload;
};

export const validateRequest = (schema: z.ZodSchema) => {
  return async (req: Request, _res: Response, next: NextFunction) => {
    try {
      const validated = await schema.parseAsync(req.body);
      req.body = validated;
      next();
    } catch (error) {
      next(error);
    }
  };
};

export const globalErrorHandler = (
  err: Error,
  _req: Request,
  res: Response,
  _next: NextFunction
) => {
  if (process.env.SENTRY_DSN) {
    Sentry.captureException(err);
  }

  if (err instanceof AppError) {
    logger.error({ code: err.code, msg: err.message, meta: err.meta });
    return res
      .status(err.statusCode)
      .json(buildErrorPayload(err.code, err.message, err.statusCode, err.meta, err.stack));
  }

  if (err instanceof z.ZodError) {
    logger.warn({ err: err.errors, msg: 'Validation error' });
    return res
      .status(400)
      .json(
        buildErrorPayload(
          ErrorCode.VALIDATION_ERROR,
          'Validation failed',
          400,
          err.errors,
          err.stack
        )
      );
  }

  logger.error({ err, msg: 'Unhandled exception' });
  return res
    .status(500)
    .json(buildErrorPayload(ErrorCode.INTERNAL_ERROR, 'An unexpected error occurred', 500, undefined, err.stack));
};

export const setAuthCookie = (res: Response, accessToken: string, refreshToken: string) => {
  const isProduction = process.env.APP_ENV === 'production';
  
  res.cookie('jwt', accessToken, {
    httpOnly: true,
    secure: isProduction,
    sameSite: 'strict',
    maxAge: 15 * 60 * 1000,
  });

  res.cookie('refreshToken', refreshToken, {
    httpOnly: true,
    secure: isProduction,
    sameSite: 'strict',
    maxAge: 30 * 24 * 60 * 60 * 1000,
  });
};

export const clearAuthCookie = (res: Response) => {
  res.clearCookie('jwt');
  res.clearCookie('refreshToken');
};

export const csrfProtection = (req: Request, res: Response, next: NextFunction) => {
  const safeMethods = ['GET', 'HEAD', 'OPTIONS'];
  
  if (safeMethods.includes(req.method)) {
    const token = crypto.randomBytes(32).toString('hex');
    res.setHeader('X-CSRF-Token', token);
    return next();
  }

  const tokenInHeader = req.headers['x-csrf-token'] as string | undefined;
  const tokenInBody = (req.body as any)?.csrfToken as string | undefined;
  const token = tokenInHeader || tokenInBody;

  if (!token) {
    return next(new AppError(ErrorCode.VALIDATION_ERROR, 'CSRF token missing'));
  }

  if (!/^[a-f0-9]{64}$/.test(token)) {
    return next(new AppError(ErrorCode.VALIDATION_ERROR, 'CSRF token invalid'));
  }

  next();
};

export const rateLimit = (options: { windowMs: number; max: number }) => {
  return async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { getRedisClient } = await import('./redis');
      const redis = getRedisClient();
      
      const forwarded = req.headers['x-forwarded-for'];
      const ip = (typeof forwarded === 'string' ? forwarded.split(',')[0] : req.ip) || req.socket.remoteAddress || 'unknown';
      
      const key = `rate_limit:${req.baseUrl}${req.path}:${ip}`;

      const current = await redis.incr(key);
      if (current === 1) {
        await redis.expire(key, Math.ceil(options.windowMs / 1000));
      }

      if (current > options.max) {
        const ttl = await redis.ttl(key);
        return next(
          new AppError(ErrorCode.RATE_LIMIT_EXCEEDED, 'Too many requests, please try again later', {
            retryAfter: ttl,
          })
        );
      }

      next();
    } catch (error) {
      logger.error({ err: error, msg: 'Rate limiter error' });
      const failOpen = process.env.APP_ENV !== 'production' && process.env.APP_ENV !== 'staging';
      if (failOpen) {
        return next();
      }
      return next(new AppError(ErrorCode.EXTERNAL_SERVICE_ERROR, 'Rate limiter unavailable'));
    }
  };
};
