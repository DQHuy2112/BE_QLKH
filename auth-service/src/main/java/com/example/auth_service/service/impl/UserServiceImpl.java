package com.example.auth_service.service.impl;

import com.example.auth_service.dto.CreateUserRequest;
import com.example.auth_service.dto.ResetPasswordRequest;
import com.example.auth_service.dto.UpdateUserPermissionsRequest;
import com.example.auth_service.dto.UpdateUserRequest;
import com.example.auth_service.dto.UserDto;
import com.example.auth_service.entity.AdPermission;
import com.example.auth_service.entity.AdRole;
import com.example.auth_service.entity.AdUser;
import com.example.auth_service.exception.NotFoundException;
import com.example.auth_service.exception.DuplicateException;
import com.example.auth_service.exception.ValidationException;
import com.example.auth_service.repository.AdPermissionRepository;
import com.example.auth_service.repository.AdRoleRepository;
import com.example.auth_service.repository.AdUserRepository;
import com.example.auth_service.service.EmailService;
import com.example.auth_service.service.RefreshTokenService;
import com.example.auth_service.service.UserService;
import com.example.auth_service.util.ChangeLogUtils;
import com.example.auth_service.util.ActivityLogHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private final AdUserRepository userRepository;
    private final AdRoleRepository roleRepository;
    private final AdPermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogHelper activityLogHelper;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    public UserServiceImpl(AdUserRepository userRepository,
                          AdRoleRepository roleRepository,
                          AdPermissionRepository permissionRepository,
                          PasswordEncoder passwordEncoder,
                          ActivityLogHelper activityLogHelper,
                          EmailService emailService,
                          RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogHelper = activityLogHelper;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public Page<UserDto> searchUsers(String username, String email, String phone, Boolean active, Long roleId, Pageable pageable) {
        Page<AdUser> users = userRepository.searchUsers(username, email, phone, active, roleId, pageable);
        return users.map(UserDto::fromEntity);
    }

    @Override
    public UserDto getUserById(Long id) {
        AdUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        return UserDto.fromEntity(user);
    }

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateException("Tên đăng nhập đã tồn tại: " + request.getUsername());
        }

        AdUser user = new AdUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setProvince(request.getProvince());
        user.setDistrict(request.getDistrict());
        user.setWard(request.getWard());
        user.setCountry(request.getCountry());
        user.setActive(request.getActive() != null ? request.getActive() : true);
        // Email verification defaults
        user.setEmailVerified(false);
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String token = generateOpaqueToken();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, 24);
            user.setEmailVerificationToken(token);
            user.setEmailVerificationTokenExpiry(cal.getTime());
        }
        // Khởi tạo thông tin bảo mật đăng nhập
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        // Set roles if provided
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<AdRole> roles = new HashSet<>();
            for (Long roleId : request.getRoleIds()) {
                roleRepository.findById(roleId).ifPresent(roles::add);
            }
            user.setRoles(roles);
        }

        AdUser savedUser = userRepository.save(user);

        // Send verification email if possible
        if (savedUser.getEmail() != null
                && !savedUser.getEmail().isBlank()
                && savedUser.getEmailVerificationToken() != null
                && !Boolean.TRUE.equals(savedUser.getEmailVerified())) {
            emailService.sendEmailVerification(savedUser.getEmail(), savedUser.getEmailVerificationToken(), savedUser.getUsername());
        }
        
        // Log activity
        activityLogHelper.logActivity(
            "CREATE_USER",
            "USER",
            savedUser.getId(),
            savedUser.getUsername(),
            String.format("Created new user: %s", savedUser.getUsername())
        );
        
        return UserDto.fromEntity(savedUser);
    }

    private String generateOpaqueToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        AdUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        // Snapshot trước khi cập nhật để log before/after
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("username", user.getUsername());
        before.put("firstName", user.getFirstName());
        before.put("lastName", user.getLastName());
        before.put("email", user.getEmail());
        before.put("phone", user.getPhone());
        before.put("address", user.getAddress());
        before.put("province", user.getProvince());
        before.put("district", user.getDistrict());
        before.put("ward", user.getWard());
        before.put("country", user.getCountry());
        before.put("active", user.getActive());

        if (request.getUsername() != null) {
            // Check if new username already exists (excluding current user)
            userRepository.findByUsername(request.getUsername())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(id)) {
                            throw new DuplicateException("Tên đăng nhập đã tồn tại: " + request.getUsername());
                        }
                    });
            user.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getProvince() != null) {
            user.setProvince(request.getProvince());
        }
        if (request.getDistrict() != null) {
            user.setDistrict(request.getDistrict());
        }
        if (request.getWard() != null) {
            user.setWard(request.getWard());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        // Update roles if provided
        if (request.getRoleIds() != null) {
            Set<AdRole> roles = new HashSet<>();
            for (Long roleId : request.getRoleIds()) {
                roleRepository.findById(roleId).ifPresent(roles::add);
            }
            user.setRoles(roles);
        }

        user.setUpdatedAt(new Date());
        AdUser updatedUser = userRepository.save(user);

        // Snapshot sau khi cập nhật
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("username", updatedUser.getUsername());
        after.put("firstName", updatedUser.getFirstName());
        after.put("lastName", updatedUser.getLastName());
        after.put("email", updatedUser.getEmail());
        after.put("phone", updatedUser.getPhone());
        after.put("address", updatedUser.getAddress());
        after.put("province", updatedUser.getProvince());
        after.put("district", updatedUser.getDistrict());
        after.put("ward", updatedUser.getWard());
        after.put("country", updatedUser.getCountry());
        after.put("active", updatedUser.getActive());

        String details = ChangeLogUtils.buildChangeDetails(before, after);

        // Log activity với chi tiết before/after
        activityLogHelper.logActivity(
                "UPDATE_USER",
                "USER",
                updatedUser.getId(),
                updatedUser.getUsername(),
                details
        );

        return UserDto.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        AdUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        
        String username = user.getUsername();
        Long userId = user.getId();
        
        userRepository.delete(user);
        
        // Log activity
        activityLogHelper.logActivity(
            userId,
            username,
            "DELETE_USER",
            "USER",
            id,
            username,
            String.format("Deleted user: %s", username)
        );
    }

    @Override
    @Transactional
    public String resetUserPassword(Long id, ResetPasswordRequest request) {
        AdUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        String newPassword;
        if (request.getGenerateRandomPassword() != null && request.getGenerateRandomPassword()) {
            // Generate random password
            newPassword = generateRandomPassword();
        } else if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            newPassword = request.getNewPassword();
        } else {
            throw new ValidationException("Vui lòng cung cấp mật khẩu mới hoặc chọn tạo mật khẩu ngẫu nhiên");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(new Date());
        userRepository.save(user);
        // revoke refresh tokens so old sessions cannot continue after password reset
        refreshTokenService.revokeAllUserTokens(user.getId());
        
        // Log activity
        activityLogHelper.logActivity(
            "RESET_PASSWORD",
            "USER",
            user.getId(),
            user.getUsername(),
            String.format("Reset password for user: %s", user.getUsername())
        );

        return newPassword; // Return the password so admin can share it with user
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    @Override
    @Transactional
    public UserDto updateUserPermissions(Long id, UpdateUserPermissionsRequest request) {
        AdUser user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        Set<AdPermission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            for (Long permissionId : request.getPermissionIds()) {
                permissionRepository.findById(permissionId).ifPresent(permissions::add);
            }
        }
        user.setPermissions(permissions);
        user.setUpdatedAt(new Date());

        AdUser updatedUser = userRepository.save(user);
        
        // Log activity
        activityLogHelper.logActivity(
            "UPDATE_USER_PERMISSIONS",
            "USER",
            updatedUser.getId(),
            updatedUser.getUsername(),
            String.format("Updated direct permissions for user: %s", updatedUser.getUsername())
        );
        
        return UserDto.fromEntity(updatedUser);
    }
}

