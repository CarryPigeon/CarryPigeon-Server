package team.carrypigeon.backend.chat.domain.service.file;

import io.minio.MinioClient;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileServiceTests {

    @Test
    void uploadIfNotExists_whenSameSizeExists_shouldReturnFalse() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(3L);
        when(minioClient.statObject(any())).thenReturn(stat);

        FileService service = new FileService(minioClient);
        setBucketName(service, "bucket1");

        boolean uploaded = service.uploadIfNotExists("o", new ByteArrayInputStream(new byte[]{1, 2, 3}), 3L, "text/plain");
        assertFalse(uploaded);
        verify(minioClient, never()).putObject(any());
    }

    @Test
    void uploadIfNotExists_whenNotFound_shouldUploadAndReturnTrue() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        ErrorResponseException ex = mock(ErrorResponseException.class);
        when(ex.errorResponse()).thenReturn(new ErrorResponse("NoSuchKey", "m", "b", "o", "h", "r", "res"));
        when(minioClient.statObject(any())).thenThrow(ex);

        FileService service = new FileService(minioClient);
        setBucketName(service, "bucket1");

        boolean uploaded = service.uploadIfNotExists("o", new ByteArrayInputStream(new byte[]{1}), 1L, "text/plain");
        assertTrue(uploaded);
        verify(minioClient, times(1)).putObject(any());
    }

    @Test
    void uploadIfNotExists_whenOtherError_shouldRethrow() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        ErrorResponseException ex = mock(ErrorResponseException.class);
        when(ex.errorResponse()).thenReturn(new ErrorResponse("AccessDenied", "m", "b", "o", "h", "r", "res"));
        when(minioClient.statObject(any())).thenThrow(ex);

        FileService service = new FileService(minioClient);
        setBucketName(service, "bucket1");

        assertThrows(ErrorResponseException.class, () ->
                service.uploadIfNotExists("o", new ByteArrayInputStream(new byte[]{1}), 1L, "text/plain"));
    }

    @Test
    void uploadDownloadDelete_stat_shouldDelegateToMinioClient() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        FileService service = new FileService(minioClient);
        setBucketName(service, "bucket1");

        service.upload("o", new ByteArrayInputStream(new byte[]{1, 2}), 2L, "text/plain");
        verify(minioClient, times(1)).putObject(any());

        service.downloadFile("o");
        verify(minioClient, times(1)).getObject(any());

        service.statFile("o");
        verify(minioClient, times(1)).statObject(any());

        service.deleteFile("o");
        verify(minioClient, times(1)).removeObject(any());
    }

    private static void setBucketName(FileService service, String bucketName) throws Exception {
        Field field = FileService.class.getDeclaredField("bucketName");
        field.setAccessible(true);
        field.set(service, bucketName);
    }
}
