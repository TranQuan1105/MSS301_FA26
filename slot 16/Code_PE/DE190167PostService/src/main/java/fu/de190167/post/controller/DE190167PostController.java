package fu.de190167.post.controller;

import fu.de190167.post.common.OnCreate;
import fu.de190167.post.common.PostConstants;
import fu.de190167.post.common.ResponseUtil;
import fu.de190167.post.dto.ApiResponse;
import fu.de190167.post.dto.PostDTO;
import fu.de190167.post.entity.PostStatus;
import fu.de190167.post.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/posts", produces = MediaType.APPLICATION_JSON_VALUE)
public class DE190167PostController {

    private final PostService postService;

    public DE190167PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> createPost(
            @Validated({Default.class, OnCreate.class}) @RequestBody PostDTO dto) {
        return ResponseUtil.toResponse(postService.createPost(dto), 201);
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> updatePost(@PathVariable("postId") Long postId,
                                                  @Valid @RequestBody PostDTO dto) {
        return ResponseUtil.toResponse(postService.updatePost(postId, dto), 200);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse> getPostById(@PathVariable("postId") Long postId) {
        return ResponseUtil.toResponse(postService.getPostById(postId), 200);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse> cancelPost(@PathVariable("postId") Long postId) {
        return ResponseUtil.toResponse(postService.cancelPost(postId), 200);
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getPostList(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "advertiserName", required = false) String advertiserName,
            @RequestParam(value = "status", required = false) PostStatus status) {
        if (page < 0 || size < 1 || size > PostConstants.MAX_PAGE_SIZE) {
            return ResponseUtil.toResponse(ApiResponse.error(PostConstants.VALIDATION_FAILED,
                    "Validation failed: page must be >= 0 and size must be between 1 and 100"), 200);
        }
        return ResponseUtil.toResponse(
                postService.getPostList(advertiserName, status, PageRequest.of(page, size)), 200);
    }
}
