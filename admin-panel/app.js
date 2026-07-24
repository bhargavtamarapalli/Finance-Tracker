// Central App Controller for Obsidian Web & Mobile Admin Panel

let currentTab = 'dashboard';
let currentMode = 'web';
let currentPeriod = 'Monthly';

// Dynamic state — seeded from mockData.js (load order: constants → mockData → this file)
let usersList    = [...MOCK_USERS];
let apiKeysList  = [...MOCK_API_KEYS];

// Initialize application
document.addEventListener('DOMContentLoaded', () => {
    switchTab('dashboard');
    setViewMode('web');
    setupMobileSidebarDrawer();
    
    // Start Mobile Simulator Clock — interval from TIMING constant
    updateSimulatorClock();
    setInterval(updateSimulatorClock, TIMING.CLOCK_UPDATE_INTERVAL_MS);

    // Set up persistent event delegation for Mobile Simulator bottom nav
    const phoneScreen = document.getElementById('phone-screen-container');
    if (phoneScreen) {
        phoneScreen.addEventListener('click', (e) => {
            const link = e.target.closest('nav a');
            if (link) {
                e.preventDefault();
                const text = link.innerText.toLowerCase();
                if (text.includes('overview') || text.includes('dashboard') || text.includes('home')) {
                    switchTab('dashboard');
                } else if (text.includes('alerts') || text.includes('users') || text.includes('history')) {
                    switchTab('users');
                } else if (text.includes('nodes') || text.includes('analytics')) {
                    switchTab('analytics');
                } else if (text.includes('settings')) {
                    switchTab('settings');
                }
            }
        });
    }
});

// Update Mobile Simulator status bar clock with real-time system clock
function updateSimulatorClock() {
    const timeEl = document.querySelector('.status-bar-time');
    if (timeEl) {
        const now = new Date();
        let hours = now.getHours();
        const minutes = String(now.getMinutes()).padStart(2, '0');
        timeEl.textContent = `${hours}:${minutes}`;
    }
}

// View mode switcher: 'web' or 'mobile'
function setViewMode(mode) {
    currentMode = mode;
    
    const btnWeb = document.getElementById('btn-view-web');
    const btnMobile = document.getElementById('btn-view-mobile');
    const desktopSidebar = document.getElementById('desktop-sidebar');
    const webViewport = document.getElementById('web-viewport');
    const mobileViewport = document.getElementById('mobile-viewport');

    if (mode === 'web') {
        btnWeb.className = "flex items-center gap-2 px-4 py-2 rounded-xl text-label-sm font-semibold transition-all duration-200 bg-primary text-on-primary shadow-lg shadow-primary/10";
        btnMobile.className = "flex items-center gap-2 px-4 py-2 rounded-xl text-label-sm font-semibold transition-all duration-200 text-on-surface-variant hover:text-on-surface";
        desktopSidebar.classList.remove('hidden');
        webViewport.classList.remove('hidden');
        mobileViewport.classList.add('hidden');
        mobileViewport.classList.remove('flex');
    } else {
        btnMobile.className = "flex items-center gap-2 px-4 py-2 rounded-xl text-label-sm font-semibold transition-all duration-200 bg-primary text-on-primary shadow-lg shadow-primary/10";
        btnWeb.className = "flex items-center gap-2 px-4 py-2 rounded-xl text-label-sm font-semibold transition-all duration-200 text-on-surface-variant hover:text-on-surface";
        desktopSidebar.classList.add('hidden');
        mobileViewport.classList.remove('hidden');
        mobileViewport.classList.add('flex');
        webViewport.classList.add('hidden');
    }

    renderCurrentScreen();
}

// Switch navigation tabs
function switchTab(tabId) {
    currentTab = tabId;
    
    // Update sidebar navigation active styling
    const tabs = ['dashboard', 'users', 'analytics', 'settings'];
    tabs.forEach(tab => {
        const navBtn = document.getElementById(`nav-${tab}`);
        if (navBtn) {
            if (tab === tabId) {
                navBtn.className = "w-full flex items-center gap-4 px-4 py-3 text-primary bg-primary-container/20 border-r-2 border-primary rounded-xl scale-[1.02] shadow-lg shadow-primary/5 transition-all text-left";
            } else {
                navBtn.className = "w-full flex items-center gap-4 px-4 py-3 text-on-surface-variant hover:bg-surface-variant hover:text-on-surface transition-all duration-200 rounded-xl group text-left";
            }
        }
    });

    renderCurrentScreen();
}

// Render dynamic screen template based on active tab and viewport mode
function renderCurrentScreen() {
    const containerId = currentMode === 'web' ? 'web-viewport' : 'phone-screen-container';
    const container = document.getElementById(containerId);
    
    if (!container) return;
    
    let html = templates[currentTab][currentMode];
    if (!html) {
        html = `<div class="p-8 text-center text-on-surface-variant">Screen template not loaded yet.</div>`;
    }
    
    // Intercept and inject dynamic users list into the users template on runtime
    if (currentTab === 'users') {
        if (currentMode === 'web') {
            const tbodyStart = html.indexOf('<tbody class="divide-y divide-outline-variant/10">');
            const tbodyEnd = html.indexOf('</tbody>', tbodyStart);
            if (tbodyStart !== -1 && tbodyEnd !== -1) {
                const prefix = html.substring(0, tbodyStart + '<tbody class="divide-y divide-outline-variant/10">'.length);
                const suffix = html.substring(tbodyEnd);
                html = prefix + getUsersTableHtml(usersList) + suffix;
            }
            // Sync user count badges
            html = html.replace('24,812', (24812 + usersList.length - 4).toLocaleString());
            html = html.replace('4,529', (4529 + usersList.filter(u => u.type === 'Business').length - 3).toLocaleString());
        } else {
            const cardsStart = html.indexOf('<!-- User Cards -->');
            const cardsContainerStart = html.indexOf('<div class="space-y-3">', cardsStart);
            const cardsContainerEnd = html.indexOf('</div>\n</section>', cardsContainerStart);
            if (cardsContainerStart !== -1 && cardsContainerEnd !== -1) {
                const prefix = html.substring(0, cardsContainerStart + '<div class="space-y-3">'.length);
                const suffix = html.substring(cardsContainerEnd);
                html = prefix + getUsersMobileHtml(usersList) + suffix;
            }
            // Sync user count badges
            html = html.replace('12,842', (12842 + usersList.length - 4).toLocaleString());
            html = html.replace('3,105', (3105 + usersList.filter(u => u.type === 'Business').length - 3).toLocaleString());
        }
    }
    
    container.innerHTML = html;
    attachScreenInteractiveEvents();
}

// Generate Web Users high-density table rows
function getUsersTableHtml(users) {
    return users.map((user, index) => {
        const initials = user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
        const avatar = user.avatarUrl 
            ? `<img class="w-full h-full object-cover" src="${user.avatarUrl}"/>` 
            : `<span class="font-body-md ${user.avatarColor.split(' ')[1]} font-bold">${initials}</span>`;
        
        const statusClass = user.status === 'Active' ? 'status-active' : 'status-suspended';
        const typeIcon = user.type === 'Business' ? 'business_center' : 'person';
        const typeColor = user.type === 'Business' ? 'text-secondary' : 'text-on-surface-variant';
        
        return `
        <tr class="hover:bg-white/5 transition-colors group">
            <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                    <div class="w-10 h-10 rounded-full ${user.avatarUrl ? 'overflow-hidden border border-white/10' : user.avatarColor} flex items-center justify-center">
                        ${avatar}
                    </div>
                    <div>
                        <p class="font-body-md text-body-md font-bold text-on-surface">${user.name}</p>
                        <p class="font-label-sm text-label-sm text-on-surface-variant">${user.email}</p>
                    </div>
                </div>
            </td>
            <td class="px-6 py-4 text-center">
                <span class="px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-tighter ${statusClass}">${user.status}</span>
            </td>
            <td class="px-6 py-4">
                <div class="flex items-center gap-2">
                    <span class="material-symbols-outlined ${typeColor} text-lg">${typeIcon}</span>
                    <span class="font-body-md text-body-md text-on-surface">${user.type}</span>
                </div>
            </td>
            <td class="px-6 py-4 font-label-sm text-label-sm text-on-surface-variant">
                ${user.lastActive}
            </td>
            <td class="px-6 py-4 text-right">
                <button class="p-2 rounded-lg hover:bg-surface-variant text-on-surface-variant transition-colors action-menu-trigger" data-index="${index}">
                    <span class="material-symbols-outlined">more_horiz</span>
                </button>
            </td>
        </tr>
        `;
    }).join('');
}

