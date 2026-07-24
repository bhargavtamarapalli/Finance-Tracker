/**
 * constants.js — Single source of truth for all app-wide constants.
 * NO magic numbers or hardcoded strings allowed anywhere else in the codebase.
 */

/** Avatar colour palette \u2014 must match Tailwind design tokens */
const AVATAR_COLOUR_PALETTE = [
    'bg-primary/20 text-primary',
    'bg-secondary/20 text-secondary',
    'bg-tertiary/20 text-tertiary',
    'bg-outline/20 text-outline'
];

/** All millisecond timing values for animations and delays */
const TIMING = Object.freeze({
    TOAST_DISPLAY_MS:        3500,
    TOAST_ANIMATE_DELAY_MS:  10,
    SHEET_CLOSE_DELAY_MS:    300,
    DEPLOY_PROGRESS_TICK_MS: 40,
    EXPORT_STAGE_ONE_MS:     1200,
    EXPORT_STAGE_TWO_MS:     1000,
    LOGIN_REDIRECT_MS:       1500,
    ACTION_MENU_CLOSE_MS:    10,
    CLOCK_UPDATE_INTERVAL_MS: 60000,
});

/** User account status labels */
const STATUS = Object.freeze({
    ACTIVE:    'Active',
    SUSPENDED: 'Suspended',
});

/** Subscription plan type labels */
const PLAN = Object.freeze({
    BASIC:    'Basic',
    BUSINESS: 'Business',
});

/** Toast notification type keys */
const TOAST_TYPE = Object.freeze({
    SUCCESS: 'success',
    WARNING: 'warning',
    ERROR:   'error',
});

/** Incident severity level keys */
const SEVERITY = Object.freeze({
    CRITICAL: 'critical',
    WARNING:  'warning',
    INFO:     'info',
});

/** Navigation tab IDs — must match template keys in templates.js */
const TAB = Object.freeze({
    DASHBOARD: 'dashboard',
    USERS:     'users',
    ANALYTICS: 'analytics',
    SETTINGS:  'settings',
});

/**
 * Toggle switch IDs mapped to context-specific ON/OFF toast messages.
 * Keys must match the id attribute on each <input type="checkbox"> in Settings template.
 */
const TOGGLE_MESSAGES = Object.freeze({
    'toggle-2fa':           { on: '\uD83D\uDD10 Two-Factor Authentication enforced for all users',        off: '2FA enforcement disabled'              },
    'toggle-auto-suspend':  { on: '\uD83D\uDD50 Auto-suspend enabled \u2014 30-day inactivity threshold',   off: 'Auto-suspend disabled'                 },
    'toggle-maintenance':   { on: '\u26A0\uFE0F Maintenance Mode ENABLED \u2014 users see maintenance page', off: '\u2705 Platform is live. Maintenance disabled' },
    'toggle-email-alerts':  { on: '\uD83D\uDCE7 Email alert notifications enabled',                        off: 'Email notifications paused'            },
    'toggle-sms-alerts':    { on: '\uD83D\uDCF1 SMS alert notifications enabled',                          off: 'SMS alerts paused'                     },
    'toggle-incidents':     { on: '\uD83D\uDD14 Critical incident alerts active',                          off: 'Incident notifications muted'          },
});

/** Form field validation constraints */
const VALIDATION = Object.freeze({
    MIN_NAME_LENGTH: 2,
    EMAIL_REGEX:     /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
});

/** Mock admin credentials used by login.html */
const MOCK_ADMIN_CREDENTIALS = Object.freeze({
    EMAIL:    'admin@obsidian.io',
    PASSWORD: 'admin123',
});

/** Incident severity display config */
const SEVERITY_CONFIG = Object.freeze({
    [SEVERITY.CRITICAL]: { icon: 'error',   colour: 'text-error',    bg: 'bg-error/10',    border: 'border-error/20'    },
    [SEVERITY.WARNING]:  { icon: 'warning', colour: 'text-tertiary', bg: 'bg-tertiary/10', border: 'border-tertiary/20' },
    [SEVERITY.INFO]:     { icon: 'info',    colour: 'text-primary',  bg: 'bg-primary/10',  border: 'border-primary/20'  },
});
