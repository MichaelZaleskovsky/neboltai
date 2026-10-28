package org.neboltai.artwork.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Runs against real libvips in Docker; skips only when the native executable is absent. */
class PyramidalTiffConverterTest {
    @TempDir Path directory;

    @BeforeAll
    static void requireVips() throws Exception {
        Process process;
        try {
            process = new ProcessBuilder("vips", "--version").start();
        } catch (java.io.IOException e) {
            assumeTrue(false, "libvips is not installed; integration tests run in the Docker build");
            return;
        }
        assertTrue(process.waitFor(10, TimeUnit.SECONDS));
        assertEquals(0, process.exitValue());
    }

    @Test
    void jpegBecomesTiledPyramidWithFullResolutionPixelsPreserved() throws Exception {
        verifyConversion("jpeg", BufferedImage.TYPE_INT_RGB);
    }

    @Test
    void grayscaleTiffBecomesTiledPyramid() throws Exception {
        verifyConversion("tiff", BufferedImage.TYPE_BYTE_GRAY);
    }

    private void verifyConversion(String format, int imageType) throws Exception {
        var original = new BufferedImage(1200, 800, imageType);
        for (int y = 0; y < 800; y++) {
            for (int x = 0; x < 1200; x++) {
                int value = (x + y) % 256;
                original.setRGB(x, y, (value << 16) | (value << 8) | value);
            }
        }
        Path input = directory.resolve("input.image");
        assertTrue(ImageIO.write(original, format, input.toFile()));
        Path output = directory.resolve("pyramid.tif");
        new PyramidalTiffConverter().convert(input, output);
        try (var stream = ImageIO.createImageInputStream(output.toFile())) {
            var reader = ImageIO.getImageReaders(stream).next();
            try {
                reader.setInput(stream);
                assertEquals(3, reader.getNumImages(true));
                assertEquals(1200, reader.getWidth(0));
                assertEquals(800, reader.getHeight(0));
                assertEquals(600, reader.getWidth(1));
                assertEquals(400, reader.getHeight(1));
                assertEquals(300, reader.getWidth(2));
                assertEquals(200, reader.getHeight(2));
                for (int level = 0; level < 3; level++) {
                    assertTrue(reader.isImageTiled(level));
                    assertEquals(512, reader.getTileWidth(level));
                    assertEquals(512, reader.getTileHeight(level));
                }
                // Deflate does not add another lossy encoding to the decoded source pixels.
                BufferedImage decoded = ImageIO.read(input.toFile());
                BufferedImage converted = reader.read(0);
                for (int y = 0; y < 800; y += 17) {
                    for (int x = 0; x < 1200; x += 19) {
                        assertEquals(decoded.getRGB(x, y), converted.getRGB(x, y));
                    }
                }
            } finally { reader.dispose(); }
        }
        assertFalse(Files.exists(directory.resolve("conversion.log")));
    }
}
