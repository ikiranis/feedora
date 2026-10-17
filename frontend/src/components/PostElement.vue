<template>
    <div v-if="showDetails" class="custom-card h-100" :class="{ 'unread-post': !post.read, 'read-post': post.read, 'selected-post': selected }" @click="handleCardClick">
        <!-- Media: YouTube thumbnail or first image of the post; clicking it opens the post in the modal -->
        <div v-if="thumbnailUrl" class="card-media" :class="{ 'is-short': youtubeVideo?.isShort }"
            @click="emit('open', post.id)">
            <img v-if="youtubeVideo?.isShort" :src="thumbnailUrl" class="media-backdrop" alt="" aria-hidden="true" />
            <img :src="thumbnailUrl" class="media-image" alt="" loading="lazy" @error="thumbnailFailed = true" />
            <span v-if="youtubeVideo" class="media-badge">
                <svg v-if="youtubeVideo.isShort" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M17.77 10.32l-1.2-.5L18 9.06a3.74 3.74 0 0 0-3.5-6.62L6 6.94a3.74 3.74 0 0 0 .23 6.74l1.2.49L6 14.93a3.75 3.75 0 0 0 3.5 6.63l8.5-4.5a3.74 3.74 0 0 0-.23-6.74zM10 14.65V9.35L15 12z"/>
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M23.5 6.2a3 3 0 0 0-2.1-2.1C19.5 3.6 12 3.6 12 3.6s-7.5 0-9.4.5A3 3 0 0 0 .5 6.2 31.4 31.4 0 0 0 0 12a31.4 31.4 0 0 0 .5 5.8 3 3 0 0 0 2.1 2.1c1.9.5 9.4.5 9.4.5s7.5 0 9.4-.5a3 3 0 0 0 2.1-2.1A31.4 31.4 0 0 0 24 12a31.4 31.4 0 0 0-.5-5.8zM9.6 15.6V8.4l6.2 3.6z"/>
                </svg>
                {{ youtubeVideo.isShort ? 'Shorts' : 'YouTube' }}
            </span>
            <span v-if="youtubeVideo" class="media-play">
                <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M11.596 8.697l-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393z"/>
                </svg>
            </span>
            <span v-if="youtubeVideo && embeddable === false" class="media-external">{{ watchOnYoutube }}</span>
            <VideoDuration v-if="youtubeVideo" :seconds="post.duration" />
        </div>

        <div class="card-content">
            <!-- The title opens the post in the modal (Ctrl/Cmd/Shift-click opens the original link) -->
            <a :href="post.link" target="_blank" class="post-title" @click="handleGridTitleClick"><LiveBadge :post="post" class="me-2" />{{ post.title }}</a>
            <div class="post-source">
                <img v-if="siteIcon" :src="siteIcon" class="site-icon source-icon" alt="" loading="lazy" @error="iconFailed = true" />
                <span class="text-truncate feed-link" role="button" :title="language.get('Show the posts of this feed') || 'Show the posts of this feed'"
                    @click.stop="post.feed && emit('show-feed', post.feed.id)">{{ post.feed?.title }}</span>
                <span v-if="post.feed?.folderName" class="post-folder text-truncate">&middot; {{ post.feed.folderName }}</span>
            </div>
            <p v-if="!youtubeVideo && excerpt" class="post-excerpt" :class="{ 'no-media': !thumbnailUrl }">{{ excerpt }}</p>

            <div class="card-footer-row">
                <span class="post-time" :title="post.pubDate ? new Date(post.pubDate).toLocaleString() : ''">{{ shortDate }}</span>
                <div class="d-flex align-items-center gap-3">
                    <button v-if="logCount" type="button" class="footer-action save-button logs-action" @click.stop="emit('show-logs', post)"
                        :title="language.get('The content of this post couldn\'t be fully retrieved') + ' — ' + language.get('View logs')">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
                            <path d="M7.938 2.016A.13.13 0 0 1 8.002 2a.13.13 0 0 1 .063.016.15.15 0 0 1 .054.057l6.857 11.667c.036.06.035.124.002.183a.2.2 0 0 1-.054.06.1.1 0 0 1-.066.017H1.146a.1.1 0 0 1-.066-.017.2.2 0 0 1-.054-.06.18.18 0 0 1 .002-.183L7.884 2.073a.15.15 0 0 1 .054-.057m1.044-.45a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767z"/>
                            <path d="M7.002 12a1 1 0 1 1 2 0 1 1 0 0 1-2 0M7.1 5.995a.905.905 0 1 1 1.8 0l-.35 3.507a.552.552 0 0 1-1.1 0z"/>
                        </svg>
                    </button>
                    <button type="button" class="footer-action save-button" :class="{ saved: post.saved }" @click.stop="emit('toggle-saved', post.id)"
                        :title="post.saved ? (language.get('Remove from saved') || 'Remove from saved') : (language.get('Save for later') || 'Save for later')">
                        <svg v-if="post.saved" xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
                            <path d="M2 2v13.5a.5.5 0 0 0 .74.439L8 13.069l5.26 2.87A.5.5 0 0 0 14 15.5V2a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2z"/>
                        </svg>
                        <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
                            <path d="M2 2a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v13.5a.5.5 0 0 1-.777.416L8 13.101l-5.223 2.815A.5.5 0 0 1 2 15.5V2zm2-1a1 1 0 0 0-1 1v12.566l4.723-2.482a.5.5 0 0 1 .554 0L13 14.566V2a1 1 0 0 0-1-1H4z"/>
                        </svg>
                    </button>
                    <a :href="post.link" target="_blank" class="footer-action" :title="language.get('Open original') || 'Open original'" @click.stop>
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
                            <path fill-rule="evenodd" d="M8.636 3.5a.5.5 0 0 0-.5-.5H1.5A1.5 1.5 0 0 0 0 4.5v10A1.5 1.5 0 0 0 1.5 16h10a1.5 1.5 0 0 0 1.5-1.5V7.864a.5.5 0 0 0-1 0V14.5a.5.5 0 0 1-.5.5h-10a.5.5 0 0 1-.5-.5v-10a.5.5 0 0 1 .5-.5h6.636a.5.5 0 0 0 .5-.5z"/>
                            <path fill-rule="evenodd" d="M16 .5a.5.5 0 0 0-.5-.5h-5a.5.5 0 0 0 0 1h3.793L6.146 9.146a.5.5 0 1 0 .708.708L15 1.707V5.5a.5.5 0 0 0 1 0v-5z"/>
                        </svg>
                    </a>
                    <button type="button" class="footer-action read-indicator" :class="{ unread: !post.read }" @click.stop="emit('toggle-read', post.id)"
                        :title="post.read ? (language.get('Mark as unread') || 'Mark as unread') : (language.get('Mark as read') || 'Mark as read')"></button>
                </div>
            </div>
        </div>
    </div>
    <div v-else class="list-row" :class="{ 'unread-post': !post.read, 'read-post': post.read, 'selected-post': selected }" @click="handleCardClick">
        <!-- The opened post shows its title and actions in the details header instead of the row -->
        <div v-if="!selected" class="list-line">
            <button type="button" class="read-dot" :class="{ unread: !post.read }" @click.stop="emit('toggle-read', post.id)"
                :title="post.read ? (language.get('Mark as unread') || 'Mark as unread') : (language.get('Mark as read') || 'Mark as read')"></button>
            <div class="list-source" :title="post.feed?.folderName ? `${post.feed?.title} — ${post.feed.folderName}` : post.feed?.title">
                <img v-if="siteIcon" :src="siteIcon" class="site-icon list-source-icon" alt="" loading="lazy" @error="iconFailed = true" />
                <span class="text-truncate feed-link" role="button" :title="language.get('Show the posts of this feed') || 'Show the posts of this feed'"
                    @click.stop="post.feed && emit('show-feed', post.feed.id)">{{ post.feed?.title }}</span>
            </div>
            <div class="list-main">
                <span v-if="youtubeVideo" class="list-video-badge">
                    <svg v-if="youtubeVideo.isShort" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M17.77 10.32l-1.2-.5L18 9.06a3.74 3.74 0 0 0-3.5-6.62L6 6.94a3.74 3.74 0 0 0 .23 6.74l1.2.49L6 14.93a3.75 3.75 0 0 0 3.5 6.63l8.5-4.5a3.74 3.74 0 0 0-.23-6.74zM10 14.65V9.35L15 12z"/>
                    </svg>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M23.5 6.2a3 3 0 0 0-2.1-2.1C19.5 3.6 12 3.6 12 3.6s-7.5 0-9.4.5A3 3 0 0 0 .5 6.2 31.4 31.4 0 0 0 0 12a31.4 31.4 0 0 0 .5 5.8 3 3 0 0 0 2.1 2.1c1.9.5 9.4.5 9.4.5s7.5 0 9.4-.5a3 3 0 0 0 2.1-2.1A31.4 31.4 0 0 0 24 12a31.4 31.4 0 0 0-.5-5.8zM9.6 15.6V8.4l6.2 3.6z"/>
                    </svg>
                </span>
                <LiveBadge :post="post" class="align-self-center" />
                <a :href="post.link" target="_blank" class="list-title" @click="handleTitleClick">{{ post.title }}</a>
                <span v-if="excerpt" class="list-excerpt">{{ excerpt }}</span>
            </div>
            <button type="button" class="list-save save-button" :class="{ saved: post.saved }" @click.stop="emit('toggle-saved', post.id)"
                    :title="post.saved ? (language.get('Remove from saved') || 'Remove from saved') : (language.get('Save for later') || 'Save for later')">
                    <svg v-if="post.saved" xmlns="http://www.w3.org/2000/svg" width="15" height="15" fill="currentColor" viewBox="0 0 16 16">
                        <path d="M2 2v13.5a.5.5 0 0 0 .74.439L8 13.069l5.26 2.87A.5.5 0 0 0 14 15.5V2a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2z"/>
                    </svg>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" width="15" height="15" fill="currentColor" viewBox="0 0 16 16">
                        <path d="M2 2a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v13.5a.5.5 0 0 1-.777.416L8 13.101l-5.223 2.815A.5.5 0 0 1 2 15.5V2zm2-1a1 1 0 0 0-1 1v12.566l4.723-2.482a.5.5 0 0 1 .554 0L13 14.566V2a1 1 0 0 0-1-1H4z"/>
                    </svg>
                </button>
            <span class="list-time" :title="post.pubDate ? new Date(post.pubDate).toLocaleString() : ''">{{ shortDate }}</span>
        </div>
        <!-- Expanded details for the selected post -->
        <div v-if="selected" class="list-details" @click.stop>
            <header class="details-header">
                <LiveBadge :post="post" class="title-live-badge" />
                <a :href="post.link" target="_blank" class="details-title">{{ post.title }}</a>
                <div class="details-meta">
                    <img v-if="siteIcon" :src="siteIcon" class="details-icon" alt="" @error="iconFailed = true" />
                    <span class="text-truncate feed-link" role="button" :title="language.get('Show the posts of this feed') || 'Show the posts of this feed'"
                        @click.stop="post.feed && emit('show-feed', post.feed.id)">{{ post.feed?.title }}</span>
                    <span v-if="post.author" class="text-truncate">&middot; {{ post.author }}</span>
                    <span v-if="post.pubDate" class="flex-shrink-0">&middot; {{ new Date(post.pubDate).toLocaleString() }}</span>
                </div>
                <div class="details-actions">
                    <button type="button" class="details-action save-button" :class="{ saved: post.saved }" @click="emit('toggle-saved', post.id)"
                        :title="post.saved ? (language.get('Remove from saved') || 'Remove from saved') : (language.get('Save for later') || 'Save for later')">
                        <svg v-if="post.saved" xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                            <path d="M2 2v13.5a.5.5 0 0 0 .74.439L8 13.069l5.26 2.87A.5.5 0 0 0 14 15.5V2a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2z"/>
                        </svg>
                        <svg v-else xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                            <path d="M2 2a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v13.5a.5.5 0 0 1-.777.416L8 13.101l-5.223 2.815A.5.5 0 0 1 2 15.5V2zm2-1a1 1 0 0 0-1 1v12.566l4.723-2.482a.5.5 0 0 1 .554 0L13 14.566V2a1 1 0 0 0-1-1H4z"/>
                        </svg>
                    </button>
                    <a :href="post.link" target="_blank" class="details-action" :title="language.get('Open original') || 'Open original'">
                        <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                            <path fill-rule="evenodd" d="M8.636 3.5a.5.5 0 0 0-.5-.5H1.5A1.5 1.5 0 0 0 0 4.5v10A1.5 1.5 0 0 0 1.5 16h10a1.5 1.5 0 0 0 1.5-1.5V7.864a.5.5 0 0 0-1 0V14.5a.5.5 0 0 1-.5.5h-10a.5.5 0 0 1-.5-.5v-10a.5.5 0 0 1 .5-.5h6.636a.5.5 0 0 0 .5-.5z"/>
                            <path fill-rule="evenodd" d="M16 .5a.5.5 0 0 0-.5-.5h-5a.5.5 0 0 0 0 1h3.793L6.146 9.146a.5.5 0 1 0 .708.708L15 1.707V5.5a.5.5 0 0 0 1 0v-5z"/>
                        </svg>
                    </a>
                    <button v-if="logCount" type="button" class="details-action logs-action" @click="emit('show-logs', post)"
                        :title="language.get('The content of this post couldn\'t be fully retrieved') + ' — ' + language.get('View logs')">
                        <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                            <path d="M7.938 2.016A.13.13 0 0 1 8.002 2a.13.13 0 0 1 .063.016.15.15 0 0 1 .054.057l6.857 11.667c.036.06.035.124.002.183a.2.2 0 0 1-.054.06.1.1 0 0 1-.066.017H1.146a.1.1 0 0 1-.066-.017.2.2 0 0 1-.054-.06.18.18 0 0 1 .002-.183L7.884 2.073a.15.15 0 0 1 .054-.057m1.044-.45a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767z"/>
                            <path d="M7.002 12a1 1 0 1 1 2 0 1 1 0 0 1-2 0M7.1 5.995a.905.905 0 1 1 1.8 0l-.35 3.507a.552.552 0 0 1-1.1 0z"/>
                        </svg>
                    </button>
                    <button type="button" class="details-action ms-auto" :title="language.get('Close') || 'Close'" @click="emit('close')">
                        <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 16 16">
                            <path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8z"/>
                        </svg>
                    </button>
                </div>
            </header>
            <div v-if="youtubeEmbedUrl && embeddable" class="ratio ratio-16x9 video-embed video-embed-summary">
                <iframe :src="youtubeEmbedUrl" :title="post.title" allowfullscreen
                    @load="youtubeEmbedStore.watchPlayer($event.target as HTMLIFrameElement, youtubeVideo!.id)"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"></iframe>
            </div>
            <!-- Thumbnail while checking the video, or a link to YouTube when it can't be embedded -->
            <a v-else-if="youtubeVideo" :href="post.link" target="_blank" class="card-media video-embed video-embed-summary d-block"
                :class="{ 'is-short': youtubeVideo.isShort }" :title="watchOnYoutube">
                <img v-if="youtubeVideo.isShort" :src="youtubeThumbnailUrl!" class="media-backdrop" alt="" aria-hidden="true" />
                <img :src="youtubeThumbnailUrl!" class="media-image" alt="" />
                <span v-if="embeddable === false" class="media-play visible">
                    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 16 16" fill="currentColor">
                        <path d="M11.596 8.697l-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393z"/>
                    </svg>
                </span>
                <span v-if="embeddable === false" class="media-external">{{ watchOnYoutube }}</span>
                <VideoDuration :seconds="post.duration" />
            </a>
            <div v-if="contentLoading" class="content-loading">
                <span class="spinner-border spinner-border-sm" role="status"></span>
                {{ language.get('Loading full article...') || 'Loading full article...' }}
            </div>
            <!-- While the full content loads, show only the spinner so the description isn't swapped out (no flicker) -->
            <div v-else-if="fullContent || post.description" class="summary-description"
                :class="{ 'post-content': fullContent }" v-html="fullContent || post.description"
                @error.capture="hideBrokenImage"></div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { PostType } from '@/types';
