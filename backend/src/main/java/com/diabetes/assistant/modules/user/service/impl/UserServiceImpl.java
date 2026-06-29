package com.diabetes.assistant.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.constants.RoleConstants;
import com.diabetes.assistant.common.constants.StatusConstants;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.DateTimeUtil;
import com.diabetes.assistant.common.utils.JwtUtil;
import com.diabetes.assistant.common.utils.PasswordUtil;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import com.diabetes.assistant.modules.user.dto.LoginRequest;
import com.diabetes.assistant.modules.user.dto.RegisterRequest;
import com.diabetes.assistant.modules.user.dto.ResetPasswordRequest;
import com.diabetes.assistant.modules.user.dto.UpdateUserRequest;
import com.diabetes.assistant.modules.user.dto.UpdateUserStatusRequest;
import com.diabetes.assistant.modules.user.entity.User;
import com.diabetes.assistant.modules.user.mapper.UserMapper;
import com.diabetes.assistant.modules.user.service.EmailVerificationService;
import com.diabetes.assistant.modules.user.service.UserService;
import com.diabetes.assistant.modules.user.vo.LoginResponse;
import com.diabetes.assistant.modules.user.vo.UserResponse;
import com.diabetes.assistant.modules.user.vo.UserStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserQueryApi {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final EmailVerificationService emailVerificationService;

    @Override
    public String entry() {
        return "用户模块功能开发中";
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = required(request == null ? null : request.getUsername(), "username不能为空");
        String password = required(request.getPassword(), "password不能为空");
        String phone = normalize(request.getPhone());
        String email = required(request.getEmail(), "邮箱不能为空");
        String emailCode = required(request.getEmailCode(), "邮箱验证码不能为空");

        if (existsByUsername(username, null)) {
            throw new BusinessException(409, "用户名已存在");
        }
        if (StringUtils.hasText(phone) && existsByPhone(phone, null)) {
            throw new BusinessException(409, "手机号已存在");
        }
        if (existsByEmail(email, null)) {
            throw new BusinessException(409, "邮箱已被注册");
        }
        emailVerificationService.verifyAndConsume(email, EmailVerificationService.PURPOSE_REGISTER, emailCode);

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setPhone(phone);
        user.setEmail(normalize(email));
        user.setAvatar(normalize(request.getAvatar()));
        user.setRole(RoleConstants.PATIENT);
        user.setStatus(StatusConstants.ACTIVE);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        userMapper.insert(user);
        return toResponse(user, true, false, false);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String account = required(request == null ? null : request.getAccount(), "account不能为空");
        String password = required(request.getPassword(), "password不能为空");
        User user = findByAccount(account);
        if (user == null || !PasswordUtil.matches(password, user.getPasswordHash())) {
            throw new BusinessException(400, "账号或密码错误");
        }
        if (StatusConstants.DISABLED.equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginTime(now);
        user.setUpdateTime(now);
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, toResponse(user, false, true, false));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String account = required(request == null ? null : request.getAccount(), "account不能为空");
        String newPassword = required(request.getNewPassword(), "new_password不能为空");
        if (newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BusinessException(400, "新密码长度需为6-32位");
        }
        String email = required(request.getEmail(), "邮箱不能为空");
        String emailCode = required(request.getEmailCode(), "邮箱验证码不能为空");

        User user = findByAccount(account);
        if (user == null) {
            throw new BusinessException(404, "账号不存在");
        }
        if (!email.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(400, "账号与邮箱不匹配，无法重置密码");
        }
        emailVerificationService.verifyAndConsume(email, EmailVerificationService.PURPOSE_RESET_PASSWORD, emailCode);

        user.setPasswordHash(PasswordUtil.hashPassword(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public UserResponse getCurrentUser(Integer userId) {
        User user = getExistingUser(userId);
        return toResponse(user, true, true, true);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(Integer userId, UpdateUserRequest request) {
        User user = getExistingUser(userId);
        UpdateUserRequest safeRequest = request == null ? new UpdateUserRequest() : request;

        if (safeRequest.getUsername() != null) {
            String username = required(safeRequest.getUsername(), "username不能为空");
            if (existsByUsername(username, userId)) {
                throw new BusinessException(409, "用户名已被占用");
            }
            user.setUsername(username);
        }

        if (safeRequest.getPhone() != null) {
            String phone = normalize(safeRequest.getPhone());
            if (StringUtils.hasText(phone) && existsByPhone(phone, userId)) {
                throw new BusinessException(409, "手机号已被占用");
            }
            user.setPhone(phone);
        }
        if (safeRequest.getEmail() != null) {
            user.setEmail(normalize(safeRequest.getEmail()));
        }
        if (safeRequest.getAvatar() != null) {
            user.setAvatar(normalize(safeRequest.getAvatar()));
        }

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return toResponse(user, true, false, true);
    }

    @Override
    public PageResult<UserResponse> listUsers(Integer adminUserId, Integer page, Integer pageSize, String keyword, String role, String status) {
        requireAdmin(adminUserId);
        int currentPage = validPage(page);
        int currentPageSize = validPageSize(pageSize);
        String normalizedRole = normalize(role);
        String normalizedStatus = normalize(status);
        if (StringUtils.hasText(normalizedRole) && !RoleConstants.PATIENT.equals(normalizedRole) && !RoleConstants.ADMIN.equals(normalizedRole)) {
            throw new BusinessException(400, "role参数不合法");
        }
        if (StringUtils.hasText(normalizedStatus) && !StatusConstants.ACTIVE.equals(normalizedStatus) && !StatusConstants.DISABLED.equals(normalizedStatus)) {
            throw new BusinessException(400, "status参数不合法");
        }

        String normalizedKeyword = normalize(keyword);
        Integer keywordUserId = parseInteger(normalizedKeyword);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(StringUtils.hasText(normalizedRole), User::getRole, normalizedRole)
                .eq(StringUtils.hasText(normalizedStatus), User::getStatus, normalizedStatus)
                .and(StringUtils.hasText(normalizedKeyword), nested -> nested
                        .like(User::getUsername, normalizedKeyword)
                        .or()
                        .like(User::getPhone, normalizedKeyword)
                        .or()
                        .like(User::getEmail, normalizedKeyword)
                        .or(keywordUserId != null)
                        .eq(keywordUserId != null, User::getUserId, keywordUserId))
                .orderByDesc(User::getCreateTime)
                .orderByDesc(User::getUserId);

        Page<User> result = userMapper.selectPage(Page.of(currentPage, currentPageSize), wrapper);
        List<UserResponse> list = result.getRecords().stream()
                .map(user -> toResponse(user, true, true, false))
                .toList();
        return new PageResult<>(list, result.getTotal(), currentPage, currentPageSize);
    }

    @Override
    @Transactional
    public UserStatusResponse updateUserStatus(Integer adminUserId, Integer userId, UpdateUserStatusRequest request) {
        requireAdmin(adminUserId);
        if (adminUserId != null && adminUserId.equals(userId)) {
            throw new BusinessException(400, "不能修改当前管理员自身状态");
        }
        String status = required(request == null ? null : request.getStatus(), "status不能为空");
        if (!StatusConstants.ACTIVE.equals(status) && !StatusConstants.DISABLED.equals(status)) {
            throw new BusinessException(400, "status只能是active或disabled");
        }
        User user = getExistingUser(userId);
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return new UserStatusResponse(user.getUserId(), user.getStatus(), DateTimeUtil.format(user.getUpdateTime()));
    }

    @Override
    public UserBasicDTO getUserBasicById(Integer userId) {
        User user = userMapper.selectById(userId);
        return user == null ? null : toBasicDTO(user);
    }

    @Override
    public List<Integer> searchUserIdsByKeyword(String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return List.of();
        }
        Integer keywordUserId = parseInteger(normalizedKeyword);
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .like(User::getUsername, normalizedKeyword)
                        .or()
                        .like(User::getPhone, normalizedKeyword)
                        .or()
                        .like(User::getEmail, normalizedKeyword)
                        .or(keywordUserId != null)
                        .eq(keywordUserId != null, User::getUserId, keywordUserId))
                .stream()
                .map(User::getUserId)
                .toList();
    }

    @Override
    public List<Integer> listUserIdsByKeyword(String keyword) {
        return searchUserIdsByKeyword(keyword);
    }

    @Override
    public boolean existsActiveUser(Integer userId) {
        User user = userMapper.selectById(userId);
        return user != null && StatusConstants.ACTIVE.equals(user.getStatus());
    }

    @Override
    public boolean isAdmin(Integer userId) {
        User user = userMapper.selectById(userId);
        return user != null && RoleConstants.ADMIN.equals(user.getRole()) && StatusConstants.ACTIVE.equals(user.getStatus());
    }

    private void requireAdmin(Integer adminUserId) {
        if (!isAdmin(adminUserId)) {
            throw new BusinessException(403, "无权限访问管理员接口");
        }
    }

    private User getExistingUser(Integer userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private User findByAccount(String account) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, account)
                .or()
                .eq(User::getPhone, account)
                .or()
                .eq(User::getEmail, account)
                .last("LIMIT 1"));
    }

    private boolean existsByUsername(String username, Integer excludeUserId) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .ne(excludeUserId != null, User::getUserId, excludeUserId)) > 0;
    }

    private boolean existsByPhone(String phone, Integer excludeUserId) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .ne(excludeUserId != null, User::getUserId, excludeUserId)) > 0;
    }

    private boolean existsByEmail(String email, Integer excludeUserId) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, normalize(email))
                .ne(excludeUserId != null, User::getUserId, excludeUserId)) > 0;
    }

    private UserResponse toResponse(User user, boolean includeContact, boolean includeLastLogin, boolean includeUpdateTime) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .phone(includeContact ? user.getPhone() : null)
                .email(includeContact ? user.getEmail() : null)
                .avatar(user.getAvatar())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginTime(includeLastLogin ? DateTimeUtil.format(user.getLastLoginTime()) : null)
                .createTime(DateTimeUtil.format(user.getCreateTime()))
                .updateTime(includeUpdateTime ? DateTimeUtil.format(user.getUpdateTime()) : null)
                .build();
    }

    private UserBasicDTO toBasicDTO(User user) {
        UserBasicDTO dto = new UserBasicDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        return dto;
    }

    private String required(String value, String message) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(400, message);
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int validPage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int validPageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, 100);
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
