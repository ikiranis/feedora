<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { language } from '@/functions/languageStore';
import { authStore } from '@/functions/authStore';
import { getAppVersion } from '@/api/general';

/** One keyboard shortcut, as a key label paired with the action it performs. */
interface ShortcutRow {
    /** The key or key combination, already formatted for display (e.g. "← / →"). */
    keys: string;
    /** Translation key of the action the shortcut performs. */
    action: string;
}

/** One entry of a labelled list: a short lead-in term and the explanation that follows it. */
interface HelpEntry {
    /** Translation key of the term the entry describes (a button, a filter, a page). */
    term: string;
    /** Translation key of the explanation. */
    text: string;
}

/**
 * A single piece of content inside a help section. Sections are described as data rather than markup,
 * so the table of contents and the search filter are both derived from the same source.
 */
type HelpBlock =
    | { kind: 'text'; text: string }
    | { kind: 'entries'; entries: HelpEntry[] }
    | { kind: 'keys'; rows: ShortcutRow[] }
    | { kind: 'tip'; text: string };

/** One help section: a card in the content column and a row in the table of contents. */
interface HelpSection {
    /** Anchor id, used by the table of contents to scroll to the section. */
    id: string;
    /** Translation key of the section heading. */
    title: string;
    /** Translation key of the one-line summary shown under the heading. */
    summary: string;
    /** Key into {@link ICONS} for the glyph shown beside the heading. */
    icon: string;
    /** The section's content, in the order it is rendered. */
    blocks: HelpBlock[];
}

/**
 * The inner markup of each section glyph, drawn inside a 16x16 viewBox. Every value is a literal in
 * this file, so rendering them with v-html introduces no untrusted markup.
 */
