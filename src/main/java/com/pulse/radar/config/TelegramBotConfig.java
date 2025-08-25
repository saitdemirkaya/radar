package com.pulse.radar.config;

import com.pulse.radar.service.MyTelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {
    private final MyTelegramBot myTelegramBot;

    public TelegramBotConfig(MyTelegramBot myTelegramBot) {
        this.myTelegramBot = myTelegramBot;
    }

    @Bean
    public TelegramBotsApi telegramBotsApi() throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(myTelegramBot);
        return botsApi;
    }
}
