package com.diabetes.assistant.modules.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.PageUtils;
import com.diabetes.assistant.modules.profile.contract.PatientProfileQueryApi;
import com.diabetes.assistant.modules.profile.contract.dto.PatientProfileDTO;
import com.diabetes.assistant.modules.profile.dto.AdminProfileListItem;
import com.diabetes.assistant.modules.profile.dto.SaveProfileRequest;
import com.diabetes.assistant.modules.profile.dto.SaveProfileResponse;
import com.diabetes.assistant.modules.profile.entity.PatientProfile;
import com.diabetes.assistant.modules.profile.mapper.PatientProfileMapper;
import com.diabetes.assistant.modules.profile.service.ProfileService;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService, PatientProfileQueryApi {

    private final PatientProfileMapper patientProfileMapper;
    private final UserQueryApi userQueryApi;

    @Override
    public PatientProfileDTO getMyProfile(Integer userId) {
        return getProfileByUserId(userId);
    }

    @Override
    public SaveProfileResponse saveProfile(Integer userId, SaveProfileRequest request) {
        if (!userQueryApi.existsActiveUser(userId)) {
            throw new BusinessException(400, "用户不存在或已停用");
        }

        PatientProfile existing = findByUserId(userId);
        PatientProfile profile = existing == null ? new PatientProfile() : existing;
        profile.setUserId(userId);
        profile.setAge(request.getAge());
        profile.setGender(request.getGender());
        profile.setHeightCm(request.getHeightCm());
        profile.setBaseWeightKg(request.getBaseWeightKg());
        profile.setBaseWaistCm(request.getBaseWaistCm());
        profile.setFamilyHistory(request.getFamilyHistory());
        profile.setChronicHistory(request.getChronicHistory());
        profile.setAllergyHistory(request.getAllergyHistory());

        if (existing == null) {
            patientProfileMapper.insert(profile);
        } else {
            patientProfileMapper.updateById(profile);
        }

        PatientProfile saved = findByUserId(userId);
        return new SaveProfileResponse(saved.getProfileId(), saved.getUpdateTime());
    }

    @Override
    public PageResult<AdminProfileListItem> adminListProfiles(String keyword, String gender,
                                                              Integer minAge, Integer maxAge,
                                                              Integer page, Integer pageSize) {
        int currentPage = PageUtils.normalizePage(page);
        int size = PageUtils.normalizePageSize(pageSize);

        LambdaQueryWrapper<PatientProfile> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(gender)) {
            wrapper.eq(PatientProfile::getGender, gender);
        }
        if (minAge != null) {
            wrapper.ge(PatientProfile::getAge, minAge);
        }
        if (maxAge != null) {
            wrapper.le(PatientProfile::getAge, maxAge);
        }
        if (StringUtils.hasText(keyword)) {
            List<Integer> userIds = userQueryApi.listUserIdsByKeyword(keyword);
            if (userIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, currentPage, size);
            }
            wrapper.in(PatientProfile::getUserId, userIds);
        }
        wrapper.orderByDesc(PatientProfile::getUpdateTime);

        Page<PatientProfile> pageResult = patientProfileMapper.selectPage(new Page<>(currentPage, size), wrapper);
        List<AdminProfileListItem> list = pageResult.getRecords().stream()
                .map(this::toAdminListItem)
                .toList();
        return new PageResult<>(list, pageResult.getTotal(), currentPage, size);
    }

    @Override
    public PatientProfileDTO adminGetProfileDetail(Integer userId) {
        return getProfileByUserId(userId);
    }

    @Override
    public PatientProfileDTO getProfileByUserId(Integer userId) {
        PatientProfile profile = findByUserId(userId);
        return profile == null ? null : toDto(profile);
    }

    @Override
    public String getProfileSummaryByUserId(Integer userId) {
        PatientProfile profile = findByUserId(userId);
        return profile == null ? null : buildSummary(profile);
    }

    @Override
    public boolean hasProfile(Integer userId) {
        return findByUserId(userId) != null;
    }

    private PatientProfile findByUserId(Integer userId) {
        return patientProfileMapper.selectOne(new LambdaQueryWrapper<PatientProfile>()
                .eq(PatientProfile::getUserId, userId)
                .last("LIMIT 1"));
    }

    private PatientProfileDTO toDto(PatientProfile profile) {
        PatientProfileDTO dto = new PatientProfileDTO();
        dto.setProfileId(profile.getProfileId());
        dto.setUserId(profile.getUserId());
        dto.setAge(profile.getAge());
        dto.setGender(profile.getGender());
        dto.setHeightCm(profile.getHeightCm());
        dto.setBaseWeightKg(profile.getBaseWeightKg());
        dto.setBaseWaistCm(profile.getBaseWaistCm());
        dto.setFamilyHistory(profile.getFamilyHistory());
        dto.setChronicHistory(profile.getChronicHistory());
        dto.setAllergyHistory(profile.getAllergyHistory());
        dto.setProfileSummary(buildSummary(profile));
        dto.setCreateTime(profile.getCreateTime());
        dto.setUpdateTime(profile.getUpdateTime());
        return dto;
    }

    private AdminProfileListItem toAdminListItem(PatientProfile profile) {
        AdminProfileListItem item = new AdminProfileListItem();
        item.setProfileId(profile.getProfileId());
        item.setUserId(profile.getUserId());
        item.setAge(profile.getAge());
        item.setGender(profile.getGender());
        item.setHeightCm(profile.getHeightCm());
        item.setBaseWeightKg(profile.getBaseWeightKg());
        item.setUpdateTime(profile.getUpdateTime());

        UserBasicDTO user = userQueryApi.getUserBasicById(profile.getUserId());
        if (user != null) {
            item.setUsername(user.getUsername());
        }
        return item;
    }

    private String buildSummary(PatientProfile profile) {
        StringBuilder summary = new StringBuilder();
        if (profile.getAge() != null) {
            summary.append("年龄").append(profile.getAge()).append("岁");
        }
        if (StringUtils.hasText(profile.getGender())) {
            if (!summary.isEmpty()) {
                summary.append("，");
            }
            summary.append("性别").append(profile.getGender());
        }
        if (profile.getHeightCm() != null) {
            if (!summary.isEmpty()) {
                summary.append("，");
            }
            summary.append("身高").append(profile.getHeightCm()).append("cm");
        }
        if (profile.getBaseWeightKg() != null) {
            if (!summary.isEmpty()) {
                summary.append("，");
            }
            summary.append("基础体重").append(profile.getBaseWeightKg()).append("kg");
        }
        if (StringUtils.hasText(profile.getFamilyHistory())) {
            if (!summary.isEmpty()) {
                summary.append("，");
            }
            summary.append("家族病史：").append(profile.getFamilyHistory());
        }
        return summary.isEmpty() ? null : summary.toString();
    }
}
