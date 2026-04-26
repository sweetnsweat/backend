package com.capstone.backend.global.log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
public class DevelopmentLogController {

    private final Path logFile;
    private final boolean enabled;
    private final int maxLines;

    public DevelopmentLogController(@Value("${logging.file.name:logs/backend.log}") String logFile,
                                    @Value("${app.dev.logs.enabled:false}") boolean enabled,
                                    @Value("${app.dev.logs.max-lines:500}") int maxLines) {
        this.logFile = Path.of(logFile);
        this.enabled = enabled;
        this.maxLines = maxLines;
    }

    @GetMapping(value = "/dev/logs", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> logs(@RequestParam(defaultValue = "200") int lines) throws IOException {
        if (!enabled) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        if (!Files.exists(logFile)) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Log file is not ready yet");
        }

        int effectiveLines = Math.max(1, Math.min(lines, maxLines));
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                .body(readTail(effectiveLines));
    }

    private String readTail(int lines) throws IOException {
        Deque<String> tail = new ArrayDeque<>(lines);
        try (var stream = Files.lines(logFile, StandardCharsets.UTF_8)) {
            stream.forEach(line -> {
                if (tail.size() == lines) {
                    tail.removeFirst();
                }
                tail.addLast(line);
            });
        }
        return String.join(System.lineSeparator(), List.copyOf(tail)) + System.lineSeparator();
    }
}