import { markAsRead } from '@/api/post';
import { postContentStore } from '@/functions/postContentStore';
import { hideBrokenImage } from '@/functions/hideBrokenImage';
import { youtubeEmbedStore } from '@/functions/youtubeEmbedStore';
import LiveBadge from '@/components/LiveBadge.vue';
import VideoDuration from '@/components/VideoDuration.vue';
import { language } from '@/functions/languageStore';
import { computed, ref, watch } from 'vue';

const props = defineProps<{
    post: PostType;
    showDetails: boolean;
    selected?: boolean;
}>();

const iconFailed = ref(false);

/**
 * Favicon URL of the post's site, based on the feed's website (or the post link as fallback).
 */
const siteIcon = computed(() => {
    if (iconFailed.value) return null;
    try {
        const hostname = new URL(props.post.feed?.htmlUrl || props.post.link).hostname;
        return `https://www.google.com/s2/favicons?domain=${hostname}&sz=32`;
    } catch {
        return null;
    }
});

const thumbnailFailed = ref(false);

/**
 * YouTube video info if the post links to a video (watch, youtu.be, shorts, embed or live links).
 */
const youtubeVideo = computed(() => {
    try {
        const url = new URL(props.post.link);
        const host = url.hostname.replace(/^(www\.|m\.)/, '');
        let videoId: string | null = null;
        let isShort = false;

        if (host === 'youtu.be') {
            videoId = url.pathname.slice(1).split('/')[0];
        } else if (host === 'youtube.com' || host === 'youtube-nocookie.com') {
            const match = url.pathname.match(/^\/(shorts|embed|live)\/([\w-]+)/);
            videoId = match ? match[2] : url.searchParams.get('v');
            isShort = match?.[1] === 'shorts';
        }

        return videoId && /^[\w-]{11}$/.test(videoId) ? { id: videoId, isShort } : null;
    } catch {
        return null;
    }
});

