package com.hanttracker.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hanttracker.api.dto.GameDto;
import com.hanttracker.api.dto.GameDtos.CreateGameRequest;
import com.hanttracker.api.service.GameService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Runs without a Gemini key, as the dev profile does. */
@SpringBootTest(properties = "app.ocr.gemini.api-key=")
@AutoConfigureMockMvc
class SheetControllerTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private GameService games;

    @Test
    @WithMockUser(roles = "ADMIN")
    void answers503WithoutAnApiKey() throws Exception {
        GameDto game = games.create(new CreateGameRequest(LocalDate.of(2026, 8, 18), List.of(1L, 2L), null));
        MockMultipartFile image = new MockMultipartFile("image", "sheet.jpg", "image/jpeg", new byte[] {1, 2, 3});

        mvc.perform(multipart("/api/games/{id}/sheet", game.id()).file(image))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void refusesSomethingThatIsNotAnImage() throws Exception {
        GameDto game = games.create(new CreateGameRequest(LocalDate.of(2026, 8, 18), List.of(1L, 2L), null));
        MockMultipartFile text = new MockMultipartFile("image", "sheet.txt", "text/plain", new byte[] {1});

        mvc.perform(multipart("/api/games/{id}/sheet", game.id()).file(text))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void isAdminOnly() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "sheet.jpg", "image/jpeg", new byte[] {1});

        mvc.perform(multipart("/api/games/{id}/sheet", 1L).file(image)).andExpect(status().isForbidden());
    }
}
