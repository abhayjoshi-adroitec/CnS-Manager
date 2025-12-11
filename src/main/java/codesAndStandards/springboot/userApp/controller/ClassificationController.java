package codesAndStandards.springboot.userApp.controller;

import codesAndStandards.springboot.userApp.dto.ClassificationDto;
import codesAndStandards.springboot.userApp.entity.User;
import codesAndStandards.springboot.userApp.repository.UserRepository;
import codesAndStandards.springboot.userApp.service.ActivityLogService;
import codesAndStandards.springboot.userApp.service.ClassificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8080", allowCredentials = "true")
public class ClassificationController {

    private final ClassificationService classificationService;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private static final Logger logger = LoggerFactory.getLogger(ClassificationController.class);

    @PostMapping
    public ResponseEntity<ClassificationDto> createClassification(@Valid @RequestBody ClassificationDto classificationDto) {
        String username = getCurrentUsername();
        try {
            Long userId = getCurrentUserId();
            ClassificationDto created = classificationService.createClassification(classificationDto, userId);

            activityLogService.logByUsername(username, ActivityLogService.CLASSIFICATION_ADD,
                    "Created classification: '" + classificationDto.getClassificationName() + "'");

            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            activityLogService.logByUsername(username, ActivityLogService.CLASSIFICATION_ADD_FAIL,
                    "Failed to create classification: '" + classificationDto.getClassificationName() + "' (Reason: " + e.getMessage() + ")");
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassificationDto> updateClassification(@PathVariable Long id,
                                                                  @Valid @RequestBody ClassificationDto classificationDto) {
        String username = getCurrentUsername();
        try {
            Long userId = getCurrentUserId();

            // Fetch old classification name
            ClassificationDto oldClassification = classificationService.getClassificationById(id);
            String oldName = oldClassification.getClassificationName();

            // Update classification
            ClassificationDto updated = classificationService.updateClassification(id, classificationDto, userId);
            String newName = updated.getClassificationName();

            activityLogService.logByUsername(
                    username,
                    ActivityLogService.CLASSIFICATION_EDIT,
                    "Updated classification from '" + oldName + "' to '" + newName + "'"
            );

            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            activityLogService.logByUsername(
                    username,
                    ActivityLogService.CLASSIFICATION_EDIT_FAIL,
                    "Failed to update classification (Reason: " + e.getMessage() + ")"
            );
            throw e;
        }
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteClassification(@PathVariable Long id) {
        String username = getCurrentUsername();
        try {
            Long userId = getCurrentUserId();

            // Fetch name before deletion
            ClassificationDto classification = classificationService.getClassificationById(id);
            String className = classification.getClassificationName();

            // Delete classification
            classificationService.deleteClassification(id, userId);

            activityLogService.logByUsername(
                    username,
                    ActivityLogService.CLASSIFICATION_DELETE,
                    "Deleted classification '" + className + "'"
            );

            // Response
            Map<String, String> response = new HashMap<>();
            response.put("message", "Classification '" + className + "' deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            activityLogService.logByUsername(
                    username,
                    ActivityLogService.CLASSIFICATION_DELETE_FAIL,
                    "Failed to delete classification with ID: " + id + " (Reason: " + e.getMessage() + ")"
            );
            throw e;
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<ClassificationDto> getClassificationById(@PathVariable Long id) {
        return ResponseEntity.ok(classificationService.getClassificationById(id));
    }

    @PreAuthorize("hasAnyAuthority('Manager', 'Admin')")
    @GetMapping
    public ResponseEntity<List<ClassificationDto>> getAllClassifications() {
        return ResponseEntity.ok(classificationService.getAllClassifications());
    }

    @GetMapping("/my-classifications")
    public ResponseEntity<List<ClassificationDto>> getMyClassifications() {
        return ResponseEntity.ok(classificationService.getClassificationsByUser(getCurrentUserId()));
    }

    @GetMapping("/my-edited-classifications")
    public ResponseEntity<List<ClassificationDto>> getMyEditedClassifications() {
        return ResponseEntity.ok(classificationService.getClassificationsEditedByUser(getCurrentUserId()));
    }

    // 🔹 NEW ENDPOINT - Get documents by classification ID -----------------------------------------------------

    /**
     * Get all documents that are using a specific classification
     * Returns list of documents with id and title
     */
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<Map<String, Object>>> getDocumentsByClassification(@PathVariable Long id) {
        try {
            List<Map<String, Object>> documents = classificationService.getDocumentsByClassificationId(id);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            logger.error("Failed to fetch documents for classification ID: " + id, e);
            throw e;
        }
    }

    // ============================================================================
    // 🆕 NEW ENDPOINT FOR BULK UPLOAD - Get all classification names as simple string list
    // ============================================================================

    /**
     * Get all classifications as a simple list of classification names (strings only)
     * Used by bulk upload feature to populate dropdowns
     * Returns: Sorted list of unique classification names
     *
     * Example response: ["Standard", "Specification", "Manual", "Procedure"]
     */
    @GetMapping("/all")
    public ResponseEntity<List<String>> getAllClassificationNames() {
        try {
            logger.debug("Fetching all classification names for bulk upload");

            // Get all classifications and extract just the names
            List<ClassificationDto> allClassifications = classificationService.getAllClassifications();

            List<String> classificationNames = allClassifications.stream()
                    .map(ClassificationDto::getClassificationName)
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            logger.info("Returning {} unique classification names", classificationNames.size());

            return ResponseEntity.ok(classificationNames);
        } catch (Exception e) {
            logger.error("Failed to fetch classification names for bulk upload", e);
            // Return empty list instead of error to prevent frontend issues
            return ResponseEntity.ok(List.of());
        }
    }

    // ✅ Utility: Get User ID
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            throw new RuntimeException("User not found with username: " + authentication.getName());
        }
        return user.getId();
    }

    // ✅ Utility: Get Username
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "Unknown";
    }
}