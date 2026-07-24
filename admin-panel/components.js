/**
 * components.js — Shared UI builder functions for the Obsidian Admin Panel.
 * Every function returns an HTML string built entirely from design tokens.
 * ZERO magic values — all constants from constants.js.
 * NOTE: constants.js must be loaded before this file.
 */

// ---------------------------------------------------------------------------
// MODAL BUILDERS
// ---------------------------------------------------------------------------

/**
 * Builds a standard modal shell with header, scrollable body, and footer.
 * @param {string} title       - Modal heading text
 * @param {string} icon        - Material Symbol icon name
 * @param {string} bodyHtml    - Inner body HTML content
 * @param {string} [footerHtml] - Optional footer action buttons HTML
 * @returns {string} Full modal HTML string
 */
function buildModalShell(title, icon, bodyHtml, footerHtml = '') {
    return `
    <div class="glass-card w-full max-w-md p-6 rounded-2xl border border-white/10 shadow-2xl relative screen-fade-in mx-4 max-h-[90vh] overflow-y-auto">
      <button
        class="absolute top-4 right-4 text-on-surface-variant hover:text-on-surface transition-colors p-1.5 rounded-lg hover:bg-white/5"
        onclick="closeModal()"
        aria-label="Close dialog"
      >
        <span class="material-symbols-outlined">close</span>
      </button>
      <h3 class="text-headline-md font-bold text-on-surface mb-6 flex items-center gap-3 pr-8">
        <span class="material-symbols-outlined text-primary text-2xl">${icon}</span>
        ${title}
      </h3>
      <div class="modal-body">${bodyHtml}</div>
      ${footerHtml ? `<div class="modal-footer pt-4 border-t border-white/5 mt-4">${footerHtml}</div>` : ''}
    </div>`;
}

/**
 * Builds a destructive-action confirmation modal with red danger styling.
 * Optionally requires the user to type an exact string before enabling confirm.
 * @param {string}  title           - Danger action title text
 * @param {string}  subtitle        - Explanatory subtitle text
 * @param {string}  confirmLabel    - Confirm button label
 * @param {string}  onConfirmJs     - JS expression to execute on confirm click
 * @param {string}  [typedNameGuard] - If set, user must type this to enable confirm
 * @returns {string} Full danger modal HTML string
 */
