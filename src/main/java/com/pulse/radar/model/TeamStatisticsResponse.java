package com.pulse.radar.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamStatisticsResponse {

    private Data data;

    @Getter
    @Setter
    public static class Data {
        private long id;

        @JsonProperty("sport_id")
        private long sportId;

        @JsonProperty("country_id")
        private long countryId;

        @JsonProperty("venue_id")
        private long venueId;

        private String gender;
        private String name;

        @JsonProperty("short_code")
        private String shortCode;

        @JsonProperty("image_path")
        private String imagePath;

        private int founded;
        private String type;
        private boolean placeholder;

        @JsonProperty("last_played_at")
        private String lastPlayedAt;

        private List<Statistic> statistics;
    }

    @Getter
    @Setter
    public static class Statistic {
        private long id;

        @JsonProperty("team_id")
        private long teamId;

        @JsonProperty("season_id")
        private long seasonId;

        @JsonProperty("has_values")
        private boolean hasValues;

        private List<Detail> details;
    }

    @Getter
    @Setter
    public static class Detail {
        private long id;

        @JsonProperty("team_statistic_id")
        private long teamStatisticId;

        @JsonProperty("type_id")
        private long typeId;

        private Value value;
        private Type type;
    }

    @Getter
    @Setter
    public static class Value {
        private All all;
        private Home home;
        private Away away;

        @JsonProperty("minutes_per_assist")
        private Double minutesPerAssist;

        @JsonProperty("assists_per_game")
        private Double assistsPerGame;

        @JsonProperty("total_assists")
        private Integer totalAssists;

        @JsonProperty("most_substituted_players")
        private List<MostSubstitutedPlayer> mostSubstitutedPlayers;
    }

    @Getter
    @Setter
    public static class All {
        private Integer count;
        private Double average;
        private Integer first;
        private Double percentage;
    }

    @Getter
    @Setter
    public static class Home {
        private Integer count;
        private Double average;
        private Integer first;
        private Double percentage;
    }

    @Getter
    @Setter
    public static class Away {
        private Integer count;
        private Double average;
        private Integer first;
        private Double percentage;
    }

    @Getter
    @Setter
    public static class MostSubstitutedPlayer {
        @JsonProperty("player_id")
        private long playerId;

        private int in;
        private int out;
        private int total;
    }

    @Getter
    @Setter
    public static class Type {
        private long id;
        private String name;
        private String code;

        @JsonProperty("developer_name")
        private String developerName;

        @JsonProperty("model_type")
        private String modelType;

        @JsonProperty("stat_group")
        private String statGroup;
    }
}