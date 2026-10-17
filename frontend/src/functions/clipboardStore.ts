import { ref } from 'vue';

// The text copied last, kept for a moment so the copy buttons can show a confirmation
const copiedText = ref<string | null>(null);
let resetTimer: ReturnType<typeof setTimeout> | undefined;

/**
 * Copies the text with the Clipboard API, falling back to a hidden textarea where it's not available
 * (e.g. insecure contexts or the JavaFX web view).
 */
const writeText = async (text: string): Promise<boolean> => {
    try {
        await navigator.clipboard.writeText(text);
        return true;
    } catch {
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        try {
            return document.execCommand('copy');
        } finally {
            textarea.remove();
        }
    }
};

export const clipboardStore = {
    async copy(text: string): Promise<boolean> {
        const copied = await writeText(text);
        if (copied) {
            copiedText.value = text;
            clearTimeout(resetTimer);
            resetTimer = setTimeout(() => copiedText.value = null, 2000);
        }
        return copied;
    },

    isCopied(text: string): boolean {
        return copiedText.value === text;
    },
};
