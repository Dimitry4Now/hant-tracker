package com.hanttracker.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hanttracker.api.domain.UserAccount;
import com.hanttracker.api.domain.UserRole;
import com.hanttracker.api.repo.UserAccountRepository;
import com.hanttracker.api.security.RateLimits;
import com.hanttracker.api.service.Mailer;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/** Dev's mailer writes to the log; the spy hands the tests what was "sent". */
@SpringBootTest
@AutoConfigureMockMvc
class EmailAccountTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private RateLimits limits;

    @MockitoSpyBean
    private Mailer mailer;

    @BeforeEach
    void reset() {
        limits.clear();
        clearInvocations(mailer);
    }

    private UserAccount player(boolean verified) {
        UserAccount user = new UserAccount();
        String name = "e" + UUID.randomUUID().toString().substring(0, 8);
        user.setFullName(name);
        user.setDisplayName(name);
        user.setEmail(name + "@fake.test");
        user.setPasswordHash(encoder.encode("own-password"));
        user.setRole(UserRole.USER);
        user.setEmailVerified(verified);
        return users.save(user);
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    /** The text of the last email sent to {@code to}. */
    private String lastEmailTo(String to) {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq(to), anyString(), text.capture());
        return text.getValue();
    }

    private static String find(String regex, String text) {
        Matcher m = Pattern.compile(regex).matcher(text);
        assertThat(m.find()).as("%s in %s", regex, text).isTrue();
        return m.group(1);
    }

    private static String json(String key, String value) {
        return "{\"%s\":\"%s\"}".formatted(key, value);
    }

    @Test
    void aMadeUpEmailHasToBeReplacedByAConfirmedOne() throws Exception {
        UserAccount user = player(false);
        String token = login(user.getEmail(), "own-password");
        mvc.perform(get("/api/games").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.requiredStep").value("CONFIRM_EMAIL"));

        String real = user.getDisplayName() + "@real.test";
        mvc.perform(post("/api/account/email")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", real)))
                .andExpect(status().isAccepted());
        String code = find("code is (\\d{6})", lastEmailTo(real));

        String wrong = code.equals("000000") ? "111111" : "000000";
        mvc.perform(post("/api/account/email/confirm")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("code", wrong)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/account/email/confirm")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("code", code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(real))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.requiredStep").isEmpty());

        mvc.perform(get("/api/games").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
    }

    @Test
    void aCodeAllowsOnlyAFewWrongTries() throws Exception {
        UserAccount user = player(false);
        String token = login(user.getEmail(), "own-password");
        String real = user.getDisplayName() + "@real.test";
        mvc.perform(post("/api/account/email")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", real)));
        String code = find("code is (\\d{6})", lastEmailTo(real));
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/account/email/confirm")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("code", wrong)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/account/email/confirm")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("code", code)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherAccountsEmailCantBeClaimed() throws Exception {
        UserAccount user = player(false);
        UserAccount other = player(true);
        String token = login(user.getEmail(), "own-password");
        mvc.perform(post("/api/account/email")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", other.getEmail())))
                .andExpect(status().isConflict());
    }

    @Test
    void aResetLinkSetsANewPasswordOnce() throws Exception {
        UserAccount user = player(true);
        String old = login(user.getEmail(), "own-password");

        mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", user.getEmail().toUpperCase())))
                .andExpect(status().isAccepted());
        String link = lastEmailTo(user.getEmail());
        assertThat(link).contains("http://localhost:4200/reset-password?token=");
        String resetToken = find("token=([\\w-]+)", link);

        String reset = "{\"token\":\"%s\",\"newPassword\":\"brand-new-one\"}".formatted(resetToken);
        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reset))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reset))
                .andExpect(status().isBadRequest());

        login(user.getEmail(), "brand-new-one");
        // Sessions from before the reset are signed out.
        mvc.perform(get("/api/games").header(HttpHeaders.AUTHORIZATION, old)).andExpect(status().isUnauthorized());
    }

    @Test
    void resetLinksGoOnlyToConfirmedEmailsAndTheAnswerIsTheSame() throws Exception {
        UserAccount unconfirmed = player(false);
        for (String email : new String[] {unconfirmed.getEmail(), "nobody@real.test"}) {
            mvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("email", email)))
                    .andExpect(status().isAccepted());
        }
        verify(mailer, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void theProfileCantChangeTheEmailWithoutACode() throws Exception {
        UserAccount user = player(true);
        String token = login(user.getEmail(), "own-password");
        mvc.perform(
                        put("/api/account/profile")
                                .header(HttpHeaders.AUTHORIZATION, token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"displayName\":\"x\",\"email\":\"elsewhere@real.test\",\"currentPassword\":\"own-password\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theLoginPageIsToldResetsWork() throws Exception {
        mvc.perform(get("/api/auth/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordResetByEmail").value(true));
    }
}
