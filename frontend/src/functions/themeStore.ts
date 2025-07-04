import { ref, watch } from 'vue';

export type Theme = 'light' | 'dark';

// Reactive theme state
const currentTheme = ref<Theme>('dark');

// Initialize theme from localStorage or default to dark mode
const initializeTheme = () => {
    const savedTheme = localStorage.getItem('app-theme') as Theme;
    
    if (savedTheme && (savedTheme === 'light' || savedTheme === 'dark')) {
        currentTheme.value = savedTheme;
    } else {
        // Default to dark mode for new users
        currentTheme.value = 'dark';
    }
    
    applyTheme(currentTheme.value);
};

// Apply theme to document
const applyTheme = (theme: Theme) => {
    const html = document.documentElement;
    
    if (theme === 'dark') {
        html.setAttribute('data-bs-theme', 'dark');
        html.classList.add('dark-theme');
        html.classList.remove('light-theme');
    } else {
        html.setAttribute('data-bs-theme', 'light');
        html.classList.add('light-theme');
        html.classList.remove('dark-theme');
    }
};

// Toggle theme
const toggleTheme = () => {
    currentTheme.value = currentTheme.value === 'light' ? 'dark' : 'light';
};

// Set specific theme
const setTheme = (theme: Theme) => {
    currentTheme.value = theme;
};

// Watch for theme changes and persist to localStorage
watch(currentTheme, (newTheme) => {
    localStorage.setItem('app-theme', newTheme);
    applyTheme(newTheme);
}, { immediate: true });

// Listen for system theme changes
const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
mediaQuery.addEventListener('change', () => {
    // Only apply system preference if no manual theme is set
    if (!localStorage.getItem('app-theme')) {
        // Default to dark mode even if system prefers light
        currentTheme.value = 'dark';
    }
});

export const themeStore = {
    currentTheme: currentTheme,
    initializeTheme,
    toggleTheme,
    setTheme,
    isDark: () => currentTheme.value === 'dark',
    isLight: () => currentTheme.value === 'light'
};
