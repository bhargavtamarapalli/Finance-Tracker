import { Request, Response } from 'express';
import { AuthService } from '../services/auth.service';
import { VerifyTokenSchema } from '../schemas/auth.schema';
import { catchAsync, setAuthCookie, clearAuthCookie } from '@finance/shared';

export class AuthController {
  constructor(private authService: AuthService) {}

  login = catchAsync(async (req: Request, res: Response) => {
    const dto = VerifyTokenSchema.parse(req.body);
    const result = await this.authService.verifyFirebaseToken(dto.idToken);
    
    setAuthCookie(res, result.accessToken, result.refreshToken);

    return res.status(200).json({
      success: true,
      data: {
        user: result.user,
        accessToken: result.accessToken
      }
    });
  });

  logout = catchAsync(async (_req: Request, res: Response) => {
    clearAuthCookie(res);
    return res.status(200).json({
      success: true,
      message: 'Logged out successfully'
    });
  });

  me = catchAsync(async (req: Request, res: Response) => {
    return res.status(200).json({
      success: true,
      data: {
        user: req.user
      }
    });
  });
}
