package fu.de190167.user.common;

import fu.de190167.user.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

/**
 * Maps business status -> HTTP code:
 * 1=OK (200/201) · 2=Validation (406) · 3=Duplicate username (226) · 4=NotFound (404) · 0=Server (500).
 */
public final class ResponseUtil {

    private ResponseUtil() {
    }

    public static ResponseEntity<ApiResponse> toResponse(ApiResponse body, int successHttpCode) {
        int http = switch (body.getStatus()) {
            case UserConstants.SUCCESS -> successHttpCode;
            case UserConstants.VALIDATION_FAILED -> 406;
            case UserConstants.DUPLICATE_USERNAME -> 226;
            case UserConstants.NOT_FOUND -> 404;
            default -> 500;
        };
        return ResponseEntity.status(http).body(body);
    }
}
