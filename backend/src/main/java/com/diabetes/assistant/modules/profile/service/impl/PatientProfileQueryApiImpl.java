package com.diabetes.assistant.modules.profile.service.impl;

import com.diabetes.assistant.modules.profile.contract.PatientProfileQueryApi;
import com.diabetes.assistant.modules.profile.contract.dto.PatientProfileDTO;
import org.springframework.stereotype.Service;

@Service
public class PatientProfileQueryApiImpl implements PatientProfileQueryApi {

    @Override
    public PatientProfileDTO getProfileByUserId(Integer userId) {
        return null;
    }

    @Override
    public String getProfileSummaryByUserId(Integer userId) {
        return "No profile data";
    }

    @Override
    public boolean hasProfile(Integer userId) {
        return false;
    }
}