const youtubeEmbedUrl = computed(() =>
    youtubeVideo.value ? youtubeEmbedStore.embedUrl(youtubeVideo.value.id) : null
);

const youtubeThumbnailUrl = computed(() =>
    youtubeVideo.value ? `https://i.ytimg.com/vi/${youtubeVideo.value.id}/hqdefault.jpg` : null
);

// Whether the video can be played embedded (undefined until checked); some uploaders disable embedding
const embeddable = computed(() => youtubeVideo.value ? youtubeEmbedStore.isEmbeddable(youtubeVideo.value.id) : undefined);
const watchOnYoutube = computed(() => language.get('Watch on YouTube') || 'Watch on YouTube');

// The video of a post opened in list mode is checked before showing the player
watch([() => props.selected && !props.showDetails, youtubeVideo], ([opened, video]) => {
    if (opened && video) youtubeEmbedStore.check(video.id);
}, { immediate: true });

/**
 * Parsed post description (DOMParser doesn't load images or run scripts).
 */
const descriptionDoc = computed(() =>
    props.post.description ? new DOMParser().parseFromString(props.post.description, 'text/html') : null
);

/**
 * Thumbnail for the card: the YouTube thumbnail for videos, otherwise the first image in the description.
 */
const thumbnailUrl = computed(() => {
    if (thumbnailFailed.value) return null;
    if (youtubeThumbnailUrl.value) return youtubeThumbnailUrl.value;

    const src = descriptionDoc.value?.querySelector('img[src]')?.getAttribute('src');
    return src && /^https?:\/\//.test(src) ? src : null;
});

