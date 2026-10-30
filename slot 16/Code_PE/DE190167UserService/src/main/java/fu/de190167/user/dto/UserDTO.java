package fu.de190167.user.dto;

import fu.de190167.user.common.OnCreate;
import fu.de190167.user.common.UserConstants;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Format rules (Default group) apply to both POST and PUT and ignore null values.
 * Mandatory rules (OnCreate group) only apply to POST.
 */
public class UserDTO {

    private Long userId;

    @NotBlank(groups = OnCreate.class, message = "username is required")
    @Size(max = 30, message = "username must be at most 30 characters")
    @Pattern(regexp = ".*\\S.*", message = "username must not be blank")
    private String username;

    @NotBlank(groups = OnCreate.class, message = "email is required")
    @Email(message = "email is invalid")
    @Size(max = 100, message = "email must be at most 100 characters")
    private String email;

    @NotNull(groups = OnCreate.class, message = "role is required")
    @Pattern(regexp = UserConstants.ROLE_REGEX, message = "role must be USER, CREATOR or ADMIN")
    private String role;

    @NotNull(groups = OnCreate.class, message = "ratePerPost is required")
    @DecimalMin(value = "0", inclusive = false, message = "ratePerPost must be > 0")
    @Digits(integer = 16, fraction = 2, message = "ratePerPost must fit decimal(18,2)")
    private BigDecimal ratePerPost;

    @NotNull(groups = OnCreate.class, message = "maxDailyPosts is required")
    @Min(value = 1, message = "maxDailyPosts must be >= 1")
    @Max(value = 10, message = "maxDailyPosts must be <= 10")
    private Integer maxDailyPosts;

    @NotNull(groups = OnCreate.class, message = "level is required")
    @Min(value = 1, message = "level must be >= 1")
    private Integer level;

    // Optional; defaults to ACTIVE on create
    @Pattern(regexp = UserConstants.STATUS_REGEX, message = "status must be ACTIVE, SUSPENDED or DEACTIVATED")
    private String status;

    public UserDTO() {
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public BigDecimal getRatePerPost() { return ratePerPost; }
    public void setRatePerPost(BigDecimal ratePerPost) { this.ratePerPost = ratePerPost; }

    public Integer getMaxDailyPosts() { return maxDailyPosts; }
    public void setMaxDailyPosts(Integer maxDailyPosts) { this.maxDailyPosts = maxDailyPosts; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