// Generate Mobile Users list cards
function getUsersMobileHtml(users) {
    return users.map((user, index) => {
        const initials = user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
        const avatar = user.avatarUrl 
            ? `<img class="w-full h-full object-cover" src="${user.avatarUrl}"/>` 
            : `<span class="font-body-md ${user.avatarColor.split(' ')[1]} font-bold">${initials}</span>`;
            
        const statusClass = user.status === 'Active' ? 'bg-secondary/10 text-secondary border border-secondary/20' : 'bg-error/10 text-error border border-error/20';
        const typeBadge = user.type === 'Business' ? 'bg-secondary/10 text-secondary border border-secondary/20' : 'bg-surface-variant text-on-surface-variant border border-white/5';
        const statusDot = user.status === 'Active' ? '• Active' : '• Suspended';
        const statusDotColor = user.status === 'Active' ? 'text-on-surface-variant' : 'text-error font-bold';
        
        return `
        <div class="user-card glass-card rounded-xl p-4 transition-all duration-300 active:scale-[0.98] ${user.status === 'Suspended' ? 'border-error/20' : ''}" data-index="${index}" onclick="toggleMobileCard(this)">
            <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                    <div class="w-12 h-12 rounded-full overflow-hidden border ${user.status === 'Suspended' ? 'border-error/30 grayscale' : 'border-secondary/30'} flex items-center justify-center">
                        ${avatar}
                    </div>
                    <div>
                        <h3 class="font-body-lg text-on-surface font-semibold">${user.name}</h3>
                        <div class="flex items-center gap-2 mt-0.5">
                            <span class="${typeBadge} font-label-sm text-[10px] px-2 py-0.5 rounded-full">${user.type.toUpperCase()}</span>
                            <span class="${statusDotColor} text-[10px] uppercase font-label-sm">${statusDot}</span>
                        </div>
                    </div>
                </div>
                <span class="material-symbols-outlined text-on-surface-variant transition-transform expand-icon">expand_more</span>
            </div>
            <div class="expanded-content">
                <div class="grid grid-cols-3 gap-2 border-t border-white/5 pt-4">
                    <button class="flex flex-col items-center gap-1 p-2 rounded-lg hover:bg-surface-variant transition-colors group mobile-suspend-btn" data-index="${index}">
                        <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">${user.status === 'Active' ? 'block' : 'check_circle'}</span>
                        <span class="text-[10px] font-label-sm text-on-surface-variant">${user.status === 'Active' ? 'Suspend' : 'Reactivate'}</span>
                    </button>
                    <button class="flex flex-col items-center gap-1 p-2 rounded-lg hover:bg-surface-variant transition-colors group mobile-reset-btn" data-index="${index}">
                        <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">lock_reset</span>
                        <span class="text-[10px] font-label-sm text-on-surface-variant">Reset</span>
                    </button>
                    <button class="flex flex-col items-center gap-1 p-2 rounded-lg hover:bg-surface-variant transition-colors group mobile-profile-btn" data-index="${index}">
                        <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">account_circle</span>
                        <span class="text-[10px] font-label-sm text-on-surface-variant">Profile</span>
                    </button>
                </div>
            </div>
        </div>
        `;
    }).join('');
}

// Mobile specific accordion accordion expand
function toggleMobileCard(element) {
    const isExpanded = element.classList.contains('card-active');
    
    // Collapse all card accordions
    document.querySelectorAll('.user-card').forEach(card => {
        card.classList.remove('card-active');
        const icon = card.querySelector('.expand-icon');
        if (icon) icon.style.transform = 'rotate(0deg)';
    });

    if (!isExpanded) {
        element.classList.add('card-active');
        const icon = element.querySelector('.expand-icon');
        if (icon) icon.style.transform = 'rotate(180deg)';
    }
}

// Bind interactive events dynamically per active tab
function attachScreenInteractiveEvents() {
    if (currentTab === 'dashboard') {
        setupOverviewInteractions();
    }
    
    if (currentTab === 'users') {
        // Wire search filters
        const searchInput = document.querySelector('input[placeholder*="Search users"]');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                const query = e.target.value.toLowerCase().trim();
                filterUsersList(query);
            });
        }

        // Wire Add User trigger
        const addBtn = Array.from(document.querySelectorAll('button')).find(btn => btn.innerText.includes('Add User'));
        if (addBtn) addBtn.onclick = openAddUserModal;

        const mobileAddBtn = document.querySelector('#mobile-viewport button.rounded-full');
        if (mobileAddBtn) mobileAddBtn.onclick = openAddUserModal;

        // Wire Web Table Actions Trigger
        const triggers = document.querySelectorAll('.action-menu-trigger');
        triggers.forEach(trig => {
            trig.onclick = (e) => {
                e.stopPropagation();
                const index = parseInt(trig.getAttribute('data-index'));
                openUserActionsMenu(index, trig);
            };
        });

        document.querySelectorAll('.mobile-suspend-btn').forEach(btn => {
            btn.onclick = (e) => {
                e.stopPropagation();
                const index = parseInt(btn.getAttribute('data-index'));
                openSuspendConfirmModal(index);
            };
        });
        document.querySelectorAll('.mobile-reset-btn').forEach(btn => {
            btn.onclick = (e) => {
                e.stopPropagation();
                const index = parseInt(btn.getAttribute('data-index'));
                resetUserPassword(index);
            };
        });
        document.querySelectorAll('.mobile-profile-btn').forEach(btn => {
            btn.onclick = (e) => {
                e.stopPropagation();
                const index = parseInt(btn.getAttribute('data-index'));
                openEnhancedProfileModal(index);
            };
        });

        // Wire Filter Sheet trigger
        const filterBtn = Array.from(document.querySelectorAll('button')).find(btn => btn.innerText.trim() === 'Filter' || btn.innerText.includes('Filter'));
        if (filterBtn) filterBtn.onclick = openFilterUsersSheet;

        // Wire Export download buttons
        const downloadBtn = Array.from(document.querySelectorAll('button')).find(btn => btn.innerText.includes('download') || btn.querySelector('span')?.innerText === 'download');
        if (downloadBtn) {
            downloadBtn.onclick = () => {
                showToast('Users list CSV exported successfully.', TOAST_TYPE.SUCCESS);
            };
        }
    }

    if (currentTab === 'analytics') {
        // Wire sync buttons
        const refreshBtn = Array.from(document.querySelectorAll('button')).find(btn => btn.innerText.includes('Refresh') || btn.innerText.includes('Sync') || btn.innerText.includes('Update') || btn.querySelector('span')?.innerText === 'sync');
        if (refreshBtn) {
            refreshBtn.onclick = () => {
                showToast("Synchronizing system clusters...", "warning");
                setTimeout(() => {
                    showToast("Analytics synchronized with 12 active node groups.");
                }, 1200);
            };
        }
    }

    if (currentTab === 'settings') {
        setupSettingsInteractions();
    }
}

// Filter users table/list matching query
function filterUsersList(query) {
    if (currentMode === 'web') {
        const rows = document.querySelectorAll('tbody tr');
        rows.forEach(row => {
            const name = row.cells[0]?.innerText.toLowerCase() || '';
            const email = row.cells[0]?.innerText.toLowerCase() || '';
            if (name.includes(query) || email.includes(query)) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });
    } else {
        const cards = document.querySelectorAll('#phone-screen-container .user-card');
        cards.forEach(card => {
            const textContent = card.innerText.toLowerCase();
            if (textContent.includes(query) || query === '') {
                card.style.display = '';
            } else {
                card.style.display = 'none';
            }
        });
    }
}