const ICONS: Record<string, string> = {
    start: '<path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16"/>'
        + '<path d="M6.271 5.055a.5.5 0 0 1 .52.038l3.5 2.5a.5.5 0 0 1 0 .814l-3.5 2.5A.5.5 0 0 1 6 '
        + '10.5v-5a.5.5 0 0 1 .271-.445"/>',
    posts: '<path d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5L14 4.5zm-3-.5a.5.5 0 0 '
        + '1-.5-.5V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V5h-2.5A1.5 1.5 0 0 1 11 4z"/>'
        + '<path d="M5 7h6v1H5V7zm0 2h6v1H5V9zm0 2h4v1H5v-1z"/>',
    post: '<path d="M0 3a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2zm2-1a1 1 0 0 '
        + '0-1 1v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1z"/>'
        + '<path d="M3 4.5a.5.5 0 0 1 .5-.5h9a.5.5 0 0 1 0 1h-9a.5.5 0 0 1-.5-.5m0 3a.5.5 0 0 1 '
        + '.5-.5h9a.5.5 0 0 1 0 1h-9a.5.5 0 0 1-.5-.5m0 3a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5"/>',
    keyboard: '<path d="M14 5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1zM2 4a2 2 0 0 '
        + '0-2 2v5a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z"/>'
        + '<path d="M13 10.25a.25.25 0 0 1 .25-.25h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5a.25.25 '
        + '0 0 1-.25-.25zm0-2a.25.25 0 0 1 .25-.25h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5a.25.25 '
        + '0 0 1-.25-.25zm-5 0A.25.25 0 0 1 8.25 8h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5A.25.25 0 '
        + '0 1 8 8.75zm2 0a.25.25 0 0 1 .25-.25h1.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-1.5a.25.25 0 '
        + '0 1-.25-.25zm1 2a.25.25 0 0 1 .25-.25h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5a.25.25 0 '
        + '0 1-.25-.25zm-5-2A.25.25 0 0 1 6.25 8h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5A.25.25 0 '
        + '0 1 6 8.75zm-2 0A.25.25 0 0 1 4.25 8h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5A.25.25 0 0 '
        + '1 4 8.75zm-2 0A.25.25 0 0 1 2.25 8h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5A.25.25 0 0 1 '
        + '2 8.75zm11-2a.25.25 0 0 1 .25-.25h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5a.25.25 0 0 '
        + '1-.25-.25zm-2 0a.25.25 0 0 1 .25-.25h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5a.25.25 0 0 '
        + '1-.25-.25zm-2 0A.25.25 0 0 1 9.25 6h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5A.25.25 0 0 '
        + '1 9 6.75zm-2 0A.25.25 0 0 1 7.25 6h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5A.25.25 0 0 '
        + '1 7 6.75zm-2 0A.25.25 0 0 1 5.25 6h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5A.25.25 0 0 '
        + '1 5 6.75zm-3 0A.25.25 0 0 1 2.25 6h1.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-1.5A.25.25 0 '
        + '0 1 2 6.75zm0 4a.25.25 0 0 1 .25-.25h.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-.5a.25.25 0 '
        + '0 1-.25-.25zm2 0a.25.25 0 0 1 .25-.25h5.5a.25.25 0 0 1 .25.25v.5a.25.25 0 0 1-.25.25h-5.5a.25.25 '
        + '0 0 1-.25-.25z"/>',
    feeds: '<path d="M14 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1zM2 0a2 2 0 0 '
        + '0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2z"/>'
        + '<path d="M5.5 12a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0m-3-8.5a1 1 0 0 1 1-1c5.523 0 10 4.477 10 10a1 '
        + '1 0 1 1-2 0 8 8 0 0 0-8-8 1 1 0 0 1-1-1m0 4a1 1 0 0 1 1-1 6 6 0 0 1 6 6 1 1 0 1 1-2 0 4 4 0 0 '
        + '0-4-4 1 1 0 0 1-1-1"/>',
    updates: '<path fill-rule="evenodd" d="M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2z"/>'
        + '<path d="M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 '
        + '1 8 4.466"/>',
    live: '<path d="M3.05 3.05a7 7 0 0 0 0 9.9.5.5 0 0 1-.707.707 8 8 0 0 1 0-11.314.5.5 0 0 1 .707.707m2.122 '
        + '2.122a4 4 0 0 0 0 5.656.5.5 0 1 1-.708.708 5 5 0 0 1 0-7.072.5.5 0 0 1 .708.708m5.656-.708a.5.5 0 0 1 '
        + '.708 0 5 5 0 0 1 0 7.072.5.5 0 1 1-.708-.708 4 4 0 0 0 0-5.656.5.5 0 0 1 0-.708m2.122-2.12a.5.5 0 0 1 '
        + '.707 0 8 8 0 0 1 0 11.313.5.5 0 0 1-.707-.707 7 7 0 0 0 0-9.9.5.5 0 0 1 0-.707M10 8a2 2 0 1 1-4 0 2 2 '
        + '0 0 1 4 0"/>',
    parser: '<path d="M10.478 1.647a.5.5 0 1 0-.956-.294l-4 13a.5.5 0 0 0 .956.294zM4.854 4.146a.5.5 0 0 1 0 '
        + '.708L1.707 8l3.147 3.146a.5.5 0 0 1-.708.708l-3.5-3.5a.5.5 0 0 1 0-.708l3.5-3.5a.5.5 0 0 1 .708 '
        + '0m6.292 0a.5.5 0 0 0 0 .708L14.293 8l-3.147 3.146a.5.5 0 0 0 .708.708l3.5-3.5a.5.5 0 0 0 '
        + '0-.708l-3.5-3.5a.5.5 0 0 0-.708 0"/>',
    extension: '<path d="M1 0 0 1l2.2 3.081a1 1 0 0 0 .815.419h.07a1 1 0 0 1 .708.293l2.675 2.675-2.617 2.654A3.003 '
        + '3.003 0 0 0 0 13a3 3 0 1 0 5.878-.851l2.654-2.617.968.968-.305.914a1 1 0 0 0 .242 1.023l3.27 3.27a.997.997 '
        + '0 0 0 1.414 0l1.586-1.586a.997.997 0 0 0 0-1.414l-3.27-3.27a1 1 0 0 0-1.023-.242L10.5 9.5l-.96-.96 '
        + '2.68-2.643A3.005 3.005 0 0 0 16 3q0-.405-.102-.777l-2.14 2.141L12 4l-.364-1.757L13.777.102a3 3 0 0 '
        + '0-3.675 3.68L7.462 6.46 4.793 3.793a1 1 0 0 1-.293-.707v-.071a1 1 0 0 0-.419-.814zm9.646 10.646a.5.5 0 '
        + '0 1 .708 0l2.914 2.915a.5.5 0 0 1-.707.707l-2.915-2.914a.5.5 0 0 1 0-.708M3 11l.471.242.529.026.287.445'
        + '.445.287.026.529L5 13l-.242.471-.026.529-.445.287-.287.445-.529.026L3 15l-.471-.242L2 14.732l-.287-.445'
        + 'L1.268 14l-.026-.529L1 13l.242-.471.026-.529.445-.287.287-.445.529-.026z"/>',
    settings: '<path fill-rule="evenodd" d="M11.5 2a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3M9.05 3a2.5 2.5 0 0 1 4.9 '
        + '0H16v1h-2.05a2.5 2.5 0 0 1-4.9 0H0V3zM4.5 7a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3M2.05 8a2.5 2.5 0 0 1 '
        + '4.9 0H16v1H6.95a2.5 2.5 0 0 1-4.9 0H0V8zm9.45 4a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3m-2.45 1a2.5 2.5 0 '
        + '0 1 4.9 0H16v1h-2.05a2.5 2.5 0 0 1-4.9 0H0v-1z"/>',
    users: '<path d="M7 14s-1 0-1-1 1-4 5-4 5 3 5 4-1 1-1 1zm4-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6m-5.784 6A2.24 2.24 0 '
        + '0 1 5 13c0-1.355.68-2.75 1.936-3.72A6.3 6.3 0 0 0 5 9c-4 0-5 3-5 4s1 1 1 1zM4.5 8a2.5 2.5 0 1 0 '
        + '0-5 2.5 2.5 0 0 0 0 5"/>',
    logs: '<path d="M5 10.5a.5.5 0 0 1 .5-.5h2a.5.5 0 0 1 0 1h-2a.5.5 0 0 1-.5-.5zm0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 '
        + '0 1 0 1h-5a.5.5 0 0 1-.5-.5zm0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5zm0-2a.5.5 0 0 1 '
        + '.5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5z"/>'
        + '<path d="M3 0h10a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2v-1h1v1a1 1 0 0 0 1 1h10a1 1 0 0 0 '
        + '1-1V2a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1v1H1V2a2 2 0 0 1 2-2z"/>',
    tip: '<path d="M2 6a6 6 0 1 1 10.174 4.31c-.203.196-.359.4-.453.619l-.762 1.769A.5.5 0 0 1 10.5 '
        + '13h-5a.5.5 0 0 1-.46-.302l-.761-1.77a2 2 0 0 0-.453-.618A5.98 5.98 0 0 1 2 6m6-5a5 5 0 0 '
        + '0-3.479 8.592c.263.254.514.564.676.941L5.83 12h4.342l.632-1.467c.162-.377.413-.687.676-.941A5 5 0 0 0 '
        + '8 1"/><path d="M5 14.5a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1l-.224.447a1 1 0 0 1-.894.553H6.618a1 1 0 0 '
        + '1-.894-.553L5.5 15a.5.5 0 0 1-.5-.5"/>',
    search: '<path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001q.44.58.98 1.121l3.85 3.85a1 1 0 0 0 '
        + '1.415-1.414l-3.85-3.85a7 7 0 0 0-1.121-.98zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0"/>',
    support: '<path d="M.05 3.555A2 2 0 0 1 2 2h12a2 2 0 0 1 1.95 1.555L8 8.414zM0 4.697v7.104l5.803-3.558zM6.761 '
        + '8.83l-6.57 4.027A2 2 0 0 0 2 14h12a2 2 0 0 0 1.808-1.144l-6.57-4.027L8 9.586zm3.436-.586L16 '
        + '11.801V4.697z"/>'
};

