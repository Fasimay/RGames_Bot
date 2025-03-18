package com.RGames.RGames;


import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BotConfig {

    @Value("${discord.token}")
    private String token;

    @Bean
    public JDA jda(GameCommandListener gameCommandListener, RerollCommandListener rerollCommandListener) throws InterruptedException {
        JDA jda =  JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(gameCommandListener, rerollCommandListener)
                .build()
                .awaitReady();

        return jda;
    }
}