/**
 * Plain-text excerpt of the description.
 */
const excerpt = computed(() => descriptionDoc.value?.body.textContent?.replace(/\s+/g, ' ').trim() || '');

/**
 * Publication time for today's posts, otherwise the date.
 */
const shortDate = computed(() => {
    if (!props.post.pubDate) return '';
    const date = new Date(props.post.pubDate);
    return date.toDateString() === new Date().toDateString()
        ? date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : date.toLocaleDateString([], { day: 'numeric', month: 'short', year: 'numeric' });
});

watch(() => props.post.id, () => {
    iconFailed.value = false;
    thumbnailFailed.value = false;
});

// Full article HTML from the shared cache (preloaded by the posts list), shown when the post is opened in list mode
const fullContent = computed(() => postContentStore.get(props.post.id) ?? null);
const contentLoading = computed(() => postContentStore.isLoading(props.post.id));
// Log entries of the post, e.g. when its page couldn't be read
const logCount = computed(() => postContentStore.logCount(props.post.id));
const isOpened = computed(() => !!props.selected && !props.showDetails && !youtubeVideo.value);

/**
 * Loads the full article content when the post is opened (expanded) in list mode, if it wasn't preloaded.
 */
watch([isOpened, () => props.post.id], ([opened]) => {
    if (opened) postContentStore.load(props.post.id);
}, { immediate: true });

