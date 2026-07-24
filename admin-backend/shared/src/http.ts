import { logger } from './logger';

export interface RetryOptions {
  retries: number;
  minDelayMs: number;
  maxDelayMs: number;
  factor: number;
  jitter: number;
  retryableStatusCodes: number[];
  logRetries: boolean;
}

export interface ExternalRetryOptions extends Partial<RetryOptions> {
  method?: string;
  idempotent?: boolean;
  retryOnNetworkError?: boolean;
  operation?: string;
}

const DEFAULT_RETRY_OPTIONS: RetryOptions = {
  retries: Number(process.env.EXTERNAL_RETRY_COUNT ?? 2),
  minDelayMs: Number(process.env.EXTERNAL_RETRY_MIN_DELAY_MS ?? 200),
  maxDelayMs: Number(process.env.EXTERNAL_RETRY_MAX_DELAY_MS ?? 2000),
  factor: 2,
  jitter: 0.2,
  retryableStatusCodes: [408, 429, 500, 502, 503, 504],
  logRetries: true
};

const NETWORK_ERROR_CODES = new Set([
  'ECONNRESET',
  'ECONNREFUSED',
  'ECONNABORTED',
  'EAI_AGAIN',
  'ENOTFOUND',
  'ETIMEDOUT',
  'EHOSTUNREACH',
  'EPIPE'
]);

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const normalizeNumber = (value: unknown): number | null => {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string' && value.trim() !== '' && !Number.isNaN(Number(value))) {
    return Number(value);
  }
  return null;
};

const getStatusCode = (error: unknown): number | null => {
  const err = error as any;
  return normalizeNumber(err?.response?.status ?? err?.status ?? err?.statusCode ?? err?.status_code);
};

const getErrorCode = (error: unknown): string | null => {
  const err = error as any;
  if (typeof err?.code === 'string') return err.code;
  return null;
};

export const isIdempotentMethod = (method?: string): boolean => {
  const normalized = (method || 'GET').toUpperCase();
  return ['GET', 'HEAD', 'PUT', 'DELETE', 'OPTIONS'].includes(normalized);
};

export const isRetryableStatus = (status: number, retryableStatusCodes: number[]) =>
  retryableStatusCodes.includes(status);

export const shouldRetryHttpError = (
  error: unknown,
  retryableStatusCodes: number[],
  retryOnNetworkError: boolean
): boolean => {
  const status = getStatusCode(error);
  if (status !== null) {
    return isRetryableStatus(status, retryableStatusCodes);
  }

  const code = getErrorCode(error);
  if (code && retryOnNetworkError) {
    return NETWORK_ERROR_CODES.has(code);
  }

  return false;
};

const computeDelayMs = (attempt: number, options: RetryOptions) => {
  const exponential = options.minDelayMs * Math.pow(options.factor, attempt - 1);
  const capped = Math.min(options.maxDelayMs, exponential);
  const jitter = capped * options.jitter * (Math.random() * 2 - 1);
  return Math.max(0, Math.round(capped + jitter));
};

export async function withRetry<T>(
  fn: (attempt: number) => Promise<T>,
  options?: Partial<RetryOptions>,
  shouldRetry?: (error: unknown) => boolean
): Promise<T> {
  const merged: RetryOptions = { ...DEFAULT_RETRY_OPTIONS, ...options };
  const maxAttempts = Math.max(1, merged.retries + 1);
  let attempt = 0;

  while (attempt < maxAttempts) {
    try {
      attempt += 1;
      return await fn(attempt);
    } catch (error) {
      const canRetry =
        attempt < maxAttempts && (shouldRetry ? shouldRetry(error) : shouldRetryHttpError(error, merged.retryableStatusCodes, true));

      if (!canRetry) {
        throw error;
      }

      const delayMs = computeDelayMs(attempt, merged);
      if (merged.logRetries && process.env.APP_ENV !== 'test') {
        logger.warn(
          {
            attempt,
            maxAttempts,
            delayMs,
            status: getStatusCode(error),
            code: getErrorCode(error),
            operation: (options as ExternalRetryOptions | undefined)?.operation
          },
          'Retrying external call'
        );
      }
      await sleep(delayMs);
    }
  }
  throw new Error('Unreachable');
}

export async function withExternalRetry<T>(
  fn: (attempt: number) => Promise<T>,
  options?: ExternalRetryOptions
): Promise<T> {
  const merged: RetryOptions = { ...DEFAULT_RETRY_OPTIONS, ...options };
  const method = options?.method;
  const idempotent = options?.idempotent ?? isIdempotentMethod(method);
  const retryOnNetworkError = options?.retryOnNetworkError ?? idempotent;

  const shouldRetry = (error: unknown) => {
    const status = getStatusCode(error);
    if (status !== null) {
      return idempotent && isRetryableStatus(status, merged.retryableStatusCodes);
    }
    return retryOnNetworkError && shouldRetryHttpError(error, merged.retryableStatusCodes, retryOnNetworkError);
  };

  return withRetry(fn, merged, shouldRetry);
}

export async function fetchJsonWithRetry<T>(
  input: RequestInfo | URL,
  init?: RequestInit,
  options?: ExternalRetryOptions
): Promise<{ data: T; response: Response }> {
  const method = init?.method ?? options?.method ?? 'GET';
  return withExternalRetry(async () => {
    const response = await fetch(input, init);
    let data: any = null;
    let parseError: Error | null = null;
    try {
      data = await response.json();
    } catch (e) {
      parseError = e instanceof Error ? e : new Error(String(e));
    }

    if (!response.ok) {
      const error: any = new Error(`HTTP ${response.status}`);
      error.status = response.status;
      error.data = data;
      if (parseError) {
        error.parseError = parseError;
      }
      throw error;
    }

    if (parseError && response.ok) {
      const contentType = response.headers.get('content-type');
      const contentLength = response.headers.get('content-length');
      const isActuallyEmpty = response.status === 204 || contentLength === '0' || !contentType?.includes('application/json');

      if (isActuallyEmpty) {
        return { data: null as any as T, response };
      }

      const error = new Error(`Failed to parse JSON response: ${parseError.message}`);
      (error as any).status = response.status;
      (error as any).parseError = parseError;
      throw error;
    }

    return { data: data as T, response };
  }, { ...options, method });
}
