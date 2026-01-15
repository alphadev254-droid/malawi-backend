package mw.nwra.ewaterpermit.controller;

import mw.nwra.ewaterpermit.model.CoreLicenseApplication;
import mw.nwra.ewaterpermit.service.CoreLicenseApplicationService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.Auditor;
import mw.nwra.ewaterpermit.constant.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import mw.nwra.ewaterpermit.model.SysUserAccount;


@RestController
@RequestMapping("/v1/workflow")

public class BoardMinutesController {

    private static final Logger log = LoggerFactory.getLogger(BoardMinutesController.class);

    @Autowired
    private CoreLicenseApplicationService applicationService;

    @Autowired
    private Auditor auditor;

    @PostMapping("/upload-board-minutes/{applicationId}")
    public ResponseEntity<?> uploadBoardMinutes(
            @PathVariable String applicationId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {

        try {
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            // Delete existing file if any
            if (application.getBoardMinutesDocument() != null) {
                try {
                    Files.deleteIfExists(Paths.get(application.getBoardMinutesDocument()));
                } catch (IOException e) {
                    log.warn("Could not delete existing board minutes file: {}", e.getMessage());
                }
            }

            // Save new file
            String uploadDir = "uploads/board-minutes/" + applicationId;
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = "board-minutes" + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update application
            application.setBoardMinutesDocument(filePath.toString());
            applicationService.editCoreLicenseApplication(application);
            
            // Audit log
            SysUserAccount user = AppUtil.getLoggedInUser(token);
            auditor.audit(Action.CREATE, "BoardMinutes", applicationId, user, "Uploaded board minutes document");

            return ResponseEntity.ok(Map.of("message", "Board minutes uploaded successfully"));

        } catch (Exception e) {
            log.error("Error uploading board minutes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload board minutes"));
        }
    }

    @GetMapping("/board-minutes/{applicationId}")
    public ResponseEntity<?> getBoardMinutes(
            @PathVariable String applicationId,
            @RequestHeader("Authorization") String token) {

        try {
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            if (application.getBoardMinutesDocument() != null) {
                Path filePath = Paths.get(application.getBoardMinutesDocument());
                if (Files.exists(filePath)) {
                    // Extract filename from path
                    String filename = filePath.getFileName().toString();
                    // Get file size
                    long fileSize = Files.size(filePath);
                    
                    return ResponseEntity.ok(Map.of(
                            "exists", true,
                            "filename", filename,
                            "size", fileSize,
                            "path", application.getBoardMinutesDocument(),
                            "url", "/v1/workflow/download-board-minutes/" + applicationId
                    ));
                }
            }

            return ResponseEntity.ok(Map.of("exists", false));

        } catch (Exception e) {
            log.error("Error getting board minutes info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get board minutes info"));
        }
    }

    @DeleteMapping("/delete-board-minutes/{applicationId}")
    public ResponseEntity<?> deleteBoardMinutes(
            @PathVariable String applicationId,
            @RequestHeader("Authorization") String token) {

        try {
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            // Delete file
            if (application.getBoardMinutesDocument() != null) {
                Files.deleteIfExists(Paths.get(application.getBoardMinutesDocument()));
                application.setBoardMinutesDocument(null);
                applicationService.editCoreLicenseApplication(application);
                
                // Audit log
                SysUserAccount user = AppUtil.getLoggedInUser(token);
                auditor.audit(Action.DELETE, "BoardMinutes", applicationId, user, "Deleted board minutes document");
            }

            return ResponseEntity.ok(Map.of("message", "Board minutes deleted successfully"));

        } catch (Exception e) {
            log.error("Error deleting board minutes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete board minutes"));
        }
    }

    @GetMapping("/download-board-minutes/{applicationId}/{token}")
    public ResponseEntity<?> downloadBoardMinutes(
            @PathVariable String applicationId,
            @PathVariable String token) {

        try {
            // Verify token and get user
            mw.nwra.ewaterpermit.model.SysUserAccount user;
            try {
                user = AppUtil.getLoggedInUser("Bearer " + token);
            } catch (Exception e) {
                log.error("Invalid token: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired token"));
            }

            // Check if user is an applicant
            if (user.getSysUserGroup() != null &&
                "applicant".equalsIgnoreCase(user.getSysUserGroup().getName())) {
                log.warn("Applicant user {} attempted to access board minutes", user.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Applicants are not authorized to view board minutes"));
            }

            // Get application
            CoreLicenseApplication application = applicationService.getCoreLicenseApplicationById(applicationId);
            if (application == null || application.getBoardMinutesDocument() == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(application.getBoardMinutesDocument());
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String filename = filePath.getFileName().toString();

                // Determine content type based on file extension
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error downloading board minutes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred while processing your request"));
        }
    }
}
