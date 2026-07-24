import { z } from 'zod';

export const VerifyTokenSchema = z.object({
  idToken: z.string().min(1, 'Firebase token is required')
});