// Setup Overview / Dashboard Period Switcher and Exports
function setupOverviewInteractions() {
    const buttons = document.querySelectorAll('button');
    let mBtn, qBtn, expBtn;

    buttons.forEach(btn => {
        if (btn.innerText.trim() === 'Monthly') mBtn = btn;
        if (btn.innerText.trim() === 'Quarterly') qBtn = btn;
        if (btn.innerText.includes('Export Report')) expBtn = btn;
    });

    if (mBtn && qBtn) {
        const activeClass = "bg-surface-container-high text-on-surface font-bold px-4 py-2 rounded-xl text-body-md shadow-md";
        const inactiveClass = "text-on-surface-variant hover:text-on-surface px-4 py-2 rounded-xl text-body-md transition-colors";

        // Set initial classes dynamically
        if (currentPeriod === 'Monthly') {
            mBtn.className = activeClass;
            qBtn.className = inactiveClass;
        } else {
            qBtn.className = activeClass;
            mBtn.className = inactiveClass;
        }

        mBtn.onclick = () => {
            currentPeriod = 'Monthly';
            mBtn.className = activeClass;
            qBtn.className = inactiveClass;
            updateOverviewMetrics();
            showToast("Overview period set to Monthly.");
        };

        qBtn.onclick = () => {
            currentPeriod = 'Quarterly';
            qBtn.className = activeClass;
            mBtn.className = inactiveClass;
            updateOverviewMetrics();
            showToast("Overview period set to Quarterly.");
        };
    }

    if (expBtn) {
        expBtn.onclick = () => {
            showToast("Compiling financial indexes...", "warning");
            setTimeout(() => {
                showToast("Generating PDF report...", "warning");
                setTimeout(() => {
                    showToast("Obsidian_Revenue_Oct26.pdf downloaded successfully!");
                }, 1000);
            }, 1000);
        };
    }

    const viewAllLink = document.querySelector('a[href*="#"]');
    if (viewAllLink && viewAllLink.innerText.includes('View All')) {
        viewAllLink.onclick = (e) => {
            e.preventDefault();
            switchTab('users');
        };
    }
}

// Update dashboard text parameters depending on active period
function updateOverviewMetrics() {
    const totalRevEl = Array.from(document.querySelectorAll('h3')).find(h3 => h3.innerText.includes('$1,284,500') || h3.innerText.includes('$3,853,500'));
    const mrrEl = Array.from(document.querySelectorAll('h3')).find(h3 => h3.innerText.includes('$142,320') || h3.innerText.includes('$426,960'));
    
    if (currentPeriod === 'Monthly') {
        if (totalRevEl) totalRevEl.innerText = "$1,284,500";
        if (mrrEl) mrrEl.innerText = "$142,320";
    } else {
        if (totalRevEl) totalRevEl.innerText = "$3,853,500";
        if (mrrEl) mrrEl.innerText = "$426,960";
    }
}

// Global modal dialog open utility
function openModal(contentHtml) {
    const modal = document.getElementById('modal-container');
    if (!modal) return;
    modal.innerHTML = contentHtml;
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

// Global modal dialog close utility
function closeModal() {
    const modal = document.getElementById('modal-container');
    if (!modal) return;
    modal.classList.add('hidden');
    modal.classList.remove('flex');
    modal.innerHTML = '';
}

// Global slide-out sheet panel open utility
function openSheet(contentHtml) {
    const sheet = document.getElementById('sheet-container');
    if (!sheet) return;
    sheet.innerHTML = contentHtml;
    sheet.classList.remove('hidden');
    sheet.classList.add('flex');
    
    setTimeout(() => {
        const panel = sheet.querySelector('.sheet-panel');
        if (panel) {
            panel.classList.remove('translate-x-full');
        }
    }, 10);
}

// Global slide-out sheet panel close utility
function closeSheet() {
    const sheet = document.getElementById('sheet-container');
    if (!sheet) return;
    const panel = sheet.querySelector('.sheet-panel');
    if (panel) {
        panel.classList.add('translate-x-full');
    }
    setTimeout(() => {
        sheet.classList.add('hidden');
        sheet.classList.remove('flex');
        sheet.innerHTML = '';
    }, TIMING.SHEET_CLOSE_DELAY_MS);
}

// Add user modal popup window details
function openAddUserModal() {
    const content = `
    <div class="glass-card w-full max-w-md p-6 rounded-2xl border border-white/10 shadow-2xl relative screen-fade-in mx-4">
        <button class="absolute top-4 right-4 text-on-surface-variant hover:text-on-surface transition-colors" onclick="closeModal()">
            <span class="material-symbols-outlined">close</span>
        </button>
        <h3 class="text-headline-md font-bold text-on-surface mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">person_add</span>
            Add New User
        </h3>
        <div class="space-y-4">
            <div class="flex flex-col gap-1.5">
                <label class="text-label-sm text-on-surface-variant font-medium">Full Name</label>
                <input id="new-user-name" class="bg-surface-container border border-white/10 rounded-lg px-4 py-2.5 text-on-surface focus:ring-1 focus:ring-primary focus:outline-none transition-all" type="text" placeholder="e.g. Liam Vance"/>
            </div>
            <div class="flex flex-col gap-1.5">
                <label class="text-label-sm text-on-surface-variant font-medium">Email Address</label>
                <input id="new-user-email" class="bg-surface-container border border-white/10 rounded-lg px-4 py-2.5 text-on-surface focus:ring-1 focus:ring-primary focus:outline-none transition-all" type="email" placeholder="e.g. liam@obsidian.io"/>
            </div>
            <div class="flex flex-col gap-1.5">
                <label class="text-label-sm text-on-surface-variant font-medium">Profile Type</label>
                <select id="new-user-type" class="bg-surface-container border border-white/10 rounded-lg px-4 py-2.5 text-on-surface focus:ring-1 focus:ring-primary focus:outline-none transition-all cursor-pointer">
                    <option value="Basic">Basic Profile</option>
                    <option value="Business">Business Profile</option>
                </select>
            </div>
            <div class="flex justify-end gap-3 pt-4">
                <button class="px-4 py-2 rounded-xl text-on-surface-variant hover:text-on-surface transition-colors font-semibold" onclick="closeModal()">Cancel</button>
                <button class="px-6 py-2 bg-primary text-on-primary font-bold rounded-xl active:scale-95 transition-all" onclick="submitAddUserForm()">Add User</button>
            </div>
        </div>
    </div>
    `;
    openModal(content);
}

// User form submit validations and state changes
function submitAddUserForm() {
    const nameInput = document.getElementById('new-user-name');
    const emailInput = document.getElementById('new-user-email');
    const typeSelect = document.getElementById('new-user-type');
    
    if (!nameInput || !emailInput || !typeSelect) return;
    
    const name = nameInput.value.trim();
    const email = emailInput.value.trim();
    const type = typeSelect.value;
    
    if (!name || !email) {
        showToast("Please fill out all fields.", "error");
        return;
    }
    
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showToast("Invalid email address format.", "error");
        return;
    }
    
    const randColor = AVATAR_COLOUR_PALETTE[Math.floor(Math.random() * AVATAR_COLOUR_PALETTE.length)];
    
    const newUser = {
        name,
        email,
        status: "Active",
        type,
        lastActive: "Just now",
        avatarColor: randColor
    };
    
    usersList.unshift(newUser);
    closeModal();
    showToast(`User ${name} has been added successfully!`);
    renderCurrentScreen();
}

// Open Action Dropdown absolute menu
function openUserActionsMenu(index, triggerElement) {
    const user = usersList[index];
    const rect = triggerElement.getBoundingClientRect();
    
    let overlay = document.getElementById('user-actions-overlay');
    if (overlay) overlay.remove();
    
    overlay = document.createElement('div');
    overlay.id = 'user-actions-overlay';
    overlay.className = 'fixed z-50 bg-surface-container-high border border-white/10 rounded-xl shadow-2xl p-1.5 min-w-[160px] animate-fade-in';
    
    overlay.style.top = `${rect.bottom + window.scrollY + 6}px`;
    overlay.style.left = `${rect.right - 160 + window.scrollX}px`;
    
        overlay.innerHTML = `
        <button class="w-full flex items-center gap-3 px-3 py-2 text-sm text-left hover:bg-white/5 rounded-lg text-on-surface transition-colors" onclick="openSuspendConfirmModal(${index})">
            <span class="material-symbols-outlined text-[18px]">${user.status === STATUS.ACTIVE ? 'block' : 'check_circle'}</span>
            ${user.status === STATUS.ACTIVE ? 'Suspend User' : 'Activate User'}
        </button>
        <button class="w-full flex items-center gap-3 px-3 py-2 text-sm text-left hover:bg-white/5 rounded-lg text-on-surface transition-colors" onclick="resetUserPassword(${index})">
            <span class="material-symbols-outlined text-[18px]">lock_reset</span>
            Reset Password
        </button>
        <button class="w-full flex items-center gap-3 px-3 py-2 text-sm text-left hover:bg-white/5 rounded-lg text-on-surface transition-colors" onclick="openEnhancedProfileModal(${index})">
            <span class="material-symbols-outlined text-[18px]">account_circle</span>
            View Profile
        </button>
        <div class="my-1 border-t border-white/5"></div>
        <button class="w-full flex items-center gap-3 px-3 py-2 text-sm text-left hover:bg-error/10 rounded-lg text-error transition-colors" onclick="openDeleteUserModal(${index})">
            <span class="material-symbols-outlined text-[18px]">delete</span>
            Delete User
        </button>
    `;
    
    document.body.appendChild(overlay);
    
    const closeListener = (e) => {
        if (!overlay.contains(e.target) && e.target !== triggerElement && !triggerElement.contains(e.target)) {
            overlay.remove();
            document.removeEventListener('click', closeListener);
        }
    };
    
    setTimeout(() => {
        document.addEventListener('click', closeListener);
    }, TIMING.ACTION_MENU_CLOSE_MS);
}

