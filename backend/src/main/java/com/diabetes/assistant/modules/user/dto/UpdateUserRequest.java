package com.diabetes.assistant.modules.user.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {

    private String username;
    private String phone;
    private String email;
    private String avatar;
}