/** Whether the logged-in user is an administrator; gates the admin-only help entries. */
const isAdmin = authStore.isAdmin;

/** The running application version, shown in the header. Left blank when it cannot be read. */
const version = ref('');

/** The current text typed into the help search box. */
const query = ref('');

/** Id of the section currently under the top of the viewport, highlighted in the contents. */
const activeId = ref('');

/** The rendered section elements, keyed by section id, so the scroll spy can observe them. */
const sectionEls = new Map<string, HTMLElement>();

/** The observer driving {@link activeId}; rebuilt whenever the rendered sections change. */
let observer: IntersectionObserver | null = null;

/** The shortcuts of the posts page, mirroring the list its own H modal shows. */
const POST_SHORTCUTS: ShortcutRow[] = [
    { keys: '← / →', action: 'Select the previous / next post' },
    { keys: '↑ / ↓', action: 'Select the post above / below, or scroll in list mode and in the opened post' },
    { keys: 'Enter / O', action: 'Open / close the selected post' },
    { keys: 'Esc', action: 'Close the opened post' },
    { keys: 'B', action: 'Open the link of the selected post in a background tab' },
    { keys: 'F', action: 'Save the selected post for later / unsave it' },
    { keys: 'S', action: 'Add the selected post to the favorites / remove it' },
    { keys: 'C', action: 'Copy the link of the selected post to the clipboard' },
    { keys: 'W', action: 'Show / hide the article of the link preview inside the selected post' },
    { keys: 'R', action: 'Show the new posts, when there are any' },
    { keys: 'A', action: 'Add a feed (on every page)' },
    { keys: 'H', action: 'Show / hide the keyboard shortcuts' }
];

/**
 * Every help section, in reading order. The admin-only sections are filtered out for users who cannot
 * reach the pages they describe.
 */
