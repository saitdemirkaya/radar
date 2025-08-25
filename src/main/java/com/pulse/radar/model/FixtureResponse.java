package com.pulse.radar.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FixtureResponse {
    private DataObject data;
    private List<Subscription> subscription;
    @JsonProperty("rate_limit")
    private RateLimit rateLimit;
    private String timezone;

    @Data
    public static class DataObject {
        private long id;
        @JsonProperty("sport_id")
        private int sportId;
        @JsonProperty("league_id")
        private int leagueId;
        @JsonProperty("season_id")
        private int seasonId;
        @JsonProperty("stage_id")
        private long stageId;
        @JsonProperty("group_id")
        private Integer groupId;
        @JsonProperty("aggregate_id")
        private Integer aggregateId;
        @JsonProperty("round_id")
        private long roundId;
        @JsonProperty("state_id")
        private int stateId;
        @JsonProperty("venue_id")
        private int venueId;
        private String name;
        @JsonProperty("starting_at")
        private String startingAt;
        @JsonProperty("result_info")
        private Object resultInfo;
        private String leg;
        private Object details;
        private int length;
        private boolean placeholder;
        @JsonProperty("has_odds")
        private boolean hasOdds;
        @JsonProperty("has_premium_odds")
        private boolean hasPremiumOdds;
        @JsonProperty("starting_at_timestamp")
        private long startingAtTimestamp;
        private List<Statistic> statistics;

        @Data
        public static class Statistic {
            private long id;
            @JsonProperty("fixture_id")
            private long fixtureId;
            @JsonProperty("type_id")
            private int typeId;
            @JsonProperty("participant_id")
            private int participantId;
            private StatisticData data;
            private String location;
            private StatisticType type;

            @Data
            public static class StatisticData {
                private int value;
            }

            @Data
            public static class StatisticType {
                private int id;
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
    }

    @Data
    public static class Subscription {
        private Meta meta;
        private List<Plan> plans;
        @JsonProperty("add_ons")
        private List<AddOn> addOns;
        private List<Widget> widgets;

        @Data
        public static class Meta {
            @JsonProperty("trial_ends_at")
            private String trialEndsAt;
            @JsonProperty("ends_at")
            private String endsAt;
            @JsonProperty("current_timestamp")
            private long currentTimestamp;
        }

        @Data
        public static class Plan {
            private String plan;
            private String sport;
            private String category;
        }

        @Data
        public static class AddOn {
            @JsonProperty("add_on")
            private String addOn;
            private String sport;
            private String category;
        }

        @Data
        public static class Widget {
            // Eğer widget'lar detaylı bir yapıdaysa, buraya eklenebilir.
        }
    }

    @Data
    public static class RateLimit {
        @JsonProperty("resets_in_seconds")
        private int resetsInSeconds;
        private int remaining;
        @JsonProperty("requested_entity")
        private String requestedEntity;
    }
}