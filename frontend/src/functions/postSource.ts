import { PostType } from '@/types';

/**
 * Name of the post's source: its feed's title, or the site of its link for pages saved from the browser, which have no feed.
 */
export const postSource = (post: PostType): string => {
    if (post.feed) return post.feed.title;
    try {
        return new URL(post.link).hostname.replace(/^www\./, '');
    } catch {
        return '';
    }
};
