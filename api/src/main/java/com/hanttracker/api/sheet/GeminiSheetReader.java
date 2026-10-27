package com.hanttracker.api.sheet;

import com.hanttracker.api.dto.SheetReadingDto;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a sheet with Google's Gemini API. The model gets the photo and a
 * description of how our sheets are laid out, and answers in JSON shaped by a
 * response schema, so no table detection of our own is needed.
 */
@Component
@ConditionalOnProperty(name = "app.ocr.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiSheetReader implements SheetReader {

    private static final String PROMPT = """
            This is a photo of a handwritten score sheet from a card game, %d players.

            Layout:
            - The header row has one column per player, labelled with the player's initial,
              usually a Cyrillic letter (Д, Н, А, Л, ...). There are exactly %d player columns.
            - Under the header, each row holds every player's running total after one round.
              Rounds are written top to bottom, often in ruled blocks of four. There are 12 or 16
              regular rounds, and sometimes one more, separately boxed row for the final round.
              Totals can be negative.
            - Below the rounds the players work out the money: rows whose numbers mostly carry an
              explicit + or - sign, and a final sum row. These are not rounds; mark them MONEY.
            - The right margin may hold a side tally and the date. They are not rounds; leave them out.

            Reading rules:
            - A 7 is written with a crossbar. It is a digit, never a + sign.
            - When a number was corrected or written over, read the final value.
            - Use null for a cell you cannot read, and set uncertain to true for any cell
              you are not sure about.
            - Return every round row and every money row, top to bottom, with exactly %d cells each.
            - headers: the column labels as written. date: the date on the sheet as yyyy-MM-dd,
              or null if there is none.
            """;

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "headers", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                    "date", Map.of("type", "STRING", "nullable", true),
                    "rows", Map.of(
                            "type", "ARRAY",
                            "items", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "kind", Map.of("type", "STRING", "enum", List.of("ROUND", "MONEY")),
                                            "totals", Map.of(
                                                    "type", "ARRAY",
                                                    "items", Map.of("type", "INTEGER", "nullable", true)),
                                            "uncertain", Map.of(
                                                    "type", "ARRAY", "items", Map.of("type", "BOOLEAN"))),
                                    "required", List.of("kind", "totals", "uncertain"),
                                    "propertyOrdering", List.of("kind", "totals", "uncertain")))),
            "required", List.of("headers", "rows"),
            "propertyOrdering", List.of("headers", "date", "rows"));

    private final RestClient client;
    private final JsonMapper json;
    private final String apiKey;
    private final String model;

    public GeminiSheetReader(
            JsonMapper json,
            @Value("${app.ocr.gemini.api-key:}") String apiKey,
            @Value("${app.ocr.gemini.model}") String model) {
        // A sheet takes the model a while to read; the default timeouts are too short.
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        requests.setReadTimeout(Duration.ofSeconds(90));
        this.client = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .requestFactory(requests)
                .build();
        this.json = json;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public SheetReadingDto read(byte[] image, String mimeType, int players) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Sheet reading is not configured");
        }
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("text", PROMPT.formatted(players, players, players)),
                        Map.of("inlineData", Map.of(
                                "mimeType", mimeType,
                                "data", Base64.getEncoder().encodeToString(image)))))),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json",
                        "responseSchema", SCHEMA));
        try {
            String response = client.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
            return json.readValue(answerText(response), SheetReadingDto.class);
        } catch (RestClientResponseException e) {
            // The free tier has a daily quota; running out of it is worth telling apart.
            HttpStatus status = e.getStatusCode().value() == 429 ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status, "Gemini could not read the sheet", e);
        } catch (RestClientException | JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini could not read the sheet", e);
        }
    }

    /** The JSON the model wrote, out of Gemini's response envelope. */
    private String answerText(String response) {
        StringBuilder text = new StringBuilder();
        for (JsonNode part : json.readTree(response).path("candidates").path(0).path("content").path("parts")) {
            text.append(part.path("text").asString(""));
        }
        if (text.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned no reading");
        }
        return text.toString();
    }
}
