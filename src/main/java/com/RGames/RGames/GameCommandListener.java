package com.RGames.RGames;


import com.RGames.RGames.DataBase.DataBaseService;
import com.RGames.RGames.DataBase.Entity.Game;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GameCommandListener extends ListenerAdapter {

    private final DataBaseService dbService;
    private final Map<Long, Integer> currentMapPage = new HashMap<>();
    private final Map<String, Long> messageOwners = new HashMap<>();

    public GameCommandListener(DataBaseService dbService) {
        this.dbService = dbService;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        System.out.println("Получено сообщение: " + event.getMessage().getContentRaw()); // Проверка

        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        String user = event.getAuthor().getName();

        switch (message.toLowerCase()) {
            case "!games":
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Выбор игры \uD83C\uDFAE")
                        .setDescription("Выберете категорию игр, которая вас интересует:")
                        .setColor(0x00ff00);

                event.getChannel().sendMessageEmbeds(embed.build())
                        .setActionRow(
                                Button.primary("td_games", "Tower Defense"),
                                Button.success("fun_games", "Fun Games"),
                                Button.danger("web_games", "Web Games")
                        ).queue(sentMessage -> {
                            messageOwners.put(sentMessage.getId(), event.getAuthor().getIdLong());
                        });

        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.isAcknowledged()) {
            System.out.println("Кнопка уже обработана в другом классе: " + event.getComponentId());
            return;
        }

        String buttonId = event.getComponentId();
        String messageId = event.getMessageId();
        Long userId = event.getUser().getIdLong();
        Long categoryId = null;

        // Проверяем, принадлежит ли сообщение этому обработчику
        if (!messageOwners.containsKey(messageId)) {
            // Если сообщение не зарегистрировано для этого обработчика, выходим
            return;
        }

        // Затем проверяем, является ли пользователь владельцем
        long ownerId = messageOwners.get(messageId);
        if (event.getUser().getIdLong() != ownerId) {
            event.reply("Иди в пизду не твоё").setEphemeral(true).queue();
            return;
        }

        // В зависимости от того какую пользователь выбрал команду бот присваивает значение полю category_Id
        switch (buttonId) {
            case "td_games":
                categoryId = 1L;
                break;
            case "fun_games":
                categoryId = 2L;
                break;
            case "web_games":
                categoryId = 3L;
                break;
            case "prev_page":
            case "next_page":
                categoryId = 2L;
                break;
            case "back_main":
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Выбор игры \uD83C\uDFAE")
                        .setDescription("Выберете категорию игр, которая вас интересует:")
                        .setColor(0x00ff00);

                event.editMessageEmbeds(embed.build())
                        .setActionRow(
                                Button.primary("td_games", "Tower Defense"),
                                Button.success("fun_games", "Fun Games"),
                                Button.danger("web_games", "Web Games")
                        ).queue();
            default:
                event.reply("Unknown game type button!").setEphemeral(true).queue();
                return;
        }

        List<Game> games = dbService.getGamesByCategoryId(categoryId);

        if (games.isEmpty()) {
            event.reply("Игры в этой категории не добавлены!").setEphemeral(true).queue();
            return;
        }

        if (categoryId == 2L) {
            // Для категории Fun Games делит выдачу на страницы по 5 игр

            int pageSize = 5;
            int page = currentMapPage.getOrDefault(userId, 0);
            int maxPages = (int) Math.ceil((double) games.size() / (double) pageSize);

            if (buttonId.equals("next_page") && page < maxPages -1) {
                page++;
            } else if (buttonId.equals("prev_page") && page > 0) {
                page--;
            }
            currentMapPage.put(userId, page);

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Список Fun Games (стр. " + (page + 1) + "/" + maxPages + ")")
                    .setColor(0x00ff00);

            int start = page * pageSize;
            int end = Math.min(start + pageSize, games.size());
            for (int i = start; i < end; i++) {
                Game game = games.get(i);
                embed.addField(game.getName(), game.getDescription() + "\n[Ссылка](" + game.getGameUrl() + ")", false);
            }

            event.editMessageEmbeds(embed.build())
                    .setActionRow(
                            Button.primary("prev_page", "◀ Назад").withDisabled(page == 0),
                            Button.primary("back_main","Вернутся назад"),
                            Button.primary("next_page", "Вперёд ▶").withDisabled(page == maxPages - 1)
                    ).queue();
        } else if (categoryId == 3L) {
            // Для категории web_games – выводим только описание и ссылку на игру (без wiki, codes, tier list)

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Список Web Games")
                    .setColor(0x00ff00);

            for (Game game : games) {
                String gameUrl = (game.getGameUrl() != null && !game.getGameUrl().isEmpty())
                        ? "[Перейти к игре](" + game.getGameUrl() + ")"
                        : "Нет ссылки";
                String fieldValue = game.getDescription() + "\n" + gameUrl;
                embed.addField(game.getName(), fieldValue, false);
            }

            event.editMessageEmbeds(embed.build())
                    .queue();

        } else {
            // Для всех других категорий игр выводим полную информацию

            List<FileUpload> uploads = new ArrayList<>();
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Список игры в выбранной категории")
                    .setColor(0x00ff00);

            for (Game game : games) {
                String gameLink = (game.getGameUrl() != null && !game.getGameUrl().isEmpty())
                        ? "[Перейти к игре](" + game.getGameUrl() + ")"
                        : "Нет ссылки";
                String wikiLink = (game.getWikiUrl() != null && !game.getWikiUrl().isEmpty())
                        ? "[Wiki](" + game.getWikiUrl() + ")"
                        : "Нет wiki";
                String codesLink = (game.getCodesUrl() != null && !game.getCodesUrl().isEmpty())
                        ? "[Codes](" + game.getCodesUrl() + ")"
                        : "Нет кодов";
                String tierListLink = (game.getTierListLink() != null && !game.getTierListLink().isEmpty())
                        ? "[Tier List](" + game.getTierListLink() + ")"
                        : "Нет тир-листа";

                String fieldValue = game.getDescription() + "\n" + gameLink + "\n" + wikiLink + "\n" + codesLink + "\n" + tierListLink;
                embed.addField(game.getName(), fieldValue, false);
            }

            event.editMessageEmbeds(embed.build())
                    .setFiles(uploads)
                    .queue();
        }
    }
}
