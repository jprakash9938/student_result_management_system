/**
 * Antigravity Web Core Utilities
 * Common shared JS utilities for AJAX, Security, CSRF, and UI components.
 */

// Global state holding logged in user details
let CURRENT_USER = null;

/**
 * Extract cookie value by name. Used to fetch XSRF-TOKEN.
 */
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

/**
 * Standard AJAX fetch wrapper.
 * Inject CSRF headers, handles content types, and captures common HTTP errors globally.
 */
async function apiCall(url, options = {}) {
    options.headers = options.headers || {};
    
    // Add default JSON headers if it's not FormData (for Excel uploads)
    if (!(options.body instanceof FormData)) {
        if (!options.headers['Content-Type']) {
            options.headers['Content-Type'] = 'application/json';
        }
    }
    options.headers['Accept'] = 'application/json';

    // Inject CSRF Token on all state-changing operations (POST, PUT, DELETE)
    const method = (options.method || 'GET').toUpperCase();
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
        const csrfToken = getCookie('XSRF-TOKEN');
        if (csrfToken) {
            options.headers['X-XSRF-TOKEN'] = csrfToken;
        }
    }

    // Enable credentials (cookies) to persist session
    options.credentials = 'same-origin';

    try {
        const response = await fetch(url, options);
        
        // Handle Session Expired
        if (response.status === 401) {
            // Check if we are already on login page to avoid infinite loops
            if (!window.location.pathname.endsWith('login.html')) {
                showToast('Your session has expired. Redirecting to login...', 'warning');
                setTimeout(() => {
                    window.location.href = '/login.html';
                }, 1500);
            }
            throw new Error('Unauthorized session.');
        }

        // Handle Access Denied
        if (response.status === 403) {
            showToast('Access Denied: You do not have permissions to perform this action.', 'error');
            throw new Error('Access denied (403).');
        }

        // If no content return null
        if (response.status === 24) {
            return null;
        }

        // Parse JSON if response is JSON, otherwise return text/blob
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            const data = await response.json();
            if (!response.ok) {
                // If it is our validation error response
                const msg = data.message || extractValidationErrors(data) || 'An error occurred';
                showToast(msg, 'error');
                throw new Error(msg);
            }
            return data;
        } else {
            if (!response.ok) {
                const text = await response.text();
                showToast(text || 'An error occurred', 'error');
                throw new Error(text);
            }
            return response;
        }

    } catch (error) {
        console.error('API Error: ', error);
        throw error;
    }
}

/**
 * Format field validation errors from global handler.
 */
function extractValidationErrors(data) {
    if (typeof data === 'object' && !data.message) {
        // Validation constraint map e.g. { rollNumber: "Roll number is required" }
        return Object.values(data).join(', ');
    }
    return null;
}

/**
 * Render custom Toast Notifications securely without innerHTML.
 */
function showToast(message, type = 'success') {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    // Add corresponding icon
    const icon = document.createElement('i');
    if (type === 'success') icon.className = 'fas fa-check-circle text-success';
    else if (type === 'error') icon.className = 'fas fa-exclamation-circle text-danger';
    else if (type === 'warning') icon.className = 'fas fa-exclamation-triangle text-warning';
    else icon.className = 'fas fa-info-circle text-info';

    const textSpan = document.createElement('span');
    textSpan.textContent = message; // Safe text injection to prevent XSS

    toast.appendChild(icon);
    toast.appendChild(textSpan);
    container.appendChild(toast);

    // Auto-remove toast after timeout
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(-10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => {
            toast.remove();
        }, 300);
    }, 4000);
}

/**
 * Secure DOM-based modal confirmation dialog to replace native window.confirm().
 */
