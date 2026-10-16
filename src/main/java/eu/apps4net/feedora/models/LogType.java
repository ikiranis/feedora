package eu.apps4net.feedora.models;

public enum LogType {
    // The feed URL couldn't be downloaded (network error, HTTP error status)
    FEED_FETCH,
    // The feed was downloaded but isn't valid RSS/Atom
    FEED_PARSE,
    // The feed was removed automatically, e.g. because its URL returns an HTML page
    FEED_REMOVED,
    // The feed's information (e.g. title) couldn't be read when adding it
    FEED_INFO,
    // The feed's parsing statistics couldn't be saved
    FEED_STATISTICS,
    // A feed entry couldn't be saved as a post
    POST_SAVE,
    // The post's page couldn't be downloaded or its article extracted
    CONTENT_EXTRACT,
    // No content longer than the description was found for the post
    CONTENT_NOT_FOUND,
    // YouTube videos couldn't be checked for being scheduled or live streams (e.g. YouTube asked to sign in)
    YOUTUBE_LIVE_CHECK,
    // Summary of a run of the feed parser: feeds checked, failures and new posts
    FEED_PARSING
}
