package fu.de190167.post.client;

import fu.de190167.post.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Calls UserService. A 404 from UserService surfaces as FeignException.NotFound. */
@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserClient {

    @GetMapping("/api/users/{userId}")
    ApiResponse getUserById(@PathVariable("userId") Long userId);
}
