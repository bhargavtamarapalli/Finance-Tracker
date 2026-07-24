import { Pool, PoolConfig } from 'pg';

let pool: Pool | null = null;

export const getDbPool = (): Pool => {
  if (!pool) {
    const isProd = process.env.APP_ENV === 'production';
    const isSupabase = process.env.DATABASE_URL?.includes('supabase.co') || process.env.DATABASE_URL?.includes('supabase.com') || process.env.DATABASE_URL?.includes('pooler.supabase.com');

    const config: PoolConfig = process.env.DATABASE_URL 
      ? { connectionString: process.env.DATABASE_URL }
      : {
          host: process.env.DB_HOST,
          port: process.env.DB_PORT ? parseInt(process.env.DB_PORT, 10) : 5432,
          database: process.env.DB_NAME,
          user: process.env.DB_USER,
          password: process.env.DB_PASSWORD,
        };
    
    // --- PRODUCTION / SUPABASE SETTINGS ---
    if (isProd || isSupabase) {
      config.max = 10;
      config.idleTimeoutMillis = 30000;
      config.connectionTimeoutMillis = 5000;
      config.ssl = (isProd || isSupabase)
        ? { rejectUnauthorized: false } 
        : false;
    } 
    // --- DEVELOPMENT / LOCAL PG SETTINGS ---
    else {
      config.max = 20;
      config.idleTimeoutMillis = 30000;
      config.connectionTimeoutMillis = 2000;
    }

    pool = new Pool(config);

    pool.on('error', (err) => {
      console.error('Unexpected error on idle pg client', err);
      process.exit(-1);
    });
  }
  return pool;
};
