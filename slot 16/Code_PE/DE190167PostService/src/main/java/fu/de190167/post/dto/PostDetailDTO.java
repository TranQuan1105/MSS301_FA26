package fu.de190167.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fu.de190167.post.common.PostConstants;

import java.math.BigDecimal;
import java.util.Date;

/** Same as PostDTO but userId is replaced by the full user object from UserService. */
public class PostDetailDTO {

    private Long postId;
    private String advertiserName;
    private String advertiserEmail;
    private String advertiserPhone;

    @JsonFormat(pattern = PostConstants.DATE_PATTERN, timezone = PostConstants.TIME_ZONE)
    private Date startDate;

    @JsonFormat(pattern = PostConstants.DATE_PATTERN, timezone = PostConstants.TIME_ZONE)
    private Date endDate;

    private Integer numberOfPosts;
    private BigDecimal totalCost;
    private String status;
    private UserDTO user;

    public PostDetailDTO() {
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

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
}