const sections = computed<HelpSection[]>(() => {
    const all: Array<HelpSection & { requiresAdmin?: boolean }> = [
        {
            id: 'getting-started',
            title: 'Getting started',
            summary: 'What Feedora does, and how to find your way around it.',
            icon: 'start',
            blocks: [
                {
                    kind: 'text',
                    text: 'Feedora follows your RSS and Atom feeds and YouTube channels, checks each one in the background, '
                        + 'and collects their new posts in one place for you to read. Every user has their own feeds, '
                        + 'folders and posts.'
                },
                {
                    kind: 'entries',
                    entries: [
                        {
                            term: '1. Add your feeds',
                            text: 'Press A on any page, or use Add Feed on the Feeds page, and paste the address of a feed, '
                                + 'a website or a YouTube channel. You can also import an OPML file from another reader.'
                        },
                        {
                            term: '2. Organise them in folders',
                            text: 'Choose a folder for each feed when you add it, and create new folders from the Posts page.'
                        },
                        {
                            term: '3. Read',
                            text: 'The Posts page shows the unread posts of all your feeds, newest first. Posts are marked '
                                + 'as read as you scroll past them.'
                        }
                    ]
                },
                {
                    kind: 'entries',
                    entries: [
                        {
                            term: 'Menu',
                            text: 'Every page is one click away from the menu on the left. On a narrow screen it opens '
                                + 'from the menu button at the top.'
                        },
                        {
                            term: 'Theme',
                            text: 'The sun and moon button switches between the dark and the light theme, and your choice '
                                + 'is remembered in this browser.'
                        },
                        {
                            term: 'Feedora logo',
                            text: 'Takes you to all the posts of all your folders.'
                        }
                    ]
                }
            ]
        },
        {
            id: 'posts',
            title: 'The Posts page',
            summary: 'Browsing, filtering and marking your posts.',
            icon: 'posts',
            blocks: [
                {
                    kind: 'text',
                    text: 'The left column of the Posts page lists All Posts, Saved, Favorites, your folders and their '
                        + 'feeds, each with its number of unread posts. Click one to show only its posts. Click the arrow '
                        + 'of a folder to show its feeds.'
                },
                {
                    kind: 'entries',
                    entries: [
                        {
                            term: 'Unread / All',
                            text: 'Show only the unread posts, or the read ones too.'
                        },
                        {
                            term: 'Mark all as read',
                            text: 'Marks every post of the current view as read. Its arrow marks only the posts older than '
                                + '1 day, 3 days, 1 week, 2 weeks or 1 month.'
                        },
                        {
                            term: 'Show Details / Show Summary',
                            text: 'Switches between cards with images and descriptions, and a compact list of titles.'
                        },
                        {
                            term: 'Feed colors',
                            text: 'In the cards view, gives each card a light background in the color of its feed.'
                        },
                        {
                            term: 'Hide YouTube Shorts',
                            text: 'Hides the YouTube Shorts of the selected folder. It is remembered for each folder.'
                        },
                        {
                            term: 'Parse RSS Feeds',
                            text: 'Checks the feeds of the current view for new posts right away, with a progress bar. '
                                + 'The time of the last check is shown under the search box.'
                        },
                        {
                            term: 'Search',
                            text: 'Filters the posts by their title or description.'
                        },
                        {
                            term: 'New posts badge',
                            text: 'Shows how many new posts arrived since the list was loaded. Click it, or press R, to '
                                + 'show them.'
                        },
                        {
                            term: 'Edit folders',
                            text: 'The pencil next to Folders lets you rename or delete folders. The eye button shows or '
                                + 'hides the empty folders. When a folder is deleted, its feeds move to the default folder.'
                        },
                        {
                            term: 'Feed menu',
                            text: 'When a feed is selected, its name at the top opens a menu to edit the feed, see its '
                                + 'statistics or unfollow it.'
                        }
                    ]
                },
                {
                    kind: 'tip',
                    text: 'Read posts are deleted automatically after 30 days. Saved posts are never deleted, so save the '
                        + 'ones you want to keep.'
                }
            ]
        },
        {
            id: 'reading',
            title: 'Reading a post',
            summary: 'The full article, videos, and the actions of each post.',
            icon: 'post',
            blocks: [
                {
                    kind: 'text',
                    text: 'Click a post, or press Enter, to open it. Feedora fetches the full article from the original '
                        + 'page, cleans it up and shows it with its main image, so you rarely need to leave the app. The '
                        + 'next posts are loaded in advance, so they open instantly.'
                },
                {
                    kind: 'entries',
                    entries: [
                        {
                            term: 'Save for later',
                            text: 'Keeps the post in Saved, where it is never deleted.'
                        },
                        {
                            term: 'Favorites',
                            text: 'Marks the post with a star and lists it under Favorites.'
                        },
                        {
                            term: 'Mark as unread',
                            text: 'Brings a read post back to the unread ones.'
                        },
                        {
                            term: 'Copy link and share',
                            text: 'Copy the link of the post, or share it from the share menu.'
                        },
                        {
                            term: 'Open original',
                            text: 'Opens the post on its website, in a new tab.'
                        },
                        {
                            term: 'View logs',
                            text: 'Shows the problems Feedora had reading this post, if there were any.'
                        }
                    ]
                },
                {
                    kind: 'entries',
                    entries: [
                        {
                            term: 'YouTube',
                            text: 'Videos play inside Feedora and show their duration. Live streams and premieres get a '
                                + 'live badge that is kept up to date.'
                        },
                        {
                            term: 'Tweets',
                            text: 'Posts from X / Twitter are shown with their photos and link previews.'
                        },
                        {
                            term: 'Link previews',
                            text: 'A post that links to an article shows a preview of it. Press W to read that article '
                                + 'inside the post.'
                        },
                        {
                            term: 'On mobile',
                            text: 'Swipe left or right to move between posts, and pinch to zoom.'
                        }
                    ]
                }
            ]
        },
        {
            id: 'shortcuts',
            title: 'Keyboard shortcuts',
            summary: 'Read, save and move between posts without the mouse.',
            icon: 'keyboard',
            blocks: [
                { kind: 'keys', rows: POST_SHORTCUTS },
                {
                    kind: 'tip',
                    text: 'Press H on the Posts page to see this list at any time.'
                }
            ]
        },
        {
            id: 'feeds',
            title: 'Feeds and folders',
            summary: 'Adding, editing and importing your feeds.',
            icon: 'feeds',
            blocks: [
                {
                    kind: 'text',
                    text: 'The Feeds page lists all the feeds you follow, with a search box to find one by name.'
                },
                {
                    kind: 'entries',
                    entries: [
                        {
                            term: 'Add Feed',
                            text: 'Paste the address of a feed or of a website. Feedora finds the feeds of the website and '
                                + 'lets you choose one when there are many, reads the title of the feed, and lets you choose '
                                + 'its folder.'
                        },
                        {
                            term: 'YouTube channels and playlists',
                            text: 'Paste the normal address of a YouTube channel or playlist, and Feedora turns it into '
                                + 'its feed.'
                        },
                        {
                            term: 'Edit Feed',
                            text: 'Change the title, the folder and the card color of a feed. By default, the card color '
                                + 'is the main color of the feed\'s favicon.'
                        },
                        {
                            term: 'Statistics',
                            text: 'Shows how often the feed posts, how often it is checked, its last and next check, and '
                                + 'its failed checks. Feeds with problems are listed first.'
                        },
                        {
                            term: 'Unfollow feed',
                            text: 'Removes the feed and its posts from your account.'
                        },
                        {
                            term: 'Export OPML',
                            text: 'Downloads your feeds and folders as an OPML file, to back them up or move them to '
                                + 'another reader.'
                        },
                        {
                            term: 'Import OPML',
                            text: 'Adds the feeds and folders of an OPML file exported from another reader. The feeds are '
                                + 'read in the background.'
                        }
                    ]
                },
                {
                    kind: 'tip',
                    text: 'Importing an OPML file replaces everything you have: your existing feeds, folders and posts '
                        + 'are deleted first. Export an OPML file before you import one.'
                },
                {
                    kind: 'tip',
                    text: 'A feed with an http:// address whose checks keep failing is marked with a warning. Editing it '
                        + 'to use https:// usually fixes it.'
                }
            ]
        },
        {
            id: 'updates',
            title: 'How feeds are updated',
            summary: 'When Feedora checks your feeds for new posts.',
            icon: 'updates',
            blocks: [
                {
                    kind: 'text',
                    text: 'Feedora checks your feeds in the background, while it runs. Each feed is checked as often as it '
                        + 'actually posts, so busy feeds are fresh and quiet ones don\'t waste requests. Up to 8 feeds are '
                        + 'checked at the same time, and posts that already exist are skipped.'
                },
                {
                    kind: 'entries',
                    entries: [
                        { term: 'More than 30 posts per hour', text: 'Checked every minute.' },
                        { term: 'More than 10 posts per hour', text: 'Checked every 5 minutes.' },
                        { term: 'More than 1 post per hour', text: 'Checked every 10 minutes.' },
                        { term: 'More than 1 post per day', text: 'Checked every hour.' },
                        { term: 'Up to 1 post per day', text: 'Checked every 6 hours.' }
                    ]
                },
                {
                    kind: 'tip',
                    text: 'To check a feed or a folder right away, select it on the Posts page and click Parse RSS Feeds.'
                }
            ]
        },
        {
            id: 'live',
            title: 'Live Newsfeed',
            summary: 'A page that keeps itself up to date, for a second screen.',
            icon: 'live',
            blocks: [
                {
                    kind: 'text',
                    text: 'The Live Newsfeed shows your latest unread posts and checks for new ones on its own. New posts '
                        + 'appear at the top one at a time, 5 seconds apart, or 2 seconds apart when more than 50 are '
                        + 'waiting. The badge at the top shows how many are still waiting.'
                },
                {
                    kind: 'entries',
                    entries: [
                        {
                            term: 'Pause / Resume',
                            text: 'Stops new posts from appearing while you read, and lets them in again.'
                        },
                        {
                            term: 'Back to top',
                            text: 'New posts also wait while you are scrolled down. The arrow takes you back to the top '
                                + 'and lets them in.'
                        },
                        {
                            term: 'Offline',
                            text: 'Shown when the last check for new posts failed. Feedora keeps trying.'
                        }
                    ]
                }
            ]
        },
        {
            id: 'parser',
            title: 'Parser',
            summary: 'Make a feed for a website that doesn\'t have one.',
            icon: 'parser',
            blocks: [
                {
                    kind: 'text',
                    text: 'Some websites have no RSS feed. On the Parser page, enter the address of such a page and '
                        + 'Feedora reads it and suggests the elements of the page that look like its list of posts, with '
                        + 'how many posts each one has. Choose one, or write your own CSS selector, and set how often the '
                        + 'page is read.'
                },
                {
                    kind: 'text',
                    text: 'Feedora then reads the page on that schedule and makes an RSS feed of it. Click the RSS button '
                        + 'of the page to add that feed to your feeds, like any other feed.'
                },
                {
                    kind: 'entries',
                    entries: [
                        { term: 'Parse this page now', text: 'Reads the page right away and updates its feed.' },
                        { term: 'Edit this page', text: 'Change the title, the element of the posts or the frequency.' },
                        { term: 'Error', text: 'Shown when the last reading of the page failed. Hover it to see why.' }
                    ]
                }
            ]
        },
        {
            id: 'extension',
            title: 'Feedora Companion',
            summary: 'The optional browser extension.',
            icon: 'extension',
            blocks: [
                {
                    kind: 'text',
                    text: 'Feedora Companion is a browser extension for Chrome, Edge, Brave and Firefox, found in the '
                        + 'extension folder of Feedora\'s source code.'
                },
                {
                    kind: 'entries',
                    entries: [
                        {
                            term: 'Background tabs',
                            text: 'With the extension, B opens the selected post in a background tab, which web pages '
                                + 'can\'t do on their own.'
                        },
                        {
                            term: 'Save the current page',
                            text: 'Its toolbar button saves the page you are on to Feedora\'s Saved posts.'
                        },
                        {
                            term: 'Add the feed of the current page',
                            text: 'Its toolbar button also adds the feed of the page you are on to your feeds.'
                        }
                    ]
                },
                {
                    kind: 'tip',
                    text: 'The extension works by itself on localhost. If Feedora runs on another address, open Feedora, '
                        + 'click the extension\'s button and choose Enable the companion here.'
                }
            ]
        },
        {
            id: 'settings',
            title: 'Settings',
            summary: 'How Feedora looks, and how it works.',
            icon: 'settings',
            blocks: [
                {
                    kind: 'text',
                    text: 'The Display settings are kept in this browser, so each device can have its own.'
                },
                {
                    kind: 'entries',
                    entries: [
                        { term: 'Language', text: 'The language of the interface: English or Greek.' },
                        { term: 'Date format', text: 'Whether dates show the day or the month first.' }
                    ]
                }
            ]
        },
        {
            id: 'app-settings',
            title: 'App Settings',
            summary: 'The settings of the whole application.',
            icon: 'settings',
            requiresAdmin: true,
            blocks: [
                {
                    kind: 'text',
                    text: 'Only the admin sees these settings, under App Settings on the Settings page. They apply to '
                        + 'every user.'
                },
                {
                    kind: 'entries',
                    entries: [
                        {
                            term: 'registrationEnabled',
                            text: 'Whether new people can sign up by themselves. Turn it off to add the users yourself '
                                + 'from the Users page.'
                        },
                        {
                            term: 'youtubeApiKey',
                            text: 'An optional YouTube Data API key, for checking live streams and premieres more '
                                + 'reliably. Without it, Feedora reads the video pages.'
                        },
                        {
                            term: 'liveNewsfeedRefreshSeconds',
                            text: 'How often the Live Newsfeed checks for new posts.'
                        },
                        {
                            term: 'defaultActionsLanguage',
                            text: 'The default language of the messages.'
                        }
                    ]
                }
            ]
        },
        {
            id: 'users',
            title: 'Users',
            summary: 'Accounts, roles and statistics.',
            icon: 'users',
            blocks: [
                {
                    kind: 'text',
                    text: 'The first user who signs up becomes the admin. Every other user is a regular user, unless the '
                        + 'admin changes their role. On the Users page you can see how many folders, feeds, posts, saved '
                        + 'posts and favorites you have, and change your username, email or password.'
                },
                {
                    kind: 'text',
                    text: 'When you log in, check Remember me to stay signed in on this device for 30 days.'
                }
            ]
        },
        {
            id: 'users-admin',
            title: 'Managing users',
            summary: 'What the admin can do on the Users page.',
            icon: 'users',
            requiresAdmin: true,
            blocks: [
                {
                    kind: 'entries',
                    entries: [
                        { term: 'Add User', text: 'Creates an account, without the user having to sign up.' },
                        { term: 'Edit this user', text: 'Changes the username, the email or the password of a user.' },
                        { term: 'Role', text: 'Makes a user an admin, or a regular user again.' },
                        {
                            term: 'Delete this user',
                            text: 'Deletes the user with all of their folders, feeds, posts and logs.'
                        }
                    ]
                }
            ]
        },
        {
            id: 'logs',
            title: 'Logs',
            summary: 'What went wrong while reading your feeds.',
            icon: 'logs',
            blocks: [
                {
                    kind: 'text',
                    text: 'Feedora records the problems it has fetching your feeds, reading them and extracting their '
                        + 'articles. The Logs page lists them, filtered by level: errors, warnings or info. It is the first '
                        + 'place to look when a feed stops bringing new posts.'
                },
                {
                    kind: 'text',
                    text: 'The logs of a single feed or post can also be opened from that feed or post. Old log entries '
                        + 'are deleted automatically after 30 days, and Clear logs deletes them all.'
                }
            ]
        }
    ];

    return all.filter(section => !section.requiresAdmin || isAdmin.value);
});

