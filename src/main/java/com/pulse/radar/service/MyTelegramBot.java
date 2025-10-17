package com.pulse.radar.service;

import com.pulse.radar.model.FixtureResponse;
import com.pulse.radar.model.LiveScoresResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MyTelegramBot extends TelegramLongPollingBot {

    public static final String BALL_POSSESSION = "ball-possession";
    public static final String SHOTS_TOTAL = "shots-total";
    public static final String SHOTS_ON_TARGET = "shots-on-target";
    public static final String SHOTS_OFF_TARGET = "shots-off-target";
    public static final String SHOTS_BLOCKED = "shots-blocked";
    public static final String HIT_WOODWORK = "hit-woodwork";
    public static final String BIG_CHANCES_MISSED = "big-chances-missed";
    public static final String GOALS = "goals";

    List<String> teamName = new ArrayList<>();
    List<String> ivsTeamName = new ArrayList<>();
    List<String> yuksekVerimTeams = new ArrayList<>();


    double verim = 5.6;
    double buyukVerim = 9.5;
    private final SportMonksService sportMonksService;

    public MyTelegramBot(SportMonksService sportMonksService) {
        this.sportMonksService = sportMonksService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void clearLists() {
        teamName.clear();
        ivsTeamName.clear();
        yuksekVerimTeams.clear();
        System.out.println("Listeler temizlendi!");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            try {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId("-636719291");
                LiveScoresResponse response = sportMonksService.getLiveMatches();

                StringBuilder sb = new StringBuilder("📊 Canlı Maçlar:\n\n");
                response.getData().forEach(match ->
                        sb.append("⚽ ").append(match.getName())
                                .append(" ⏰ ").append(match.getStartingAt())
                                .append("\n")
                );

                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        Map<String, Double> teamsAverageGoals = new HashMap<>();
        System.out.println("=======================================");
        System.out.println("            RADAR STARTED!             ");
        System.out.println("=======================================");
        while (true) {
            boolean waitHour = false;
            SendMessage sendMessage;
            StringBuilder result = new StringBuilder();
            try {
                sendMessage = new SendMessage();
                sendMessage.setChatId("-636719291");
                if (teamsAverageGoals.isEmpty()) {
                    teamsAverageGoals = sportMonksService.getTeamsStats();
                }
                LiveScoresResponse response = sportMonksService.getLiveMatches();

                if (response.getData() != null) {
                    for (LiveScoresResponse.MatchData datum : response.getData()) {
                        //453->Polonya, 486->Rusya, 570->İspanya Copa Del Rey, 390->İtalya Kupa, 244->Hırvatistan, 27->Caraboa Cup, 609-> Ukrayna
                        if (datum.getLeagueId() == 453 || datum.getLeagueId() == 486 || datum.getLeagueId() == 570 || datum.getLeagueId() == 390 || datum.getLeagueId() == 244 || datum.getLeagueId() == 27 || datum.getLeagueId() == 609) {
                            continue;
                        }
                        FixtureResponse matchStatistic = sportMonksService.getMatchStatistic(datum.getId());
                        result.append(parseAndPrint(matchStatistic, datum.getId(), datum.getLeagueId(), teamsAverageGoals));
                    }
                } else {
                    waitHour = true;
                }

                if (!result.isEmpty()) {
                    sendMessage.setText(result.toString());
                    try {
                        execute(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
                try {
                    sendMessage = new SendMessage();
                    sendMessage.setChatId("-636719291");
                    sendMessage.setText("Radar hata aldı manuel kontrole geçin!" + e.getMessage());
                    execute(sendMessage);
                    Thread.sleep(200000);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                } catch (TelegramApiException ex) {
                    System.out.println("Hata mesajı atarken de hata alındı aq!");
                    ;
                }
            }
            try {
                int wait = waitHour ? 360000 : 60000; // 1 saat = 3600000 ms, 1 dakika = 60000 ms
                Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
                System.out.println("Şu anki Tarama dk'sı: " + calendar.getTime());
                calendar.add(Calendar.MILLISECOND, wait); // Add wait time in milliseconds
                System.out.println("Sıradaki Tarama dk'sı: " + calendar.getTime());
                System.out.println("----------------------------------------------------------------");
                Thread.sleep(wait);
                waitHour = false;
            } catch (InterruptedException e) {
                System.exit(0);
            }
        }
    }

    private String parseAndPrint(FixtureResponse fixtureResponse, long fixtureId, long leagueId, Map<String, Double> teamsAverageGoals) {

        List<String> targetCodes = Arrays.asList(BALL_POSSESSION, SHOTS_TOTAL, SHOTS_ON_TARGET, SHOTS_OFF_TARGET, SHOTS_BLOCKED, HIT_WOODWORK, BIG_CHANCES_MISSED, GOALS);

        String name = fixtureResponse.getData().getName();
        if (name == null || !name.contains(" vs ")) {
            System.out.println("Invalid match name format: " + name);
            return ""; // Exit early if the name is invalid
        }

        String[] teams = name.split(" vs ");
        if (teams.length < 2) {
            System.out.println("Invalid match name format after split: " + name);
            return ""; // Exit early if the split result is invalid
        }

        String homeTeamName = teams[0].trim();
        String awayTeamName = teams[1].trim();
        // Get the statistics from the response
        System.out.println("\n \nMatch: " + fixtureResponse.getData().getName());
        int matchMinute = sportMonksService.getMatchMinute(fixtureId);
        System.out.println("\nMatch Minute: " + matchMinute);
        if (matchMinute < 15 || matchMinute > 75) {
            return "";
        }


        List<FixtureResponse.DataObject.Statistic> statistics = fixtureResponse.getData().getStatistics();

        if (statistics == null || statistics.isEmpty()) {
            System.out.println("No statistics available for this match: " + fixtureResponse.getData().getName());
            return "";
        }

        // Extract home statistics
        System.out.println("\nHome Statistics:");
        Map<String, Integer> homeValues = extractValuesByLocation(statistics, "home", targetCodes);
        homeValues.forEach((code, value) -> System.out.println("Code: " + code + ", Value: " + value));

        // Extract away statistics
        System.out.println("\nAway Statistics:");
        Map<String, Integer> awayValues = extractValuesByLocation(statistics, "away", targetCodes);
        awayValues.forEach((code, value) -> System.out.println("Code: " + code + ", Value: " + value));

        return calculate(homeValues, awayValues, matchMinute, homeTeamName, awayTeamName, leagueId, teamsAverageGoals);
    }

    private String calculate(Map<String, Integer> homeValues, Map<String, Integer> awayValues, int min, String homeTeamName, String awayTeamName, long leagueId, Map<String, Double> teamsAverageGoals) {

        StringBuilder result = new StringBuilder("");

        String homeScore = homeValues.get(GOALS).toString();
        String awayScore = awayValues.get(GOALS).toString();
        System.out.println("\nMatch Score: " + homeScore + "- " + awayScore);
        if (!homeScore.equals("0") && !awayScore.equals("0")) {
            return "";
        }

        if (min > 75 || min < 15) {
            return "";
        }
        Integer homeDirek = homeValues.get(HIT_WOODWORK);
        Integer homeKacan = homeValues.get(BIG_CHANCES_MISSED);

        Integer awayDirek = awayValues.get(HIT_WOODWORK);
        Integer awayKacan = awayValues.get(BIG_CHANCES_MISSED);

        Integer homeTotalShot = homeValues.get(SHOTS_TOTAL) + homeKacan;
        Integer homeToplaOynama = homeValues.get(BALL_POSSESSION);
        int homeToplaOynamaVeSut = homeToplaOynama * homeTotalShot;
        Integer homeTeamIsabetli = homeValues.get(SHOTS_ON_TARGET) + homeDirek + homeKacan;
        double homeIsabetOrani = getIsabetOrani(homeTeamIsabetli, homeTotalShot);
        double homeVerim = (double) homeToplaOynamaVeSut / min;

        Integer awayTotalShot = awayValues.get(SHOTS_TOTAL) + awayKacan;
        Integer awayToplaOynama = awayValues.get(BALL_POSSESSION);
        int awayToplaOynamaVeSut = awayToplaOynama * awayTotalShot;
        Integer awayTeamIsabetli = awayValues.get(SHOTS_ON_TARGET) + awayDirek + awayKacan;
        double awayIsabetOrani = getIsabetOrani(awayTeamIsabetli, awayTotalShot);
        double awayVerim = (double) awayToplaOynamaVeSut / min;


        if (min >= 15 && min <= 20) {
            if ((homeTotalShot >= 5 &&
                    homeTeamIsabetli >= 2 &&
                    homeVerim >= 14) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(homeTotalShot).append("İ.Ş: ").append(homeTeamIsabetli).append("T.O: ").append(homeToplaOynama).append("Verim: ").append(homeVerim);
                Double homeMbg = teamsAverageGoals.get(homeTeamName);
                if (homeMbg != null && homeMbg > 1.5) {
                    result.append(homeTeamName).append(" takımının ilk yarı golü de alınabilir. Mbg: ").append(String.format("%.2f", homeMbg)).append("\n\n");
                }
                System.out.println(result);
            }
            if (((awayTotalShot) >= 5 &&
                    awayTeamIsabetli >= 2 &&
                    awayVerim >= 14) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(awayTotalShot).append("İ.Ş: ").append(awayTeamIsabetli).append("T.O: ").append(awayToplaOynama).append("Verim: ").append(awayVerim);
                Double awayMbg = teamsAverageGoals.get(awayTeamName);
                if (awayMbg != null && awayMbg > 1.5) {
                    result.append(awayTeamName).append(" takımının ilk yarı golü de alınabilir. Mbg: ").append(String.format("%.2f", awayMbg)).append("\n\n");
                }
                System.out.println(result);
            }
        } else if (min >= 21 && min <= 25) {
            if (((homeTotalShot) >= 6 &&
                    homeTeamIsabetli >= 2 &&
                    homeVerim >= 14) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(homeTotalShot).append("İ.Ş: ").append(homeTeamIsabetli).append("T.O: ").append(homeToplaOynama).append("Verim: ").append(homeVerim);
                Double homeMbg = teamsAverageGoals.get(homeTeamName);
                if (homeMbg != null && homeMbg > 1.5) {
                    result.append(homeTeamName).append(" takımının ilk yarı golü de alınabilir. Mbg: ").append(String.format("%.2f", homeMbg)).append("\n\n");
                }
                System.out.println(result);
            }
            if (((awayTotalShot) >= 6 &&
                    awayTeamIsabetli >= 2 &&
                    awayVerim >= 14) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(awayTotalShot).append("İ.Ş: ").append(awayTeamIsabetli).append("T.O: ").append(awayToplaOynama).append("Verim: ").append(awayVerim);
                Double awayMbg = teamsAverageGoals.get(awayTeamName);
                if (awayMbg != null && awayMbg > 1.5) {
                    result.append(awayTeamName).append(" takımının ilk yarı golü de alınabilir. Mbg: ").append(String.format("%.2f", awayMbg)).append("\n\n");
                }
                System.out.println(result);
            }
        } else if (min >= 26 && min <= 30) {
            if (((homeTotalShot) >= 6 &&
                    homeTeamIsabetli >= 2 &&
                    homeVerim >= 12) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(homeTotalShot).append("İ.Ş: ").append(homeTeamIsabetli).append("T.O: ").append(homeToplaOynama).append("Verim: ").append(homeVerim);
                Double homeMbg = teamsAverageGoals.get(homeTeamName);
                if (homeMbg != null && homeMbg > 1.5) {
                    result.append(homeTeamName).append(" takımının ilk yarı golü de alınabilir. Mbg: ").append(String.format("%.2f", homeMbg)).append("\n\n");
                }
                System.out.println(result);
            }
            if (((awayTotalShot) >= 6 &&
                    awayTeamIsabetli >= 2 &&
                    awayVerim >= 12) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(awayTotalShot).append("İ.Ş: ").append(awayTeamIsabetli).append("T.O: ").append(awayToplaOynama).append("Verim: ").append(awayVerim);
                Double awayMbg = teamsAverageGoals.get(awayTeamName);
                if (awayMbg != null && awayMbg > 1.5) {
                    result.append(awayTeamName).append(" takımının ilk yarı golü de alınabilir. Mbg: ").append(String.format("%.2f", awayMbg)).append("\n\n");
                }
                System.out.println(result);
            }
        } else if (min >= 31 && min <= 45) {
            if (((homeTotalShot) >= 7 &&
                    homeTeamIsabetli >= 3 &&
                    homeVerim >= 8.5) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(homeTotalShot).append("İ.Ş: ").append(homeTeamIsabetli).append("T.O: ").append(homeToplaOynama).append("Verim: ").append(homeVerim);
                System.out.println(result);
            }
            if (((awayTotalShot) >= 7 &&
                    awayTeamIsabetli >= 3 &&
                    awayVerim >= 8.5) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(awayTotalShot).append("İ.Ş: ").append(awayTeamIsabetli).append("T.O: ").append(awayToplaOynama).append("Verim: ").append(awayVerim);
                System.out.println(result);
            }
        }
        if (min >= 46 && min <= 55) {
            if (((homeTotalShot) >= 8 &&
                    homeTeamIsabetli >= 3 &&
                    homeVerim >= 8.5) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(homeTotalShot).append("İ.Ş: ").append(homeTeamIsabetli).append("T.O: ").append(homeToplaOynama).append("Verim: ").append(homeVerim);
                System.out.println(result);
            }
            if (((awayTotalShot) >= 8 &&
                    awayTeamIsabetli >= 3 &&
                    awayVerim >= 8.5) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(awayTotalShot).append("İ.Ş: ").append(awayTeamIsabetli).append("T.O: ").append(awayToplaOynama).append("Verim: ").append(awayVerim);
                System.out.println(result);
            }
        }
        if (min >= 56 && min <= 65) {
            if (((homeTotalShot) >= 10 &&
                    homeTeamIsabetli >= 4 &&
                    homeVerim >= 9.5) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(homeTotalShot).append("İ.Ş: ").append(homeTeamIsabetli).append("T.O: ").append(homeToplaOynama).append("Verim: ").append(homeVerim);
                System.out.println(result);
            }
            if (((awayTotalShot) >= 10 &&
                    awayTeamIsabetli >= 4 &&
                    awayVerim >= 9.5) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(awayTotalShot).append("İ.Ş: ").append(awayTeamIsabetli).append("T.O: ").append(awayToplaOynama).append("Verim: ").append(awayVerim);
                System.out.println(result);
            }
        }
        if (min >= 66 && min <= 75) {
            if (((homeTotalShot) >= 12 &&
                    homeTeamIsabetli >= 3 &&
                    homeVerim >= 9.5) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(homeTotalShot).append("İ.Ş: ").append(homeTeamIsabetli).append("T.O: ").append(homeToplaOynama).append("Verim: ").append(homeVerim);
                System.out.println(result);
            }
            if (((awayTotalShot) >= 12 &&
                    awayTeamIsabetli >= 3 &&
                    awayVerim >= 9.5) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" 0.5 üst \n");
                result.append("T.Ş: ").append(awayTotalShot).append("İ.Ş: ").append(awayTeamIsabetli).append("T.O: ").append(awayToplaOynama).append("Verim: ").append(awayVerim);
                System.out.println(result);
            }
        }


        if (min >= 15 && min <= 25) {

            boolean homeVerimBuyukSonuc = (double) homeToplaOynamaVeSut / min >= 20;
            if (homeVerimBuyukSonuc && homeIsabetOrani <= 2 && (homeTotalShot) > 4) {

                if (!yuksekVerimTeams.contains(homeTeamName) && homeScore.equals("0")) {
                    yuksekVerimTeams.add(homeTeamName);
                    result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                    result.append("ilk yarı bir gol (0.5 üst) orta güvenden alınabilir bol şans!\n\n");
                    System.out.println(result);
                }
            }

            boolean awayVerimBuyukSonuc = awayVerim >= 20;
            if (awayVerimBuyukSonuc && awayIsabetOrani <= 2 && (awayTotalShot) > 4) {
                if (!yuksekVerimTeams.contains(awayTeamName) && awayScore.equals("0")) {
                    yuksekVerimTeams.add(awayTeamName);
                    result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                    result.append("ilk yarı bir gol (0.5 üst) orta güvenden alınabilir bol şans!\n\n");
                    System.out.println(result);
                }
            }

        }
        {

            try {

                if (((homeTotalShot) >= 3 && min >= 30 &&
                        homeToplaOynama >= 70 &&
                        !ivsTeamName.contains(homeTeamName) &&
                        homeScore.equals("0"))) {
                    ivsTeamName.add(homeTeamName);
                    result.append("Dakika : ").append(min).append(" ");
                    result.append("İyi ki varsın görkem can beyhan \n ").append(homeTeamName).append("\n");
                    result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                    result.append(homeTeamName).append(" takımının bir golü yüksek güvenden alınabilir bol şans!\n\n");
                    System.out.println(result);
                }
                if (((awayTotalShot) >= 3 && min >= 30 &&
                        awayToplaOynama >= 70 &&
                        !ivsTeamName.contains(awayTeamName) &&
                        awayScore.equals("0"))) {
                    ivsTeamName.add(awayTeamName);
                    result.append("Dakika : ").append(min).append(" ");
                    result.append("İyi ki varsın görkem can beyhan \n").append(awayTeamName).append("\n");
                    result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                    result.append(awayTeamName).append(" takımının bir golü yüksek güvenden alınabilir bol şans!\n\n");
                    System.out.println(result);
                }

                if (min <= 32) {
                    if (homeTotalShot >= 8 &&
                            !ivsTeamName.contains(homeTeamName) &&
                            homeScore.equals("0")) {
                        ivsTeamName.add(homeTeamName);
                        result.append("Dakika : ").append(min).append(" ");
                        result.append("İyi ki varsın sait \n ").append(homeTeamName).append("\n");
                        result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                        result.append(homeTeamName).append(" takımının bir golü yüksek güvenden alınabilir bol şans!\n\n");
                        System.out.println(result);
                    }
                    if (awayTotalShot >= 8 &&
                            !ivsTeamName.contains(awayTeamName) &&
                            awayScore.equals("0")) {
                        ivsTeamName.add(awayTeamName);
                        result.append("Dakika : ").append(min).append(" ");
                        result.append("İyi ki varsın sait \n ").append(awayTeamName).append("\n");
                        result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                        result.append(awayTeamName).append(" takımının bir golü yüksek güvenden alınabilir bol şans!\n\n");
                        System.out.println(result);
                    }
                } else {

                    if (homeTotalShot >= min / 4 &&
                            !ivsTeamName.contains(homeTeamName) &&
                            homeScore.equals("0")) {
                        ivsTeamName.add(homeTeamName);
                        result.append("Dakika : ").append(min).append(" ");
                        result.append("İyi ki varsın sait \n ").append(homeTeamName).append("\n");
                        result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                        result.append(homeTeamName).append(" takımının bir golü yüksek güvenden alınabilir bol şans!\n\n");
                        System.out.println(result);
                    }
                    if (awayTotalShot >= min / 4 &&
                            !ivsTeamName.contains(awayTeamName) &&
                            awayScore.equals("0")) {
                        ivsTeamName.add(awayTeamName);
                        result.append("Dakika : ").append(min).append(" ");
                        result.append("İyi ki varsın sait \n ").append(awayTeamName).append("\n");
                        result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                        result.append(awayTeamName).append(" takımının bir golü yüksek güvenden alınabilir bol şans!\n\n");
                        System.out.println(result);
                    }
                }


            } catch (Exception e) {
                System.out.println("Denemede hata");
            }
        }
        return result.toString();

    }

    private static Map<String, Integer> extractValuesByLocation(List<FixtureResponse.DataObject.Statistic> statistics, String location, List<String> targetCodes) {
// Initialize the map with all target codes set to 0
        Map<String, Integer> result = targetCodes.stream()
                .collect(Collectors.toMap(code -> code, code -> 0));

        // Update the map with actual values from the statistics
        statistics.stream()
                .filter(stat -> location.equals(stat.getLocation()) && targetCodes.contains(stat.getType().getCode()))
                .forEach(stat -> result.put(stat.getType().getCode(), stat.getData().getValue()));

        return result;
    }

    private double getIsabetOrani(int homeTeamİsabetli, int homeTotalShot) {
        return homeTeamİsabetli == 0 ?
                9.9 : (double) (homeTotalShot) / homeTeamİsabetli;
    }


    @Override
    public String getBotUsername() {
        return "Radarscore_bot";
    }

    @Override
    public String getBotToken() {

        return "5009378188:AAH-A1_XVu9eCuAP3Wz_xAGe6iaai_P1bkI";
    }

}