const emit = defineEmits<{
    'post-read': [postId: string]
    'toggle-saved': [postId: string]
    'toggle-read': [postId: string]
    'show-logs': [post: PostType]
    'show-feed': [feedId: string]
    'close': []
    'open': [postId: string]
}>();

/**
 * List mode title: a plain click expands the post in the app (the row's click handler);
 * Ctrl/Cmd/Shift-click (and middle-click) still open the original link.
 */
const handleTitleClick = (event: MouseEvent) => {
    if (event.ctrlKey || event.metaKey || event.shiftKey) {
        event.stopPropagation();
    } else {
        event.preventDefault();
    }
};

/**
 * Grid card title: a plain click opens the post in the modal;
 * Ctrl/Cmd/Shift-click (and middle-click) still open the original link.
 */
const handleGridTitleClick = (event: MouseEvent) => {
    if (event.ctrlKey || event.metaKey || event.shiftKey) {
        event.stopPropagation();
        return;
    }
    event.preventDefault();
    emit('open', props.post.id);
};

const handleCardClick = async () => {
    // Only mark as read if it's currently unread
    if (!props.post.read) {
        try {
            await markAsRead(props.post.id);
            // Emit event to notify parent component that the post has been marked as read
            emit('post-read', props.post.id);
        } catch (error) {
            console.error('Failed to mark post as read:', error);
        }
    }
};
</script>

