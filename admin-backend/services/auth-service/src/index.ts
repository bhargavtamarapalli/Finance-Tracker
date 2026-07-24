import dotenv from 'dotenv';
dotenv.config();

import express from 'express';
import cookieParser from 'cookie-parser';
import { authRouter } from './routes/auth.routes';
import { attachAuthUser, globalErrorHandler, logger } from '@finance/shared';

const app = express();
const port = process.env.PORT || 3001;

app.use(express.json());
app.use(cookieParser());

// Attach authenticated user metadata from JWT/Session cookies
app.use(attachAuthUser);

// Routes
app.use('/api/auth', authRouter);

// Global Error Handler
app.use(globalErrorHandler);

app.listen(port, () => {
  logger.info(`Auth Service listening on port ${port}`);
});