function showConfirmModal(title, text, onConfirm) {
    let overlay = document.querySelector('#confirm-modal-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'confirm-modal-overlay';
        overlay.className = 'modal-overlay';
        
        const frame = document.createElement('div');
        frame.className = 'modal-frame';

        const header = document.createElement('div');
        header.className = 'modal-header';
        const h3 = document.createElement('h3');
        h3.id = 'confirm-modal-title';
        header.appendChild(h3);

        const body = document.createElement('div');
        body.className = 'modal-body';
        const p = document.createElement('p');
        p.id = 'confirm-modal-text';
        p.style.color = 'var(--text-secondary)';
        body.appendChild(p);

        const footer = document.createElement('div');
        footer.className = 'modal-footer';
        
        const cancelBtn = document.createElement('button');
        cancelBtn.className = 'btn btn-secondary';
        cancelBtn.textContent = 'Cancel';
        cancelBtn.onclick = () => closeModal(overlay);

        const confirmBtn = document.createElement('button');
        confirmBtn.id = 'confirm-modal-yes-btn';
        confirmBtn.className = 'btn btn-primary';
        confirmBtn.textContent = 'Confirm';

        footer.appendChild(cancelBtn);
        footer.appendChild(confirmBtn);
        
        frame.appendChild(header);
        frame.appendChild(body);
        frame.appendChild(footer);
        overlay.appendChild(frame);
        document.body.appendChild(overlay);
    }

    document.getElementById('confirm-modal-title').textContent = title;
    document.getElementById('confirm-modal-text').textContent = text;
    
    const confirmBtn = document.getElementById('confirm-modal-yes-btn');
    confirmBtn.onclick = () => {
        closeModal(overlay);
        onConfirm();
    };

    // Open modal
    setTimeout(() => overlay.classList.add('open'), 50);
}

function closeModal(overlay) {
    overlay.classList.remove('open');
}

/**
 * Fetch current user and populate layout avatar/username.
 */
async function getCurrentUser() {
    try {
        const user = await apiCall('/api/auth/me');
        if (!user || !user.authenticated) return null;

        CURRENT_USER = user;

        // Populating avatar/username in layout if exists
        const userNameEl = document.getElementById('header-user-name');
        if (userNameEl) {
            userNameEl.textContent = user.fullName;
        }
        const avatarEl = document.getElementById('header-user-avatar');
        if (avatarEl && user.fullName) {
            avatarEl.textContent = user.fullName.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
        }

        return user;
    } catch (e) {
        return null;
    }
}

/**
 * Invalidate session and clear user state on logout.
 */
async function handleLogout() {
    try {
        await apiCall('/api/auth/logout', { method: 'POST' });
        showToast('Logged out successfully.');
        setTimeout(() => {
            window.location.href = '/login.html';
        }, 500);
    } catch (e) {
        showToast('Logout request failed.', 'error');
    }
}

/**
 * Dark Theme Logic
 */
function initTheme() {
    const savedTheme = localStorage.getItem('theme');
    const isDark = savedTheme === 'dark' || (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches);
    
    if (isDark) {
        document.body.classList.add('dark-theme');
    } else {
        document.body.classList.remove('dark-theme');
    }
    updateThemeButtonIcon();
}

function toggleTheme() {
    document.body.classList.toggle('dark-theme');
    const isDark = document.body.classList.contains('dark-theme');
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
    updateThemeButtonIcon();
}

function updateThemeButtonIcon() {
    const icon = document.getElementById('theme-icon');
    const label = document.getElementById('theme-label');
    if (!icon) return;

    const isDark = document.body.classList.contains('dark-theme');
    if (isDark) {
        icon.className = 'fas fa-sun';
        if (label) label.textContent = 'Light Mode';
    } else {
        icon.className = 'fas fa-moon';
        if (label) label.textContent = 'Dark Mode';
    }
}

// Initialise theme automatically when script loads
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    const btn = document.getElementById('theme-toggle-btn');
    if (btn) {
        btn.addEventListener('click', toggleTheme);
    }
    
    // Inject FontAwesome stylesheet dynamically
    if (!document.querySelector('link[href*="font-awesome"]')) {
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css';
        document.head.appendChild(link);
    }

    // Mobile Menu Toggle Logic
    const mobileMenuBtn = document.getElementById('mobile-menu-btn');
    const sidebar = document.getElementById('app-sidebar');
    const overlay = document.getElementById('sidebar-overlay');

    if (mobileMenuBtn && sidebar && overlay) {
        mobileMenuBtn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
            overlay.classList.toggle('open');
        });

        overlay.addEventListener('click', () => {
            sidebar.classList.remove('open');
            overlay.classList.remove('open');
        });
    }

    // Fetch and display user profile info in the header
    getCurrentUser();
});