/**
 * Collects every translated string a section renders, so the search box matches on the section's whole
 * content rather than only on its heading.
 *
 * @param section the section to flatten
 * @return the section's searchable text, lowercased
 */
const searchableText = (section: HelpSection): string => {
    const parts: string[] = [language.get(section.title), language.get(section.summary)];

    for (const block of section.blocks) {
        if (block.kind === 'text' || block.kind === 'tip') {
            parts.push(language.get(block.text));
        } else if (block.kind === 'entries') {
            for (const entry of block.entries) {
                parts.push(language.get(entry.term), language.get(entry.text));
            }
        } else {
            for (const row of block.rows) {
                parts.push(row.keys, language.get(row.action));
            }
        }
    }

    return parts.join(' ').toLowerCase();
};

/** The sections left after the search box, or all of them when nothing has been typed. */
const filteredSections = computed<HelpSection[]>(() => {
    const term = query.value.trim().toLowerCase();
    if (!term) return sections.value;

    return sections.value.filter(section => searchableText(section).includes(term));
});

/**
 * Stores (or forgets) a rendered section element so the scroll spy can observe it.
 *
 * @param id the section's anchor id
 * @param el the rendered element, or null as the section is unmounted
 */
const setSectionEl = (id: string, el: HTMLElement | null): void => {
    if (el) sectionEls.set(id, el);
    else sectionEls.delete(id);
};

