package mw.nwra.ewaterpermit.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileUploadUtil {
    
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    public static String uploadProfilePicture(MultipartFile file, String uploadsPath) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        // Validate file type
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, GIF, and WebP images are allowed.");
        }
        
        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB.");
        }
        
        // Create profile-pictures directory if it doesn't exist
        Path profilePicturesDir = Paths.get(uploadsPath, "profile-pictures");
        if (!Files.exists(profilePicturesDir)) {
            Files.createDirectories(profilePicturesDir);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".jpg";
        
        String uniqueFilename = "profile_" + UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + fileExtension;
        
        // Save file
        Path filePath = profilePicturesDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);
        
        // Return relative URL path
        return "/uploads/profile-pictures/" + uniqueFilename;
    }
    
    public static void deleteProfilePicture(String profilePictureUrl, String uploadsPath) {
        if (profilePictureUrl == null || profilePictureUrl.isEmpty()) {
            return;
        }
        
        try {
            // Extract filename from URL
            String filename = profilePictureUrl.substring(profilePictureUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(uploadsPath, "profile-pictures", filename);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (Exception e) {
            // Log error but don't throw exception
            System.err.println("Error deleting profile picture: " + e.getMessage());
        }
    }
}