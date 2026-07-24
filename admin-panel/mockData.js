/**
 * mockData.js — Centralised fake data for all Admin Panel screens.
 * Single source of truth — app.js reads from here.
 * NOTE: constants.js must be loaded before this file.
 */

/**
 * Extended user list with full field set for rich profile modals.
 * Schema: { name, email, status, type, lastActive, avatarColor,
 *           systemId, joinDate, sessions, device, region, avatarUrl? }
 */
const MOCK_USERS = [
    {
        name:        'Elena Alistair',
        email:       'elena@obsidian.io',
        status:      STATUS.ACTIVE,
        type:        PLAN.BUSINESS,
        lastActive:  '2 mins ago',
        avatarColor: 'bg-primary/20 text-primary',
        systemId:    'USR-10001',
        joinDate:    'Oct 8, 2023',
        sessions:    47,
        device:      'macOS / Chrome 118',
        region:      'IN \u2014 Mumbai',
    },
    {
        name:        'Marcus Thorne',
        email:       'm.thorne@vortex.com',
        status:      STATUS.SUSPENDED,
        type:        PLAN.BASIC,
        lastActive:  '14 Oct, 2023',
        avatarColor: 'bg-tertiary/20 text-tertiary',
        systemId:    'USR-10002',
        joinDate:    'Mar 15, 2023',
        sessions:    12,
        device:      'Windows / Edge 119',
        region:      'US \u2014 New York',
    },
    {
        name:        'Sarah Jenkins',
        email:       'sarah.j@global.net',
        status:      STATUS.ACTIVE,
        type:        PLAN.BUSINESS,
        lastActive:  '1 hour ago',
        avatarColor: 'bg-secondary/20 text-secondary',
        systemId:    'USR-10003',
        joinDate:    'Jan 22, 2023',
        sessions:    104,
        device:      'macOS / Safari 17',
        region:      'UK \u2014 London',
    },
    {
        name:        'Julianne West',
        email:       'j.west@techflow.io',
        status:      STATUS.ACTIVE,
        type:        PLAN.BUSINESS,
        lastActive:  'Yesterday',
        avatarColor: 'bg-outline/20 text-outline',
        systemId:    'USR-10004',
        joinDate:    'Aug 3, 2023',
        sessions:    29,
        device:      'iOS / Safari 17',
        region:      'SG \u2014 Singapore',
        avatarUrl:   'https://lh3.googleusercontent.com/aida-public/AB6AXuBbQEoPwPYDAysmw4OBvYqkLwQMIZeUNmmApbvCxnK-PFaeYOitLwkTEW9omvsFrXRcA3jG-07m0vyXkhKRQMW4DFqEiVVa_NtH12hTKvlJeYaJxUhxisXBGKWVINSBvlo07499zGhu_AngaPfT8V3MZq5vrTz6RAZa9_Vn3XUm2BrdCENqsd0lWp4tz74In9tuJRUzp_xSR3QbPSGYL87ymLXV7l-cbVp4vADlAiyIBGHg5JFN6nskYg',
    },
    {
        name:        'Ravi Patel',
        email:       'ravi.p@startup.io',
        status:      STATUS.ACTIVE,
        type:        PLAN.BASIC,
        lastActive:  '3 hours ago',
        avatarColor: 'bg-primary/20 text-primary',
        systemId:    'USR-10005',
        joinDate:    'Sep 11, 2023',
        sessions:    63,
        device:      'Android / Chrome 118',
        region:      'IN \u2014 Bengaluru',
    },
    {
        name:        'Lyra Steinfeld',
        email:       'lyra@nordic.eu',
        status:      STATUS.ACTIVE,
        type:        PLAN.BUSINESS,
        lastActive:  '2 days ago',
        avatarColor: 'bg-secondary/20 text-secondary',
        systemId:    'USR-10006',
        joinDate:    'Jul 30, 2023',
        sessions:    87,
        device:      'macOS / Firefox 120',
        region:      'SE \u2014 Stockholm',
    },
    {
        name:        'Omar Kassim',
        email:       'o.kassim@fintech.ae',
        status:      STATUS.SUSPENDED,
        type:        PLAN.BUSINESS,
        lastActive:  '5 days ago',
        avatarColor: 'bg-tertiary/20 text-tertiary',
        systemId:    'USR-10007',
        joinDate:    'Jun 1, 2023',
        sessions:    8,
        device:      'Windows / Chrome 118',
        region:      'AE \u2014 Dubai',
    },
    {
        name:        'Priya Sharma',
        email:       'priya.s@enterprise.com',
        status:      STATUS.ACTIVE,
        type:        PLAN.BUSINESS,
        lastActive:  '30 mins ago',
        avatarColor: 'bg-outline/20 text-outline',
        systemId:    'USR-10008',
        joinDate:    'Feb 14, 2023',
        sessions:    211,
        device:      'macOS / Chrome 119',
        region:      'IN \u2014 Delhi',
    },
];

