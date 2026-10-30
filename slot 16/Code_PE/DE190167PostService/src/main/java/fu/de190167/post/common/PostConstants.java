package fu.de190167.post.common;

public final class PostConstants {

    private PostConstants() {
    }

    public static final String STATUS_REGEX = "DRAFT|SCHEDULED|PUBLISHED|CANCELLED";

    // User status coming from UserService
    public static final String USER_ACTIVE = "ACTIVE";

    // Dates are "dd/MM/yyyy" without time; parse and compute in the same zone to avoid off-by-one days
    public static final String DATE_PATTERN = "dd/MM/yyyy";
    public static final String TIME_ZONE = "Asia/Ho_Chi_Minh";

    // Business status codes (ApiResponse.status)
    public static final int SERVER_ERROR = 0;
    public static final int SUCCESS = 1;
    public static final int VALIDATION_FAILED = 2;
    public static final int NOT_FOUND = 4;
    public static final int USER_NOT_ACTIVE = 5;

    public static final int MAX_PAGE_SIZE = 100;
}
