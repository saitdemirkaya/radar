package com.pulse.radar.service;

import com.pulse.radar.model.FixtureResponse;
import com.pulse.radar.model.LiveScoresResponse;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class MyTelegramBot extends TelegramLongPollingBot {

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

            try {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId("-636719291");
                LiveScoresResponse response = sportMonksService.getLiveMatches();

                for (LiveScoresResponse.MatchData datum : response.getData()) {
                    FixtureResponse matchStatistic = sportMonksService.getMatchStatistic(datum.getId());
                }
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

        /*ProcessMatch processMatch = new ProcessMatch();
        while (true) {
            SendMessage sendMessage;

            try {
                System.out.println("\n ARAMA BAŞLADI \n");

                LiveScoresResponse liveScores = calculateStatistics.getLiveScores();
                Map<TeamsValues, List<StatusValue>> searchResultMap = new SendReqToWebSite().getMatchStatistics();
                sendMessage = new SendMessage();
                sendMessage.setChatId("-636719291");
                String result = processMatch.calculate(searchResultMap, teamGoals);
                if (!result.isEmpty()) {
                    sendMessage.setText(result);
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
                    sendMessage.setText("Radar hata aldı manuel kontrole geçin!");
                    execute(sendMessage);
                    Thread.sleep(200000);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                } catch (TelegramApiException ex) {
                    System.out.println("Hata mesajı atarken de hata alındı!");
                    ;
                }
                continue;
            }


            try {
                long LOWER_RANGE = 30000; //assign lower range value
                long UPPER_RANGE = 90000; //assign upper range value
                Random random = new Random();


                long randomValue = LOWER_RANGE +
                        (long) (random.nextDouble() * (UPPER_RANGE - LOWER_RANGE));
                Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
                System.out.println("Şu anki Tarama dk'sı: " + calendar.getTime());
                calendar.add(Calendar.SECOND, (int) randomValue / 1000);
                System.out.println("Sıradaki Tarama dk'sı: " + calendar.getTime());
                System.out.println("----------------------------------------------------------------");
                Thread.sleep(randomValue);
            } catch (InterruptedException e) {
                System.exit(0);
            }

        }*/

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

