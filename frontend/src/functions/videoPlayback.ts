/**
 * Autoplay of tweet videos when their post is selected or opened. One video plays at a time: starting one pauses
 * the others (GIFs, which play muted in a loop without controls, are left alone).
 */

const isPlayer = (video: HTMLVideoElement) => video.controls && !video.loop;

/**
 * Plays a video, pausing the other players. Browsers may refuse sound before the user interacts with the page,
 * then it plays muted (the user can unmute it from its controls).
 */
export const playVideo = (video: HTMLVideoElement | null | undefined) => {
    if (!video || !video.isConnected) return;
    document.querySelectorAll('video').forEach(other => {
        if (other !== video && isPlayer(other) && !other.paused) other.pause();
    });
    video.play().catch(error => {
        if (error?.name !== 'NotAllowedError' || video.muted) return;
        video.muted = true;
        video.play().catch(() => {});
    });
};

/**
 * Plays the first video player inside an element, e.g. the opened post's content.
 */
export const playFirstVideo = (container: HTMLElement | null | undefined) => {
    const video = Array.from(container?.querySelectorAll('video') ?? []).find(isPlayer);
    playVideo(video);
};

/**
 * Pauses a video player, e.g. when its post is no longer selected.
 */
export const pauseVideo = (video: HTMLVideoElement | null | undefined) => {
    if (video && !video.paused) video.pause();
};

/**
 * Whether the post modal is open; selected cards don't play their video behind it.
 */
export const isModalOpen = () => !!document.querySelector('.post-modal');
