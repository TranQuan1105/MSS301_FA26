package fu.de190167.user.controller;

import fu.de190167.user.common.OnCreate;
import fu.de190167.user.common.ResponseUtil;
import fu.de190167.user.common.UserConstants;
import fu.de190167.user.dto.ApiResponse;
import fu.de190167.user.dto.UserDTO;
import fu.de190167.user.entity.Role;
import fu.de190167.user.entity.UserStatus;
import fu.de190167.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class DE190167UserController {

    private final UserService userService;

    public DE190167UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> createUser(
            @Validated({Default.class, OnCreate.class}) @RequestBody UserDTO dto) {
        return ResponseUtil.toResponse(userService.createUser(dto), 201);
    }

    @PutMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> updateUser(@PathVariable("userId") Long userId,
                                                  @Valid @RequestBody UserDTO dto) {
        return ResponseUtil.toResponse(userService.updateUser(userId, dto), 200);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable("userId") Long userId) {
        return ResponseUtil.toResponse(userService.getUserById(userId), 200);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deactivateUser(@PathVariable("userId") Long userId) {
        return ResponseUtil.toResponse(userService.deactivateUser(userId), 200);
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getUserList(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "status", required = false) UserStatus status) {
        if (page < 0 || size < 1 || size > UserConstants.MAX_PAGE_SIZE) {
            return ResponseUtil.toResponse(ApiResponse.error(UserConstants.VALIDATION_FAILED,
                    "Validation failed: page must be >= 0 and size must be between 1 and 100"), 200);
        }
        return ResponseUtil.toResponse(
                userService.getUserList(role, status, PageRequest.of(page, size)), 200);
    }
}
