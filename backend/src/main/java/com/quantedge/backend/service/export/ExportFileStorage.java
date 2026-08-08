package com.quantedge.backend.service.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import com.quantedge.backend.entity.User;
import com.quantedge.backend.exception.ReportGenerationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Writes generated export bytes to the configured local filesystem path, per user. */
@Component
public class ExportFileStorage {

    private final Path basePath;

    public ExportFileStorage(@Value("${export.base-path}") String basePath) {
        this.basePath = Path.of(basePath);
    }

    public String save(User user, String reportPrefix, String extension, byte[] content) {
        try {
            Path userDir = basePath.resolve(user.getId().toString());
            Files.createDirectories(userDir);
            String filename = reportPrefix + "_" + Instant.now().toEpochMilli() + "." + extension;
            Files.write(userDir.resolve(filename), content);
            return filename;
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to write export file to disk", e);
        }
    }
}
