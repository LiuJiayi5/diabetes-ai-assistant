package com.diabetes.assistant.modules.content.service.impl;

import com.diabetes.assistant.modules.content.service.ContentService;
import org.springframework.stereotype.Service;

@Service
public class ContentServiceImpl implements ContentService {

    @Override
    public String entry() {
        return "内容模块功能开发中";
    }
}
