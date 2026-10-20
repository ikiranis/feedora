import { PostType } from '@/types';

/**
 * Helpers for posts from Twitter/X: tweets have no real title, and the link in their text is shown as a preview
 * (the page's title and image, found by the backend when the feed was parsed, see LinkPreviewExtractor).
 */

export interface LinkPreview {
    url: string;
    title: string | null;
    image: string | null;
    // Site of the link, null when it's only known by its short link (t.co)
    host: string | null;
}

// Same rule as the backend (LinkPreviewExtractor.URL)
const URL_PATTERN = /https?:\/\/[^\s<>"]+/g;

const hostname = (url: string) => {
    try {
        return new URL(url).hostname.toLowerCase();
    } catch {
        return null;
    }
};

const isTwitterHost = (host: string) => /(^|\.)(twitter|x)\.com$/.test(host);

const isPreviewable = (url: string) => {
    const host = hostname(url);
    return !!host && !isTwitterHost(host);
};

/**
 * Whether a post link points to Twitter/X.
 */
export const isTweetLink = (link: string) => {
    const host = hostname(link);
    return !!host && isTwitterHost(host);
};

/**
 * Image of Twitter's link card, which the feed gives as the tweet's image.
 */
export const isCardImage = (url: string | null | undefined) => !!url && /\/\/pbs\.twimg\.com\/card_img\//.test(url);

/**
 * Twitter's link card in a description: a link whose text is the page title, e.g. <a href="https://t.co/…">Title - Site</a>.
 */
const findCardAnchor = (body: HTMLElement) =>
    Array.from(body.querySelectorAll<HTMLAnchorElement>('a[href]')).find(anchor => {
        const text = anchor.textContent?.trim() || '';
        return isPreviewable(anchor.getAttribute('href') || '') && text !== '' && !/^https?:\/\//.test(text);
    });

/**
 * First link in a text that isn't to Twitter/X, as written in the text.
 */
const findTextLink = (text: string) => text.match(URL_PATTERN)?.find(isPreviewable);

const parse = (html: string) => new DOMParser().parseFromString(html, 'text/html').body;

/**
 * Preview of a tweet's link: the one found by the backend, otherwise Twitter's link card in the description
 * (for tweets added before previews existed). The card's image is the tweet's thumbnail.
 */
export const tweetLinkPreview = (post: PostType): LinkPreview | null => {
    if (!isTweetLink(post.link)) return null;

    const card = post.description ? findCardAnchor(parse(post.description)) : undefined;
    const url = post.linkUrl || card?.getAttribute('href');
    if (!url) return null;

    const host = hostname(url)?.replace(/^www\./, '') ?? null;
    return {
        url,
        title: post.linkTitle || card?.textContent?.trim() || null,
        image: post.linkImage || (isCardImage(post.thumbnail) ? post.thumbnail : null),
        host: host === 't.co' ? null : host,
    };
};

/**
 * Removes an element, and its parent if that's left empty (e.g. the paragraph of a link card).
 */
const removeWithEmptyParent = (element: Element, body: HTMLElement) => {
    const parent = element.parentElement;
    element.remove();
    if (parent && parent !== body && !parent.textContent?.trim() && !parent.querySelector('img, video, iframe')) parent.remove();
};

/**
 * Image URL without its size/format variant, e.g. pbs.twimg.com/media/X.jpg and pbs.twimg.com/media/X?format=jpg&name=small.
 */
const imageKey = (url: string) => {
    try {
        const { host, pathname } = new URL(url);
        return host + pathname.replace(/\.\w+$/, '');
    } catch {
        return url;
    }
};

/**
 * Whether two URLs are the same image, in any size or format.
 */
export const sameImage = (a: string, b: string) => imageKey(a) === imageKey(b);

/**
 * Removes the link a preview shows from a tweet's HTML: Twitter's link card (with its paragraph, if left empty)
 * and the first link in the text, which the card repeats (e.g. "Title https://reut.rs/…" followed by the card).
 * Same rule as the backend (LinkPreviewExtractor.findLink).
 * When the preview shows an image, the same image in the HTML (or Twitter's card image) is removed too.
 */
export const removePreviewedLink = (html: string, previewImage?: string | null) => {
    const body = parse(html);

    const card = findCardAnchor(body);
    if (card) removeWithEmptyParent(card, body);

    let imageRemoved = false;
    if (previewImage) {
        body.querySelectorAll<HTMLImageElement>('img[src]').forEach(img => {
            const src = img.getAttribute('src')!;
            if (isCardImage(src) || sameImage(src, previewImage)) {
                // An image wrapped in its link goes with the link
                const link = img.parentElement instanceof HTMLAnchorElement && !img.parentElement.textContent?.trim() ? img.parentElement : img;
                removeWithEmptyParent(link, body);
                imageRemoved = true;
            }
        });
    }

    return removeTextLink(body) || card || imageRemoved ? body.innerHTML : html;
};

/**
 * Removes the first link in the text nodes of an element, in document order.
 *
 * @return Whether a link was removed
 */
const removeTextLink = (node: Node): boolean => {
    if (node.nodeType === Node.TEXT_NODE) {
        const link = findTextLink(node.textContent || '');
        if (link) node.textContent = node.textContent!.replace(link, '');
        return !!link;
    }
    // Links written as anchors (<a href="…">https://…</a>) go with their anchor
    if (node instanceof HTMLAnchorElement && findTextLink(node.textContent || '') === node.textContent?.trim()) {
        node.remove();
        return true;
    }
    return Array.from(node.childNodes).some(removeTextLink);
};

/**
 * Whether a video is a tweet's GIF, which Twitter plays muted in a loop (see TweetMediaExtractor.isGif).
 */
export const isTweetGif = (video: string) => video.startsWith('https://video.twimg.com/tweet_video/');

/**
 * Plain text of HTML; line breaks and paragraphs become spaces, so words don't run together.
 */
export const plainText = (html: string | null | undefined) => {
    if (!html) return '';
    const body = parse(html);
    body.querySelectorAll('br').forEach(br => br.replaceWith(' '));
    body.querySelectorAll('p, div, li').forEach(block => block.append(' '));
    return body.textContent?.replace(/\s+/g, ' ').trim() || '';
};
