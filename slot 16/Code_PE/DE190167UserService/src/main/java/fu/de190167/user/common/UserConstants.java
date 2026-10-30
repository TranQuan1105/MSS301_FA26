package fu.de190167.user.common;

public final class UserConstants {

    private UserConstants() {
    }

    public static final String ROLE_REGEX = "USER|CREATOR|ADMIN";
    public static final String STATUS_REGEX = "ACTIVE|SUSPENDED|DEACTIVATED";

    // Business status codes (ApiResponse.status)
    public static final int SERVER_ERROR = 0;
    public static final int SUCCESS = 1;
    public static final int VALIDATION_FAILED = 2;
    public static final int DUPLICATE_USERNAME = 3;
    public static final int NOT_FOUND = 4;

    public static final int MAX_PAGE_SIZE = 100;
}
