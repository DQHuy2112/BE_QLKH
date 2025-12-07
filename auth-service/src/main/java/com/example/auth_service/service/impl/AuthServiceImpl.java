package com.example.auth_service.service.impl;

import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.LoginResponse;
import com.example.auth_service.dto.UpdateProfileRequest;
import com.example.auth_service.dto.UserProfileDto;
import com.example.auth_service.entity.AdUser;
import com.example.auth_service.exception.NotFoundException;
import com.example.auth_service.repository.AdUserRepository;
import com.example.auth_service.security.JwtService;
import com.example.auth_service.service.AuthService;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AdUserRepository userRepository;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           AdUserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // lấy UserDetails từ Authentication
        org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) auth.getPrincipal();

        String token = jwtService.generateToken(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new LoginResponse(token, userDetails.getUsername(), roles);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile(String username) {
        AdUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        return UserProfileDto.fromEntity(user);
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(String username, UpdateProfileRequest request) {
        AdUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));

        // Cập nhật các trường có thể chỉnh sửa
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
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        user.setUpdatedAt(new Date());
        user = userRepository.save(user);

        return UserProfileDto.fromEntity(user);
    }

    @Override
    @Transactional
    public void deleteAccount(String username) {
        AdUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        
        // Thay vì xóa, disable tài khoản để giữ lại dữ liệu
        user.setActive(false);
        user.setUpdatedAt(new Date());
        userRepository.save(user);
    }
}