/**
 * Rebuilds the scroll spy over the sections currently rendered. The observer watches a band near the top
 * of the viewport, so the heading the reader has just scrolled to is the one highlighted.
 */
const observeSections = (): void => {
    observer?.disconnect();
    if (typeof IntersectionObserver === 'undefined') return;

    observer = new IntersectionObserver(entries => {
        const visible = entries
            .filter(entry => entry.isIntersecting)
            .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);

        if (visible.length > 0) activeId.value = visible[0].target.id;
    }, { rootMargin: '-15% 0px -70% 0px', threshold: 0 });

    for (const el of sectionEls.values()) observer.observe(el);
};

/**
 * Scrolls a section into view from the table of contents and marks it active straight away, so the
 * highlight doesn't wait for the smooth scroll to land.
 *
 * @param id the section's anchor id
 */
const goToSection = (id: string): void => {
    activeId.value = id;
    sectionEls.get(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

/** Clears the search box and returns the page to its full contents. */
const clearSearch = (): void => {
    query.value = '';
};

// The rendered set changes with the search box, so the observer is rebuilt whenever it does; an observer
// still holding removed elements would never fire again.
watch(filteredSections, async () => {
    await nextTick();
    observeSections();
});

onMounted(async () => {
    version.value = (await getAppVersion()) ?? '';

    await nextTick();
    observeSections();
    activeId.value = filteredSections.value[0]?.id ?? '';
});

onBeforeUnmount(() => {
    observer?.disconnect();
    observer = null;
});
</script>

<template>
    <div class="help-scroll">
    <div class="help">
        <header class="help__hero">
            <div class="help__hero-glow" aria-hidden="true"></div>

            <img class="help__hero-logo" src="/feedora.svg?v=8" alt="" aria-hidden="true" />

            <div class="help__hero-text">
                <h1 class="help__hero-title">{{ language.get('Help') }}</h1>

                <p class="help__hero-lead">
                    {{ language.get('Everything Feedora can do, in one place: from adding your first feed to the keyboard shortcuts that make reading quick.') }}
                </p>

                <div class="help__hero-meta">
                    <span v-if="version" class="help__badge">Feedora {{ version }}</span>
                    <span class="help__badge help__badge--muted">
                        {{ filteredSections.length }} {{ language.get('topics') }}
                    </span>
                </div>
            </div>
        </header>

        <div class="help__search">
            <svg class="help__search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"
                 fill="currentColor" aria-hidden="true" v-html="ICONS.search"></svg>

            <input
                v-model="query"
                type="search"
                class="help__search-input"
                :placeholder="language.get('Search the help')"
                :aria-label="language.get('Search the help')"
            />

            <button v-if="query" class="help__search-clear" type="button" :title="language.get('Clear')"
                    :aria-label="language.get('Clear')" @click="clearSearch">
                &times;
            </button>
        </div>

        <div class="help__layout">
            <nav class="help__toc" :aria-label="language.get('Contents')">
                <div class="help__toc-inner">
                    <div class="help__toc-title">{{ language.get('Contents') }}</div>

                    <button
                        v-for="section in filteredSections"
                        :key="section.id"
                        type="button"
                        class="help__toc-link"
                        :class="{ 'help__toc-link--active': section.id === activeId }"
                        @click="goToSection(section.id)"
                    >
                        <svg class="help__toc-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"
                             fill="currentColor" aria-hidden="true" v-html="ICONS[section.icon]"></svg>
                        <span>{{ language.get(section.title) }}</span>
                    </button>
                </div>
            </nav>

            <div class="help__content">
                <p v-if="filteredSections.length === 0" class="help__empty">
                    {{ language.get('No help topic matches your search.') }}
                </p>

                <section
                    v-for="section in filteredSections"
                    :key="section.id"
                    :id="section.id"
                    :ref="el => setSectionEl(section.id, el as unknown as HTMLElement | null)"
                    class="help__section"
                >
                    <header class="help__section-head">
                        <span class="help__section-icon" aria-hidden="true">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor"
                                 v-html="ICONS[section.icon]"></svg>
                        </span>

                        <div>
                            <h2 class="help__section-title">{{ language.get(section.title) }}</h2>
                            <p class="help__section-summary">{{ language.get(section.summary) }}</p>
                        </div>
                    </header>

                    <template v-for="(block, index) in section.blocks">
                        <p v-if="block.kind === 'text'" :key="`t${index}`" class="help__text">
                            {{ language.get(block.text) }}
                        </p>

                        <dl v-else-if="block.kind === 'entries'" :key="`e${index}`" class="help__entries">
                            <div v-for="entry in block.entries" :key="entry.term" class="help__entry">
                                <dt class="help__entry-term">{{ language.get(entry.term) || entry.term }}</dt>
                                <dd class="help__entry-text">{{ language.get(entry.text) }}</dd>
                            </div>
                        </dl>

                        <div v-else-if="block.kind === 'keys'" :key="`k${index}`" class="help__keys">
                            <div v-for="row in block.rows" :key="row.keys" class="help__key-row">
                                <kbd class="help__key">{{ row.keys }}</kbd>
                                <span class="help__key-action" :title="language.get(row.action)">
                                    {{ language.get(row.action) }}
                                </span>
                            </div>
                        </div>

                        <aside v-else :key="`p${index}`" class="help__tip">
                            <svg class="help__tip-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"
                                 fill="currentColor" aria-hidden="true" v-html="ICONS.tip"></svg>
                            <span>{{ language.get(block.text) }}</span>
                        </aside>
                    </template>
                </section>

                <section v-if="filteredSections.length > 0" class="help__section help__section--support">
                    <header class="help__section-head">
                        <span class="help__section-icon" aria-hidden="true">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor"
                                 v-html="ICONS.support"></svg>
                        </span>

                        <div>
                            <h2 class="help__section-title">{{ language.get('Still stuck?') }}</h2>
                            <p class="help__section-summary">
                                {{ language.get('Something missing, broken, or simply not where you expected it?') }}
                            </p>
                        </div>
                    </header>

                    <p class="help__text">
                        {{ language.get('The Support page has the mail address to write to, the issue tracker to report a bug on, and a way to support the development of Feedora.') }}
                    </p>

                    <router-link class="help__support-link" :to="{ name: 'Support' }">
                        {{ language.get('Go to Support') }}
                    </router-link>
                </section>
            </div>
        </div>
    </div>
    </div>
</template>

<style scoped lang="scss">
// On desktop the page scrolls inside its own view, under the collapse and theme buttons, so they and the
// sidebar stay in place. On mobile the sidebar is a sticky top bar, so the whole page scrolls as usual.
@media (min-width: 992px) {
    .help-scroll {
        height: calc(100vh - 50px); /* Viewport minus the collapse and theme buttons */
        overflow-y: auto;
    }
}

// The page's one accent, taken from Bootstrap's primary colour so it follows the active theme.
.help {
    --help-accent: var(--bs-primary);
    --help-accent-soft: rgba(var(--bs-primary-rgb), 0.12);
    --help-accent-line: rgba(var(--bs-primary-rgb), 0.35);

    max-width: 1180px;
    width: 100%;
    margin: 0 auto;
    padding: 1.5rem 1rem 4rem;
}

.help__hero {
    position: relative;
    overflow: hidden;
    display: flex;
    align-items: center;
    gap: 1.5rem;
    padding: 1.75rem;
    border: 1px solid var(--bs-border-color);
    border-radius: 18px;
    background: linear-gradient(135deg, var(--help-accent-soft), transparent 65%), var(--bs-body-bg);
}

// A soft wash behind the logo, keeping the header from reading as a flat panel.
.help__hero-glow {
    position: absolute;
    top: -60%;
    right: -10%;
    width: 320px;
    height: 320px;
    border-radius: 50%;
    background: radial-gradient(circle, var(--help-accent-soft), transparent 70%);
    pointer-events: none;
}

.help__hero-logo {
    position: relative;
    width: 76px;
    height: 76px;
    flex-shrink: 0;
    filter: drop-shadow(0 6px 14px rgba(0, 0, 0, 0.18));
}

.help__hero-text {
    position: relative;
    min-width: 0;
}

.help__hero-title {
    margin: 0 0 0.35rem;
    font-size: 1.9rem;
    font-weight: 700;
    line-height: 1.1;
}

.help__hero-lead {
    margin: 0;
    max-width: 62ch;
    color: var(--bs-secondary-color);
}

.help__hero-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-top: 0.9rem;
}

.help__badge {
    padding: 0.2rem 0.65rem;
    border-radius: 999px;
    background-color: var(--help-accent-soft);
    border: 1px solid var(--help-accent-line);
    color: var(--bs-emphasis-color);
    font-size: 0.78rem;
    font-weight: 600;

    &--muted {
        background-color: var(--bs-tertiary-bg);
        border-color: var(--bs-border-color);
        color: var(--bs-secondary-color);
    }
}

.help__search {
    position: relative;
    display: flex;
    align-items: center;
    margin: 1.25rem 0 1.5rem;
}

.help__search-icon {
    position: absolute;
    left: 0.9rem;
    width: 16px;
    height: 16px;
    color: var(--bs-secondary-color);
    pointer-events: none;
}

.help__search-input {
    width: 100%;
    padding: 0.65rem 2.4rem;
    border: 1px solid var(--bs-border-color);
    border-radius: 12px;
    background-color: var(--bs-body-bg);
    color: var(--bs-body-color);

    &:focus {
        outline: none;
        border-color: var(--help-accent-line);
        box-shadow: 0 0 0 0.2rem var(--help-accent-soft);
    }
}

.help__search-clear {
    position: absolute;
    right: 0.6rem;
    width: 26px;
    height: 26px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    border-radius: 50%;
    background: transparent;
    color: var(--bs-secondary-color);
    font-size: 1.2rem;
    line-height: 1;

    &:hover {
        background-color: var(--bs-tertiary-bg);
        color: var(--bs-body-color);
    }
}

.help__layout {
    display: grid;
    grid-template-columns: 232px minmax(0, 1fr);
    gap: 2rem;
    align-items: start;
}

.help__toc {
    position: sticky;
    top: 0;
}

.help__toc-inner {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
    padding-bottom: 1rem;
}

.help__toc-title {
    padding: 0 0.65rem 0.5rem;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.09em;
    text-transform: uppercase;
    color: var(--bs-secondary-color);
}

.help__toc-link {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    width: 100%;
    padding: 0.45rem 0.65rem;
    border: none;
    border-left: 2px solid transparent;
    border-radius: 0 8px 8px 0;
    background: transparent;
    color: var(--bs-secondary-color);
    font-size: 0.9rem;
    text-align: left;
    transition: background-color 0.15s ease, color 0.15s ease;

    &:hover {
        background-color: var(--bs-tertiary-bg);
        color: var(--bs-body-color);
    }

    &--active {
        border-left-color: var(--help-accent);
        background-color: var(--help-accent-soft);
        color: var(--bs-emphasis-color);
        font-weight: 600;
    }
}

.help__toc-icon {
    width: 15px;
    height: 15px;
    flex-shrink: 0;
    opacity: 0.85;
}

.help__content {
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
    min-width: 0;
}

.help__empty {
    padding: 2.5rem 1rem;
    text-align: center;
    color: var(--bs-secondary-color);
}

.help__section {
    // Scrolled to from the contents; the offset keeps the heading clear of the top of the scroller.
    scroll-margin-top: 1rem;
    padding: 1.5rem;
    border: 1px solid var(--bs-border-color);
    border-radius: 16px;
    background-color: var(--bs-body-bg);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);

    &--support {
        background: linear-gradient(135deg, var(--help-accent-soft), transparent 60%), var(--bs-body-bg);
    }
}

