package com.minyaryung.livelab.infra.storage;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3FileStorageTest {

    private final S3Client s3 = mock(S3Client.class);
    private final S3FileStorage storage = new S3FileStorage(
            s3, "bucket", "https://assets.example.com");

    @Test
    void usesDetectedPngTypeInsteadOfCallerSuppliedMetadata() throws Exception {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.html", "text/html", png);

        String url = storage.upload(file);

        assertThat(url).endsWith(".png");
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
        assertThat(requestCaptor.getValue().key()).endsWith(".png");
    }

    @Test
    void rejectsHtmlEvenWhenCallerClaimsItIsAnImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> storage.upload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PNG");
        verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
