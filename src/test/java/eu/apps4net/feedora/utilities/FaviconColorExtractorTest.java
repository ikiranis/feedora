package eu.apps4net.feedora.utilities;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class FaviconColorExtractorTest {

    @Test
    void theMostCommonColorWinsOverWhiteAndSmallerColors() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 32, 32);
        graphics.setColor(new Color(0xbb, 0x19, 0x19));
        graphics.fillRect(0, 0, 32, 16);
        graphics.setColor(new Color(0x19, 0x19, 0xbb));
        graphics.fillRect(0, 16, 8, 8);
        graphics.dispose();

        assertThat(FaviconColorExtractor.mainColor(image)).isEqualTo("#bb1919");
    }

    @Test
    void transparentPixelsAreSkipped() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(0x10, 0x80, 0x20));
        graphics.fillRect(4, 4, 8, 8);
        graphics.dispose();

        assertThat(FaviconColorExtractor.mainColor(image)).isEqualTo("#108020");
    }

    @Test
    void blackWhiteAndGreyFaviconsHaveNoColor() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 16, 16);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(4, 4, 4, 8);
        graphics.setColor(Color.GRAY);
        graphics.fillRect(8, 4, 4, 8);
        graphics.dispose();

        assertThat(FaviconColorExtractor.mainColor(image)).isEmpty();
        assertThat(FaviconColorExtractor.mainColor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB))).isEmpty();
    }

    @Test
    void faviconUrlUsesTheWebsiteHost() {
        assertThat(FaviconColorExtractor.faviconUrl("https://www.bbc.co.uk/news"))
                .isEqualTo("https://www.google.com/s2/favicons?domain=www.bbc.co.uk&sz=64");
        assertThat(FaviconColorExtractor.faviconUrl("")).isNull();
        assertThat(FaviconColorExtractor.faviconUrl("not a url")).isNull();
    }
}
