import { Router } from 'express';
import { getDbPool, requireAuth } from '@finance/shared';
import { AuthController } from '../controllers/auth.controller';
import { AuthService } from '../services/auth.service';
import { UserRepository } from '../repositories/user.repository';

const router = Router();

// Wiring dependencies
const db = getDbPool();
const userRepo = new UserRepository(db);
const authService = new AuthService(userRepo);
const authController = new AuthController(authService);

// Health Check
router.get('/health', (_req, res) => { res.json({ status: 'ok', service: 'auth-service' }) });

// Public Routes
router.post('/login', authController.login);

// Protected Routes
router.use(requireAuth);
router.post('/logout', authController.logout);
router.get('/me', authController.me);

export { router as authRouter };
