package com.capitec.securefile.storage.service;

import com.capitec.securefile.storage.config.SecurefileS3Properties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class StatementObjectStorageService {

    private final S3Client s3Client;
    private final SecurefileS3Properties properties;

    public StatementObjectStorageService(S3Client s3Client, SecurefileS3Properties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    public StoredStatementObject storeStatement(String key, String contentType, byte[] content) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (S3Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to upload statement to object storage",
                    ex);
        }

        return new StoredStatementObject(key, content.length, sha256Hex(content));
    }

    public byte[] loadStatement(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();

        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return response.asByteArray();
        } catch (NoSuchKeyException ex) {
            throw new ResponseStatusException(NOT_FOUND, "Statement file not found", ex);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new ResponseStatusException(NOT_FOUND, "Statement file not found", ex);
            }
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to download statement from object storage",
                    ex);
        }
    }

    public boolean statementExists(String key) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();

        try {
            s3Client.headObject(request);
            return true;
        } catch (S3Exception ex) {
            return ex.statusCode() != 404 && ex.statusCode() != 400 && throwStorageLookupFailure(ex);
        }
    }

    private boolean throwStorageLookupFailure(S3Exception ex) {
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to verify statement in object storage",
                ex);
    }

    private String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to compute statement checksum",
                    ex);
        }
    }

    public record StoredStatementObject(String key, long fileSizeBytes, String checksum) {
    }
}
