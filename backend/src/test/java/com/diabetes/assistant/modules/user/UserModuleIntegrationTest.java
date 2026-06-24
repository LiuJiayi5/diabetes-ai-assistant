package com.diabetes.assistant.modules.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.assistant.common.constants.RoleConstants;
import com.diabetes.assistant.common.constants.StatusConstants;
import com.diabetes.assistant.common.utils.JwtUtil;
import com.diabetes.assistant.common.utils.PasswordUtil;
import com.diabetes.assistant.modules.user.entity.User;
import com.diabetes.assistant.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private Integer patientId;
    private Integer adminId;

    @BeforeEach
    void setUp() {
        userMapper.delete(null);
        patientId = insertUser("patient1", "13800000001", "patient1@example.com", RoleConstants.PATIENT, StatusConstants.ACTIVE, "123456");
        adminId = insertUser("admin", "13900000000", "admin@example.com", RoleConstants.ADMIN, StatusConstants.ACTIVE, "admin123");
    }

    @Test
    void registerSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"new_patient","password":"123456","phone":"13800000002","email":"new@example.com","avatar":"/uploads/avatar/default.png"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user_id").exists())
                .andExpect(jsonPath("$.data.username").value("new_patient"))
                .andExpect(jsonPath("$.data.role").value("patient"))
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.password_hash").doesNotExist());
    }

    @Test
    void registerMissingUsernameReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void registerMissingPasswordReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_patient\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void registerDuplicateUsernameReturns409() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"patient1\",\"password\":\"123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void registerDuplicatePhoneReturns409() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_patient\",\"password\":\"123456\",\"phone\":\"13800000001\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void passwordStoredAsHash() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hash_user\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());

        User user = selectByUsername("hash_user");
        assertThat(user.getPasswordHash()).isNotEqualTo("123456");
        assertThat(PasswordUtil.matches("123456", user.getPasswordHash())).isTrue();
    }

    @Test
    void loginSuccessReturnsTokenAndUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"patient1\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.user.username").value("patient1"))
                .andExpect(jsonPath("$.data.user.password_hash").doesNotExist());
    }

    @Test
    void usernameLoginSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"patient1\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void phoneLoginSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13800000001\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("patient1"));
    }

    @Test
    void wrongPasswordLoginFails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"patient1\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void missingAccountLoginFails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"missing\",\"password\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        insertUser("disabled_user", "13800000003", "disabled@example.com", RoleConstants.PATIENT, StatusConstants.DISABLED, "123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"disabled_user\",\"password\":\"123456\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void loginUpdatesLastLoginTime() throws Exception {
        assertThat(userMapper.selectById(patientId).getLastLoginTime()).isNull();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"patient1\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());

        assertThat(userMapper.selectById(patientId).getLastLoginTime()).isNotNull();
    }

    @Test
    void getMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getMeWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/user/me").header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getMeWithValidTokenSuccess() throws Exception {
        mockMvc.perform(get("/api/user/me").header("Authorization", bearerToken(patientId, "patient1", RoleConstants.PATIENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(patientId))
                .andExpect(jsonPath("$.data.phone").value("13800000001"))
                .andExpect(jsonPath("$.data.password_hash").doesNotExist());
    }

    @Test
    void updateMeSuccess() throws Exception {
        mockMvc.perform(put("/api/user/me")
                        .header("Authorization", bearerToken(patientId, "patient1", RoleConstants.PATIENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"patient_new\",\"phone\":\"13800000004\",\"email\":\"new@example.com\",\"avatar\":\"/uploads/avatar/u1.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("patient_new"))
                .andExpect(jsonPath("$.data.phone").value("13800000004"))
                .andExpect(jsonPath("$.data.update_time").isString());
    }

    @Test
    void updateMeIgnoresRoleStatusAndPasswordHash() throws Exception {
        mockMvc.perform(put("/api/user/me")
                        .header("Authorization", bearerToken(patientId, "patient1", RoleConstants.PATIENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"patient_new\",\"role\":\"admin\",\"status\":\"disabled\",\"password_hash\":\"plain\"}"))
                .andExpect(status().isOk());

        User user = userMapper.selectById(patientId);
        assertThat(user.getRole()).isEqualTo(RoleConstants.PATIENT);
        assertThat(user.getStatus()).isEqualTo(StatusConstants.ACTIVE);
        assertThat(user.getPasswordHash()).isNotEqualTo("plain");
    }

    @Test
    void updateMeDuplicateUsernameReturns409() throws Exception {
        insertUser("other", "13800000005", "other@example.com", RoleConstants.PATIENT, StatusConstants.ACTIVE, "123456");

        mockMvc.perform(put("/api/user/me")
                        .header("Authorization", bearerToken(patientId, "patient1", RoleConstants.PATIENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"other\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void updateMeDuplicatePhoneReturns409() throws Exception {
        insertUser("other", "13800000005", "other@example.com", RoleConstants.PATIENT, StatusConstants.ACTIVE, "123456");

        mockMvc.perform(put("/api/user/me")
                        .header("Authorization", bearerToken(patientId, "patient1", RoleConstants.PATIENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000005\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void patientCannotAccessAdminUserList() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearerToken(patientId, "patient1", RoleConstants.PATIENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearerToken(adminId, "admin", RoleConstants.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].password_hash").doesNotExist());
    }

    @Test
    void adminUserListPaginationWorks() throws Exception {
        insertUser("p2", "13800000006", "p2@example.com", RoleConstants.PATIENT, StatusConstants.ACTIVE, "123456");

        mockMvc.perform(get("/api/admin/users?page=1&page_size=1").header("Authorization", bearerToken(adminId, "admin", RoleConstants.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.page_size").value(1));
    }

    @Test
    void adminUserListKeywordWorks() throws Exception {
        insertUser("search_target", "13800000007", "target@example.com", RoleConstants.PATIENT, StatusConstants.ACTIVE, "123456");

        mockMvc.perform(get("/api/admin/users?keyword=target").header("Authorization", bearerToken(adminId, "admin", RoleConstants.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].username").value("search_target"));
    }

    @Test
    void adminUserListKeywordMatchesUserId() throws Exception {
        mockMvc.perform(get("/api/admin/users?keyword=" + patientId).header("Authorization", bearerToken(adminId, "admin", RoleConstants.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].user_id").value(patientId));
    }

    @Test
    void adminUserListRoleAndStatusFilterWorks() throws Exception {
        insertUser("disabled_patient", "13800000008", "dp@example.com", RoleConstants.PATIENT, StatusConstants.DISABLED, "123456");

        mockMvc.perform(get("/api/admin/users?role=patient&status=disabled").header("Authorization", bearerToken(adminId, "admin", RoleConstants.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].status").value("disabled"));
    }

    @Test
    void adminCanUpdateUserStatus() throws Exception {
        mockMvc.perform(put("/api/admin/users/" + patientId + "/status")
                        .header("Authorization", bearerToken(adminId, "admin", RoleConstants.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"disabled\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(patientId))
                .andExpect(jsonPath("$.data.status").value("disabled"));
    }

    @Test
    void adminUpdateInvalidStatusReturns400() throws Exception {
        mockMvc.perform(put("/api/admin/users/" + patientId + "/status")
                        .header("Authorization", bearerToken(adminId, "admin", RoleConstants.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"locked\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void adminUpdateMissingUserReturns404() throws Exception {
        mockMvc.perform(put("/api/admin/users/99999/status")
                        .header("Authorization", bearerToken(adminId, "admin", RoleConstants.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"disabled\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    private Integer insertUser(String username, String phone, String email, String role, String status, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(status);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user.getUserId();
    }

    private User selectByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private String bearerToken(Integer userId, String username, String role) {
        return "Bearer " + jwtUtil.generateToken(userId, username, role);
    }
}
