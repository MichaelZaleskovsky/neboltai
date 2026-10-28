package org.neboltai.artwork.service;

import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageService {
    private final MinioClient minioClient;

    @Value("${app.minio.master-bucket}")
    private String masterBucket;

    @Value("${app.minio.preview-bucket}")
    private String previewBucket;

    public void deleteImage(UUID imageId) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(masterBucket).object(imageId.toString()).build());
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(previewBucket).object(imageId.toString()).build());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cannot delete image from MinIO", e);
        }
    }

    public void requireImage(UUID imageId) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(masterBucket).object(imageId.toString()).build());
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())
                    || "NoSuchObject".equals(e.errorResponse().code())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found in MinIO", e);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cannot check image in MinIO", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cannot check image in MinIO", e);
        }
    }
}
