package eu.apps4net.feedora.exceptions;

/**
 * Thrown when a web page can't be read by the parser, because it answers with a captcha,
 * a Cloudflare (or similar) challenge, or an access error.
 */
public class PageBlockedException extends Exception {
    public PageBlockedException(String message) {
        super(message);
    }
}
