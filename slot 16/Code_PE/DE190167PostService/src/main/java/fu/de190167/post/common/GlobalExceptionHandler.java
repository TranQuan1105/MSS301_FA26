package fu.de190167.post.common;

import fu.de190167.post.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid on request body failed -> 406 / status 2
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return validationFailed(msg);
    }

    // Malformed JSON or bad date format (not dd/MM/yyyy) -> 406 / status 2
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return validationFailed("Invalid request body (dates must be dd/MM/yyyy)");
    }

    // Path/query param of wrong type (e.g. /api/posts/abc, status=XXX) -> 406 / status 2
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ApiResponse> handleBadParam(Exception ex) {
        return validationFailed("Invalid request parameter");
    }

    // Anything else, including UserService being down (FeignException other than 404)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleAll(Exception ex) {
        return ResponseEntity.status(500)
                .body(ApiResponse.error(PostConstants.SERVER_ERROR, "Internal server error: " + ex.getMessage()));
    }

    private ResponseEntity<ApiResponse> validationFailed(String msg) {
        return ResponseEntity.status(406)
                .body(ApiResponse.error(PostConstants.VALIDATION_FAILED, "Validation failed: " + msg));
    }
}
