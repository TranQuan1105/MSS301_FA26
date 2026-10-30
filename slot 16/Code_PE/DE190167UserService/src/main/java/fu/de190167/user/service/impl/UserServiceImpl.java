package fu.de190167.user.service.impl;

import fu.de190167.user.common.UserConstants;
import fu.de190167.user.dto.ApiResponse;
import fu.de190167.user.dto.PageDTO;
import fu.de190167.user.dto.UserDTO;
import fu.de190167.user.entity.Role;
import fu.de190167.user.entity.User;
import fu.de190167.user.entity.UserStatus;
import fu.de190167.user.repository.UserRepository;
import fu.de190167.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ApiResponse createUser(UserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            return ApiResponse.error(UserConstants.DUPLICATE_USERNAME, "Username is duplicated");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setRole(Role.valueOf(dto.getRole()));
        user.setRatePerPost(dto.getRatePerPost());
        user.setMaxDailyPosts(dto.getMaxDailyPosts());
        user.setLevel(dto.getLevel());
        // Default status is ACTIVE when not provided
        user.setStatus(dto.getStatus() == null ? UserStatus.ACTIVE : UserStatus.valueOf(dto.getStatus()));

        User saved = userRepository.save(user);
        return ApiResponse.ok("User created successfully", toDTO(saved));
    }

    @Override
    @Transactional
    public ApiResponse updateUser(Long userId, UserDTO dto) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ApiResponse.error(UserConstants.NOT_FOUND, "User is not found");
        }
        if (dto.getUsername() != null
                && userRepository.existsByUsernameAndUserIdNot(dto.getUsername(), userId)) {
            return ApiResponse.error(UserConstants.DUPLICATE_USERNAME, "Username is duplicated");
        }

        // Partial update: only non-null fields
        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getRole() != null) user.setRole(Role.valueOf(dto.getRole()));
        if (dto.getRatePerPost() != null) user.setRatePerPost(dto.getRatePerPost());
        if (dto.getMaxDailyPosts() != null) user.setMaxDailyPosts(dto.getMaxDailyPosts());
        if (dto.getLevel() != null) user.setLevel(dto.getLevel());
        if (dto.getStatus() != null) user.setStatus(UserStatus.valueOf(dto.getStatus()));

        User saved = userRepository.save(user);
        return ApiResponse.ok("User updated successfully", toDTO(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse getUserById(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ApiResponse.error(UserConstants.NOT_FOUND, "User is not found");
        }
        return ApiResponse.ok("Successful", toDTO(user));
    }

    @Override
    @Transactional
    public ApiResponse deactivateUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ApiResponse.error(UserConstants.NOT_FOUND, "User is not found");
        }
        user.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(user);
        return ApiResponse.ok("User is set to DEACTIVATED successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse getUserList(Role role, UserStatus status, Pageable pageable) {
        Page<User> page = userRepository.search(role, status, pageable);
        List<UserDTO> content = page.getContent().stream().map(this::toDTO).toList();
        PageDTO pageDTO = new PageDTO(
                page.getSize(), page.getNumber(), page.getTotalPages(),
                page.getTotalElements(), page.isFirst(), page.isLast(), content);
        return ApiResponse.ok("Successful", pageDTO);
    }

    private UserDTO toDTO(User u) {
        UserDTO d = new UserDTO();
        d.setUserId(u.getUserId());
        d.setUsername(u.getUsername());
        d.setEmail(u.getEmail());
        d.setRole(u.getRole().name());
        d.setRatePerPost(u.getRatePerPost());
        d.setMaxDailyPosts(u.getMaxDailyPosts());
        d.setLevel(u.getLevel());
        d.setStatus(u.getStatus().name());
        return d;
    }
}
