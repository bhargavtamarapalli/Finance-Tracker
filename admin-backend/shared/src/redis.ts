import Redis, { RedisOptions } from 'ioredis';
import { logger } from './logger';

let redisClient: Redis | null = null;

export const getRedisClient = (): Redis => {
  if (!redisClient) {
    const redisUrl = process.env.REDIS_URL;
    const isProd = process.env.APP_ENV === 'production';
    const isUpstash = redisUrl?.includes('upstash.io') || redisUrl?.startsWith('rediss://');
    
    const options: RedisOptions = (redisUrl || isUpstash)
      ? { 
          connectionName: 'finance-backend',
          retryStrategy: (times) => Math.min(times * 200, 3000),
          lazyConnect: true,
          maxRetriesPerRequest: null,
          tls: isUpstash ? { rejectUnauthorized: false } : undefined
        }
      : {
          host: process.env.REDIS_HOST || 'localhost',
          port: process.env.REDIS_PORT ? parseInt(process.env.REDIS_PORT, 10) : 6379,
          maxRetriesPerRequest: null,
          retryStrategy: (times) => Math.min(times * 100, 3000),
          lazyConnect: true,
        };

    redisClient = redisUrl ? new Redis(redisUrl, options) : new Redis(options);
    
    if (isProd) {
      logger.info('Redis initialized in PRODUCTION mode');
    }

    redisClient.on('error', (err: any) => {
      const silentCodes = ['ECONNRESET', 'ETIMEDOUT', 'EHOSTUNREACH'];
      if (silentCodes.includes(err.code)) {
        logger.debug(`Redis connection transient error (${err.code}), retrying...`);
        return;
      }
      console.error('Redis Client Error', err);
    });
  }
  return redisClient;
};
