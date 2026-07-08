package com.minyaryung.livelab.infra.storage;

import com.minyaryung.livelab.domain.blog.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import java.net.URI;

@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Bean
    public FileStorage fileStorage(
            @Value("${livelab.blog.s3.endpoint:}") String endpoint,
            @Value("${livelab.blog.s3.region:ap-northeast-2}") String region,
            @Value("${livelab.blog.s3.access-key:}") String accessKey,
            @Value("${livelab.blog.s3.secret-key:}") String secretKey,
            @Value("${livelab.blog.s3.bucket:}") String bucket,
            @Value("${livelab.blog.s3.public-url:}") String publicUrl) {
        if (accessKey == null || accessKey.isBlank()) {
            log.info("S3 access key not configured — blog image upload disabled");
            return null;
        }
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        if (!endpoint.contains("amazonaws.com")) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        S3Client s3 = builder.build();
        log.info("S3 storage configured — bucket={}", bucket);
        return new S3FileStorage(s3, bucket, publicUrl);
    }
}
