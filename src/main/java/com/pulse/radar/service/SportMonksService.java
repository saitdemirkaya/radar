package com.pulse.radar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.radar.model.FixtureResponse;
import com.pulse.radar.model.LiveScoresResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final String ALL_SEASONS = "https://api.sportmonks.com/v3/football/seasons/search/2026?";
    private static final String ALL_TEAMS = "https://api.sportmonks.com/v3/football/teams/seasons/";

    private static final String AVERAGE_GOALS_URL = "https://api.sportmonks.com/v3/football/teams/";


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

    public void getTeamGoalsAverage(Long seasonId, List<Long> teamIds, Map<String, Double> teamGoalsAverageMap) {

        for (Long teamId : teamIds) {
            String url = "https://api.sportmonks.com/v3/football/teams/" + teamId +
                    "?" + API_TOKEN + "&include=statistics.details.type&filter=teamstatisticSeasons:" + seasonId;

            try {
                String jsonResponse = restTemplate.getForObject(url, String.class);
                JsonNode rootNode = objectMapper.readTree(jsonResponse);

                // Extract team name
                String teamName = rootNode.path("data").path("name").asText();

                // Extract statistics
                JsonNode statisticsNode = rootNode.path("data").path("statistics");
                if (statisticsNode.isArray()) {
                    for (JsonNode statistic : statisticsNode) {
                        JsonNode detailsNode = statistic.path("details");
                        if (detailsNode.isArray()) {
                            for (JsonNode detail : detailsNode) {
                                String developerName = detail.path("type").path("developer_name").asText();
                                if ("GOALS".equals(developerName)) {
                                    double average = detail.path("value").path("all").path("average").asDouble();
                                    teamGoalsAverageMap.put(teamName, average);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch team statistics", e);
            }
        }

    }

    public Map<String, Double> getTeamsStats() {

        Map<String, Double> teamGoalsAverageMap = new HashMap<>();
        List<Long> seasonIds = getSeasonIds();
        seasonIds.add(387L); //İtalya 2
        seasonIds.add(573L); // İsveç
        seasonIds.add(444L); // Norveç
        for (Long seasonId : seasonIds) {
            List<Long> teamIds = getTeamsIds(seasonId);
            getTeamGoalsAverage(seasonId, teamIds, teamGoalsAverageMap);
        }
        return teamGoalsAverageMap;
    }

    private List<Long> getSeasonIds() {
        List<Long> seasonIds = new ArrayList<>();
        try {
            String jsonResponse = restTemplate.getForObject(ALL_SEASONS + API_TOKEN, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode dataNode = rootNode.path("data");

            if (dataNode.isArray()) {
                for (JsonNode season : dataNode) {
                    String leagueId = season.path("league_id").asText();
                    if(isaUnnecessaryCup(leagueId) || isaUnnecessaryLeague(leagueId)){
                        continue;
                    }
                    seasonIds.add(season.path("id").asLong());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return seasonIds;
    }

    private static boolean isaUnnecessaryCup(String leagueId) {
        return leagueId.equals("2") || leagueId.equals("5") || leagueId.equals("2286") || leagueId.equals("390") || leagueId.equals("24") || leagueId.equals("27");
    }

    private static boolean isaUnnecessaryLeague(String leagueId) {
        return leagueId.equals("453") || leagueId.equals("486") || leagueId.equals("570")  || leagueId.equals("244")  || leagueId.equals("609") ;
    }

    private List<Long> getTeamsIds(Long seasonId) {
        List<Long> teamsIds = new ArrayList<>();
        try {
            String jsonResponse = restTemplate.getForObject(ALL_TEAMS + seasonId + "?" + API_TOKEN, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode dataNode = rootNode.path("data");

            if (dataNode.isArray()) {
                for (JsonNode season : dataNode) {
                    teamsIds.add(season.path("id").asLong());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return teamsIds;
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
                Integer secondHalfMinutes = null;
                Integer firstHalfMinutes = null;
                for (JsonNode period : periodsNode) {
                    String description = period.path("description").asText();
                    if ("2nd-half".equalsIgnoreCase(description)) {
                        secondHalfMinutes = period.path("minutes").asInt();
                    } else if ("1st-half".equalsIgnoreCase(description)) {
                        firstHalfMinutes = period.path("minutes").asInt();
                    }
                }
                if (secondHalfMinutes != null) {
                    return secondHalfMinutes;
                }
                // Yoksa 1. yarı dakikasını döner
                if (firstHalfMinutes != null) {
                    return firstHalfMinutes;
                }
            }

            // Return 0 if "2nd-half" is not found
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("SportMonks API çağrısı başarısız oldu", e);
        }
    }
}
