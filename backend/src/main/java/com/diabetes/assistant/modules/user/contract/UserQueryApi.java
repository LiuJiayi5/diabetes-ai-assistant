package com.diabetes.assistant.modules.user.contract;

import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;

public interface UserQueryApi {

    UserBasicDTO getUserBasicById(Integer userId);

    boolean existsActiveUser(Integer userId);

    boolean isAdmin(Integer userId);
}
