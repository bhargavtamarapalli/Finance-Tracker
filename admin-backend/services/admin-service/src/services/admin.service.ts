import crypto from 'crypto';
import { AdminRepository } from '../repositories/admin.repository';
import { firebaseAuth } from '../config/firebase';
import { User, AdminAction, Announcement, Role, AppError, ErrorCode } from '@finance/shared';

export class AdminService {
  constructor(private adminRepo: AdminRepository) {}

  async getUsers(page: number, limit: number): Promise<{ users: User[], total: number }> {
    return this.adminRepo.listUsers(page, limit);
  }

  async suspendUser(uid: string, performedBy: string): Promise<void> {
    await this.adminRepo.updateUserStatus(uid, 'SUSPENDED');

    try {
      await firebaseAuth.revokeRefreshTokens(uid);
      await firebaseAuth.setCustomUserClaims(uid, {
        admin: false,
        role: 'user',
        suspended: true
      });
    } catch (e: any) {
      console.error(`Failed to revoke Firebase claims for suspended user ${uid}`, e);
    }

    await this.adminRepo.logAction({
      id: crypto.randomUUID(),
      type: 'USER_SUSPEND',
      targetUserId: uid,
      performedBy,
      description: `Suspended user account: ${uid}`
    });
  }

  async reactivateUser(uid: string, performedBy: string): Promise<void> {
    await this.adminRepo.updateUserStatus(uid, 'ACTIVE');

    try {
      const usersRes = await this.adminRepo.listUsers(1, 1000);
      const user = usersRes.users.find(u => u.id === uid);
      if (user) {
        await firebaseAuth.setCustomUserClaims(uid, {
          admin: user.role === 'admin' || user.role === 'superAdmin',
          role: user.role
        });
      }
    } catch (e: any) {
      console.error(`Failed to restore Firebase claims for reactivated user ${uid}`, e);
    }

    await this.adminRepo.logAction({
      id: crypto.randomUUID(),
      type: 'USER_ACTIVATE',
      targetUserId: uid,
      performedBy,
      description: `Reactivated user account: ${uid}`
    });
  }

  async updateUserRole(uid: string, role: Role, performedBy: string): Promise<void> {
    await this.adminRepo.updateUserRole(uid, role);

    try {
      await firebaseAuth.setCustomUserClaims(uid, {
        admin: role === 'admin' || role === 'superAdmin',
        role: role
      });
    } catch (e: any) {
      console.error(`Failed to sync role custom claims for user ${uid}`, e);
    }

    await this.adminRepo.logAction({
      id: crypto.randomUUID(),
      type: 'USER_ROLE_UPDATE',
      targetUserId: uid,
      performedBy,
      description: `Updated user role to: ${role}`
    });
  }

  async publishAnnouncement(title: string, content: string, category: string, authorId: string): Promise<void> {
    await this.adminRepo.createAnnouncement({
      id: crypto.randomUUID(),
      title,
      content,
      category,
      authorId,
      status: 'PUBLISHED'
    });

    await this.adminRepo.logAction({
      id: crypto.randomUUID(),
      type: 'ANNOUNCEMENT_PUBLISH',
      performedBy: authorId,
      description: `Published new system announcement: "${title}"`
    });
  }

  async getAnnouncements(): Promise<Announcement[]> {
    return this.adminRepo.listAnnouncements();
  }

  async archiveAnnouncement(id: string, performedBy: string): Promise<void> {
    await this.adminRepo.updateAnnouncementStatus(id, 'ARCHIVED');
    
    await this.adminRepo.logAction({
      id: crypto.randomUUID(),
      type: 'ANNOUNCEMENT_ARCHIVE',
      performedBy,
      description: `Archived announcement ID: ${id}`
    });
  }

  async getAuditLogs(page: number, limit: number): Promise<{ logs: AdminAction[], total: number }> {
    return this.adminRepo.listAuditLogs(page, limit);
  }
}