// User state action functions
function toggleUserStatus(index) {
    const user = usersList[index];
    user.status = user.status === 'Active' ? 'Suspended' : 'Active';
    
    const overlay = document.getElementById('user-actions-overlay');
    if (overlay) overlay.remove();
    
    showToast(`User ${user.name} is now ${user.status}.`);
    renderCurrentScreen();
}

function resetUserPassword(index) {
    const user = usersList[index];
    
    const overlay = document.getElementById('user-actions-overlay');
    if (overlay) overlay.remove();
    
    showToast(`Password reset link dispatched to ${user.email}.`);
}

function viewUserProfile(index) {
    const user = usersList[index];
    
    const overlay = document.getElementById('user-actions-overlay');
    if (overlay) overlay.remove();
    
    const initials = user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
    const avatar = user.avatarUrl 
        ? `<img class="w-24 h-24 rounded-full object-cover border-2 border-primary/30" src="${user.avatarUrl}"/>` 
        : `<div class="w-24 h-24 rounded-full ${user.avatarColor} flex items-center justify-center text-3xl font-bold border-2 border-primary/20">${initials}</div>`;
        
    const content = `
    <div class="glass-card w-full max-w-md p-6 rounded-2xl border border-white/10 shadow-2xl relative screen-fade-in mx-4 text-center">
        <button class="absolute top-4 right-4 text-on-surface-variant hover:text-on-surface transition-colors" onclick="closeModal()">
            <span class="material-symbols-outlined">close</span>
        </button>
        
        <div class="flex flex-col items-center gap-4 mb-6">
            ${avatar}
            <div>
                <h3 class="text-headline-md font-bold text-on-surface">${user.name}</h3>
                <p class="text-on-surface-variant text-body-md">${user.email}</p>
            </div>
        </div>
        
        <div class="bg-surface-container rounded-xl p-4 text-left space-y-3 mb-6">
            <div class="flex justify-between border-b border-white/5 pb-2 text-sm">
                <span class="text-on-surface-variant">Profile Status</span>
                <span class="font-bold ${user.status === 'Active' ? 'text-secondary' : 'text-error'}">${user.status}</span>
            </div>
            <div class="flex justify-between border-b border-white/5 pb-2 text-sm">
                <span class="text-on-surface-variant">Access Tier</span>
                <span class="text-on-surface font-semibold">${user.type}</span>
            </div>
            <div class="flex justify-between border-b border-white/5 pb-2 text-sm">
                <span class="text-on-surface-variant">Last Active</span>
                <span class="text-on-surface font-semibold">${user.lastActive}</span>
            </div>
            <div class="flex justify-between text-sm">
                <span class="text-on-surface-variant">System Account ID</span>
                <span class="font-mono text-xs text-primary">USR-${10000 + index}</span>
            </div>
        </div>
        
        <button class="w-full py-2.5 bg-surface-container-high border border-white/10 rounded-xl font-bold text-on-surface hover:bg-white/5 transition-colors" onclick="closeModal()">
            Close Profile
        </button>
    </div>
    `;
    openModal(content);
}

// Slide sheet actions for managing key credentials
function openApiKeysSheet() {
    const sheetContent = `
    <div class="sheet-panel w-full max-w-md bg-surface-container-low border-l border-white/10 h-full p-6 flex flex-col shadow-2xl translate-x-full transition-transform duration-300 relative z-50">
        <div class="flex items-center justify-between border-b border-white/5 pb-4 mb-6">
            <h3 class="text-headline-sm font-bold text-on-surface flex items-center gap-2">
                <span class="material-symbols-outlined text-secondary">key</span>
                Manage API Keys
            </h3>
            <button class="material-symbols-outlined text-on-surface-variant hover:text-on-surface transition-colors p-2 rounded-full hover:bg-white/5" onclick="closeSheet()">
                close
            </button>
        </div>
        
        <p class="text-on-surface-variant text-sm mb-6">
            Generate and manage access keys for external service integrations and CLI tools. Keep private keys secure.
        </p>
        
        <div class="flex-1 overflow-y-auto space-y-4 pr-1" id="api-keys-list-container">
            ${getApiKeysListHtml()}
        </div>
        
        <div class="border-t border-white/5 pt-6 mt-6 space-y-3">
            <button class="w-full py-3 bg-secondary text-on-secondary font-bold rounded-xl active:scale-95 transition-all flex items-center justify-center gap-2" onclick="generateNewApiKey()">
                <span class="material-symbols-outlined">add</span>
                Generate New Key
            </button>
            <button class="w-full py-3 border border-white/10 rounded-xl font-bold text-on-surface-variant hover:text-on-surface transition-colors" onclick="closeSheet()">
                Close Panel
            </button>
        </div>
    </div>
    `;
    openSheet(sheetContent);
}

function getApiKeysListHtml() {
    if (apiKeysList.length === 0) {
        return `
        <div class="text-center py-12 text-on-surface-variant">
            <span class="material-symbols-outlined text-4xl opacity-30 mb-2">vpn_key</span>
            <p class="text-sm">No active API keys found.</p>
        </div>
        `;
    }
    
    return apiKeysList.map((keyObj, idx) => `
    <div class="bg-surface-container rounded-xl p-4 border border-white/5 relative">
        <div class="flex justify-between items-start mb-2">
            <span class="font-bold text-sm text-on-surface">${keyObj.name}</span>
            <button class="text-error hover:text-error-container p-1 rounded transition-colors" onclick="deleteApiKey(${idx})">
                <span class="material-symbols-outlined text-[18px]">delete</span>
            </button>
        </div>
        <div class="flex items-center justify-between bg-black/20 rounded px-3 py-2 mb-1.5">
            <span class="font-mono text-xs text-on-surface-variant">${keyObj.key}</span>
            <button class="text-secondary hover:text-secondary-fixed p-1 rounded transition-colors" onclick="copyApiKeyToClipboard('${keyObj.key}')">
                <span class="material-symbols-outlined text-[16px]">content_copy</span>
            </button>
        </div>
        <span class="text-[10px] text-on-surface-variant">Created on ${keyObj.created}</span>
    </div>
    `).join('');
}

// Generate new random mock API Key
function generateNewApiKey() {
    const name = prompt("Enter a name/label for this API key:", "CLI Node Integration");
    if (name === null) return;
    const finalName = name.trim() || "Generated Integration Key";
    
    const randomHex = Array.from({length: 8}, () => Math.floor(Math.random()*16).toString(16)).join('');
    const newKey = {
        name: finalName,
        key: `obs_live_${randomHex}`,
        created: new Date().toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' })
    };
    
    apiKeysList.push(newKey);
    const container = document.getElementById('api-keys-list-container');
    if (container) {
        container.innerHTML = getApiKeysListHtml();
    }
    showToast("New API key generated successfully!");
}

function copyApiKeyToClipboard(key) {
    navigator.clipboard.writeText(key).then(() => {
        showToast("API key copied to clipboard!");
    }).catch(() => {
        showToast("Failed to copy API key.", "error");
    });
}

function deleteApiKey(index) {
    const deletedName = apiKeysList[index].name;
    apiKeysList.splice(index, 1);
    const container = document.getElementById('api-keys-list-container');
    if (container) {
        container.innerHTML = getApiKeysListHtml();
    }
    showToast(`Key "${deletedName}" has been deleted.`);
}