.help__section-head {
    display: flex;
    align-items: flex-start;
    gap: 0.9rem;
    margin-bottom: 1.1rem;
}

.help__section-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    flex-shrink: 0;
    border-radius: 11px;
    background-color: var(--help-accent-soft);
    color: var(--help-accent);

    svg {
        width: 20px;
        height: 20px;
    }
}

.help__section-title {
    margin: 0;
    font-size: 1.2rem;
    font-weight: 700;
}

.help__section-summary {
    margin: 0.15rem 0 0;
    font-size: 0.9rem;
    color: var(--bs-secondary-color);
}

.help__text {
    margin: 0 0 1rem;
    max-width: 78ch;

    &:last-child {
        margin-bottom: 0;
    }
}

.help__entries {
    margin: 0 0 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.7rem;

    &:last-child {
        margin-bottom: 0;
    }
}

.help__entry {
    padding-left: 0.9rem;
    border-left: 2px solid var(--bs-border-color);
    max-width: 78ch;

    &:hover {
        border-left-color: var(--help-accent-line);
    }
}

.help__entry-term {
    font-weight: 600;
    color: var(--bs-emphasis-color);
}

.help__entry-text {
    margin: 0.1rem 0 0;
    color: var(--bs-secondary-color);
}

.help__keys {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 0.4rem 1.25rem;
    margin: 0 0 1rem;
    padding: 0.9rem 1rem;
    border-radius: 12px;
    background-color: var(--bs-tertiary-bg);
}

