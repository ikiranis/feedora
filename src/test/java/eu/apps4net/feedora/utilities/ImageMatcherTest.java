package eu.apps4net.feedora.utilities;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ImageMatcherTest {

    /**
     * A picture of random blocks (a 24x12 grid, from a seed), drawn at any size.
     */
    private static BufferedImage picture(int width, int height, long seed) {
        Random random = new Random(seed);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        for (int row = 0; row < 12; row++) {
            for (int column = 0; column < 24; column++) {
                graphics.setColor(new Color(random.nextInt(0x1000000)));
                graphics.fillRect(column * width / 24, row * height / 12, width / 24 + 1, height / 12 + 1);
            }
        }
        graphics.dispose();
        return image;
    }

    @Test
    void matchesTheSamePictureAtDifferentSizes() {
        assertThat(ImageMatcher.samePicture(picture(1200, 630, 1), picture(680, 357, 1))).isTrue();
    }

    @Test
    void doesNotMatchDifferentPictures() {
        assertThat(ImageMatcher.samePicture(picture(1200, 630, 1), picture(1200, 630, 2))).isFalse();
    }

    @Test
    void doesNotMatchPicturesOfDifferentShapes() {
        assertThat(ImageMatcher.samePicture(picture(1200, 630, 1), picture(630, 630, 1))).isFalse();
    }
}
