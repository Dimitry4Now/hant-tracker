package com.hanttracker.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserRole;
import com.hanttracker.api.repo.UserAccountRepository;
import com.hanttracker.api.security.RateLimits;
import com.hanttracker.api.seed.DefaultPasswordAudit;
import com.hanttracker.api.support.Tokens;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(Tokens.class)
class PasswordChangeTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private RateLimits limits;

    @Autowired
    private Tokens tokens;

    @Autowired
    private DefaultPasswordAudit audit;

    @BeforeEach
    void forgetCounts() {
        limits.clear();
    }

    private UserAccount player(String password) {
        UserAccount user = new UserAccount();
        String name = "p" + UUID.randomUUID().toString().substring(0, 8);
        user.setFullName(name);
        user.setDisplayName(name);
        user.setEmail(name + "@example.com");
        user.setPasswordHash(encoder.encode(password));
        user.setRole(UserRole.USER);
        // Unconfirmed emails are their own step, tested in EmailAccountTests.
        user.setEmailVerified(true);
        return users.save(user);
    }

    private String login(UserAccount user, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(user.getEmail(), password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private static String change(String current, String next) {
        return "{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(current, next);
    }

    @Test
    void theDefaultPasswordHasToBeChangedBeforeAnythingElse() throws Exception {
        UserAccount user = player("changeme");
        String token = login(user, "changeme");

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredStep").value("CHANGE_PASSWORD"));
        mvc.perform(get("/api/games").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.requiredStep").value("CHANGE_PASSWORD"));

        mvc.perform(put("/api/account/password")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(change("changeme", "changeme")))
                .andExpect(status().isBadRequest());

        String body = mvc.perform(put("/api/account/password")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(change("changeme", "a-better-one")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.requiredStep").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String fresh = "Bearer " + JsonPath.read(body, "$.token");

        // The change signs out the old token and the new one works everywhere.
        mvc.perform(get("/api/games").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/games").header(HttpHeaders.AUTHORIZATION, fresh)).andExpect(status().isOk());
    }

    @Test
    void aNewPasswordMustDifferFromTheCurrentOne() throws Exception {
        UserAccount user = player("first-password");
        String token = login(user, "first-password");
        mvc.perform(put("/api/account/password")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(change("first-password", "first-password")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anAdminResetGivesAOneTimePasswordAndSignsThePlayerOut() throws Exception {
        UserAccount user = player("forgotten-one");
        String token = login(user, "forgotten-one");

        String body = mvc.perform(post("/api/users/{id}/reset-password", user.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokens.bearer(Tokens.ADMIN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String temporary = JsonPath.read(body, "$.temporaryPassword");
        assertThat(temporary).hasSize(12);

        mvc.perform(get("/api/games").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, login(user, temporary)))
                .andExpect(jsonPath("$.requiredStep").value("CHANGE_PASSWORD"));
    }

    @Test
    void onlyAdminsResetOthersPasswords() throws Exception {
        UserAccount user = player("some-password");
        mvc.perform(post("/api/users/{id}/reset-password", user.getId())
                        .header(HttpHeaders.AUTHORIZATION, tokens.bearer(Tokens.PLAYER)))
                .andExpect(status().isForbidden());

        Long adminId = users.findByEmailIgnoreCase(Tokens.ADMIN).orElseThrow().getId();
        mvc.perform(post("/api/users/{id}/reset-password", adminId)
                        .header(HttpHeaders.AUTHORIZATION, tokens.bearer(Tokens.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aPlayerAddedWithoutAPasswordGetsAOneTimeOne() throws Exception {
        String email = "added-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String body = mvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, tokens.bearer(Tokens.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Added\",\"displayName\":\"Added\",\"email\":\"%s\",\"role\":\"USER\"}"
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.mustChangePassword").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String temporary = JsonPath.read(body, "$.temporaryPassword");

        UserAccount added = users.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(encoder.matches(temporary, added.getPasswordHash())).isTrue();
        assertThat(encoder.matches("changeme", added.getPasswordHash())).isFalse();
    }

    @Test
    void startupFlagsAccountsStillOnTheDefaultPassword() {
        UserAccount stillDefault = player("changeme");
        UserAccount changed = player("own-password");

        audit.run();

        assertThat(users.findById(stillDefault.getId()).orElseThrow().isMustChangePassword())
                .isTrue();
        assertThat(users.findById(changed.getId()).orElseThrow().isMustChangePassword())
                .isFalse();
    }
}
