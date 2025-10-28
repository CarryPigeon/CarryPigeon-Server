package team.carrypigeon.backend.chat.domain.controller.web.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import team.carrypigeon.backend.chat.domain.service.file.FileService;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.io.InputStream;

@RestController
@Slf4j
@RequestMapping("/file")
public class MinioController {
//
//    private final FileService fileService;
//
//    private final CPFileTokenManager cpFileTokenManager;
//
//    private final CPFileDAO cpFileDAO;
//
//    public MinioController(CPFileTokenManager cpFileTokenManager, FileService fileService, CPFileDAO cpFileDAO) {
//        this.cpFileTokenManager = cpFileTokenManager;
//        this.fileService = fileService;
//        this.cpFileDAO = cpFileDAO;
//    }
//
//    @PostMapping("/upload/{token}")
//    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
//                         @RequestParam("token") String token) {
//        CPFileTokenData fileTokenData = cpFileTokenManager.getFileTokenData(token);
//        if (fileTokenData == null) {
//            return ResponseEntity.status(500).body("upload fail");
//        }
//        if (fileTokenData.getApplyTime().plusMinutes(5).isBefore(TimeUtil.getCurrentLocalTime())) {
//            return ResponseEntity.status(500).body("upload fail");
//        }
//        if (!(fileTokenData.getData() instanceof CPFileBO fileBO)) {
//            return ResponseEntity.status(500).body("upload fail");
//        }
//
//        try (InputStream inputStream = file.getInputStream()) {
//            if(fileService.uploadFile(inputStream, fileBO)){
//                fileBO.setId(IdUtil.generateId());
//                fileBO.setTime(TimeUtil.getCurrentTime());
//                // 文件入库
//                cpFileDAO.addFile(fileBO);
//                return ResponseEntity.ok("upload success");
//            }
//            return  ResponseEntity.status(500).body("upload fail");
//        } catch (Exception e) {
//            log.error(e.getMessage(),e);
//        }
//        return ResponseEntity.status(500).body("upload fail");
//    }
//
//    @GetMapping("/download/{token}")
//    public ResponseEntity<StreamingResponseBody> download(@PathVariable String token) {
//        // token校验
//        CPFileTokenData fileTokenData = cpFileTokenManager.getFileTokenData(token);
//        if (fileTokenData == null) {
//            return ResponseEntity.status(403).build();
//        }
//        if (fileTokenData.getApplyTime().plusMinutes(5).isBefore(TimeUtil.getCurrentLocalTime())) {
//            return ResponseEntity.notFound().build();
//        }
//        if (!(fileTokenData.getData() instanceof CPFileBO fileBO)) {
//            return ResponseEntity.notFound().build();
//        }
//        try {
//            return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileBO.getName())
//                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                    .body(outputStream -> {
//                        // 流式传输，避免文件全部加载到内存
//                        try (InputStream inputStream = fileService.downloadFile(fileBO.getId()+".meta")) {
//                            byte[] buffer = new byte[8192]; // 8KB缓冲区
//                            int bytesRead;
//                            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                                outputStream.write(buffer, 0, bytesRead);
//                            }
//                            outputStream.flush();
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }
//                    });
//        } catch (Exception e) {
//            log.error(e.getMessage(),e);
//            return ResponseEntity.notFound().build();
//        }
//    }
}
