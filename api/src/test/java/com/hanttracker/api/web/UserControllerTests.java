package com.hanttracker.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hanttracker.api.support.Tokens;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(Tokens.class)
class UserControllerTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Tokens tokens;

    @Test
    void playersSeeNamesButNotEmailsOrFullNames() throws Exception {
        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, tokens.bearer(Tokens.PLAYER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").isNotEmpty())
                .andExpect(jsonPath("$[*].email", Matchers.everyItem(Matchers.nullValue())))
                .andExpect(jsonPath("$[*].fullName", Matchers.everyItem(Matchers.nullValue())));
    }

    @Test
    void adminsSeeEverything() throws Exception {
        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, tokens.bearer(Tokens.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", Matchers.hasItem(Tokens.PLAYER)))
                .andExpect(jsonPath("$[0].fullName").isNotEmpty());
    }
}