// Bind Settings events
function setupSettingsInteractions() {
    // Context-specific toggle messages from TOGGLE_MESSAGES constant
    const toggles = document.querySelectorAll('input[type="checkbox"]');
    toggles.forEach(toggle => {
        toggle.onchange = (e) => {
            const toggleId = e.target.id;
            const msgs = TOGGLE_MESSAGES[toggleId];
            if (msgs) {
                showToast(e.target.checked ? msgs.on : msgs.off, e.target.checked ? TOAST_TYPE.SUCCESS : TOAST_TYPE.WARNING);
            } else {
                const labelEl = e.target.closest('label, div')?.querySelector('p, span');
                const toggleName = labelEl?.innerText?.trim() || 'System setting';
                const stateText = e.target.checked ? 'enabled' : 'disabled';
                showToast(`${toggleName} is now ${stateText}.`, e.target.checked ? TOAST_TYPE.SUCCESS : TOAST_TYPE.WARNING);
            }
            // Show unsaved changes indicator
            markSettingsUnsaved();
        };
    });

    // Custom select dropdown changes
    const dropdowns = document.querySelectorAll('select');
    dropdowns.forEach(select => {
        select.onchange = (e) => {
            const label = e.target.closest('div')?.querySelector('label')?.innerText || 'System config';
            showToast(`${label} configured to: ${e.target.value}`, TOAST_TYPE.WARNING);
            markSettingsUnsaved();
        };
    });

    // Save, Discard, and Manage API Keys button wiring
    const saveButtons = document.querySelectorAll('button');
    saveButtons.forEach(btn => {
        if (btn.innerText.includes('Save System Changes') || btn.innerText.includes('Save Configuration')) {
            btn.onclick = () => {
                showToast('System configurations saved successfully!', TOAST_TYPE.SUCCESS);
                clearSettingsUnsaved();
            };
        }
        if (btn.innerText.includes('Discard')) {
            btn.onclick = () => {
                showToast('All pending changes discarded.', TOAST_TYPE.WARNING);
                clearSettingsUnsaved();
                renderCurrentScreen();
            };
        }
        if (btn.innerText.includes('Manage API Keys')) {
            btn.onclick = openApiKeysSheet;
        }
    });
}

// Drawer navigator functions for mobile displays
function setupMobileSidebarDrawer() {
    const toggleBtn = document.getElementById('mobile-sidebar-toggle');
    const drawer = document.getElementById('mobile-nav-drawer');
    const closeBtn = document.getElementById('close-drawer-btn');

    if (toggleBtn && drawer && closeBtn) {
        toggleBtn.onclick = () => {
            drawer.classList.remove('hidden');
            setTimeout(() => {
                drawer.classList.remove('opacity-0');
                drawer.querySelector('.transform').classList.remove('-translate-x-full');
            }, 10);
        };

        const closeDrawerAction = () => {
            drawer.classList.add('opacity-0');
            drawer.querySelector('.transform').classList.add('-translate-x-full');
            setTimeout(() => {
                drawer.classList.add('hidden');
            }, 300);
        };

        closeBtn.onclick = closeDrawerAction;
        drawer.onclick = (e) => {
            if (e.target === drawer) closeDrawerAction();
        };
    }
}

function closeDrawer() {
    const drawer = document.getElementById('mobile-nav-drawer');
    if (drawer) {
        drawer.classList.add('opacity-0');
        drawer.querySelector('.transform').classList.add('-translate-x-full');
        setTimeout(() => {
            drawer.classList.add('hidden');
        }, 300);
    }
}

// Show responsive notification toast messages
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `glass-card py-4 px-6 rounded-2xl border border-white/10 flex items-center gap-3 shadow-xl transition-all duration-300 transform translate-x-12 opacity-0 screen-fade-in`;
    
    let icon = 'check_circle';
    let iconColor = 'text-secondary';
    if (type === 'warning') {
        icon = 'warning';
        iconColor = 'text-primary';
    } else if (type === 'error') {
        icon = 'error';
        iconColor = 'text-tertiary';
    }

    toast.innerHTML = `
        <span class="material-symbols-outlined ${iconColor} text-[20px]">${icon}</span>
        <span class="text-sm font-semibold text-on-surface">${message}</span>
    `;

    container.appendChild(toast);
    
    setTimeout(() => {
        toast.classList.remove('translate-x-12', 'opacity-0');
    }, 10);

    setTimeout(() => {
        toast.classList.add('translate-x-12', 'opacity-0');
        setTimeout(() => { toast.remove(); }, TIMING.TOAST_ANIMATE_DELAY_MS * 30);
    }, TIMING.TOAST_DISPLAY_MS);
}

// ---------------------------------------------------------------------------
// PHASE 4 — USER MANAGEMENT FLOWS
// ---------------------------------------------------------------------------

/**
 * Opens a confirmation modal before suspending or reactivating a user.
 * Uses buildDangerModal() from components.js.
 * @param {number} index - Index of the user in usersList
 */
function openSuspendConfirmModal(index) {
    const user = usersList[index];
    if (!user) return;
    const overlay = document.getElementById('user-actions-overlay');
    if (overlay) overlay.remove();
    const isSuspending = user.status === STATUS.ACTIVE;
    const html = buildDangerModal(
        isSuspending ? `Suspend ${user.name}?` : `Reactivate ${user.name}?`,
        isSuspending
            ? `${user.name} will immediately lose all platform access.`
            : `${user.name} will regain full platform access.`,
        isSuspending ? 'Confirm Suspend' : 'Confirm Reactivate',
        `closeModal(); toggleUserStatus(${index});`
    );
    openModal(html);
}

/**
 * Opens a typed-name danger modal before permanently deleting a user.
 * Uses buildDangerModal() with typedNameGuard from components.js.
 * @param {number} index - Index of the user in usersList
 */
function openDeleteUserModal(index) {
    const user = usersList[index];
    if (!user) return;
    const overlay = document.getElementById('user-actions-overlay');
    if (overlay) overlay.remove();
    const html = buildDangerModal(
        'Permanently Delete User?',
        'This action cannot be undone. All user data will be permanently removed.',
        'Delete Permanently',
        `confirmDeleteUser(${index})`,
        user.name
    );
    openModal(html);
}

/**
 * Confirms and executes permanent user deletion after typed-name guard passed.
 * @param {number} index - Index of the user in usersList
 */
function confirmDeleteUser(index) {
    const user = usersList[index];
    if (!user) { showToast('User not found.', TOAST_TYPE.ERROR); return; }
    const name = user.name;
    usersList.splice(index, 1);
    closeModal();
    showToast(`🗑️ ${name} has been permanently deleted.`, TOAST_TYPE.WARNING);
    renderCurrentScreen();
}

/**
 * Opens the Filter Users slide-out sheet with status, type, and period chips.
 * Uses openSheet() from app.js.
 */
