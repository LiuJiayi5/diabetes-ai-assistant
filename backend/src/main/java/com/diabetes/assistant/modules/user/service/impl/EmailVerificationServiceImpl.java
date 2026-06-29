package com.diabetes.assistant.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.utils.PasswordUtil;
import com.diabetes.assistant.modules.user.dto.SendEmailCodeRequest;
import com.diabetes.assistant.modules.user.entity.EmailVerificationCode;
import com.diabetes.assistant.modules.user.entity.User;
import com.diabetes.assistant.modules.user.mapper.EmailVerificationCodeMapper;
import com.diabetes.assistant.modules.user.mapper.UserMapper;
import com.diabetes.assistant.modules.user.service.EmailVerificationService;
import com.diabetes.assistant.modules.user.vo.SendEmailCodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int CODE_TTL_SECONDS = 300;
    private static final int RESEND_SECONDS = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationCodeMapper codeMapper;
    private final UserMapper userMapper;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Override
    @Transactional
    public SendEmailCodeResponse sendCode(SendEmailCodeRequest request) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        String purpose = normalizePurpose(request == null ? null : request.getPurpose());
        validateEmailForPurpose(email, purpose);
        assertCanSend(email, purpose);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationCode entity = new EmailVerificationCode();
        entity.setEmail(email);
        entity.setPurpose(purpose);
        entity.setCodeHash(PasswordUtil.hashPassword(code));
        entity.setUsed(false);
        entity.setExpiresAt(now.plusSeconds(CODE_TTL_SECONDS));
        entity.setCreateTime(now);
        codeMapper.insert(entity);

        boolean sent = sendMailIfConfigured(email, purpose, code);
        return new SendEmailCodeResponse(email, purpose, CODE_TTL_SECONDS, sent ? null : code);
    }

    @Override
    @Transactional
    public void verifyAndConsume(String email, String purpose, String code) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPurpose = normalizePurpose(purpose);
        String normalizedCode = required(code, "邮箱验证码不能为空");

        EmailVerificationCode latest = codeMapper.selectOne(new LambdaQueryWrapper<EmailVerificationCode>()
                .eq(EmailVerificationCode::getEmail, normalizedEmail)
                .eq(EmailVerificationCode::getPurpose, normalizedPurpose)
                .eq(EmailVerificationCode::getUsed, false)
                .orderByDesc(EmailVerificationCode::getCreateTime)
                .last("LIMIT 1"));
        if (latest == null) {
            throw new BusinessException(400, "请先获取邮箱验证码");
        }
        if (latest.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "邮箱验证码已过期，请重新获取");
        }
        if (!PasswordUtil.matches(normalizedCode, latest.getCodeHash())) {
            throw new BusinessException(400, "邮箱验证码错误");
        }
        latest.setUsed(true);
        latest.setUsedTime(LocalDateTime.now());
        codeMapper.updateById(latest);
    }

    private void validateEmailForPurpose(String email, String purpose) {
        boolean exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)) > 0;
        if (PURPOSE_REGISTER.equals(purpose) && exists) {
            throw new BusinessException(409, "邮箱已被注册");
        }
        if (PURPOSE_RESET_PASSWORD.equals(purpose) && !exists) {
            throw new BusinessException(404, "该邮箱未绑定账号");
        }
    }

    private void assertCanSend(String email, String purpose) {
        EmailVerificationCode latest = codeMapper.selectOne(new LambdaQueryWrapper<EmailVerificationCode>()
                .eq(EmailVerificationCode::getEmail, email)
                .eq(EmailVerificationCode::getPurpose, purpose)
                .orderByDesc(EmailVerificationCode::getCreateTime)
                .last("LIMIT 1"));
        if (latest == null) {
            return;
        }
        long seconds = Duration.between(latest.getCreateTime(), LocalDateTime.now()).getSeconds();
        if (seconds < RESEND_SECONDS) {
            throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
        }
    }

    private boolean sendMailIfConfigured(String email, String purpose, String code) {
        if (!StringUtils.hasText(mailPassword)) {
            return false;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject(PURPOSE_REGISTER.equals(purpose) ? "糖尿病预治助手注册验证码" : "糖尿病预治助手重置密码验证码");
        message.setText("你的邮箱验证码是：" + code + "。验证码5分钟内有效，请勿转发给他人。");
        try {
            mailSender.send(message);
            return true;
        } catch (MailException exception) {
            throw new BusinessException(502, "邮箱验证码发送失败，请检查SMTP配置");
        }
    }

    private String normalizePurpose(String purpose) {
        String normalized = required(purpose, "purpose不能为空").toLowerCase(Locale.ROOT);
        if (!PURPOSE_REGISTER.equals(normalized) && !PURPOSE_RESET_PASSWORD.equals(normalized)) {
            throw new BusinessException(400, "purpose只能是register或reset_password");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = required(email, "email不能为空").toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(400, "邮箱格式不正确");
        }
        return normalized;
    }

    private String required(String value, String message) {
        String normalized = value == null ? null : value.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(400, message);
        }
        return normalized;
    }
}
