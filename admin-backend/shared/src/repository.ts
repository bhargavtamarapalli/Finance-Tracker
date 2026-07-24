import { Pool, PoolClient, QueryResult } from 'pg';

export abstract class BaseRepository {
  constructor(protected db: Pool | PoolClient) {}

  protected async query(text: string, params?: unknown[]): Promise<QueryResult> {
    return this.db.query(text, params);
  }

  protected mapToCamelCase<T>(obj: Record<string, any> | null | undefined): T {
    if (!obj) return null as any;
    const result: Record<string, any> = {};
    for (const key in obj) {
      if (Object.prototype.hasOwnProperty.call(obj, key)) {
        const camelKey = key.replace(/([-_][a-z])/g, (group) =>
          group.toUpperCase().replace('-', '').replace('_', '')
        );
        result[camelKey] = obj[key];
      }
    }
    return result as T;
  }

  protected mapToSnakeCase(obj: Record<string, any>): Record<string, any> {
    const result: Record<string, any> = {};
    for (const key in obj) {
      if (Object.prototype.hasOwnProperty.call(obj, key)) {
        const snakeKey = key.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`);
        result[snakeKey] = obj[key];
      }
    }
    return result;
  }

  public async withTransaction<T>(fn: (client: PoolClient) => Promise<T>): Promise<T> {
    if ('connect' in this.db) {
      const client = await (this.db as Pool).connect();
      try {
        await client.query('BEGIN');
        const result = await fn(client);
        await client.query('COMMIT');
        return result;
      } catch (error) {
        await client.query('ROLLBACK');
        throw error;
      } finally {
        client.release();
      }
    } else {
      return fn(this.db as PoolClient);
    }
  }

  public async withSerializableTransaction<T>(fn: (client: PoolClient) => Promise<T>): Promise<T> {
    if ('connect' in this.db) {
      const client = await (this.db as Pool).connect();
      try {
        await client.query('BEGIN ISOLATION LEVEL SERIALIZABLE');
        const result = await fn(client);
        await client.query('COMMIT');
        return result;
      } catch (error) {
        await client.query('ROLLBACK');
        throw error;
      } finally {
        client.release();
      }
    } else {
      await (this.db as PoolClient).query('SET TRANSACTION ISOLATION LEVEL SERIALIZABLE');
      return fn(this.db as PoolClient);
    }
  }
}
