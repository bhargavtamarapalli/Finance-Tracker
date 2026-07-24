import dotenv from 'dotenv';
dotenv.config();

import express from 'express';
import cookieParser from 'cookie-parser';
import { adminRouter } from './routes/admin.routes';
import { attachAuthUser, globalErrorHandler, logger } from '@finance/shared';

const app = express();
const port = process.env.PORT || 3002;

app.use(express.json());
app.use(cookieParser());

// Attach authenticated user metadata from JWT/Session cookies
app.use(attachAuthUser);

// Routes
app.use('/api/admin', adminRouter);

// Global Error Handler
app.use(globalErrorHandler);

app.listen(port, () => {
  logger.info(`Admin Service listening on port ${port}`);
});
