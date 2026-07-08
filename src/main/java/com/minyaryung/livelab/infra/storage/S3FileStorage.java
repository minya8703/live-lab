package com.minyaryung.livelab.infra.storage;

import com.minyaryung.livelab.domain.blog.FileStorage;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class S3FileStorage implements FileStorage {

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
        String filename = UUID.randomUUID().toString().substring(0, 8) + "-" + sanitize(file.getOriginalFilename());
        String key = "blog/" + date + "/" + filename;
        s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(file.getContentType()).build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return publicUrl + key;
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(publicUrl)) return;
        String key = fileUrl.substring(publicUrl.length());
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private String sanitize(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
