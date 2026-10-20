package eu.apps4net.feedora.utilities;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;

/**
 * Finds the main color of a website's favicon (or of another small image, e.g. a YouTube channel's avatar),
 * used as the light background of its feed's cards.
 * Only colorful pixels count: white, black and grey (the background or outline of most favicons) are skipped,
 * so a favicon without a clear color (e.g. a black and white logo) has none.
 */
public final class FaviconColorExtractor {

    private static final int HUE_BUCKETS = 36;
    // Pixels less saturated or darker than this are greyish and don't count
    private static final float MIN_SATURATION = 0.25f;
    private static final float MIN_BRIGHTNESS = 0.2f;
    // Share of the opaque pixels that must be colorful, so a tiny colored detail doesn't color the whole card
    private static final double MIN_COLORFUL_SHARE = 0.05;

    private FaviconColorExtractor() {}

    /**
     * Favicon URL of a website, from Google's favicon service (as shown by the frontend).
     *
     * @return The URL, or null if the website URL has no host
     */
    public static String faviconUrl(String websiteUrl) {
        try {
            String host = URI.create(websiteUrl.trim()).getHost();
            return host == null || host.isBlank() ? null : "https://www.google.com/s2/favicons?domain=" + host + "&sz=64";
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Downloads the favicon of a website and finds its main color.
     *
     * @return The color as #rrggbb, or an empty string if the favicon has no clear color or there's none
     * @throws IOException if the favicon can't be downloaded (e.g. no network), so it can be retried later
     */
    public static String mainColor(String websiteUrl) throws IOException, InterruptedException {
        String url = faviconUrl(websiteUrl);
        return url == null ? "" : mainColorOfImage(url);
    }

    /**
     * Downloads an image and finds its main color.
     *
     * @return The color as #rrggbb, or an empty string if the image has no clear color or can't be read
     * @throws IOException if the image can't be downloaded (e.g. no network), so it can be retried later
     */
    public static String mainColorOfImage(String url) throws IOException, InterruptedException {
        try {
            BufferedImage image = ImageMatcher.download(url);
            return image == null ? "" : mainColor(image);
        } catch (IOException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Main color of an image: the average of its most common hue among the colorful pixels, weighted by saturation and brightness (vivid colors stand out).
     *
     * @return The color as #rrggbb, or an empty string if the image has no clear color
     */
    static String mainColor(BufferedImage image) {
        double[] weights = new double[HUE_BUCKETS];
        double[][] rgbSums = new double[HUE_BUCKETS][3];
        int opaque = 0;
        int colorful = 0;
        float[] hsb = new float[3];

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) < 128) continue;
                opaque++;
                int r = (argb >> 16) & 0xff, g = (argb >> 8) & 0xff, b = argb & 0xff;
                Color.RGBtoHSB(r, g, b, hsb);
                if (hsb[1] < MIN_SATURATION || hsb[2] < MIN_BRIGHTNESS) continue;
                colorful++;
                int bucket = Math.min(HUE_BUCKETS - 1, (int) (hsb[0] * HUE_BUCKETS));
                double weight = hsb[1] * hsb[2];
                weights[bucket] += weight;
                rgbSums[bucket][0] += r * weight;
                rgbSums[bucket][1] += g * weight;
                rgbSums[bucket][2] += b * weight;
            }
        }
        if (opaque == 0 || colorful < opaque * MIN_COLORFUL_SHARE) return "";

        int best = 0;
        for (int i = 1; i < HUE_BUCKETS; i++) {
            if (weights[i] > weights[best]) best = i;
        }
        return String.format("#%02x%02x%02x",
                Math.round(rgbSums[best][0] / weights[best]),
                Math.round(rgbSums[best][1] / weights[best]),
                Math.round(rgbSums[best][2] / weights[best]));
    }
}