function openFilterUsersSheet() {
    const chipBase   = 'px-3 py-1.5 rounded-lg text-sm font-semibold border transition-all cursor-pointer';
    const chipActive = 'bg-primary/10 text-primary border-primary/30';
    const chipOff    = 'bg-surface-container text-on-surface-variant border-white/5 hover:border-white/10';

    const sheetContent = `
    <div class="sheet-panel w-full max-w-sm bg-surface-container-low border-l border-white/10 h-full p-6 flex flex-col shadow-2xl translate-x-full transition-transform duration-300">
        <div class="flex items-center justify-between border-b border-white/5 pb-4 mb-6">
            <h3 class="text-headline-sm font-bold text-on-surface flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">filter_list</span>
                Filter Users
            </h3>
            <button class="material-symbols-outlined text-on-surface-variant hover:text-on-surface p-2 rounded-full hover:bg-white/5 transition-colors" onclick="closeSheet()">close</button>
        </div>
        <div class="flex-1 overflow-y-auto space-y-6 pr-1">
            <section>
                <p class="text-label-sm text-on-surface-variant uppercase tracking-widest mb-3 font-semibold">Status</p>
                <div class="flex gap-2 flex-wrap" id="fsg-status">
                    <button class="${chipBase} ${chipActive}" onclick="filterChipSelect('fsg-status',this,'all')">All</button>
                    <button class="${chipBase} ${chipOff}" onclick="filterChipSelect('fsg-status',this,'active')">Active</button>
                    <button class="${chipBase} ${chipOff}" onclick="filterChipSelect('fsg-status',this,'suspended')">Suspended</button>
                </div>
            </section>
            <section>
                <p class="text-label-sm text-on-surface-variant uppercase tracking-widest mb-3 font-semibold">Profile Type</p>
                <div class="flex gap-2 flex-wrap" id="fsg-type">
                    <button class="${chipBase} ${chipActive}" onclick="filterChipSelect('fsg-type',this,'all')">All</button>
                    <button class="${chipBase} ${chipOff}" onclick="filterChipSelect('fsg-type',this,'basic')">Basic</button>
                    <button class="${chipBase} ${chipOff}" onclick="filterChipSelect('fsg-type',this,'business')">Business</button>
                </div>
            </section>
            <section>
                <p class="text-label-sm text-on-surface-variant uppercase tracking-widest mb-3 font-semibold">Last Active</p>
                <div class="flex gap-2 flex-wrap" id="fsg-period">
                    <button class="${chipBase} ${chipActive}" onclick="filterChipSelect('fsg-period',this,'any')">Any</button>
                    <button class="${chipBase} ${chipOff}" onclick="filterChipSelect('fsg-period',this,'24h')">24h</button>
                    <button class="${chipBase} ${chipOff}" onclick="filterChipSelect('fsg-period',this,'7d')">7 days</button>
                    <button class="${chipBase} ${chipOff}" onclick="filterChipSelect('fsg-period',this,'30d')">30 days</button>
                </div>
            </section>
        </div>
        <div class="border-t border-white/5 pt-6 mt-4 space-y-3">
            <button class="w-full py-3 bg-primary text-on-primary font-bold rounded-xl active:scale-95 transition-all" onclick="applyUserFiltersFromSheet()">
                Apply Filters
            </button>
            <button class="w-full py-3 border border-white/10 rounded-xl font-bold text-on-surface-variant hover:text-on-surface transition-colors" onclick="resetUserFilters()">
                Reset Filters
            </button>
        </div>
    </div>`;
    openSheet(sheetContent);
}

/** Toggles filter chip active state within its group. */
function filterChipSelect(groupId, clickedBtn, value) {
    const chipActive = 'bg-primary/10 text-primary border-primary/30';
    const chipOff    = 'bg-surface-container text-on-surface-variant border-white/5 hover:border-white/10';
    const group = document.getElementById(groupId);
    if (!group) return;
    group.querySelectorAll('button').forEach(btn => {
        btn.className = btn.className.replace(chipActive, chipOff);
    });
    clickedBtn.className = clickedBtn.className.replace(chipOff, chipActive);
    clickedBtn.setAttribute('data-selected', value);
}

/** Reads selected chip values and applies filters, then closes sheet. */
function applyUserFiltersFromSheet() {
    const statusVal = document.querySelector('#fsg-status [data-selected]')?.getAttribute('data-selected') || 'all';
    const typeVal   = document.querySelector('#fsg-type [data-selected]')?.getAttribute('data-selected')   || 'all';
    closeSheet();
    const activeFilters = [statusVal !== 'all' ? statusVal : null, typeVal !== 'all' ? typeVal : null].filter(Boolean);
    const count = activeFilters.length;
    if (count > 0) {
        showToast(`Filters applied: ${activeFilters.join(', ')} (${count} active)`, TOAST_TYPE.SUCCESS);
    } else {
        showToast('Showing all users.', TOAST_TYPE.SUCCESS);
    }
    // Apply row-level filtering
    setTimeout(() => {
        if (currentMode === 'web') {
            document.querySelectorAll('tbody tr').forEach(row => {
                const text = row.innerText.toLowerCase();
                const statusOk = statusVal === 'all' || text.includes(statusVal);
                const typeOk   = typeVal   === 'all' || text.includes(typeVal);
                row.style.display = (statusOk && typeOk) ? '' : 'none';
            });
        } else {
            document.querySelectorAll('.user-card').forEach(card => {
                const text = card.innerText.toLowerCase();
                const statusOk = statusVal === 'all' || text.includes(statusVal);
                const typeOk   = typeVal   === 'all' || text.includes(typeVal);
                card.style.display = (statusOk && typeOk) ? '' : 'none';
            });
        }
    }, TIMING.SHEET_CLOSE_DELAY_MS + 50);
}

/** Resets all filters and re-renders the current screen. */
function resetUserFilters() {
    closeSheet();
    showToast('Filters cleared — showing all users.', TOAST_TYPE.WARNING);
    setTimeout(() => renderCurrentScreen(), TIMING.SHEET_CLOSE_DELAY_MS + 50);
}

/**
 * Opens the enhanced user profile modal with full extended data.
 * Uses buildModalShell(), buildAvatarBlock(), buildStatusBadge() from components.js.
 * @param {number} index - Index of the user in usersList
 */
function openEnhancedProfileModal(index) {
    const user = usersList[index];
    if (!user) return;
    const overlay = document.getElementById('user-actions-overlay');
    if (overlay) overlay.remove();

    const field = (label, valueHtml) => `
        <div class="flex justify-between items-center py-2.5 border-b border-white/5 last:border-0">
            <span class="text-xs text-on-surface-variant">${label}</span>
            <span class="text-sm text-on-surface font-semibold text-right max-w-[60%]">${valueHtml}</span>
        </div>`;

    const bodyHtml = `
        <div class="flex flex-col items-center gap-3 mb-6 text-center">
            ${buildAvatarBlock(user, 'lg')}
            <div>
                <h4 class="text-xl font-bold text-on-surface">${user.name}</h4>
                <p class="text-on-surface-variant text-sm">${user.email}</p>
                <div class="mt-2">${buildStatusBadge(user.status)}</div>
            </div>
        </div>
        <div class="bg-surface-container rounded-xl p-4 space-y-0.5 text-sm">
            ${field('Access Tier',    user.type)}
            ${field('System ID',      `<span class="font-mono text-xs text-primary">${user.systemId || `USR-1000${index}`}</span>`)}
            ${field('Member Since',   user.joinDate  || 'N/A')}
            ${field('Last Active',    user.lastActive)}
            ${field('Total Sessions', user.sessions  || 'N/A')}
            ${field('Device',         user.device    || 'N/A')}
            ${field('Region',         user.region    || 'N/A')}
        </div>`;

    const footerHtml = `
        <div class="flex gap-3">
            <button class="flex-1 py-2.5 border border-white/10 rounded-xl font-bold text-on-surface-variant hover:text-on-surface hover:bg-white/5 transition-colors text-sm" onclick="resetUserPassword(${index}); closeModal();">
                Reset Password
            </button>
            <button class="flex-1 py-2.5 rounded-xl font-bold transition-colors text-sm border
                ${user.status === STATUS.ACTIVE
                    ? 'bg-error/10 text-error border-error/20 hover:bg-error/20'
                    : 'bg-secondary/10 text-secondary border-secondary/20 hover:bg-secondary/20'}"
                onclick="closeModal(); openSuspendConfirmModal(${index});">
                ${user.status === STATUS.ACTIVE ? 'Suspend' : 'Reactivate'}
            </button>
        </div>
        <button class="w-full mt-2 py-2.5 bg-surface-container-high border border-white/10 rounded-xl font-bold text-on-surface hover:bg-white/5 transition-colors text-sm" onclick="closeModal()">
            Close Profile
        </button>`;

    openModal(buildModalShell(`${user.name}`, 'account_circle', bodyHtml, footerHtml));
}

// ---------------------------------------------------------------------------
// PHASE 6 — SETTINGS: UNSAVED CHANGES INDICATOR
// ---------------------------------------------------------------------------

/** Shows the amber "Unsaved Changes" chip in the Settings header. */
function markSettingsUnsaved() {
    let chip = document.getElementById('unsaved-chip');
    if (!chip) {
        const header = document.querySelector('h2, h1');
        if (!header) return;
        chip = document.createElement('span');
        chip.id = 'unsaved-chip';
        chip.className = 'inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-tertiary/10 text-tertiary border border-tertiary/20 ml-3 animate-pulse';
        chip.innerHTML = '<span class="w-1.5 h-1.5 rounded-full bg-tertiary inline-block"></span> Unsaved Changes';
        header.appendChild(chip);
    }
}

/** Removes the Unsaved Changes chip. */
function clearSettingsUnsaved() {
    const chip = document.getElementById('unsaved-chip');
    if (chip) chip.remove();
}

