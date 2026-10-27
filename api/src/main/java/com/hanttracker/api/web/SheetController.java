package com.hanttracker.api.web;

import com.hanttracker.api.dto.SheetReadingDto;
import com.hanttracker.api.service.GameService;
import com.hanttracker.api.sheet.SheetReader;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reads a photo of a game's paper sheet. Nothing is saved here: the reading goes
 * back to the admin to check, and the rounds are stored with PUT /rounds after.
 */
@RestController
@RequestMapping("/api/games")
public class SheetController {

    private final GameService games;
    private final ObjectProvider<SheetReader> reader;

    public SheetController(GameService games, ObjectProvider<SheetReader> reader) {
        this.games = games;
        this.reader = reader;
    }

    @PostMapping(path = "/{id}/sheet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SheetReadingDto read(@PathVariable Long id, @RequestParam("image") MultipartFile image)
            throws IOException {
        int players = games.get(id).playerIds().size();
        String type = image.getContentType();
        if (image.isEmpty() || type == null || !type.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a photo of the sheet");
        }
        SheetReader sheetReader = reader.getIfAvailable();
        if (sheetReader == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Sheet reading is not configured");
        }
        return sheetReader.read(image.getBytes(), type, players);
    }
}
