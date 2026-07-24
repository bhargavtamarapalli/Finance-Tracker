import pino from 'pino';

// Apply basic masking so logs don't reveal sensitive PII (Rule 5)
export const maskMobile = (mobile: string): string => {
  if (!mobile || mobile.length < 4) return 'MASKED';
  return '*'.repeat(mobile.length - 4) + mobile.slice(-4);
};

export const maskEmail = (email: string): string => {
  if (!email || !email.includes('@')) return 'MASKED';
  const parts = email.split('@');
  const local = parts[0];
  const domain = parts[1];
  if (!local || !domain) return 'MASKED';
  if (local.length <= 2) return `*@${domain}`;
  return `${local.slice(0, 1)}*${local.slice(-1)}@${domain}`;
};

export const logger = pino({
  level: process.env.LOG_LEVEL || 'info',
  formatters: {
    level: (label) => {
      return { level: label };
    },
  },
});
