package fu.de190167.post.service;

import fu.de190167.post.dto.ApiResponse;
import fu.de190167.post.dto.PostDTO;
import fu.de190167.post.entity.PostStatus;
import org.springframework.data.domain.Pageable;

public interface PostService {

    ApiResponse createPost(PostDTO dto);

    ApiResponse updatePost(Long postId, PostDTO dto);

    ApiResponse getPostById(Long postId);

    ApiResponse cancelPost(Long postId);

    ApiResponse getPostList(String advertiserName, PostStatus status, Pageable pageable);
}
