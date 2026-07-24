import { Router } from 'express';
import { getDbPool, requireAuth, requireRole } from '@finance/shared';
import { AdminController } from '../controllers/admin.controller';
import { AdminService } from '../services/admin.service';
import { AdminRepository } from '../repositories/admin.repository';

const router = Router();

// Wiring dependencies
const db = getDbPool();
const adminRepo = new AdminRepository(db);
const adminService = new AdminService(adminRepo);
const adminController = new AdminController(adminService);

// Health Check
router.get('/health', (_req, res) => { res.json({ status: 'ok', service: 'admin-service' }) });

// All administrative routes require valid auth AND admin privileges
router.use(requireAuth);
router.use(requireRole('admin', 'superAdmin'));

router.get('/users', adminController.listUsers);
router.post('/users/suspend', adminController.suspendUser);
router.post('/users/reactivate', adminController.reactivateUser);
router.patch('/users/role', adminController.updateUserRole);

router.post('/announcements', adminController.publishAnnouncement);
router.get('/announcements', adminController.getAnnouncements);
router.delete('/announcements/:id', adminController.archiveAnnouncement);

router.get('/audit-logs', adminController.getAuditLogs);

export { router as adminRouter };
