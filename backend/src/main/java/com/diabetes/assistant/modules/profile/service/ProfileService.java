package com.diabetes.assistant.modules.profile.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.profile.contract.dto.PatientProfileDTO;
import com.diabetes.assistant.modules.profile.dto.AdminProfileListItem;
import com.diabetes.assistant.modules.profile.dto.SaveProfileRequest;
import com.diabetes.assistant.modules.profile.dto.SaveProfileResponse;

public interface ProfileService {

    PatientProfileDTO getMyProfile();

    SaveProfileResponse saveProfile(SaveProfileRequest request);

    PageResult<AdminProfileListItem> adminListProfiles(String keyword, String gender,
                                                       Integer minAge, Integer maxAge,
                                                       Integer page, Integer pageSize);

    PatientProfileDTO adminGetProfileDetail(Integer userId);
}
