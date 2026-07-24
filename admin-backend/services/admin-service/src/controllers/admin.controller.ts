import { Request, Response } from 'express';
import { AdminService } from '../services/admin.service';
import { UserActionSchema, RoleUpdateSchema, AnnouncementPublishSchema } from '../schemas/admin.schema';
import { catchAsync } from '@finance/shared';

export class AdminController {
  constructor(private adminService: AdminService) {}

  listUsers = catchAsync(async (req: Request, res: Response) => {
    const page = parseInt(req.query.page as string || '1', 10);
    const limit = parseInt(req.query.limit as string || '20', 10);
    
    const result = await this.adminService.getUsers(page, limit);

    return res.status(200).json({
      success: true,
      data: result.users,
      meta: { page, limit, total: result.total }
    });
  });

  suspendUser = catchAsync(async (req: Request, res: Response) => {
    const dto = UserActionSchema.parse(req.body);
    const performedBy = req.user!.userId;

    await this.adminService.suspendUser(dto.targetUid, performedBy);

    return res.status(200).json({
      success: true,
      message: 'User account has been suspended successfully'
    });
  });

  reactivateUser = catchAsync(async (req: Request, res: Response) => {
    const dto = UserActionSchema.parse(req.body);
    const performedBy = req.user!.userId;

    await this.adminService.reactivateUser(dto.targetUid, performedBy);

    return res.status(200).json({
      success: true,
      message: 'User account has been reactivated successfully'
    });
  });

  updateUserRole = catchAsync(async (req: Request, res: Response) => {
    const dto = RoleUpdateSchema.parse(req.body);
    const performedBy = req.user!.userId;

    await this.adminService.updateUserRole(dto.targetUid, dto.role, performedBy);

    return res.status(200).json({
      success: true,
      message: `User role has been updated to ${dto.role}`
    });
  });

  publishAnnouncement = catchAsync(async (req: Request, res: Response) => {
    const dto = AnnouncementPublishSchema.parse(req.body);
    const authorId = req.user!.userId;

    await this.adminService.publishAnnouncement(dto.title, dto.content, dto.category, authorId);

    return res.status(201).json({
      success: true,
      message: 'Announcement published successfully'
    });
  });

  getAnnouncements = catchAsync(async (_req: Request, res: Response) => {
    const result = await this.adminService.getAnnouncements();
    return res.status(200).json({
      success: true,
      data: result
    });
  });

  archiveAnnouncement = catchAsync(async (req: Request, res: Response) => {
    const { id } = req.params;
    const performedBy = req.user!.userId;

    await this.adminService.archiveAnnouncement(id!, performedBy);

    return res.status(200).json({
      success: true,
      message: 'Announcement archived successfully'
    });
  });

  getAuditLogs = catchAsync(async (req: Request, res: Response) => {
    const page = parseInt(req.query.page as string || '1', 10);
    const limit = parseInt(req.query.limit as string || '50', 10);

    const result = await this.adminService.getAuditLogs(page, limit);

    return res.status(200).json({
      success: true,
      data: result.logs,
      meta: { page, limit, total: result.total }
    });
  });
}
