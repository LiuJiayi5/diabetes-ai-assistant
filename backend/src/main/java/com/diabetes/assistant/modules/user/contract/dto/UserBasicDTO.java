package com.diabetes.assistant.modules.user.contract.dto;

import lombok.Data;

@Data
public class UserBasicDTO {

    private Integer userId;
    private String username;
    private String phone;
    private String email;
    private String avatar;
    private String role;
    private String status;
}
