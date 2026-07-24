import { BaseRepository } from '@finance/shared';
import { User, Role } from '@finance/shared';

export interface IUserRepository {
    findById(id: string): Promise<User | null>;
    findByEmail(email: string): Promise<User | null>;
    createUser(data: { id: string, email: string, role: Role, status: 'ACTIVE' | 'SUSPENDED' }): Promise<User>;
    updateRole(id: string, role: Role): Promise<User>;
    updateStatus(id: string, status: 'ACTIVE' | 'SUSPENDED'): Promise<User>;
    touchLastLogin(id: string): Promise<User>;
}

export class UserRepository extends BaseRepository implements IUserRepository {
    async findById(id: string): Promise<User | null> {
        const result = await this.db.query<User>(
            'SELECT id, email, role, status, created_at, last_login_at FROM users WHERE id = $1',
            [id]
        );
        return result.rows[0] ?? null;
    }

    async findByEmail(email: string): Promise<User | null> {
        const result = await this.db.query<User>(
            'SELECT id, email, role, status, created_at, last_login_at FROM users WHERE email = $1',
            [email]
        );
        return result.rows[0] ?? null;
    }

    async createUser(data: { id: string, email: string, role: Role, status: 'ACTIVE' | 'SUSPENDED' }): Promise<User> {
        const result = await this.db.query<User>(
            `INSERT INTO users (id, email, role, status) 
             VALUES ($1, $2, $3, $4) RETURNING id, email, role, status, created_at, last_login_at`,
            [data.id, data.email, data.role, data.status]
        );
        return result.rows[0]!;
    }

    async updateRole(id: string, role: Role): Promise<User> {
        const result = await this.db.query<User>(
            `UPDATE users SET role = $2 WHERE id = $1 RETURNING id, email, role, status, created_at, last_login_at`,
            [id, role]
        );
        return result.rows[0]!;
    }

    async updateStatus(id: string, status: 'ACTIVE' | 'SUSPENDED'): Promise<User> {
        const result = await this.db.query<User>(
            `UPDATE users SET status = $2 WHERE id = $1 RETURNING id, email, role, status, created_at, last_login_at`,
            [id, status]
        );
        return result.rows[0]!;
    }

    async touchLastLogin(id: string): Promise<User> {
        const result = await this.db.query<User>(
            `UPDATE users SET last_login_at = now() WHERE id = $1 RETURNING id, email, role, status, created_at, last_login_at`,
            [id]
        );
        return result.rows[0]!;
    }
}
