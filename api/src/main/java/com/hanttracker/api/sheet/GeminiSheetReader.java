package com.hanttracker.api.sheet;

import com.hanttracker.api.dto.SheetReadingDto;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
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

    private static final Logger log = LoggerFactory.getLogger(GeminiSheetReader.class);

    static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

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

    /** Requests still start within this, so the answer is back before nginx gives up at 60s. */
    private static final Duration RETRY_BUDGET = Duration.ofSeconds(30);

    private final RestClient client;
    private final JsonMapper json;
    private final String apiKey;
    /** Tried in order: each has its own free quota, and one may be busy while another is not. */
    private final List<String> models;
    private final Duration retryDelay;

    @Autowired
    public GeminiSheetReader(
            JsonMapper json,
            @Value("${app.ocr.gemini.api-key:}") String apiKey,
            @Value("${app.ocr.gemini.model}") String models) {
        this(client(), json, apiKey, models, Duration.ofSeconds(2));
    }

    GeminiSheetReader(RestClient client, JsonMapper json, String apiKey, String models, Duration retryDelay) {
        this.client = client;
        this.json = json;
        this.apiKey = apiKey;
        this.models = Arrays.stream(models.split(",")).map(String::trim).filter(m -> !m.isEmpty()).toList();
        this.retryDelay = retryDelay;
        // Says at start whether the key reached the app — it is easy to put in the wrong file.
        if (apiKey.isBlank()) {
            log.warn("Sheet reading is off: GEMINI_API_KEY is not set");
        } else {
            log.info("Sheet reading uses Gemini models {}", this.models);
        }
    }

    /**
     * Gemini's free tier is often busy for a moment, so a busy model is tried
     * once more and then the next one; a model out of quota goes straight to
     * the next. Only when every model fails does the admin hear about it.
     */
    @Override
    public SheetReadingDto read(byte[] image, String mimeType, int players) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Sheet reading is not configured");
        }
        String body = json.writeValueAsString(Map.of(
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("text", PROMPT.formatted(players, players, players)),
                        Map.of("inlineData", Map.of(
                                "mimeType", mimeType,
                                "data", Base64.getEncoder().encodeToString(image)))))),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json",
                        "responseSchema", SCHEMA)));

        long started = System.nanoTime();
        boolean busy = false;
        models:
        for (String model : models) {
            for (int attempt = 1; attempt <= 2; attempt++) {
                if (Duration.ofNanos(System.nanoTime() - started).compareTo(RETRY_BUDGET) > 0) {
                    break models;
                }
                try {
                    return json.readValue(answerText(generate(model, body)), SheetReadingDto.class);
                } catch (RestClientResponseException e) {
                    int code = e.getStatusCode().value();
                    log.warn("Gemini {} answered {}: {}", model, code, e.getResponseBodyAsString());
                    if (code == 429) {
                        continue models;
                    }
                    if (!e.getStatusCode().is5xxServerError()) {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini could not read the sheet", e);
                    }
                    busy = true;
                    if (attempt == 1) {
                        pause();
                    }
                } catch (ResourceAccessException e) {
                    log.warn("Gemini {} did not answer in time", model, e);
                    busy = true;
                    continue models;
                } catch (RestClientException | JacksonException e) {
                    log.warn("Could not read a sheet with Gemini {}", model, e);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini could not read the sheet", e);
                }
            }
        }
        // Busy and out of quota call for different advice: a minute's wait, or tomorrow.
        throw busy
                ? new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gemini is busy")
                : new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Gemini's free quota is used up");
    }

    private String generate(String model, String body) {
        return client.post()
                .uri("/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private void pause() {
        try {
            Thread.sleep(retryDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static RestClient client() {
        // A sheet takes the model a while to read; the default timeouts are too short,
        // but nginx stops waiting at 60s, so there is no point waiting longer here.
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        requests.setReadTimeout(Duration.ofSeconds(50));
        return RestClient.builder().baseUrl(BASE_URL).requestFactory(requests).build();
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
