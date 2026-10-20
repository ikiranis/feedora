package eu.apps4net.feedora.utilities;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Tells whether two images show the same picture, even when they're different files: e.g. a news account's tweet
 * whose photo is its article's image, which the link preview shows too. The images are compared by a difference
 * hash (their brightness gradients on a 9x8 grid), which survives resizing and recompression.
 */
public final class ImageMatcher {

    // Bits (of 64) that may differ between two copies of the same picture
    private static final int MAX_DISTANCE = 10;
    // Copies of the same picture have the same shape; a crop is a different picture
    private static final double MAX_ASPECT_DIFFERENCE = 0.05;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    private ImageMatcher() {}

    /**
     * Downloads two images and tells whether they show the same picture.
     *
     * @return Whether they do; false if either can't be downloaded or read (e.g. WebP, which ImageIO doesn't read)
     */
    public static boolean samePicture(String urlA, String urlB) {
        try {
            BufferedImage a = download(urlA);
            if (a == null) return false;
            BufferedImage b = download(urlB);
            return b != null && samePicture(a, b);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean samePicture(BufferedImage a, BufferedImage b) {
        double aspectA = (double) a.getWidth() / a.getHeight();
        double aspectB = (double) b.getWidth() / b.getHeight();
        if (Math.abs(aspectA - aspectB) / Math.max(aspectA, aspectB) > MAX_ASPECT_DIFFERENCE) return false;
        return Long.bitCount(differenceHash(a) ^ differenceHash(b)) <= MAX_DISTANCE;
    }

    /**
     * 64-bit difference hash: the image in grayscale on a 9x8 grid, one bit per pair of neighbouring cells,
     * set when the left one is brighter.
     */
    static long differenceHash(BufferedImage image) {
        double[][] cells = grayscaleGrid(image, 9, 8);
        long hash = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                hash = (hash << 1) | (cells[y][x] > cells[y][x + 1] ? 1 : 0);
            }
        }
        return hash;
    }

    /**
     * Average brightness of each cell of a grid laid over the image (every pixel counts, so detail doesn't alias).
     */
    private static double[][] grayscaleGrid(BufferedImage image, int columns, int rows) {
        double[][] sums = new double[rows][columns];
        int[][] counts = new int[rows][columns];
        int width = image.getWidth();
        int height = image.getHeight();
        // Large images are sampled every few pixels, plenty for a 9x8 grid
        int step = Math.max(1, Math.min(width, height) / 256);
        for (int y = 0; y < height; y += step) {
            int row = y * rows / height;
            for (int x = 0; x < width; x += step) {
                int column = x * columns / width;
                int rgb = image.getRGB(x, y);
                sums[row][column] += 0.299 * ((rgb >> 16) & 0xff) + 0.587 * ((rgb >> 8) & 0xff) + 0.114 * (rgb & 0xff);
                counts[row][column]++;
            }
        }
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (counts[row][column] > 0) sums[row][column] /= counts[row][column];
            }
        }
        return sums;
    }

    // Null if the image can't be downloaded (status other than 200) or read
    static BufferedImage download(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (compatible; Feedora)")
                .GET().build();
        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() != 200) return null;
            byte[] bytes = body.readNBytes(MAX_IMAGE_BYTES + 1);
            if (bytes.length > MAX_IMAGE_BYTES) return null;
            return ImageIO.read(new ByteArrayInputStream(bytes));
        }
    }
}
