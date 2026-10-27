package com.hanttracker.api.sheet;

import com.hanttracker.api.dto.SheetReadingDto;

/** Reads a photo of a paper score sheet. Which engine does it is set by {@code app.ocr.provider}. */
public interface SheetReader {

    /**
     * @param image the photo, as uploaded
     * @param mimeType its media type, e.g. image/jpeg
     * @param players how many player columns the sheet should have
     */
    SheetReadingDto read(byte[] image, String mimeType, int players);
}
