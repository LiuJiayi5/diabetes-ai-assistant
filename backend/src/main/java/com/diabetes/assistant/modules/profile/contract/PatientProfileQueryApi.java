package com.diabetes.assistant.modules.profile.contract;

import com.diabetes.assistant.modules.profile.contract.dto.PatientProfileDTO;

public interface PatientProfileQueryApi {

    PatientProfileDTO getProfileByUserId(Integer userId);

    String getProfileSummaryByUserId(Integer userId);

    boolean hasProfile(Integer userId);
}