function buildDangerModal(title, subtitle, confirmLabel, onConfirmJs, typedNameGuard = null) {
    const guardInputId  = 'danger-confirm-input';
    const confirmBtnId  = 'danger-confirm-btn';
    const isGuarded     = Boolean(typedNameGuard);
    const escapedGuard  = typedNameGuard ? typedNameGuard.replace(/'/g, "\\'") : '';

    const guardSection = isGuarded ? `
      <div class="mt-4 space-y-2">
        <p class="text-label-sm text-on-surface-variant">
          Type <span class="font-bold text-on-surface">&ldquo;${typedNameGuard}&rdquo;</span> to confirm:
        </p>
        <input
          id="${guardInputId}"
          class="w-full bg-surface-container border border-white/10 rounded-lg px-4 py-2.5 text-on-surface focus:ring-1 focus:ring-error/50 focus:outline-none transition-all font-mono text-sm"
          type="text"
          placeholder="Type the name exactly..."
          oninput="document.getElementById('${confirmBtnId}').disabled = (this.value !== '${escapedGuard}')"
        />
      </div>` : '';

    return `
    <div class="glass-card w-full max-w-md p-6 rounded-2xl border border-error/20 shadow-2xl shadow-error/5 relative screen-fade-in mx-4">
      <div class="flex items-start gap-4 mb-5">
        <div class="w-12 h-12 rounded-xl bg-error/10 flex items-center justify-center flex-shrink-0">
          <span class="material-symbols-outlined text-error text-2xl" style="font-variation-settings:'FILL' 1">warning</span>
        </div>
        <div class="flex-1">
          <h3 class="text-headline-md font-bold text-on-surface mb-1">${title}</h3>
          <p class="text-body-md text-on-surface-variant">${subtitle}</p>
        </div>
      </div>
      ${guardSection}
      <div class="flex gap-3 pt-4 mt-2 border-t border-white/5">
        <button
          class="flex-1 py-2.5 border border-white/10 rounded-xl font-bold text-on-surface-variant hover:text-on-surface hover:bg-white/5 transition-colors"
          onclick="closeModal()"
        >Cancel</button>
        <button
          id="${confirmBtnId}"
          class="flex-1 py-2.5 bg-error text-white font-bold rounded-xl active:scale-95 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
          onclick="${onConfirmJs}"
          ${isGuarded ? 'disabled' : ''}
        >${confirmLabel}</button>
      </div>
    </div>`;
}

/**
 * Builds a progress modal for long-running operations.
 * Use animateProgressBar() to drive the bar after openModal().
 * @param {string} title     - Operation title text
 * @param {string} subtitleId - DOM id for the subtitle element (updated by JS)
 * @param {string} barId      - DOM id for the progress bar fill element
 * @returns {string} Progress modal HTML string
 */
function buildProgressModal(title, subtitleId, barId) {
    return `
    <div class="glass-card w-full max-w-md p-6 rounded-2xl border border-white/10 shadow-2xl relative screen-fade-in mx-4">
      <div class="flex items-center gap-3 mb-5">
        <div class="w-11 h-11 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
          <span class="material-symbols-outlined text-primary" style="animation: spin 1s linear infinite">autorenew</span>
        </div>
        <h3 class="text-headline-md font-bold text-on-surface">${title}</h3>
      </div>
      <p id="${subtitleId}" class="text-body-md text-on-surface-variant mb-5">Initialising...</p>
      <div class="h-2 bg-surface-container-high rounded-full overflow-hidden mb-2">
        <div
          id="${barId}"
          class="h-full bg-gradient-to-r from-primary to-secondary rounded-full transition-all duration-75"
          style="width: 0%"
        ></div>
      </div>
      <p class="text-label-sm text-on-surface-variant text-right" id="${barId}-pct">0%</p>
    </div>`;
}

// ---------------------------------------------------------------------------
// FORM BUILDERS
// ---------------------------------------------------------------------------

/**
 * Builds a labelled text input field with inline error slot.
 * @param {string} id          - Input element id
 * @param {string} label       - Label text shown above input
 * @param {string} type        - Input type: 'text' | 'email' | 'password'
 * @param {string} placeholder - Placeholder text
 * @returns {string} Input field HTML string
 */
function buildInputField(id, label, type, placeholder) {
    return `
    <div class="flex flex-col gap-1.5">
      <label class="text-label-sm text-on-surface-variant font-medium" for="${id}">${label}</label>
      <input
        id="${id}"
        type="${type}"
        placeholder="${placeholder}"
        class="bg-surface-container border border-white/10 rounded-lg px-4 py-2.5 text-on-surface focus:ring-1 focus:ring-primary focus:outline-none transition-all"
        oninput="clearFieldError('${id}')"
      />
      <p id="${id}-error" class="text-error text-xs hidden"></p>
    </div>`;
}

/**
 * Builds a labelled <select> dropdown field.
 * @param {string}   id         - Select element id
 * @param {string}   label      - Label text shown above select
 * @param {Array<{value: string, label: string}>} options - Options array
 * @param {string}   defaultVal - Pre-selected value
 * @returns {string} Select field HTML string
 */
function buildSelectField(id, label, options, defaultVal) {
    const optionsHtml = options.map(opt =>
        `<option value="${opt.value}"${opt.value === defaultVal ? ' selected' : ''}>${opt.label}</option>`
    ).join('');
    return `
    <div class="flex flex-col gap-1.5">
      <label class="text-label-sm text-on-surface-variant font-medium" for="${id}">${label}</label>
      <select
        id="${id}"
        class="bg-surface-container border border-white/10 rounded-lg px-4 py-2.5 text-on-surface focus:ring-1 focus:ring-primary focus:outline-none transition-all cursor-pointer"
      >${optionsHtml}</select>
    </div>`;
}

// ---------------------------------------------------------------------------
// COMPONENT ATOMS
// ---------------------------------------------------------------------------

/**
 * Builds a coloured status badge pill.
 * @param {string} status - STATUS.ACTIVE or STATUS.SUSPENDED constant
 * @returns {string} Badge HTML string
 */
function buildStatusBadge(status) {
    const isActive = status === STATUS.ACTIVE;
    const cls = isActive
        ? 'bg-secondary/10 text-secondary border-secondary/20'
        : 'bg-error/10 text-error border-error/20';
    return `<span class="inline-flex items-center px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${cls}">${status}</span>`;
}

/**
 * Builds an avatar block (initials circle or image) for a user object.
 * @param {Object}         user   - User object from MOCK_USERS (needs name, avatarColor, avatarUrl?)
 * @param {'sm'|'md'|'lg'} size   - Avatar size variant
 * @returns {string} Avatar HTML string
 */
function buildAvatarBlock(user, size = 'md') {
    const sizeMap = { sm: 'w-8 h-8 text-sm', md: 'w-10 h-10 text-base', lg: 'w-20 h-20 text-2xl' };
    const sizeClasses = sizeMap[size] || sizeMap.md;
    const initials = user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();

    if (user.avatarUrl) {
        return `<div class="${sizeClasses} rounded-full overflow-hidden border-2 border-white/10 flex-shrink-0">
          <img class="w-full h-full object-cover" src="${user.avatarUrl}" alt="${user.name}" />
        </div>`;
    }
    return `<div class="${sizeClasses} rounded-full ${user.avatarColor} flex items-center justify-center font-bold flex-shrink-0 border border-white/10">${initials}</div>`;
}

/**
 * Builds a single notification list item button.
 * @param {Object}  notification - Notification object from MOCK_NOTIFICATIONS
 * @param {number}  index        - Array index (used for click handler)
 * @param {boolean} isRead       - Whether the notification has been read
 * @returns {string} Notification item HTML string
 */
function buildNotificationItem(notification, index, isRead) {
    const cfg    = SEVERITY_CONFIG[notification.type] || SEVERITY_CONFIG[SEVERITY.INFO];
    const readCls = isRead ? 'opacity-50' : '';
    return `
    <button
      class="w-full flex items-start gap-3 p-3 rounded-xl hover:bg-white/5 transition-colors text-left ${readCls}"
      onclick="handleNotificationClick(${index})"
    >
      <span class="material-symbols-outlined ${cfg.colour} text-xl flex-shrink-0 mt-0.5" style="font-variation-settings:'FILL' 1">${cfg.icon}</span>
      <div class="flex-1 min-w-0">
        <p class="text-sm text-on-surface leading-snug">${notification.message}</p>
        <p class="text-xs text-on-surface-variant mt-1">${notification.time}</p>
      </div>
    </button>`;
}

// ---------------------------------------------------------------------------
// VALIDATION HELPERS
// ---------------------------------------------------------------------------

/**
 * Shows a field-level validation error beneath a form input.
 * @param {string} fieldId  - Input element id
 * @param {string} message  - Error message to display
 */
function showFieldError(fieldId, message) {
    const input = document.getElementById(fieldId);
    const error = document.getElementById(`${fieldId}-error`);
    if (input) {
        input.classList.add('border-error/60', 'ring-1', 'ring-error/30');
        input.classList.remove('border-white/10');
    }
    if (error) {
        error.textContent = message;
        error.classList.remove('hidden');
    }
}

/**
 * Clears a field-level validation error, restoring default styles.
 * @param {string} fieldId - Input element id
 */
function clearFieldError(fieldId) {
    const input = document.getElementById(fieldId);
    const error = document.getElementById(`${fieldId}-error`);
    if (input) {
        input.classList.remove('border-error/60', 'ring-1', 'ring-error/30');
        input.classList.add('border-white/10');
    }
    if (error) {
        error.classList.add('hidden');
        error.textContent = '';
    }
}

// ---------------------------------------------------------------------------
// ANIMATION UTILITIES
// ---------------------------------------------------------------------------

/**
 * Animates a progress bar fill element from 0% to 100%.
 * Uses TIMING.DEPLOY_PROGRESS_TICK_MS for tick interval.
 * @param {string}   barId      - DOM id of the fill element
 * @param {Function} onComplete - Callback fired when 100% is reached
 */
function animateProgressBar(barId, onComplete) {
    const bar    = document.getElementById(barId);
    const pctEl  = document.getElementById(`${barId}-pct`);
    let progress = 0;
    const tick   = setInterval(() => {
        progress = Math.min(progress + 1, 100);
        if (bar)   bar.style.width   = `${progress}%`;
        if (pctEl) pctEl.textContent = `${progress}%`;
        if (progress >= 100) {
            clearInterval(tick);
            if (typeof onComplete === 'function') onComplete();
        }
    }, TIMING.DEPLOY_PROGRESS_TICK_MS);
}
