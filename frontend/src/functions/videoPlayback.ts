/**
 * Autoplay of tweet videos when their post is selected or opened. One video plays at a time: starting one pauses
 * the others (GIFs, which autoplay in a loop without controls, are left alone). Videos start muted; the user can
 * unmute them from their controls, shown on hover or click. Autoplayed videos repeat, like GIFs.
 */

const isPlayer = (video: HTMLVideoElement) => !video.autoplay;

const playerOf = (target: EventTarget | null) => target instanceof HTMLVideoElement && isPlayer(target) ? target : null;

/**
 * Shows the controls of video players only while hovered, or once clicked (tapped on touch screens). Listens on the
 * document, so it covers the videos of post contents too.
 */
export const initVideoControls = () => {
    // Media events don't bubble, capturing catches every video as it starts loading; contents of tweets parsed
    // before controls were hidden still have the attribute
    document.addEventListener('loadstart', event => {
        const video = playerOf(event.target);
        if (video && !video.matches(':hover')) video.controls = false;
    }, true);
    document.addEventListener('mouseover', event => {
        const video = playerOf(event.target);
        if (video) video.controls = true;
    });
    // Capturing, as the card's video stops its clicks from opening the modal
    document.addEventListener('click', event => {
        const video = playerOf(event.target);
        if (video) video.controls = true;
    }, true);
    document.addEventListener('mouseout', event => {
        const video = playerOf(event.target);
        // Moving onto the controls themselves stays inside the video
        if (video && !(event.relatedTarget instanceof Node && video.contains(event.relatedTarget))) video.controls = false;
    });
};

/**
 * Plays a video in a loop, pausing the other players. Browsers may refuse sound before the user interacts with the
 * page, then it plays muted (the user can unmute it from its controls).
 */
export const playVideo = (video: HTMLVideoElement | null | undefined) => {
    if (!video || !video.isConnected) return;
    document.querySelectorAll('video').forEach(other => {
        if (other !== video && isPlayer(other) && !other.paused) other.pause();
    });
    video.loop = true;
    video.play().catch(error => {
        if (error?.name !== 'NotAllowedError' || video.muted) return;
        video.muted = true;
        video.play().catch(() => {});
    });
};

/**
 * Mutes the videos inside an element; content of tweets parsed before videos started muted lacks the attribute.
 */
export const muteVideos = (container: HTMLElement | null | undefined) => {
    container?.querySelectorAll('video').forEach(video => video.muted = true);
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
