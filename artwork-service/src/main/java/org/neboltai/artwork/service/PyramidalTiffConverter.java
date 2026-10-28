package org.neboltai.artwork.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class PyramidalTiffConverter {
    private final Semaphore conversionSlot = new Semaphore(1, true);

    @Value("${app.image-conversion.vips-executable:vips}")
    private String executable = "vips";
    @Value("${app.image-conversion.timeout:PT10M}")
    private Duration timeout = Duration.ofMinutes(10);

    public void convert(Path input, Path output) {
        Process process = null;
        boolean acquired = false;
        Path log = output.resolveSibling("conversion.log");
        try {
            acquired = conversionSlot.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) throw unavailable("Image converter is busy; retry later", null);
            ProcessBuilder builder = new ProcessBuilder(executable, "tiffsave", input.toString(), output.toString(),
                    "--tile", "--pyramid", "--tile-width=512", "--tile-height=512",
                    "--compression=deflate", "--level=6", "--depth=onetile");
            builder.environment().put("VIPS_CONCURRENCY", "2");
            builder.environment().put("VIPS_DISC_THRESHOLD", "64m");
            builder.redirectErrorStream(true).redirectOutput(log.toFile());
            process = builder.start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw unavailable("Image conversion timed out", null);
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(output) || Files.size(output) == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot convert image to pyramidal TIFF");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable("Image conversion interrupted", e);
        } catch (IOException e) {
            throw unavailable("Image converter unavailable", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                // Ensure the process cannot keep writing into a directory being cleaned up.
                process.onExit().join();
            }
            if (acquired) conversionSlot.release();
            try { Files.deleteIfExists(log); } catch (IOException ignored) { }
        }
    }

    private static ResponseStatusException unavailable(String reason, Exception cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, reason, cause);
    }
}
