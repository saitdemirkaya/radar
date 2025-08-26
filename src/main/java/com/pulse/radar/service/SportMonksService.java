package com.pulse.radar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.radar.model.FixtureResponse;
import com.pulse.radar.model.LiveScoresResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SportMonksService {

    //https://api.sportmonks.com/v3/football/fixtures/19553426?include=statistics.type&api_token=6bUSKxpE0SRqcvGdK8cIvzyO7G7VB5ibpnjzSzKM7XSRS5hluHGNv6x2XFWc
    //https://api.sportmonks.com/v3/football/fi̇xtures/19441090?include=statistics.type&api_token=6bUSKxpE0SRqcvGdK8cIvzyO7G7VB5ibpnjzSzKM7XSRS5hluHGNv6x2XFWc

    private static final String API_TOKEN =
            "api_token=6bUSKxpE0SRqcvGdK8cIvzyO7G7VB5ibpnjzSzKM7XSRS5hluHGNv6x2XFWc";
    private static final String STATISTIC =
            "?include=statistics.type";

    private static final String MINUTE =
            "?include=periods;state";
    private static final String SCORE =
            "?include=scores";
    private static final String LIVE_MATCH_URL =
            "https://api.sportmonks.com/v3/football/livescores/inplay" + "?" + API_TOKEN;
    private static final String MATCH_STATISTICS_URL =
            "https://api.sportmonks.com/v3/football/fixtures/";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SportMonksService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public LiveScoresResponse getLiveMatches() {
        try {
            String json = restTemplate.getForObject(LIVE_MATCH_URL, String.class);
            return objectMapper.readValue(json, LiveScoresResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("SportMonks API çağrısı başarısız oldu", e);
        }
    }

    public FixtureResponse getMatchStatistic(long fixtureId) {
        try {
            String json = restTemplate.getForObject(MATCH_STATISTICS_URL + fixtureId + STATISTIC + "&" + API_TOKEN, String.class);
            return objectMapper.readValue(json, FixtureResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("SportMonks API çağrısı başarısız oldu", e);
        }
    }
    public String getMatchScore(long fixtureId) {
        try {
            String json = restTemplate.getForObject(MATCH_STATISTICS_URL + fixtureId + SCORE + "&" + API_TOKEN, String.class);
            JsonNode rootNode = objectMapper.readTree(json);

            // Access the "data" object
            JsonNode dataNode = rootNode.path("data");

            // Access the "scores" array
            JsonNode scoresNode = dataNode.path("scores");

            int homeScore = 0;
            int awayScore = 0;

            if (scoresNode.isArray()) {
                for (JsonNode score : scoresNode) {
                    String description = score.path("description").asText();
                    String participant = score.path("score").path("participant").asText();
                    int goals = score.path("score").path("goals").asInt();

                    if ("CURRENT".equalsIgnoreCase(description)) {
                        if ("home".equalsIgnoreCase(participant)) {
                            homeScore = goals;
                        } else if ("away".equalsIgnoreCase(participant)) {
                            awayScore = goals;
                        }
                    }
                }
            }

            return homeScore + " - " + awayScore;
        } catch (Exception e) {
            throw new RuntimeException("SportMonks API çağrısı başarısız oldu", e);
        }
    }

    public int getMatchMinute(long fixtureId) {
        try {
            String json = restTemplate.getForObject(MATCH_STATISTICS_URL + fixtureId + MINUTE + "&" + API_TOKEN, String.class);
            JsonNode rootNode = objectMapper.readTree(json);

            // Access the "data" object
            JsonNode dataNode = rootNode.path("data");

            // Access the "periods" array
            JsonNode periodsNode = dataNode.path("periods");
            JsonNode stateNode = dataNode.path("state");
            if ("HT".equalsIgnoreCase(stateNode.path("state").asText())) {
                return 45;
            }

            if (periodsNode.isArray()) {
                for (JsonNode period : periodsNode) {
                    String description = period.path("description").asText();
                    if ("2nd-half".equalsIgnoreCase(description)) {
                        return period.path("minutes").asInt();
                    } else if ("1st-half".equalsIgnoreCase(description)) {
                        return period.path("minutes").asInt();
                    }
                }
            }

            // Return 0 if "2nd-half" is not found
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("SportMonks API çağrısı başarısız oldu", e);
        }
    }
}
