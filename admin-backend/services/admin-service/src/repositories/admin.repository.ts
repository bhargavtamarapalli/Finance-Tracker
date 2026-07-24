import { BaseRepository } from '@finance/shared';
import { User, AdminAction, Announcement, Role } from '@finance/shared';

export class AdminRepository extends BaseRepository {
  async listUsers(page: number, limit: number): Promise<{ users: User[], total: number }> {
    const offset = (page - 1) * limit;
    
    const countRes = await this.db.query('SELECT COUNT(*) FROM users');
    const total = parseInt(countRes.rows[0]?.count || '0', 10);

    const usersRes = await this.db.query<User>(
      'SELECT id, email, role, status, created_at, last_login_at FROM users ORDER BY created_at DESC LIMIT $1 OFFSET $2',
      [limit, offset]
    );

    return {
      users: usersRes.rows,
      total
    };
  }

  async updateUserStatus(uid: string, status: 'ACTIVE' | 'SUSPENDED'): Promise<void> {
    await this.db.query(
      'UPDATE users SET status = $2 WHERE id = $1',
      [uid, status]
    );
  }

  async updateUserRole(uid: string, role: Role): Promise<void> {
    await this.db.query(
      'UPDATE users SET role = $2 WHERE id = $1',
      [uid, role]
    );
  }

  async logAction(action: Omit<AdminAction, 'timestamp'>): Promise<void> {
    await this.db.query(
      `INSERT INTO admin_actions (id, type, target_user_id, performed_by, description) 
       VALUES ($1, $2, $3, $4, $5)`,
      [action.id, action.type, action.targetUserId || null, action.performedBy, action.description]
    );
  }

  async listAuditLogs(page: number, limit: number): Promise<{ logs: AdminAction[], total: number }> {
    const offset = (page - 1) * limit;
    const countRes = await this.db.query('SELECT COUNT(*) FROM admin_actions');
    const total = parseInt(countRes.rows[0]?.count || '0', 10);

    const logsRes = await this.db.query<any>(
      `SELECT id, type, target_user_id AS "targetUserId", performed_by AS "performedBy", 
              description, EXTRACT(EPOCH FROM timestamp) * 1000 AS timestamp 
       FROM admin_actions ORDER BY timestamp DESC LIMIT $1 OFFSET $2`,
      [limit, offset]
    );

    return {
      logs: logsRes.rows,
      total
    };
  }

  async createAnnouncement(announcement: Omit<Announcement, 'timestamp'>): Promise<void> {
    await this.db.query(
      `INSERT INTO announcements (id, title, content, category, author_id, status) 
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [announcement.id, announcement.title, announcement.content, announcement.category, announcement.authorId, announcement.status]
    );
  }

  async listAnnouncements(status?: string): Promise<Announcement[]> {
    let query = `SELECT id, title, content, category, author_id AS "authorId", status, 
                        EXTRACT(EPOCH FROM timestamp) * 1000 AS timestamp FROM announcements`;
    const params: any[] = [];
    if (status) {
      query += ' WHERE status = $1';
      params.push(status);
    }
    query += ' ORDER BY timestamp DESC';
    const res = await this.db.query<any>(query, params);
    return res.rows;
  }

  async updateAnnouncementStatus(id: string, status: 'PUBLISHED' | 'ARCHIVED'): Promise<void> {
    await this.db.query(
      'UPDATE announcements SET status = $2 WHERE id = $1',
      [id, status]
    );
  }
}
