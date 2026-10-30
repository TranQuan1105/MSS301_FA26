package fu.de190167.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fu.de190167.post.common.OnCreate;
import fu.de190167.post.common.PostConstants;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Format rules (Default group) apply to both POST and PUT and ignore null values.
 * Mandatory rules (OnCreate group) only apply to POST.
 */
public class PostDTO {

    private Long postId;

    @NotBlank(groups = OnCreate.class, message = "advertiserName is required")
    @Size(max = 100, message = "advertiserName must be at most 100 characters")
    @Pattern(regexp = ".*\\S.*", message = "advertiserName must not be blank")
    private String advertiserName;

    @NotBlank(groups = OnCreate.class, message = "advertiserEmail is required")
    @Email(message = "advertiserEmail is invalid")
    @Size(max = 100, message = "advertiserEmail must be at most 100 characters")
    private String advertiserEmail;

    @NotBlank(groups = OnCreate.class, message = "advertiserPhone is required")
    @Size(max = 20, message = "advertiserPhone must be at most 20 characters")
    @Pattern(regexp = ".*\\S.*", message = "advertiserPhone must not be blank")
    private String advertiserPhone;

    @NotNull(groups = OnCreate.class, message = "startDate is required")
    @JsonFormat(pattern = PostConstants.DATE_PATTERN, timezone = PostConstants.TIME_ZONE)
    private Date startDate;

    @NotNull(groups = OnCreate.class, message = "endDate is required")
    @JsonFormat(pattern = PostConstants.DATE_PATTERN, timezone = PostConstants.TIME_ZONE)
    private Date endDate;

    @NotNull(groups = OnCreate.class, message = "numberOfPosts is required")
    @Min(value = 1, message = "numberOfPosts must be >= 1")
    private Integer numberOfPosts;

    // Computed by the server = ratePerPost x number of days
    private BigDecimal totalCost;

    // Optional; defaults to SCHEDULED on create
    @Pattern(regexp = PostConstants.STATUS_REGEX,
            message = "status must be DRAFT, SCHEDULED, PUBLISHED or CANCELLED")
    private String status;

    @NotNull(groups = OnCreate.class, message = "userId is required")
    private Long userId;

    public PostDTO() {
    }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getAdvertiserName() { return advertiserName; }
    public void setAdvertiserName(String advertiserName) { this.advertiserName = advertiserName; }

    public String getAdvertiserEmail() { return advertiserEmail; }
    public void setAdvertiserEmail(String advertiserEmail) { this.advertiserEmail = advertiserEmail; }

    public String getAdvertiserPhone() { return advertiserPhone; }
    public void setAdvertiserPhone(String advertiserPhone) { this.advertiserPhone = advertiserPhone; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public Integer getNumberOfPosts() { return numberOfPosts; }
    public void setNumberOfPosts(Integer numberOfPosts) { this.numberOfPosts = numberOfPosts; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
