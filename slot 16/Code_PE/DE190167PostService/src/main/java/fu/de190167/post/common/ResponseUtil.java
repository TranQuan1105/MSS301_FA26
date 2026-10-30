package fu.de190167.post.common;

import fu.de190167.post.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

/**
 * Maps business status -> HTTP code:
 * 1=OK (200/201) · 2=Validation (406) · 4=NotFound (404) · 5=User not ACTIVE (400) · 0=Server (500).
 */
public final class ResponseUtil {

    private ResponseUtil() {
    }

    public static ResponseEntity<ApiResponse> toResponse(ApiResponse body, int successHttpCode) {
        int http = switch (body.getStatus()) {
            case PostConstants.SUCCESS -> successHttpCode;
            case PostConstants.VALIDATION_FAILED -> 406;
            case PostConstants.NOT_FOUND -> 404;
            case PostConstants.USER_NOT_ACTIVE -> 400;
            default -> 500;
        };
        return ResponseEntity.status(http).body(body);
    }
}
