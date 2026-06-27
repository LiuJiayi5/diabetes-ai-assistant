package com.diabetes.assistant.modules.user.contract;

import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;

import java.util.List;

public interface UserQueryApi {

    UserBasicDTO getUserBasicById(Integer userId);

    List<Integer> searchUserIdsByKeyword(String keyword);

    boolean existsActiveUser(Integer userId);

    boolean isAdmin(Integer userId);

    List<Integer> listUserIdsByKeyword(String keyword);
}
