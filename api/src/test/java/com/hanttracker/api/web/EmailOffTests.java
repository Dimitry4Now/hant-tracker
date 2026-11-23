package com.hanttracker.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hanttracker.api.support.Tokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** A server without SMTP: nothing asks for an email, and resets are the admin's. */
@SpringBootTest(properties = "app.mail.log-only=false")
@AutoConfigureMockMvc
class EmailOffTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void theLoginPageOffersNoEmailReset() throws Exception {
        mvc.perform(get("/api/auth/options")).andExpect(jsonPath("$.passwordResetByEmail").value(false));
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(Tokens.PLAYER)))
                .andExpect(status().isServiceUnavailable());
    }
}
