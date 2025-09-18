package com.pulse.radar.service;

import com.pulse.radar.model.FixtureResponse;
import com.pulse.radar.model.LiveScoresResponse;
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

    List<String> teamName = new ArrayList<>();
    List<String> yuksekVerimTeams = new ArrayList<>();


    double verim = 5.6;
    double buyukVerim = 9.5;
    private final SportMonksService sportMonksService;

    public MyTelegramBot(SportMonksService sportMonksService) {
        this.sportMonksService = sportMonksService;
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

        while (true) {
            boolean waitHour = false;
            SendMessage sendMessage;
            StringBuilder result = new StringBuilder();
            try {
                sendMessage = new SendMessage();
                sendMessage.setChatId("-636719291");
                LiveScoresResponse response = sportMonksService.getLiveMatches();

                if (response.getData() != null) {
                    for (LiveScoresResponse.MatchData datum : response.getData()) {
                        FixtureResponse matchStatistic = sportMonksService.getMatchStatistic(datum.getId());
                        result.append(parseAndPrint(matchStatistic, datum.getId()));
                    }
                } else{
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

    private String parseAndPrint(FixtureResponse fixtureResponse, long fixtureId) {

        List<String> targetCodes = Arrays.asList(BALL_POSSESSION, SHOTS_TOTAL, SHOTS_ON_TARGET, SHOTS_OFF_TARGET, SHOTS_BLOCKED);

        String[] teams = fixtureResponse.getData().getName().split(" vs ");
        String homeTeamName = teams[0].trim();
        String awayTeamName = teams[1].trim();
        // Get the statistics from the response
        System.out.println("\n \nMatch: " + fixtureResponse.getData().getName());
        int matchMinute = sportMonksService.getMatchMinute(fixtureId);
        System.out.println("\nMatch Minute: " + matchMinute);
        if (matchMinute < 15 || matchMinute > 75) {
            return "";
        }
        String matchScore = sportMonksService.getMatchScore(fixtureId);
        System.out.println("\nMatch Score: " + matchScore);
        String[] split = matchScore.split("-");
        String homeScore = split[0].trim();
        String awayScore = split[1].trim();
        if (!homeScore.equals("0") && !awayScore.equals("0")) {
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
        return calculate(homeValues, awayValues, matchMinute, matchScore, homeTeamName, awayTeamName);
    }

    private String calculate(Map<String, Integer> homeValues, Map<String, Integer> awayValues, int min, String matchScore, String homeTeamName, String awayTeamName) {

        StringBuilder result = new StringBuilder("");

        String[] split = matchScore.split("-");
        String homeScore = split[0].trim();
        String awayScore = split[1].trim();
        if (!homeScore.equals("0") && !awayScore.equals("0")) {
            return "";
        }

        if (min > 75 || min < 15) {
            return "";
        }


        Integer homeTotalShot = homeValues.get(SHOTS_TOTAL);
        int homeToplaOynamaVeSut = homeValues.get(BALL_POSSESSION) * homeTotalShot;
        double homeIsabetOrani = getIsabetOrani(homeValues.get(SHOTS_ON_TARGET), homeTotalShot);
        double homeVerim = (double) homeToplaOynamaVeSut / min;

        Integer awayTotalShot = awayValues.get(SHOTS_TOTAL);
        int awayToplaOynamaVeSut = awayValues.get(BALL_POSSESSION) * awayTotalShot;
        double awayIsabetOrani = getIsabetOrani(awayValues.get(SHOTS_ON_TARGET), awayTotalShot);
        double awayVerim = (double) awayToplaOynamaVeSut / min;


        if (min >= 15 && min <= 20) {
            if ((homeTotalShot >= 5 &&
                    homeIsabetOrani <= 2 &&
                    homeVerim >= 16) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
                System.out.println(result);
            }
            if (((awayTotalShot) >= 5 &&
                    awayIsabetOrani <= 2 &&
                    awayVerim >= 16) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
                System.out.println(result);
            }
        } else if (min >= 21 && min <= 25) {
            if (((homeTotalShot) >= 5 &&
                    homeIsabetOrani <= 2 &&
                    homeVerim >= 14) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
                System.out.println(result);
            }
            if (((awayTotalShot) >= 5 &&
                    awayIsabetOrani <= 2 &&
                    awayVerim >= 14) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
                System.out.println(result);
            }
        } else if (min >= 26 && min <= 30) {
            if (((homeTotalShot) >= 5 &&
                    homeIsabetOrani <= 2 &&
                    homeVerim >= 12) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
                System.out.println(result);
            }
            if (((awayTotalShot) >= 5 &&
                    awayIsabetOrani <= 2 &&
                    awayVerim >= 12) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
                System.out.println(result);
            }
        } else if (min >= 31 && min <= 45) {
            if (((homeTotalShot) >= 6 &&
                    homeIsabetOrani <= 2 &&
                    homeVerim >= 8) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
                System.out.println(result);
            }
            if (((awayTotalShot) >= 6 &&
                    awayIsabetOrani <= 2 &&
                    awayVerim >= 8) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
                System.out.println(result);
            }
        }
        if (min >= 46 && min <= 60) {
            if (((homeTotalShot) >= 7 &&
                    homeIsabetOrani <= 3 &&
                    homeVerim >= 6) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
                System.out.println(result);
            }
            if (((awayTotalShot) >= 7 &&
                    awayIsabetOrani <= 3 &&
                    awayVerim >= 6) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
                System.out.println(result);
            }
        }
        if (min >= 61 && min <= 75) {
            if (((homeTotalShot) >= 8 &&
                    homeIsabetOrani <= 3 &&
                    homeVerim >= 5) &&
                    !teamName.contains(homeTeamName) &&
                    homeScore.equals("0")) {

                teamName.add(homeTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(homeTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
                System.out.println(result);
            }
            if (((awayTotalShot) >= 8 &&
                    awayIsabetOrani <= 3 &&
                    awayVerim >= 5) &&
                    !teamName.contains(awayTeamName) &&
                    awayScore.equals("0")) {
                teamName.add(awayTeamName);
                result.append("Dakika : ").append(min).append(" ");
                result.append(homeTeamName).append("-").append(awayTeamName).append(" maçında ");
                result.append(awayTeamName).append(" takımının bir golü (0.5 üst) yüksek güvenden alınabilir bol şans!\n\n");
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

