package com.diabetes.assistant.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import com.diabetes.assistant.modules.user.entity.User;
import com.diabetes.assistant.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserQueryApiImpl implements UserQueryApi {

    private static final String ROLE_ADMIN = "admin";
    private static final String STATUS_ACTIVE = "active";

    private final UserMapper userMapper;

    @Override
    public UserBasicDTO getUserBasicById(Integer userId) {
        User user = userMapper.selectById(userId);
        return user == null ? null : toDTO(user);
    }

    @Override
    public boolean existsActiveUser(Integer userId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUserId, userId)
                .eq(User::getStatus, STATUS_ACTIVE));
        return count != null && count > 0;
    }

    @Override
    public boolean isAdmin(Integer userId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUserId, userId)
                .eq(User::getRole, ROLE_ADMIN)
                .eq(User::getStatus, STATUS_ACTIVE));
        return count != null && count > 0;
    }

    private UserBasicDTO toDTO(User user) {
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
}
