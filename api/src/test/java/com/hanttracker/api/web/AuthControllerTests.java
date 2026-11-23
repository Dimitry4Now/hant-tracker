package com.hanttracker.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hanttracker.api.repo.AccountRequestRepository;
import com.hanttracker.api.security.RateLimits;
import com.hanttracker.api.support.Tokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RateLimits limits;

    @Autowired
    private AccountRequestRepository requests;

    @BeforeEach
    void forgetCounts() {
        limits.clear();
    }

    static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    static MockHttpServletRequestBuilder login(String email, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    @Test
    void logsInWithTheRightPassword() throws Exception {
        mvc.perform(login(Tokens.PLAYER, "password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(Tokens.PLAYER));
    }

    @Test
    void unknownEmailsAndWrongPasswordsGetTheSameAnswer() throws Exception {
        String wrong = mvc.perform(login(Tokens.PLAYER, "not-it"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getErrorMessage();
        String unknown = mvc.perform(login("nobody@example.com", "not-it"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getErrorMessage();
        assertThat(unknown).isEqualTo(wrong);
    }

    @Test
    void wrongPasswordsForOneEmailAreLimitedFromAnyAddress() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(login(Tokens.PLAYER, "guess" + i).with(from("10.0.0." + i))).andExpect(status().isUnauthorized());
        }
        // Even the right password waits now, so guessing on doesn't pay.
        mvc.perform(login(Tokens.PLAYER, "password").with(from("10.0.1.1"))).andExpect(status().isTooManyRequests());
        mvc.perform(login(Tokens.ADMIN, "password").with(from("10.0.1.1"))).andExpect(status().isOk());
    }

    @Test
    void loginsFromOneAddressAreLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(login("someone" + i + "@example.com", "x").with(from("10.0.2.1")));
        }
        mvc.perform(login(Tokens.PLAYER, "password").with(from("10.0.2.1"))).andExpect(status().isTooManyRequests());
        mvc.perform(login(Tokens.PLAYER, "password").with(from("10.0.2.2"))).andExpect(status().isOk());
    }

    static MockHttpServletRequestBuilder register(String email) {
        return post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"New Player","displayName":"Newbie","email":"%s","password":"long-enough"}
                        """.formatted(email));
    }

    @Test
    void signUpDoesNotTellWhetherAnEmailHasAnAccount() throws Exception {
        long before = requests.count();
        mvc.perform(register(Tokens.PLAYER).with(from("10.0.3.1"))).andExpect(status().isAccepted());
        assertThat(requests.count()).isEqualTo(before);

        mvc.perform(register("fresh@example.com").with(from("10.0.3.1"))).andExpect(status().isAccepted());
        assertThat(requests.existsByEmailIgnoreCase("fresh@example.com")).isTrue();
    }

    @Test
    void signUpsFromOneAddressAreLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(register("flood" + i + "@example.com").with(from("10.0.4.1"))).andExpect(status().isAccepted());
        }
        mvc.perform(register("flood5@example.com").with(from("10.0.4.1"))).andExpect(status().isTooManyRequests());
    }
}