// ---------------------------------------------------------------------------
// PHASE 7 — GLOBAL: NOTIFICATIONS BELL PANEL
// ---------------------------------------------------------------------------

let _notificationsPanelOpen = false;

/**
 * Toggles the notifications bell panel open/closed.
 * Populated from MOCK_NOTIFICATIONS in mockData.js.
 */
function toggleNotificationsPanel() {
    const panel = document.getElementById('notifications-panel');
    if (!panel) return;

    if (_notificationsPanelOpen) {
        panel.classList.add('hidden');
        _notificationsPanelOpen = false;
        return;
    }

    const unreadCount = notificationReadState.filter(r => !r).length;
    const itemsHtml   = MOCK_NOTIFICATIONS
        .map((n, i) => buildNotificationItem(n, i, notificationReadState[i]))
        .join('');

    panel.innerHTML = `
    <div class="glass-card rounded-2xl border border-white/10 shadow-2xl overflow-hidden">
        <div class="flex items-center justify-between px-4 py-3 border-b border-white/5">
            <h4 class="font-bold text-on-surface flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-lg" style="font-variation-settings:'FILL' 1">notifications</span>
                Notifications
                ${unreadCount > 0 ? `<span class="bg-primary text-on-primary text-[10px] font-bold px-1.5 py-0.5 rounded-full">${unreadCount}</span>` : ''}
            </h4>
            <button class="text-xs text-on-surface-variant hover:text-primary transition-colors font-semibold" onclick="markAllNotificationsRead()">
                Mark all read
            </button>
        </div>
        <div class="max-h-80 overflow-y-auto p-2 space-y-1">${itemsHtml}</div>
    </div>`;

    panel.classList.remove('hidden');
    _notificationsPanelOpen = true;

    // Close panel when clicking outside
    setTimeout(() => {
        document.addEventListener('click', _closeNotifOnOutsideClick, { once: true });
    }, TIMING.ACTION_MENU_CLOSE_MS);
}

/** Handles a notification item click — routes to relevant tab and marks read. */
function handleNotificationClick(index) {
    const notif = MOCK_NOTIFICATIONS[index];
    if (!notif) return;
    notificationReadState[index] = true;
    toggleNotificationsPanel(); // close panel
    if (notif.tab) switchTab(notif.tab);
}

/** Marks all notifications as read and re-renders the panel. */
function markAllNotificationsRead() {
    notificationReadState = notificationReadState.map(() => true);
    toggleNotificationsPanel();
    setTimeout(() => toggleNotificationsPanel(), TIMING.TOAST_ANIMATE_DELAY_MS);
    showToast('All notifications marked as read.', TOAST_TYPE.SUCCESS);
}


/** Internal handler to close notifications panel on outside click. */
function _closeNotifOnOutsideClick(e) {
    const panel = document.getElementById('notifications-panel');
    if (panel && !panel.contains(e.target)) {
        panel.classList.add('hidden');
        _notificationsPanelOpen = false;
    }
}

// ---------------------------------------------------------------------------
// ADMIN PROFILE MANAGEMENT
// ---------------------------------------------------------------------------

/** In-memory admin profile state — editable via the profile modal. */
const ADMIN_PROFILE = {
    name:       'Alex Chen',
    email:      'admin@obsidian.io',
    role:       'Super Admin',
    joined:     'March 2021',
    lastLogin:  'Today, 07:18 AM',
    sessions:   1294,
    avatarUrl:  'https://lh3.googleusercontent.com/aida-public/AB6AXuCatFiVYgDGaQm2OMroUOw8x2XhDDhSxbClqSDtr1U1RhXr2E07Tz-H_Gz2V0HQUIklbbLVMM0Gr1CKnG2uwTTVa8su1r8LKlBPIAh2ct8SwZJ_pvaBvVG5X_ljDG99oJ49O70C4yk19Lwj8gaRIK7v87L1lvMj4P1vrqJmQkbwu12iYLNEBHtIGffdeQ2xq4HBWuAgiPkQ-vWnJTYrR6RSuXYj6UcsC5jCJ0IAcWtLm7rHV-o5YNJzOw',
    twoFa:      true,
};

let _adminMenuOpen = false;

/**
 * Toggles the compact admin profile dropdown menu.
 * Triggered by clicking the Alex Chen card (sidebar) or header avatar button.
 * @param {MouseEvent} e - Click event (used for stopPropagation)
 */
function toggleAdminProfileMenu(e) {
    e.stopPropagation();
    const existing = document.getElementById('admin-profile-menu');
    if (existing) {
        existing.remove();
        _adminMenuOpen = false;
        return;
    }

    const menu = document.createElement('div');
    menu.id = 'admin-profile-menu';
    menu.className = 'fixed z-50 w-64 glass-card border border-white/10 rounded-2xl shadow-2xl overflow-hidden screen-fade-in';

    // Position: anchor below the header avatar button
    const trigger = document.getElementById('header-profile-btn') || document.getElementById('sidebar-profile-btn');
    if (trigger) {
        const rect = trigger.getBoundingClientRect();
        menu.style.top  = `${rect.bottom + 8}px`;
        menu.style.right = `${window.innerWidth - rect.right}px`;
    } else {
        menu.style.top   = '72px';
        menu.style.right = '16px';
    }

    menu.innerHTML = `
        <!-- Profile Summary Header -->
        <div class="flex items-center gap-3 p-4 border-b border-white/5 bg-primary/5">
            <div class="w-12 h-12 rounded-full overflow-hidden border-2 border-primary/30 flex-shrink-0">
                <img class="w-full h-full object-cover" src="${ADMIN_PROFILE.avatarUrl}" alt="Alex Chen"/>
            </div>
            <div class="min-w-0">
                <p class="font-bold text-on-surface text-sm truncate">${ADMIN_PROFILE.name}</p>
                <p class="text-xs text-on-surface-variant truncate">${ADMIN_PROFILE.email}</p>
                <span class="inline-flex items-center gap-1 mt-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-primary/15 text-primary border border-primary/20">
                    <span class="material-symbols-outlined text-[10px]" style="font-variation-settings:'FILL' 1">verified</span>
                    ${ADMIN_PROFILE.role}
                </span>
            </div>
        </div>
        <!-- Menu Actions -->
        <div class="p-2 space-y-0.5">
            <button class="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm text-left text-on-surface hover:bg-white/5 transition-colors"
                onclick="closeAdminMenu(); openAdminProfileModal();">
                <span class="material-symbols-outlined text-primary text-lg" style="font-variation-settings:'FILL' 0">manage_accounts</span>
                View &amp; Edit Profile
            </button>
            <button class="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm text-left text-on-surface hover:bg-white/5 transition-colors"
                onclick="closeAdminMenu(); openChangePasswordModal();">
                <span class="material-symbols-outlined text-secondary text-lg">lock_reset</span>
                Change Password
            </button>
            <button class="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm text-left text-on-surface hover:bg-white/5 transition-colors"
                onclick="closeAdminMenu(); switchTab('settings');">
                <span class="material-symbols-outlined text-on-surface-variant text-lg">settings</span>
                System Settings
            </button>
            <div class="my-1 border-t border-white/5"></div>
            <button class="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm text-left text-error hover:bg-error/10 transition-colors"
                onclick="closeAdminMenu(); confirmLogout();">
                <span class="material-symbols-outlined text-error text-lg" style="font-variation-settings:'FILL' 1">logout</span>
                Sign Out
            </button>
        </div>`;

    document.body.appendChild(menu);
    _adminMenuOpen = true;

    setTimeout(() => {
        document.addEventListener('click', _closeAdminMenuOnOutside, { once: true });
    }, TIMING.ACTION_MENU_CLOSE_MS);
}

/** Closes the admin profile dropdown menu. */
function closeAdminMenu() {
    const menu = document.getElementById('admin-profile-menu');
    if (menu) { menu.remove(); }
    _adminMenuOpen = false;
}

/** Outside-click handler to auto-dismiss admin menu. */
function _closeAdminMenuOnOutside(e) {
    const menu = document.getElementById('admin-profile-menu');
    if (menu && !menu.contains(e.target)) { closeAdminMenu(); }
}

/**
 * Opens the full Admin Profile modal screen.
 * Displays all profile data from ADMIN_PROFILE with edit and security options.
 */
