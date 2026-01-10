package team.carrypigeon.backend.chat.domain.controller.web.file;

import io.minio.StatObjectResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.chat.domain.service.file.FileService;
import team.carrypigeon.backend.chat.domain.service.file.FileTokenService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MinioControllerTests {

    @Test
    void upload_invalidToken_shouldReturn403() {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<?> resp = controller.upload("t", new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes()));
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void upload_emptyFile_shouldReturn400() {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "UPLOAD", null, LocalDateTime.now().plusSeconds(60)));

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<?> resp = controller.upload("t", new MockMultipartFile("file", "a.txt", "text/plain", new byte[0]));
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void upload_opIsNull_shouldReturn403() {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, null, null, LocalDateTime.now().plusSeconds(60)));

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<?> resp = controller.upload("t", new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes()));
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void upload_duplicate_shouldDeleteNewObjectAndReturnExistingId() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "UPLOAD", null, LocalDateTime.now().plusSeconds(60)));
        doNothing().when(fileService).upload(anyString(), any(), anyLong(), anyString());

        CPFileInfo existing = new CPFileInfo().setId(99L);
        when(fileInfoDao.getBySha256AndSize(anyString(), anyLong())).thenReturn(existing);

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<?> resp = controller.upload("t", new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes()));
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("99", resp.getBody());
        verify(fileService, times(1)).deleteFile(anyString());
        verify(fileInfoDao, never()).save(any());
    }

    @Test
    void upload_duplicate_deleteThrows_shouldStillReturnExistingId() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "UPLOAD", null, LocalDateTime.now().plusSeconds(60)));
        doNothing().when(fileService).upload(anyString(), any(), anyLong(), anyString());

        CPFileInfo existing = new CPFileInfo().setId(99L);
        when(fileInfoDao.getBySha256AndSize(anyString(), anyLong())).thenReturn(existing);
        doThrow(new RuntimeException("delete fail")).when(fileService).deleteFile(anyString());

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<?> resp = controller.upload("t", new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes()));
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("99", resp.getBody());
    }

    @Test
    void upload_saveFail_shouldReturn500() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "UPLOAD", null, LocalDateTime.now().plusSeconds(60)));
        doNothing().when(fileService).upload(anyString(), any(), anyLong(), anyString());
        when(fileInfoDao.getBySha256AndSize(anyString(), anyLong())).thenReturn(null);
        when(fileInfoDao.save(any())).thenReturn(false);

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<?> resp = controller.upload("t", new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes()));
        assertEquals(500, resp.getStatusCode().value());
    }

    @Test
    void upload_fileServiceThrows_shouldReturn500() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "UPLOAD", null, LocalDateTime.now().plusSeconds(60)));
        doThrow(new RuntimeException("upload fail")).when(fileService).upload(anyString(), any(), anyLong(), anyString());

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<?> resp = controller.upload("t", new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes()));
        assertEquals(500, resp.getStatusCode().value());
    }

    @Test
    void upload_sizeUnknown_shouldCountStreamLength() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "UPLOAD", null, LocalDateTime.now().plusSeconds(60)));
        doNothing().when(fileService).upload(anyString(), any(), anyLong(), anyString());
        when(fileInfoDao.getBySha256AndSize(anyString(), anyLong())).thenReturn(null);
        when(fileInfoDao.save(any())).thenReturn(true);

        byte[] payload = "stream-bytes".getBytes();
        MultipartFile file = new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return "a.txt";
            }

            @Override
            public String getContentType() {
                return "text/plain";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public byte[] getBytes() {
                return payload;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(payload);
            }

            @Override
            public void transferTo(java.io.File dest) {
                throw new UnsupportedOperationException();
            }
        };

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<?> resp = controller.upload("t", file);
        assertEquals(200, resp.getStatusCode().value());

        verify(fileService).upload(anyString(), any(), eq((long) payload.length), anyString());
    }

    @Test
    void upload_success_shouldReturnFileIdString() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "UPLOAD", null, LocalDateTime.now().plusSeconds(60)));
        doNothing().when(fileService).upload(anyString(), any(), anyLong(), anyString());
        when(fileInfoDao.getBySha256AndSize(anyString(), anyLong())).thenReturn(null);
        when(fileInfoDao.save(any())).thenReturn(true);

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<?> resp = controller.upload("t", new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes()));
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(((String) resp.getBody()).matches("\\d+"));
    }

    @Test
    void downloadWithToken_invalidToken_shouldReturn403() {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "UPLOAD", "1", LocalDateTime.now().plusSeconds(60)));

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<StreamingResponseBody> resp = controller.downloadWithToken("t");
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void downloadWithToken_missingFileId_shouldReturn404() {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "DOWNLOAD", "", LocalDateTime.now().plusSeconds(60)));

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<StreamingResponseBody> resp = controller.downloadWithToken("t");
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void downloadWithToken_statThrows_shouldReturn404() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "DOWNLOAD", "1", LocalDateTime.now().plusSeconds(60)));
        when(fileService.statFile("1")).thenThrow(new RuntimeException("not found"));

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<StreamingResponseBody> resp = controller.downloadWithToken("t");
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void downloadWithToken_happyPath_shouldStreamBytes() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "DOWNLOAD", "1", LocalDateTime.now().plusSeconds(60)));
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.contentType()).thenReturn("application/octet-stream");
        when(fileService.statFile("1")).thenReturn(stat);
        when(fileService.downloadFile("1")).thenReturn(new ByteArrayInputStream("abc".getBytes()));

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<StreamingResponseBody> resp = controller.downloadWithToken("t");
        assertEquals(200, resp.getStatusCode().value());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        resp.getBody().writeTo(out);
        assertEquals("abc", out.toString());
    }

    @Test
    void downloadWithToken_contentTypeNull_shouldDefaultOctetStream() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(tokenService.consume("t")).thenReturn(new FileTokenService.FileToken("t", 1L, "DOWNLOAD", "1", LocalDateTime.now().plusSeconds(60)));
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.contentType()).thenReturn(null);
        when(fileService.statFile("1")).thenReturn(stat);
        when(fileService.downloadFile("1")).thenReturn(new ByteArrayInputStream("abc".getBytes()));

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<StreamingResponseBody> resp = controller.downloadWithToken("t");
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("application/octet-stream", resp.getHeaders().getContentType().toString());
    }

    @Test
    void downloadRaw_invalidFileId_shouldReturn400() {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<StreamingResponseBody> resp = controller.downloadRaw("");
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void downloadRaw_statThrows_shouldReturn404() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        when(fileService.statFile("1")).thenThrow(new RuntimeException("not found"));

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<StreamingResponseBody> resp = controller.downloadRaw("1");
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void downloadRaw_notAllowed_shouldReturn403() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(4L * 1024 * 1024); // > 3MB
        when(stat.contentType()).thenReturn("image/png");
        when(fileService.statFile("1")).thenReturn(stat);

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<StreamingResponseBody> resp = controller.downloadRaw("1");
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void downloadRaw_notImage_shouldReturn403() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(10L);
        when(stat.contentType()).thenReturn("application/octet-stream");
        when(fileService.statFile("1")).thenReturn(stat);

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<StreamingResponseBody> resp = controller.downloadRaw("1");
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void downloadRaw_contentTypeNull_shouldReturn403() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(10L);
        when(stat.contentType()).thenReturn(null);
        when(fileService.statFile("1")).thenReturn(stat);

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<StreamingResponseBody> resp = controller.downloadRaw("1");
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void downloadRaw_allowed_shouldStreamBytes() throws Exception {
        FileService fileService = mock(FileService.class);
        FileTokenService tokenService = mock(FileTokenService.class);
        FileInfoDao fileInfoDao = mock(FileInfoDao.class);

        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(10L);
        when(stat.contentType()).thenReturn("image/png");
        when(fileService.statFile("1")).thenReturn(stat);
        when(fileService.downloadFile("1")).thenReturn(new ByteArrayInputStream("xyz".getBytes()));

        MinioController controller = new MinioController(fileService, tokenService, fileInfoDao);
        ResponseEntity<StreamingResponseBody> resp = controller.downloadRaw("1");
        assertEquals(200, resp.getStatusCode().value());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        resp.getBody().writeTo(out);
        assertEquals("xyz", out.toString());
    }
}
