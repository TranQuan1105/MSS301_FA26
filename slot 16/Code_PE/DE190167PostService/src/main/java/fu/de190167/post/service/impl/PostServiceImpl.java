package fu.de190167.post.service.impl;

import feign.FeignException;
import fu.de190167.post.client.UserClient;
import fu.de190167.post.common.PostConstants;
import fu.de190167.post.dto.*;
import fu.de190167.post.entity.Post;
import fu.de190167.post.entity.PostStatus;
import fu.de190167.post.repository.PostRepository;
import fu.de190167.post.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class PostServiceImpl implements PostService {

    private static final ZoneId ZONE = ZoneId.of(PostConstants.TIME_ZONE);

    private final PostRepository postRepository;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    public PostServiceImpl(PostRepository postRepository, UserClient userClient, ObjectMapper objectMapper) {
        this.postRepository = postRepository;
        this.userClient = userClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ApiResponse createPost(PostDTO dto) {
        // Order matters to return the right code: dates -> user exists -> user ACTIVE -> maxDailyPosts
        if (!isAfter(dto.getEndDate(), dto.getStartDate())) {
            return ApiResponse.error(PostConstants.VALIDATION_FAILED, "endDate must be after startDate");
        }

        UserDTO user = fetchUser(dto.getUserId());
        if (user == null) {
            return ApiResponse.error(PostConstants.NOT_FOUND, "User ID is not found");
        }
        if (!PostConstants.USER_ACTIVE.equals(user.getStatus())) {
            return ApiResponse.error(PostConstants.USER_NOT_ACTIVE, "User is not ACTIVE");
        }
        if (dto.getNumberOfPosts() > user.getMaxDailyPosts()) {
            return ApiResponse.error(PostConstants.VALIDATION_FAILED,
                    "numberOfPosts must not exceed user's maxDailyPosts (" + user.getMaxDailyPosts() + ")");
        }

        Post p = new Post();
        p.setAdvertiserName(dto.getAdvertiserName());
        p.setAdvertiserEmail(dto.getAdvertiserEmail());
        p.setAdvertiserPhone(dto.getAdvertiserPhone());
        p.setStartDate(dto.getStartDate());
        p.setEndDate(dto.getEndDate());
        p.setNumberOfPosts(dto.getNumberOfPosts());
        p.setUserId(dto.getUserId());
        p.setTotalCost(computeTotal(user.getRatePerPost(), dto.getStartDate(), dto.getEndDate()));
        p.setStatus(PostStatus.SCHEDULED);

        Post saved = postRepository.save(p);
        return ApiResponse.ok("Post campaign created successfully", toDTO(saved));
    }

    @Override
    @Transactional
    public ApiResponse updatePost(Long postId, PostDTO dto) {
        Post p = postRepository.findById(postId).orElse(null);
        if (p == null) {
            return ApiResponse.error(PostConstants.NOT_FOUND, "Post is not found");
        }

        // Validate the effective values BEFORE mutating the managed entity,
        // otherwise dirty checking would still flush bad values on commit.
        Date start = dto.getStartDate() != null ? dto.getStartDate() : p.getStartDate();
        Date end = dto.getEndDate() != null ? dto.getEndDate() : p.getEndDate();
        if (!isAfter(end, start)) {
            return ApiResponse.error(PostConstants.VALIDATION_FAILED, "endDate must be after startDate");
        }

        // Recompute totalCost from the user's current rate
        UserDTO user = fetchUser(p.getUserId());
        int numberOfPosts = dto.getNumberOfPosts() != null ? dto.getNumberOfPosts() : p.getNumberOfPosts();
        if (user != null && numberOfPosts > user.getMaxDailyPosts()) {
            return ApiResponse.error(PostConstants.VALIDATION_FAILED,
                    "numberOfPosts must not exceed user's maxDailyPosts (" + user.getMaxDailyPosts() + ")");
        }

        // Partial update: only non-null fields (userId is not changeable)
        if (dto.getAdvertiserName() != null) p.setAdvertiserName(dto.getAdvertiserName());
        if (dto.getAdvertiserEmail() != null) p.setAdvertiserEmail(dto.getAdvertiserEmail());
        if (dto.getAdvertiserPhone() != null) p.setAdvertiserPhone(dto.getAdvertiserPhone());
        if (dto.getStatus() != null) p.setStatus(PostStatus.valueOf(dto.getStatus()));
        p.setStartDate(start);
        p.setEndDate(end);
        p.setNumberOfPosts(numberOfPosts);
        if (user != null) {
            p.setTotalCost(computeTotal(user.getRatePerPost(), start, end));
        }

        Post saved = postRepository.save(p);
        return ApiResponse.ok("Post campaign updated successfully", toDTO(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse getPostById(Long postId) {
        Post p = postRepository.findById(postId).orElse(null);
        if (p == null) {
            return ApiResponse.error(PostConstants.NOT_FOUND, "Post is not found");
        }
        return ApiResponse.ok("Successful", toDetailDTO(p));
    }

    @Override
    @Transactional
    public ApiResponse cancelPost(Long postId) {
        Post p = postRepository.findById(postId).orElse(null);
        if (p == null) {
            return ApiResponse.error(PostConstants.NOT_FOUND, "Post is not found");
        }
        p.setStatus(PostStatus.CANCELLED);
        postRepository.save(p);
        return ApiResponse.ok("Post campaign is cancelled successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse getPostList(String advertiserName, PostStatus status, Pageable pageable) {
        String name = (advertiserName == null || advertiserName.isBlank()) ? null : advertiserName.trim();
        Page<Post> page = postRepository.search(name, status, pageable);
        List<PostDetailDTO> content = page.getContent().stream().map(this::toDetailDTO).toList();
        PageDTO pageDTO = new PageDTO(
                page.getSize(), page.getNumber(), page.getTotalPages(),
                page.getTotalElements(), page.isFirst(), page.isLast(), content);
        return ApiResponse.ok("Successful", pageDTO);
    }

    /** Calls UserService via OpenFeign; returns null when the user does not exist (HTTP 404). */
    private UserDTO fetchUser(Long userId) {
        try {
            ApiResponse res = userClient.getUserById(userId);
            if (res == null || res.getStatus() != PostConstants.SUCCESS || res.getData() == null) {
                return null;
            }
            // data arrives as a LinkedHashMap because ApiResponse.data is declared as Object
            return objectMapper.convertValue(res.getData(), UserDTO.class);
        } catch (FeignException.NotFound ex) {
            return null;
        }
        // Other FeignExceptions (service down, timeout...) propagate -> GlobalExceptionHandler -> 500/0
    }

    /** totalCost = ratePerPost x number of days (end - start). */
    private BigDecimal computeTotal(BigDecimal ratePerPost, Date start, Date end) {
        long days = ChronoUnit.DAYS.between(toLocalDate(start), toLocalDate(end));
        return ratePerPost.multiply(BigDecimal.valueOf(days));
    }

    private boolean isAfter(Date end, Date start) {
        return toLocalDate(end).isAfter(toLocalDate(start));
    }

    // Dates loaded by Hibernate are java.sql.Date, whose toInstant() throws, so handle both
    private LocalDate toLocalDate(Date date) {
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(ZONE).toLocalDate();
    }

    private PostDTO toDTO(Post p) {
        PostDTO d = new PostDTO();
        d.setPostId(p.getPostId());
        d.setAdvertiserName(p.getAdvertiserName());
        d.setAdvertiserEmail(p.getAdvertiserEmail());
        d.setAdvertiserPhone(p.getAdvertiserPhone());
        d.setStartDate(p.getStartDate());
        d.setEndDate(p.getEndDate());
        d.setNumberOfPosts(p.getNumberOfPosts());
        d.setTotalCost(p.getTotalCost());
        d.setStatus(p.getStatus().name());
        d.setUserId(p.getUserId());
        return d;
    }

    private PostDetailDTO toDetailDTO(Post p) {
        PostDetailDTO d = new PostDetailDTO();
        d.setPostId(p.getPostId());
        d.setAdvertiserName(p.getAdvertiserName());
        d.setAdvertiserEmail(p.getAdvertiserEmail());
        d.setAdvertiserPhone(p.getAdvertiserPhone());
        d.setStartDate(p.getStartDate());
        d.setEndDate(p.getEndDate());
        d.setNumberOfPosts(p.getNumberOfPosts());
        d.setTotalCost(p.getTotalCost());
        d.setStatus(p.getStatus().name());
        d.setUser(fetchUser(p.getUserId()));
        return d;
    }
}