<style scoped>
.custom-card {
    display: flex;
    flex-direction: column;
    border: 1px solid var(--border-color);
    border-radius: 1rem;
    background-color: var(--card-bg);
    overflow: hidden;
    transition: transform 0.2s, box-shadow 0.2s, border-color 0.3s;
}

[data-bs-theme="dark"] .custom-card {
    background-color: #16181d;
    border-color: #2a2e36;
}

.custom-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.25);
}

/* Media area */
.card-media {
    position: relative;
    aspect-ratio: 16 / 9;
    overflow: hidden;
    background-color: #000;
    flex-shrink: 0;
    cursor: pointer;
}

.card-media iframe {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    border: 0;
}

.media-image {
    position: relative;
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
}

/* Shorts: vertical frame centered over a blurred copy of itself */
.card-media.is-short .media-image {
    width: auto;
    margin: 0 auto;
    aspect-ratio: 9 / 16;
}

.media-backdrop {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    filter: blur(18px) brightness(0.5);
    transform: scale(1.2);
}

.media-badge {
    position: absolute;
    top: 0.75rem;
    left: 0.75rem;
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.2rem 0.55rem;
    border-radius: 0.4rem;
    background-color: rgba(0, 0, 0, 0.75);
    color: #fff;
    font-size: 0.8rem;
    font-weight: 600;
}

.media-play {
    position: absolute;
    top: 50%;
    left: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 3.25rem;
    height: 3.25rem;
    border-radius: 50%;
    background-color: rgba(0, 0, 0, 0.6);
    color: #fff;
    transform: translate(-50%, -50%);
    opacity: 0;
    transition: opacity 0.2s;
}

.card-media:hover .media-play,
.media-play.visible {
    opacity: 1;
}

/* Videos that can't be embedded open on YouTube */
.media-external {
    position: absolute;
    bottom: 0.75rem;
    left: 50%;
    padding: 0.25rem 0.75rem;
    border-radius: 0.4rem;
    background-color: rgba(0, 0, 0, 0.75);
    color: #fff;
    font-size: 0.85rem;
    font-weight: 600;
    white-space: nowrap;
    transform: translateX(-50%);
}