function openAdminProfileModal() {
    const field = (label, val) => `
        <div class="flex justify-between items-center py-2.5 border-b border-white/5 last:border-0">
            <span class="text-xs text-on-surface-variant">${label}</span>
            <span class="text-sm text-on-surface font-semibold text-right">${val}</span>
        </div>`;

    const bodyHtml = `
        <!-- Avatar + identity -->
        <div class="flex flex-col items-center gap-3 mb-6 text-center">
            <div class="relative">
                <div class="w-24 h-24 rounded-full overflow-hidden border-4 border-primary/30 shadow-xl shadow-primary/10">
                    <img class="w-full h-full object-cover" src="${ADMIN_PROFILE.avatarUrl}" alt="Alex Chen"/>
                </div>
                <span class="absolute bottom-1 right-1 w-4 h-4 rounded-full bg-secondary border-2 border-surface"></span>
            </div>
            <div>
                <h4 class="text-xl font-extrabold text-on-surface">${ADMIN_PROFILE.name}</h4>
                <p class="text-on-surface-variant text-sm">${ADMIN_PROFILE.email}</p>
                <div class="flex justify-center gap-2 mt-2">
                    <span class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-[10px] font-bold bg-primary/15 text-primary border border-primary/20">
                        <span class="material-symbols-outlined text-[10px]" style="font-variation-settings:'FILL' 1">verified</span>
                        ${ADMIN_PROFILE.role}
                    </span>
                    ${ADMIN_PROFILE.twoFa ? `<span class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-[10px] font-bold bg-secondary/15 text-secondary border border-secondary/20">
                        <span class="material-symbols-outlined text-[10px]" style="font-variation-settings:'FILL' 1">security</span>
                        2FA ON
                    </span>` : ''}
                </div>
            </div>
        </div>

        <!-- Profile data rows -->
        <div class="bg-surface-container rounded-xl p-4 space-y-0.5 text-sm mb-4">
            ${field('Member Since', ADMIN_PROFILE.joined)}
            ${field('Last Login',   ADMIN_PROFILE.lastLogin)}
            ${field('Total Sessions', ADMIN_PROFILE.sessions.toLocaleString())}
            ${field('2FA Status',   ADMIN_PROFILE.twoFa ? '🔒 Enabled' : '⚠️ Disabled')}
        </div>

        <!-- Quick action buttons inside body -->
        <div class="grid grid-cols-2 gap-3">
            <button class="flex items-center justify-center gap-2 py-2.5 rounded-xl border border-white/10 text-sm font-semibold text-on-surface hover:bg-white/5 transition-colors"
                onclick="closeModal(); openEditProfileModal();">
                <span class="material-symbols-outlined text-primary text-lg">edit</span>
                Edit Profile
            </button>
            <button class="flex items-center justify-center gap-2 py-2.5 rounded-xl border border-white/10 text-sm font-semibold text-on-surface hover:bg-white/5 transition-colors"
                onclick="closeModal(); openChangePasswordModal();">
                <span class="material-symbols-outlined text-secondary text-lg">lock_reset</span>
                Change Password
            </button>
        </div>`;

    const footerHtml = `
        <button class="w-full py-3 bg-error/10 text-error border border-error/20 rounded-xl font-bold text-sm hover:bg-error/20 transition-colors flex items-center justify-center gap-2"
            onclick="closeModal(); confirmLogout();">
            <span class="material-symbols-outlined text-lg" style="font-variation-settings:'FILL' 1">logout</span>
            Sign Out from Platform
        </button>`;

    openModal(buildModalShell('Admin Profile', 'manage_accounts', bodyHtml, footerHtml));
}

/**
 * Opens an inline edit form for the admin's name and email.
 */
function openEditProfileModal() {
    const bodyHtml = `
        <div class="space-y-4">
            ${buildInputField('edit-admin-name',  'Display Name', 'text',  'Alex Chen')}
            ${buildInputField('edit-admin-email', 'Email Address', 'email', 'admin@obsidian.io')}
        </div>`;

    // Pre-fill with current values
    setTimeout(() => {
        const nameEl  = document.getElementById('edit-admin-name');
        const emailEl = document.getElementById('edit-admin-email');
        if (nameEl)  nameEl.value  = ADMIN_PROFILE.name;
        if (emailEl) emailEl.value = ADMIN_PROFILE.email;
    }, TIMING.ACTION_MENU_CLOSE_MS);

    const footerHtml = `
        <div class="flex gap-3">
            <button class="flex-1 py-2.5 border border-white/10 rounded-xl font-bold text-on-surface-variant hover:text-on-surface hover:bg-white/5 transition-colors"
                onclick="closeModal()">Cancel</button>
            <button class="flex-1 py-2.5 gradient-button text-white font-bold rounded-xl active:scale-95 transition-all"
                onclick="saveAdminProfile()">Save Changes</button>
        </div>`;

    openModal(buildModalShell('Edit Profile', 'edit', bodyHtml, footerHtml));
}

/**
 * Saves edited admin profile values into ADMIN_PROFILE state and shows toast.
 */
function saveAdminProfile() {
    const nameEl  = document.getElementById('edit-admin-name');
    const emailEl = document.getElementById('edit-admin-email');
    let hasError  = false;

    if (!nameEl?.value?.trim()) {
        showFieldError('edit-admin-name', 'Name cannot be empty');
        hasError = true;
    }
    if (!emailEl?.value?.trim() || !VALIDATION.EMAIL_REGEX.test(emailEl.value.trim())) {
        showFieldError('edit-admin-email', 'Enter a valid email address');
        hasError = true;
    }
    if (hasError) return;

    ADMIN_PROFILE.name  = nameEl.value.trim();
    ADMIN_PROFILE.email = emailEl.value.trim();

    closeModal();
    showToast(`✅ Profile updated — ${ADMIN_PROFILE.name}`, TOAST_TYPE.SUCCESS);
}

/**
 * Opens a secure change-password modal with current / new / confirm fields.
 */
function openChangePasswordModal() {
    const bodyHtml = `
        <div class="space-y-4">
            ${buildInputField('cp-current', 'Current Password', 'password', 'Enter current password')}
            ${buildInputField('cp-new',     'New Password',     'password', 'Minimum 8 characters')}
            ${buildInputField('cp-confirm', 'Confirm New Password', 'password', 'Re-enter new password')}
        </div>
        <p class="text-xs text-on-surface-variant mt-3 px-1">
            Password must be at least 8 characters and contain a mix of letters and numbers.
        </p>`;

    const footerHtml = `
        <div class="flex gap-3">
            <button class="flex-1 py-2.5 border border-white/10 rounded-xl font-bold text-on-surface-variant hover:text-on-surface hover:bg-white/5 transition-colors"
                onclick="closeModal()">Cancel</button>
            <button class="flex-1 py-2.5 bg-primary text-on-primary font-bold rounded-xl active:scale-95 transition-all"
                onclick="saveNewPassword()">Update Password</button>
        </div>`;

    openModal(buildModalShell('Change Password', 'lock_reset', bodyHtml, footerHtml));
}

/**
 * Validates and saves the new password (mock — no real auth backend).
 */
function saveNewPassword() {
    const currentEl = document.getElementById('cp-current');
    const newEl     = document.getElementById('cp-new');
    const confirmEl = document.getElementById('cp-confirm');
    let hasError    = false;

    if (!currentEl?.value) { showFieldError('cp-current', 'Required'); hasError = true; }
    if (!newEl?.value || newEl.value.length < 8) {
        showFieldError('cp-new', 'Must be at least 8 characters'); hasError = true;
    }
    if (newEl?.value !== confirmEl?.value) {
        showFieldError('cp-confirm', 'Passwords do not match'); hasError = true;
    }
    if (hasError) return;

    closeModal();
    showToast('🔐 Password updated successfully.', TOAST_TYPE.SUCCESS);
}

/**
 * Danger confirmation modal before logging out. Redirects to login.html on confirm.
 */
function confirmLogout() {
    const html = buildDangerModal(
        'Sign Out?',
        'You will be securely signed out and redirected to the login page.',
        'Yes, Sign Me Out',
        'executeLogout()'
    );
    openModal(html);
}

/**
 * Executes the logout redirect to login.html.
 */
function executeLogout() {
    closeModal();
    showToast('Signing out…', TOAST_TYPE.WARNING);
    setTimeout(() => {
        window.location.href = 'login.html';
    }, TIMING.LOGIN_REDIRECT_MS);
}
