package com.diabetes.assistant.modules.user.service.impl;

import com.diabetes.assistant.modules.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public String entry() {
        return "用户模块功能开发中";
    }
}
