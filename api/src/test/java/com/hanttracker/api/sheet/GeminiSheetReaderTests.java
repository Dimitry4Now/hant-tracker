package com.hanttracker.api.sheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.hanttracker.api.dto.SheetReadingDto;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

class GeminiSheetReaderTests {

    private static final String FLASH = GeminiSheetReader.BASE_URL + "/models/flash:generateContent";
    private static final String LITE = GeminiSheetReader.BASE_URL + "/models/lite:generateContent";
    private static final String ANSWER = """
            {"candidates":[{"content":{"parts":[{"text":
              "{\\"headers\\":[\\"Д\\",\\"Н\\"],\\"date\\":\\"2026-08-18\\",\\"rows\\":[{\\"kind\\":\\"ROUND\\",\\"totals\\":[200,-150],\\"uncertain\\":[false,true]}]}"
            }]}}]}
            """;

    private MockRestServiceServer gemini;
    private GeminiSheetReader reader;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(GeminiSheetReader.BASE_URL);
        gemini = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        reader = new GeminiSheetReader(
                timeout -> client, System::nanoTime, JsonMapper.builder().build(), "key", "flash, lite", Duration.ZERO, Duration.ofSeconds(55));
    }

    @Test
    void readsTheAnswer() {
        gemini.expect(requestTo(FLASH)).andRespond(withSuccess(ANSWER, MediaType.APPLICATION_JSON));

        SheetReadingDto reading = reader.read(new byte[] {1}, "image/jpeg", 2);

        assertThat(reading.headers()).containsExactly("Д", "Н");
        assertThat(reading.date()).isEqualTo("2026-08-18");
        assertThat(reading.rows().getFirst().totals()).containsExactly(200, -150);
        assertThat(reading.rows().getFirst().kind()).isEqualTo(SheetReadingDto.RowKind.ROUND);
    }

    @Test
    void retriesABusyModelThenFallsBackToTheNext() {
        gemini.expect(times(2), requestTo(FLASH)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        gemini.expect(requestTo(LITE)).andRespond(withSuccess(ANSWER, MediaType.APPLICATION_JSON));

        assertThat(reader.read(new byte[] {1}, "image/jpeg", 2).headers()).hasSize(2);
        gemini.verify();
    }

    @Test
    void movesOnAtOnceWhenAModelIsOutOfQuota() {
        gemini.expect(requestTo(FLASH)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        gemini.expect(requestTo(LITE)).andRespond(withSuccess(ANSWER, MediaType.APPLICATION_JSON));

        assertThat(reader.read(new byte[] {1}, "image/jpeg", 2).headers()).hasSize(2);
        gemini.verify();
    }

    @Test
    void saysBusyWhenEveryModelIsBusy() {
        gemini.expect(times(2), requestTo(FLASH)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        gemini.expect(times(2), requestTo(LITE)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> reader.read(new byte[] {1}, "image/jpeg", 2))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }

    @Test
    void saysOutOfQuotaWhenEveryModelIs() {
        gemini.expect(requestTo(FLASH)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        gemini.expect(requestTo(LITE)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> reader.read(new byte[] {1}, "image/jpeg", 2))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void givesUpAtOnceOnABadRequest() {
        gemini.expect(requestTo(FLASH)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> reader.read(new byte[] {1}, "image/jpeg", 2))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
        gemini.verify();
    }

    @Test
    void givesEachAttemptOnlyTheTimeLeft() {
        RestClient.Builder builder = RestClient.builder().baseUrl(GeminiSheetReader.BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        AtomicLong now = new AtomicLong();
        List<Duration> timeouts = new ArrayList<>();
        // Each answer takes 25 seconds.
        GeminiSheetReader slow = new GeminiSheetReader(
                timeout -> {
                    timeouts.add(timeout);
                    now.addAndGet(Duration.ofSeconds(25).toNanos());
                    return client;
                },
                now::get, JsonMapper.builder().build(), "key", "flash, lite", Duration.ZERO, Duration.ofSeconds(55));
        server.expect(times(2), requestTo(FLASH)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // A third attempt would start with 5s left of 55, too little, so it says busy.
        assertThatThrownBy(() -> slow.read(new byte[] {1}, "image/jpeg", 2))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
        assertThat(timeouts).containsExactly(Duration.ofSeconds(55), Duration.ofSeconds(30));
        server.verify();
    }
}
