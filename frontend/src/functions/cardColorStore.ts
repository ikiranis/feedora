import { ref, watch } from 'vue';
import { Feed } from '@/types';

const STORAGE_KEY = 'posts-card-colors';

// Whether the cards get a light background in their feed's color, persisted in localStorage (on by default)
const enabled = ref(localStorage.getItem(STORAGE_KEY) !== 'false');

watch(enabled, (value) => localStorage.setItem(STORAGE_KEY, String(value)));

export const cardColorStore = {
    enabled,

    toggle() {
        enabled.value = !enabled.value;
    },

    /**
     * Color of a feed's cards: the one chosen in the feed's settings, else the main color of its favicon.
     * Null when card colors are turned off or the feed has no color.
     */
    colorOf(feed: Feed | null | undefined): string | null {
        if (!enabled.value || !feed) return null;
        return feed.cardColor || feed.faviconColor || null;
    },
};