/* Text area */
.card-content {
    display: flex;
    flex-direction: column;
    flex-grow: 1;
    padding: 1.25rem 1.5rem 1rem;
}

.post-title {
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 1.15rem;
    font-weight: 700;
    line-height: 1.35;
    color: var(--text-color);
    text-decoration: none;
    transition: color 0.2s;
}

.post-title:hover {
    color: #4d8dff;
}

.read-post .post-title {
    opacity: 0.6;
}

.read-post .media-image {
    opacity: 0.75;
}

.post-source {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-top: 0.6rem;
    color: #8b919c;
    font-size: 0.95rem;
    min-width: 0;
}

.source-icon {
    width: 20px;
    height: 20px;
    border-radius: 50%;
}

.feed-link {
    cursor: pointer;
}

.feed-link:hover {
    color: #4d8dff;
    text-decoration: underline;
}

.post-folder {
    flex-shrink: 1;
    opacity: 0.8;
}

.post-excerpt {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin: 0.75rem 0 0;
    color: #8b919c;
    font-size: 0.92rem;
    line-height: 1.45;
}

.post-excerpt.no-media {
    -webkit-line-clamp: 5;
}

.card-footer-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: auto;
    padding-top: 1.25rem;
    color: #8b919c;
}

.post-time {
    font-size: 0.95rem;
}

.footer-action {
    display: flex;
    color: inherit;
    transition: color 0.2s;
}

.footer-action:hover {
    color: var(--text-color);
}

.footer-action.logs-action,
.footer-action.logs-action:hover {
    color: #e0a800;
}

.save-button {
    padding: 0;
    border: 0;
    background: transparent;
}

.save-button.saved {
    color: #4d8dff;
}

.read-indicator {
    width: 18px;
    height: 18px;
    padding: 0;
    border: 2px solid currentColor;
    border-radius: 50%;
    background: transparent;
}

.read-indicator.unread {
    background: radial-gradient(circle, #4d8dff 0 4px, transparent 5px);
    border-color: #4d8dff;
}

.custom-card.selected-post {
    border-color: #0d6efd !important;
    box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.4);
}

.video-embed {
    border-radius: 0.375rem;
    overflow: hidden;
    cursor: auto;
}

.video-embed iframe {
    border: 0;
}

.video-embed-summary {
    max-width: 640px;
}

.site-icon {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    border-radius: 3px;
}

/* List mode rows */
.list-row {
    border-bottom: 1px solid var(--border-color);
    cursor: pointer;
    transition: background-color 0.15s;
}

[data-bs-theme="dark"] .list-row {
    border-bottom-color: #2a2e36;
}

.list-row:hover {
    background-color: rgba(127, 127, 127, 0.08);
}

/* The opened post reads like a page: no highlight background or side line */
.list-row.selected-post,
.list-row.selected-post:hover {
    background-color: transparent;
}

.list-line {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 0.6rem 0.75rem;
    min-width: 0;
}

.read-dot {
    width: 9px;
    height: 9px;
    flex-shrink: 0;
    padding: 0;
    border: 0;
    border-radius: 50%;
    background: transparent;
    /* Bigger click target than the dot itself */
    box-sizing: content-box;
    background-clip: content-box;
    margin: -6px;
    padding: 6px;
}

.read-dot:hover {
    box-shadow: inset 0 0 0 7px rgba(127, 127, 127, 0.25);
}

.read-dot.unread {
    background-color: #4d8dff;
}

.list-source {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    width: 200px;
    flex-shrink: 0;
    color: #8b919c;
    min-width: 0;
}

.list-source-icon {
    border-radius: 50%;
}

.list-main {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
    flex: 1;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
}

.list-video-badge {
    display: inline-flex;
    align-self: center;
    flex-shrink: 0;
    color: var(--text-color);
}

.list-title {
    flex-shrink: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    font-weight: 700;
    color: var(--text-color);
    text-decoration: none;
}

.list-title:hover {
    color: #4d8dff;
}

.read-post .list-title {
    font-weight: 500;
    opacity: 0.6;
}

