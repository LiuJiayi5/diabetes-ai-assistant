package com.diabetes.assistant.modules.profile.service.impl;

import com.diabetes.assistant.modules.profile.service.ProfileService;
import org.springframework.stereotype.Service;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Override
    public String entry() {
        return "健康档案模块功能开发中";
    }
}