/** API integration keys — used by Settings screen */
const MOCK_API_KEYS = [
    { name: 'Live Production Key', key: 'obs_live_8f3d9c2a', created: 'Oct 10, 2023' },
    { name: 'Development Staging',  key: 'obs_test_4a7e1b8c', created: 'Oct 14, 2023' },
];

/** System incident feed — used by Analytics screen filter */
const MOCK_INCIDENTS = [
    { id: 'INC-001', severity: SEVERITY.CRITICAL, message: 'Node-07 (US-East) offline \u2014 auto-failover initiated',             time: '2m ago',  node: 'node-07-us-east'  },
    { id: 'INC-002', severity: SEVERITY.CRITICAL, message: 'Auth service latency spike: 4,200ms avg response detected',           time: '18m ago', node: 'node-02-eu-west'  },
    { id: 'INC-003', severity: SEVERITY.CRITICAL, message: 'Marcus Thorne \u2014 3 consecutive failed login attempts',               time: '22m ago', node: 'auth-cluster'     },
    { id: 'INC-004', severity: SEVERITY.WARNING,  message: 'Memory usage on node-03 exceeded 85% threshold',                     time: '1h ago',  node: 'node-03-ap-south' },
    { id: 'INC-005', severity: SEVERITY.WARNING,  message: 'SSL certificate expiry in 14 days for api.obsidian.io',               time: '3h ago',  node: 'api-gateway'      },
    { id: 'INC-006', severity: SEVERITY.WARNING,  message: 'Nordic region traffic spike: +340% over baseline',                    time: '5h ago',  node: 'cdn-edge-eu'      },
    { id: 'INC-007', severity: SEVERITY.WARNING,  message: 'Background job queue depth reached 1,200 pending tasks',             time: '7h ago',  node: 'worker-cluster'   },
    { id: 'INC-008', severity: SEVERITY.INFO,     message: 'Scheduled database maintenance completed successfully',               time: '9h ago',  node: 'db-primary'       },
    { id: 'INC-009', severity: SEVERITY.INFO,     message: 'v2.4.1 deployed \u2014 all 6 nodes running new build',                   time: '12h ago', node: 'all-nodes'        },
    { id: 'INC-010', severity: SEVERITY.INFO,     message: 'MRR Q4 milestone: $142,320 monthly recurring revenue exceeded',      time: '1d ago',  node: 'analytics'        },
];

/** Infrastructure server nodes — used by Analytics Node Detail panel */
const MOCK_NODES = [
    { id: 'node-01-us-east',  label: 'US East (N. Virginia)', cpu: 42, memory: 68, uptime: 99.97, incidents: 0 },
    { id: 'node-02-eu-west',  label: 'EU West (Frankfurt)',   cpu: 61, memory: 74, uptime: 98.20, incidents: 1 },
    { id: 'node-03-ap-south', label: 'AP South (Mumbai)',     cpu: 89, memory: 85, uptime: 99.50, incidents: 1 },
    { id: 'node-04-us-west',  label: 'US West (Oregon)',      cpu: 35, memory: 52, uptime: 99.99, incidents: 0 },
    { id: 'node-05-eu-north', label: 'EU North (Stockholm)',  cpu: 28, memory: 44, uptime: 99.99, incidents: 0 },
    { id: 'node-06-ap-east',  label: 'AP East (Singapore)',   cpu: 55, memory: 61, uptime: 99.80, incidents: 0 },
];

/** Notification feed — used by global bell panel */
const MOCK_NOTIFICATIONS = [
    { type: SEVERITY.CRITICAL, message: 'Marcus Thorne \u2014 3 consecutive failed login attempts', time: '2m ago',  tab: TAB.USERS     },
    { type: SEVERITY.WARNING,  message: 'Nordic region Business signups spiked +15% overnight',   time: '14m ago', tab: TAB.ANALYTICS },
    { type: SEVERITY.INFO,     message: 'MRR Q4 target exceeded \u2014 $142,320 recurring revenue',  time: '1h ago',  tab: TAB.DASHBOARD },
    { type: SEVERITY.CRITICAL, message: 'Cluster Node-07 US-East offline \u2014 failover active',    time: '23h ago', tab: TAB.ANALYTICS },
    { type: SEVERITY.INFO,     message: 'v2.4.1 deployed successfully across all 6 nodes',        time: '2d ago',  tab: TAB.ANALYTICS },
];

/** Mutable read-state tracker for notifications */
let notificationReadState = new Array(MOCK_NOTIFICATIONS.length).fill(false);