.list-excerpt {
    flex: 1 1 0;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    color: #8b919c;
}

.list-save {
    display: flex;
    flex-shrink: 0;
    color: #8b919c;
    opacity: 0;
    transition: opacity 0.15s, color 0.2s;
}

/* In list mode the button only shows on hover or selection, unless the post is saved */
.list-row:hover .list-save,
.list-row.selected-post .list-save,
.list-save.saved {
    opacity: 1;
}

.list-save:hover {
    color: var(--text-color);
}

.list-save.saved {
    color: #4d8dff;
}

.list-time {
    flex-shrink: 0;
    color: #8b919c;
    font-size: 0.95rem;
}

.list-details {
    max-width: 860px;
    margin: 0 auto;
    padding: 2.5rem 1.5rem 3.5rem;
    cursor: auto;
}

.details-header {
    margin-bottom: 2rem;
}

.details-title {
    display: block;
    font-size: 2.6rem;
    font-weight: 800;
    line-height: 1.2;
    letter-spacing: -0.01em;
    color: var(--text-color);
    text-decoration: none;
}

.title-live-badge {
    margin-bottom: 0.75rem;
    font-size: 0.85rem;
}

.details-title:hover {
    color: #4d8dff;
}

.details-meta {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-top: 1.25rem;
    color: #8b919c;
    font-size: 1.05rem;
    min-width: 0;
}

.details-actions {
    display: flex;
    align-items: center;
    gap: 1.25rem;
    margin-top: 1.5rem;
}

.details-action {
    display: flex;
    padding: 0.25rem;
    border: 0;
    background: transparent;
    color: #8b919c;
    transition: color 0.2s;
}

.details-action:hover {
    color: var(--text-color);
}

.details-action.saved {
    color: #4d8dff;
}

.details-action.logs-action,
.details-action.logs-action:hover {
    color: #e0a800;
}

.details-icon {
    width: 20px;
    height: 20px;
    flex-shrink: 0;
    border-radius: 50%;
}

@media (max-width: 768px) {
    .list-source {
        width: auto;
    }

    .list-source span {
        display: none;
    }

    .list-excerpt {
        display: none;
    }

    .list-details {
        padding: 1.5rem 0.75rem 2.5rem;
    }

    .details-title {
        font-size: 1.9rem;
    }
}

.summary-description {
    font-weight: normal;
    word-break: break-word;
    font-size: 1.2rem;
    line-height: 1.65;
    overflow-x: hidden;
}

/* Keep media in the expanded description small enough to fit the row */
.summary-description :deep(img),
.summary-description :deep(video),
.summary-description :deep(iframe) {
    display: block;
    max-width: 100% !important;
    max-height: 500px !important;
    width: auto !important;
    height: auto !important;
    object-fit: contain;
    border-radius: 0.25rem;
    margin: 1rem 0;
}

.content-loading {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0;
    color: #8b919c;
    font-size: 1.1rem;
}

/* Full article content: readable line length and paragraph spacing */
.post-content {
    font-family: Georgia, "Iowan Old Style", "Times New Roman", serif;
    font-size: 1.35rem;
    line-height: 1.65;
}

.post-content :deep(p) {
    margin: 0 0 1em;
}

.post-content :deep(h1),
.post-content :deep(h2),
.post-content :deep(h3),
.post-content :deep(h4) {
    margin: 1.4em 0 0.6em;
    font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
    font-size: 1.6rem;
    font-weight: 700;
}

.post-content :deep(ul),
.post-content :deep(ol) {
    margin: 0 0 1em;
    padding-left: 1.5em;
}

.post-content :deep(blockquote) {
    margin: 0 0 1em;
    color: #8b919c;
    font-style: italic;
}

.post-content :deep(figure) {
    margin: 0 0 1em;
}

.post-content :deep(a) {
    color: #4d8dff;
}

.post-content :deep(figcaption) {
    font-size: 0.95rem;
    color: #8b919c;
}

.post-content :deep(img) {
    max-height: 500px !important;
}

/* Ensure images in post descriptions fit the card width if details are shown */
:deep(.post-img) {
    max-width: 100% !important;
    width: 100% !important;
    height: auto !important;
}
</style>
