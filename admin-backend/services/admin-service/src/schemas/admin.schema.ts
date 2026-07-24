import { z } from 'zod';

export const UserActionSchema = z.object({
  targetUid: z.string().min(1, 'targetUid is required')
});

export const RoleUpdateSchema = z.object({
  targetUid: z.string().min(1, 'targetUid is required'),
  role: z.enum(['user', 'admin', 'superAdmin'])
});

export const AnnouncementPublishSchema = z.object({
  title: z.string().min(1, 'title is required'),
  content: z.string().min(1, 'content is required'),
  category: z.string().min(1, 'category is required')
});
