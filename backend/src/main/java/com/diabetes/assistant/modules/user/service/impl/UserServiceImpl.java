package com.diabetes.assistant.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import com.diabetes.assistant.modules.user.entity.User;
import com.diabetes.assistant.modules.user.mapper.UserMapper;
import com.diabetes.assistant.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserQueryApi {

    private final UserMapper userMapper;

    @Override
    public String entry() {
        return "用户模块功能开发中";
    }

    @Override
    public UserBasicDTO getUserBasicById(Integer userId) {
        User user = userMapper.selectById(userId);
        return user == null ? null : toBasicDto(user);
    }

    @Override
    public boolean existsActiveUser(Integer userId) {
        User user = userMapper.selectById(userId);
        return user != null && "active".equals(user.getStatus());
    }

    @Override
    public boolean isAdmin(Integer userId) {
        User user = userMapper.selectById(userId);
        return user != null && "admin".equals(user.getRole());
    }

    @Override
    public List<Integer> listUserIdsByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .like(User::getUsername, keyword)
                        .or()
                        .like(User::getPhone, keyword))
                .stream()
                .map(User::getUserId)
                .toList();
    }

    private UserBasicDTO toBasicDto(User user) {
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
