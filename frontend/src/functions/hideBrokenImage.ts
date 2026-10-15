/**
 * Hides an image of the post content that failed to load (e.g. sites that don't allow hot linking),
 * instead of showing the broken image icon. Used as a capturing error listener on the content container,
 * since error events don't bubble: `@error.capture="hideBrokenImage"`.
 * A figure whose only media is the broken image is hidden too, so its caption isn't left alone.
 */
export const hideBrokenImage = (event: Event) => {
    const image = event.target;
    if (!(image instanceof HTMLImageElement)) return;

    image.style.display = 'none';
    if (image.parentElement instanceof HTMLPictureElement) {
        image.parentElement.style.display = 'none';
    }

    const figure = image.closest('figure');
    if (figure && !figure.querySelector('img:not([style*="display: none"]), video, iframe')) {
        figure.style.display = 'none';
    }
};
