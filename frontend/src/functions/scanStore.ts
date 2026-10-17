import { ref } from 'vue';

// A scan was started from a page and hasn't returned yet, so the scan progress bar checks the status often
const active = ref(false);
// Increased every time a scan ends, so pages can watch it and refresh what the scan changed
const finishedCount = ref(0);

export const scanStore = {
    active,
    finishedCount,
};
