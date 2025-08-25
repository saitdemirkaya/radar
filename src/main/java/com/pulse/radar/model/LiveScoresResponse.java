package com.pulse.radar.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class LiveScoresResponse {

    private List<MatchData> data;

    public void setData(List<MatchData> data) {
        this.data = data;
    }

    @Getter
    public static class MatchData {
        private long id;

        @JsonProperty("sport_id")
        private long sportId;

        @JsonProperty("league_id")
        private long leagueId;

        @JsonProperty("season_id")
        private long seasonId;

        @JsonProperty("stage_id")
        private long stageId;

        @JsonProperty("group_id")
        private Long groupId;

        @JsonProperty("aggregate_id")
        private Long aggregateId;

        @JsonProperty("round_id")
        private Long roundId;

        @JsonProperty("state_id")
        private long stateId;

        @JsonProperty("venue_id")
        private long venueId;

        private String name;

        @JsonProperty("starting_at")
        private String startingAt;

        @JsonProperty("result_info")
        private String resultInfo;

        private String leg;
        private String details;
        private int length;

        private boolean placeholder;

        @JsonProperty("has_odds")
        private boolean hasOdds;

        @JsonProperty("has_premium_odds")
        private boolean hasPremiumOdds;

        @JsonProperty("starting_at_timestamp")
        private long startingAtTimestamp;
    }
}
