package com.minyaryung.livelab.infra.storage;

import com.minyaryung.livelab.domain.blog.FileStorage;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class S3FileStorage implements FileStorage {

    private static final int SIGNATURE_LENGTH = 12;

    private final S3Client s3;
    private final String bucket;
    private final String publicUrl;

    public S3FileStorage(S3Client s3, String bucket, String publicUrl) {
        this.s3 = s3;
        this.bucket = bucket;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl : publicUrl + "/";
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));

        try (BufferedInputStream input = new BufferedInputStream(file.getInputStream())) {
            input.mark(SIGNATURE_LENGTH);
            ImageType imageType = ImageType.detect(input.readNBytes(SIGNATURE_LENGTH));
            input.reset();

            String filename = UUID.randomUUID().toString().substring(0, 12) + imageType.extension;
            String key = "blog/" + date + "/" + filename;
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(imageType.contentType)
                            .build(),
                    RequestBody.fromInputStream(input, file.getSize()));
            return publicUrl + key;
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(publicUrl)) return;
        String key = fileUrl.substring(publicUrl.length());
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private enum ImageType {
        PNG("image/png", ".png"),
        JPEG("image/jpeg", ".jpg"),
        GIF("image/gif", ".gif"),
        WEBP("image/webp", ".webp");

        private final String contentType;
        private final String extension;

        ImageType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        private static ImageType detect(byte[] bytes) {
            if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return PNG;
            if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) return JPEG;
            if (startsWith(bytes, 'G', 'I', 'F', '8', '7', 'a')
                    || startsWith(bytes, 'G', 'I', 'F', '8', '9', 'a')) return GIF;
            if (startsWith(bytes, 'R', 'I', 'F', 'F')
                    && bytes.length >= 12
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return WEBP;
            throw new IllegalArgumentException("PNG, JPEG, GIF, WebP 이미지만 업로드할 수 있습니다.");
        }

        private static boolean startsWith(byte[] bytes, int... signature) {
            if (bytes.length < signature.length) return false;
            for (int i = 0; i < signature.length; i++) {
                if ((bytes[i] & 0xFF) != signature[i]) return false;
            }
            return true;
        }
    }
}
