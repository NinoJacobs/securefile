package com.capitec.securefile.storage.service;

import com.capitec.securefile.storage.config.SecurefileS3Properties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class StatementObjectStorageServiceTest {

    @Mock
    private S3Client s3Client;

    private StatementObjectStorageService service;

    @BeforeEach
    void setUp() {
        service = new StatementObjectStorageService(
                s3Client,
                new SecurefileS3Properties("http://localhost:4566", "us-east-1", "securefile-statements", "test", "test", true),
                new SimpleMeterRegistry()
        );
    }

    @Test
    void storeStatementUploadsContentAndReturnsMetadata() {
        byte[] content = "statement-pdf".getBytes();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag-1").build());

        StatementObjectStorageService.StoredStatementObject storedObject =
                service.storeStatement("statements/CUST-0001/file.pdf", "application/pdf", content);

        assertThat(storedObject.key()).isEqualTo("statements/CUST-0001/file.pdf");
        assertThat(storedObject.fileSizeBytes()).isEqualTo(content.length);
        assertThat(storedObject.checksum()).isEqualTo("7f6fd032ad9fdfa81f67924ca762af8a8a2126a27f7f7e1dc79c22b157e1b762");
        verify(s3Client).putObject(
                ArgumentMatchers.argThat((PutObjectRequest request) ->
                        request.bucket().equals("securefile-statements")
                                && request.key().equals("statements/CUST-0001/file.pdf")
                                && request.contentType().equals("application/pdf")),
                any(RequestBody.class));
    }

    @Test
    void storeStatementThrowsInternalServerErrorWhenUploadFails() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("boom").build());

        assertThatThrownBy(() -> service.storeStatement("file.pdf", "application/pdf", "x".getBytes()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR));
    }

    @Test
    void loadStatementReturnsObjectBytes() {
        byte[] content = "stored-content".getBytes();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content));

        byte[] loaded = service.loadStatement("statement.pdf");

        assertThat(loaded).isEqualTo(content);
    }

    @Test
    void loadStatementReturnsNotFoundWhenObjectIsMissingByKeyException() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());

        assertThatThrownBy(() -> service.loadStatement("missing.pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    @Test
    void loadStatementReturnsNotFoundWhenObjectStorageReturns404() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("missing").build());

        assertThatThrownBy(() -> service.loadStatement("missing.pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    @Test
    void loadStatementReturnsInternalServerErrorForUnexpectedStorageFailure() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("boom").build());

        assertThatThrownBy(() -> service.loadStatement("broken.pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR));
    }

    @Test
    void statementExistsReturnsTrueWhenHeadObjectSucceeds() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertThat(service.statementExists("present.pdf")).isTrue();
    }

    @Test
    void statementExistsReturnsFalseForNotFoundAndBadRequestStatuses() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("missing").build())
                .thenThrow(S3Exception.builder().statusCode(400).message("bad request").build());

        assertThat(service.statementExists("missing.pdf")).isFalse();
        assertThat(service.statementExists("invalid.pdf")).isFalse();
    }

    @Test
    void statementExistsThrowsInternalServerErrorForUnexpectedLookupFailure() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("boom").build());

        assertThatThrownBy(() -> service.statementExists("broken.pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR));
    }
}
