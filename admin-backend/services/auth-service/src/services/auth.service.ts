import jwt from 'jsonwebtoken';
import { IUserRepository } from '../repositories/user.repository';
import { firebaseAuth } from '../config/firebase';
import { AppError, ErrorCode, User, Role } from '@finance/shared';

export interface AuthResult {
  user: {
    id: string;
    email: string;
    role: Role;
    status: 'ACTIVE' | 'SUSPENDED';
  };
  accessToken: string;
  refreshToken: string;
}

export class AuthService {
  constructor(private userRepo: IUserRepository) {}

  async verifyFirebaseToken(idToken: string): Promise<AuthResult> {
    try {
      // 1. Verify token with Firebase
      const decodedToken = await firebaseAuth.verifyIdToken(idToken);
      const uid = decodedToken.uid;
      const email = decodedToken.email;

      if (!email) {
        throw new AppError(ErrorCode.VALIDATION_ERROR, 'No email address found in Firebase token');
      }

      // 2. Fetch or provision the user in PostgreSQL
      let user = await this.userRepo.findById(uid);
      if (!user) {
        user = await this.userRepo.createUser({
          id: uid,
          email: email,
          role: 'user', // Default user role
          status: 'ACTIVE'
        });
      }

      // 3. Prevent login if user is suspended
      if (user.status === 'SUSPENDED') {
        throw new AppError(ErrorCode.INSUFFICIENT_ROLE, 'This account has been suspended by administrators');
      }

      // 4. Sync Database role with Firebase Custom Claims
      await firebaseAuth.setCustomUserClaims(uid, {
        admin: user.role === 'admin' || user.role === 'superAdmin',
        role: user.role
      });

      // 5. Update last login timestamp
      const activeUser = await this.userRepo.touchLastLogin(user.id);

      // 6. Generate secure backend tokens
      const tokens = this.generateTokens(activeUser);

      return {
        user: activeUser,
        ...tokens
      };
    } catch (error: any) {
      if (error instanceof AppError) throw error;
      throw new AppError(ErrorCode.JWT_INVALID, 'Firebase token verification failed', { error: error.message });
    }
  }

  private generateTokens(user: User) {
    const jwtSecret = process.env.APP_JWT_SECRET;
    const refreshSecret = process.env.APP_REFRESH_JWT_SECRET || jwtSecret;

    if (!jwtSecret) {
      throw new AppError(ErrorCode.INTERNAL_ERROR, 'APP_JWT_SECRET is not configured');
    }

    const payload = {
      userId: user.id,
      role: user.role,
      email: user.email
    };

    const accessToken = jwt.sign(payload, jwtSecret, { expiresIn: '15m' });
    const refreshToken = jwt.sign(payload, refreshSecret!, { expiresIn: '30d' });

    return { accessToken, refreshToken };
  }
}