.help__key-row {
    display: flex;
    align-items: center;
    gap: 0.7rem;
    min-width: 0;
}

.help__key {
    flex-shrink: 0;
    min-width: 72px;
    padding: 0.2rem 0.5rem;
    border: 1px solid var(--bs-border-color);
    border-bottom-width: 2px;
    border-radius: 6px;
    background-color: var(--bs-body-bg);
    color: var(--bs-emphasis-color);
    font-size: 0.78rem;
    font-weight: 600;
    text-align: center;
    white-space: nowrap;
}

.help__key-action {
    font-size: 0.9rem;
    color: var(--bs-secondary-color);
}

.help__tip {
    display: flex;
    align-items: flex-start;
    gap: 0.7rem;
    padding: 0.85rem 1rem;
    border-left: 3px solid var(--help-accent);
    border-radius: 0 10px 10px 0;
    background-color: var(--help-accent-soft);
    font-size: 0.92rem;

    &:not(:last-child) {
        margin-bottom: 1rem;
    }
}

.help__tip-icon {
    width: 16px;
    height: 16px;
    margin-top: 0.18rem;
    flex-shrink: 0;
    color: var(--help-accent);
}

.help__support-link {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.5rem 1.1rem;
    border-radius: 999px;
    background-color: var(--help-accent);
    color: #fff;
    font-weight: 600;
    text-decoration: none;
    transition: filter 0.15s ease;

    &:hover {
        filter: brightness(1.08);
        color: #fff;
    }
}

// Below the two-column breakpoint the contents become a horizontally scrolling strip of chips above
// the sections, rather than a sidebar squeezed into a column too narrow to read.
@media (max-width: 991px) {
    .help__layout {
        grid-template-columns: minmax(0, 1fr);
        gap: 1rem;
    }

    .help__toc {
        position: static;
    }

    .help__toc-inner {
        flex-direction: row;
        flex-wrap: nowrap;
        gap: 0.4rem;
        overflow-x: auto;
        padding-bottom: 0.5rem;
        -webkit-overflow-scrolling: touch;
    }

    .help__toc-title {
        display: none;
    }

    .help__toc-link {
        width: auto;
        flex-shrink: 0;
        border-left: none;
        border-radius: 999px;
        border: 1px solid var(--bs-border-color);
        white-space: nowrap;

        &--active {
            border-color: var(--help-accent-line);
        }
    }
}

@media (max-width: 575px) {
    .help {
        padding: 1rem 0.75rem 3rem;
    }

    .help__hero {
        flex-direction: column;
        align-items: flex-start;
        gap: 1rem;
        padding: 1.25rem;
    }

    .help__hero-logo {
        width: 56px;
        height: 56px;
    }

    .help__hero-title {
        font-size: 1.5rem;
    }

    .help__section {
        padding: 1.15rem;
    }

    .help__keys {
        grid-template-columns: minmax(0, 1fr);
    }
}
</style>
