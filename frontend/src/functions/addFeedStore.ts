import { ref } from 'vue';

// The add feed modal (mounted once in App.vue) is shown
const isOpen = ref(false);
// Increased every time a feed is added, so pages can watch it and refresh their feeds
const addedCount = ref(0);

/**
 * Shows the add feed modal.
 */
const open = () => {
    const modal = document.getElementById('addFeedModal');
    if (modal) (window as any).bootstrap.Modal.getOrCreateInstance(modal).show();
};

export const addFeedStore = {
    isOpen,
    addedCount,
    open,
};
