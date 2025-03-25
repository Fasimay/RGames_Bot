package com.RGames.RGames;

import com.RGames.RGames.DataBase.DataBaseService;
import com.RGames.RGames.DataBase.Entity.Traits;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RerollCommandListener extends ListenerAdapter {

    private final DataBaseService dbService;
    private final Map<String, Long> messageOwners = new HashMap<>();
    final Map<Long, Map<String, Long>> userChoice = new HashMap<>();
    private final Map<Long, Integer> rollCount = new HashMap<>();
    private final Map<Long, Map<String, Integer>> rollHistory = new HashMap<>();

    public RerollCommandListener(DataBaseService dbService) {
        this.dbService = dbService;
    }

    public void registerMessageOwner(String messageId, long userId) {
        messageOwners.put(messageId, userId);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        System.out.println("Получено сообщение: " + event.getName()); // Проверка

        if (!event.getName().equals("reroll")) return;


         EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Выбор игры \uD83C\uDFAE")
                .setDescription("Выберете игру, которая вас интересует:")
                .setColor(0x00ff00);

            event.replyEmbeds(embed.build())
                    .setActionRow(
                        Button.primary("reroll_AA", "Anime Adventures"),
                        Button.success("reroll_AV", "Anime Vanguards"),
                        Button.danger("reroll_ALS", "Anime Last Stand")
                    ).queue(response -> {
                        // Получаем отправленное сообщение и сохраняем его id для проверки владельца при нажатии кнопок.
                        event.getHook().retrieveOriginal().queue(message -> {
                            messageOwners.put(message.getId(), event.getUser().getIdLong());
                        });
            });
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId().trim();
        System.out.println("Button pressed: " + buttonId);

        String messageId = event.getMessageId();
        Long userId = event.getUser().getIdLong();

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

        if ("reset_rolls".equals(buttonId)) {
            // Сбрасываем выбор игры и счетчик роллов
            userChoice.remove(userId);
            rollCount.remove(userId);
            rollHistory.remove(userId);

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Выбор игры 🎮")
                    .setDescription("Выберите категорию игр, которая вас интересует:")
                    .setColor(0x00ff00);

            event.editMessageEmbeds(embed.build())
                    .setActionRow(
                            Button.primary("reroll_AA", "Anime Adventures"),
                            Button.success("reroll_AV", "Anime Vanguards"),
                            Button.danger("reroll_ALS", "Anime Last Stand")
                    )
                    .queue();

            System.out.println("Rolls reset for user: " + userId);
            return;
        }

        // Если пользователь еще не выбрал игру, обрабатываем кнопки выбора игры
        if (!userChoice.containsKey(userId)) {
            Long gameId = null;
            switch (buttonId) {
                case "reroll_AV":
                    gameId = 1L;
                    break;
                case "reroll_AA":
                    gameId = 2L;
                    break;
                case "reroll_ALS":
                    gameId = 3L;
                    break;
                default:
                    System.out.println("Unknown gameId button: '" + buttonId + "'");
                    // Вместо reply — лучше просто подтвердить взаимодействие, чтобы не возникало ошибки:
                    event.deferEdit().queue();
                    return;
            }
            userChoice.put(userId, gameId);
            // Обновляем сообщение, показывая кнопки для выбора типа ролла
            sendRollOptions(event);
            return;
        } else {
            // Если игра уже выбрана, обрабатываем кнопки типа ролла
            System.out.println("Received roll button: '" + buttonId + "' (length: " + buttonId.length() + ")");
            Long rollTypeId = null;
            if ("reroll_one_roll".equals(buttonId)) {
                rollTypeId = 1L;
            } else if ("reroll_n_roll".equals(buttonId)) {
                rollTypeId = 2L;
            } else if ("reroll_to_passive".equals(buttonId)) {
                rollTypeId = 3L;
        }
        // Если игра уже выбрана, обрабатываем кнопки типа ролла
        System.out.println("Received roll button: '" + buttonId + "' (length: " + buttonId.length() + ")");

        Long rollTypeId = switch (buttonId) {
            case "one_roll" -> 1L;
            case "n_roll" -> 2L;
            case "reroll_until_passive" -> 3L;
            default -> null;
        };

        if (rollTypeId == null) {
            System.out.println("Unknown rollTypeId button: '" + buttonId + "'");
            event.deferEdit().queue();
            return;
        }

        // Получаем выбранную игру
        Long gameId = userChoice.get(userId).get(messageId);
        if (gameId == null) {
            event.reply("Сначала выбери игру!").setEphemeral(true).queue();
            return;
        }

        // Берем список пассивок из БД
        List<Traits> traits = dbService.getTraitsByGameId(gameId);
        if (traits.isEmpty()) {
            event.reply("Пассивок в этой игре нету!").setEphemeral(true).queue();
            return;
        }

        // Обработка 1 ролла
        if (rollTypeId == 1L) {
            rollCount.put(userId, rollCount.getOrDefault(userId, 0) + 1); // Увеличиваем счетчик
            int count = rollCount.get(userId);
            Traits rolledPassive = PassiveRoller.rollOnePassive(traits);

            // Обновляем историю выпадений
            rollHistory.putIfAbsent(userId, new HashMap<>());
            Map<String, Integer> userHistory = rollHistory.get(userId);
            userHistory.put(rolledPassive.getTraitName(), userHistory.getOrDefault(rolledPassive.getTraitName(), 0) + 1);

            // Формируем строку истории выпадений в нужном формате
            StringBuilder historyText = new StringBuilder("\n\n**📜 История выпадений:**\n");
            for (Map.Entry<String, Integer> entry : userHistory.entrySet()) {
                historyText.append(String.format("%s (%d раз)\n", entry.getKey(), entry.getValue()));
            }

            if (rolledPassive != null) {
                String traitInfo = "**Название**: " + rolledPassive.getTraitName() + "\n" +
                        "**Редкость**: " + rolledPassive.getTraitRarity() + "\n" +
                        "**Шанс**: " + String.format("%.2f%%", rolledPassive.getTraitChance()) + "\n\n" +
                        "**Описание**: " + rolledPassive.getTraitDescription() + "\n\n" +
                        "**Роллов сделано**: " + count + historyText.toString();

                // Если взаимодействие уже подтверждено, обновляем через hook
                if (event.isAcknowledged()) {
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                    .setTitle("Вам выпала пассивка! 🎉")
                                    .setDescription(traitInfo)
                                    .setColor(0x00ff00)
                                    .build())
                            .setActionRow(
                                    Button.primary("one_roll", "Один ролл"),
                                    Button.success("n_roll", "Определённое количество роллов"),
                                    Button.danger("reroll_until_passive", "До выбранной пассивки"),
                                    Button.secondary("reset_rolls", "🔄 Reset")
                            ).queue();
                } else {
                    event.editMessageEmbeds(new EmbedBuilder()
                                    .setTitle("Вам выпала пассивка! 🎉")
                                    .setDescription(traitInfo)
                                    .setColor(0x00ff00)
                                    .build())
                            .setActionRow(
                                    Button.primary("one_roll", "Один ролл"),
                                    Button.success("n_roll", "Определённое количество роллов"),
                                    Button.danger("reroll_until_passive", "До выбранной пассивки"),
                                    Button.secondary("reset_rolls", "🔄 Reset")
                            ).queue();
                }
            } else {
                System.out.println("Unknown roll type button: '" + buttonId + "'");
                // Вместо reply — лучше просто подтвердить взаимодействие, чтобы не возникало ошибки:
                event.deferEdit().queue();
                return;
            }
            // Получаем выбранную игру
            Long gameId = userChoice.get(userId);
            if (gameId == null) {
                event.reply("Сначала выбери игру!").setEphemeral(true).queue();
                return;
            }
            // Берем список пассивок из БД
            List<Traits> traits = dbService.getTraitsByGameId(gameId);
            if (traits.isEmpty()) {
                event.reply("Пассивок в этой игре нету!").setEphemeral(true).queue();
                return;

            // Обработка 1 ролла    
            }
            if (rollTypeId == 1L) {
                rollCount.put(userId, rollCount.getOrDefault(userId, 0) + 1); // Увеличиваем счетчик
                int count = rollCount.get(userId);
                Traits rolledPassive = PassiveRoller.rollOnePassive(traits);

                // Обновляем историю выпадений
                rollHistory.putIfAbsent(userId, new HashMap<>());
                Map<String, Integer> userHistory = rollHistory.get(userId);
                userHistory.put(rolledPassive.getTraitName(), userHistory.getOrDefault(rolledPassive.getTraitName(), 0) + 1);

                // Формируем строку истории выпадений в нужном формате
                StringBuilder historyText = new StringBuilder("\n\n**📜 История выпадений:**\n");
                for (Map.Entry<String, Integer> entry : userHistory.entrySet()) {
                    historyText.append(String.format("%s (%d раз)\n", entry.getKey(), entry.getValue()));
                }

                if (rolledPassive != null) {
                    String traitInfo = "**Название**: " + rolledPassive.getTraitName() + "\n" +
                            "**Редкость**: " + rolledPassive.getTraitRarity() + "\n" +
                            "**Шанс**: " + String.format("%.2f%%", rolledPassive.getTraitChance()) + "\n\n" +
                            "**Описание**: " + rolledPassive.getTraitDescription() + "\n\n" +
                            "**Роллов сделано**: " + count + historyText.toString();

                    // Если взаимодействие уже подтверждено, обновляем через hook
                    if (event.isAcknowledged()) {
                        event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                        .setTitle("Вам выпала пассивка! 🎉")
                                        .setDescription(traitInfo)
                                        .setColor(0x00ff00)
                                        .build())
                                .setActionRow(
                                        Button.primary("reroll_one_roll", "Один ролл"),
                                        Button.success("reroll_n_roll", "Определённое количество роллов"),
                                        Button.danger("reroll_to_passive", "До выбранной пассивки"),
                                        Button.secondary("reset_rolls", "🔄 Reset")
                                ).queue();
                    } else {
                        event.editMessageEmbeds(new EmbedBuilder()
                                        .setTitle("Вам выпала пассивка! 🎉")
                                        .setDescription(traitInfo)
                                        .setColor(0x00ff00)
                                        .build())
                                .setActionRow(
                                        Button.primary("reroll_one_roll", "Один ролл"),
                                        Button.success("reroll_n_roll", "Определённое количество роллов"),
                                        Button.danger("reroll_to_passive", "До выбранной пассивки"),
                                        Button.secondary("reset_rolls", "🔄 Reset")
                                ).queue();
                    }
                } else {
                    event.reply("Ошибка! Не удалось выбить пассивку.").setEphemeral(true).queue();
                }
            }

            // Обработка n колличество роллов
            if (rollTypeId == 2L) {
                TextInput rollCountInput = TextInput.create("roll_count", "Количество роллов", TextInputStyle.SHORT)
                        .setPlaceholder("Введите число (например, 10)")
                        .setRequired(true)
                        .setMinLength(1)
                        .setMaxLength(1000) // Ограничение, например, 99 роллов максимум
                        .build();

                Modal modal = Modal.create("roll_count_modal", "Выбор количества роллов")
                        .addActionRow(rollCountInput)
                        .build();

                event.replyModal(modal).queue();
            }

            // Обработка роллов до пассивки
            if (rollTypeId == 3L) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Выберите пассивку 🎯")
                        .setDescription("Нажмите на кнопку с пассивкой, которую хотите получить.")
                        .setColor(0x00ff00);

                List<SelectOption> options = new ArrayList<>();
                for (Traits trait : traits) {
                    options.add(SelectOption.of(trait.getTraitName(), "roll_target_" + trait.getTraitName()));
                }

                StringSelectMenu menu = StringSelectMenu.create("passive_select_menu")
                        .setPlaceholder("Выберите пассивку")
                        .addOptions(options)
                        .build();

                event.editMessageEmbeds(embed.build()).setActionRow(menu).queue();
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        System.out.println("Получено событие модального окна. Все значения: " + event.getValues());

        // Проверяем наличие значения с идентификатором "roll_count"
        if (event.getValue("roll_count") == null) {
            System.out.println("Значение с ключом 'roll_count' не найдено.");
            event.reply("Не удалось получить значение роллов, попробуйте снова.").setEphemeral(true).queue();
            return;
        }

        String rollCountStr = event.getValue("roll_count").getAsString();
        System.out.println("Получено число: " + rollCountStr);

        long userId = event.getUser().getIdLong();

        try {
            int rollCount = Integer.parseInt(rollCountStr);
            if (rollCount <= 0 || rollCount > 10000) {
                event.reply("Число должно быть от 1 до 1000 еблан!").setEphemeral(true).queue();
                return;
            }

            // Получаем выбранную игру пользователя
            Long gameId = userChoice.get(userId);
            if (gameId == null) {
                event.reply("Сначала выберите игру!").setEphemeral(true).queue();
                return;
            }

            try {
                // Получаем пассивки из игры
                List<Traits> traits = dbService.getTraitsByGameId(gameId);
                System.out.println("Получено пассивок: " + traits.size());
                if (traits.isEmpty()) {
                    event.reply("Пассивок в этой игре нет!").setEphemeral(true).queue();
                    return;
                }

                // Отправка пассивок в метод подсчета
                List<Traits> rolledPassives = PassiveRoller.rollNPassives(traits, rollCount);

                // Группируем пассивки по названию
                Map<String, Integer> countMap = new HashMap<>();
                Map<String, Traits> traitDetails = new HashMap<>();

                for (Traits trait : rolledPassives) {
                    String name = trait.getTraitName();
                    countMap.put(name, countMap.getOrDefault(name, 0) + 1);
                    traitDetails.putIfAbsent(name, trait);
                }

                List<Map.Entry<String, Integer>> sortedPassives = new ArrayList<>(countMap.entrySet());
                sortedPassives.sort(Comparator.comparing(entry -> traitDetails.get(entry.getKey()).getTraitChance()));

                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Вам выпали такие пассивки! 🎉").setColor(0x00ff00);

                for (Map.Entry<String, Integer> entry : sortedPassives) {
                    Traits trait = traitDetails.get(entry.getKey());
                    int count = entry.getValue();

                    embed.addField(trait.getTraitName() + " (" + count + ")\u200B",
                            "**Редкость:** " + trait.getTraitRarity() + "\n" +
                            "**Шанс:** " + String.format("%.2f%%", trait.getTraitChance()), false);

                }

                event.editMessageEmbeds(embed.build()).queue();
            } catch (Exception e) {
                e.printStackTrace();
                event.reply("Произошла ошибка при обращении к базе данных!").setEphemeral(true).queue();
            }
        } catch (NumberFormatException e) {
            event.reply("Введите корректное число!").setEphemeral(true).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("passive_select_menu")) {
            String targetPassiveName = event.getSelectedOptions().get(0).getValue().replace("roll_target_", "");
            System.out.println("Выбранная пассивка: " + targetPassiveName);
            Long userId = event.getUser().getIdLong();
            Long gameId = userChoice.get(userId);

            if (gameId == null) {
                event.reply("Сначала выбери игру ебло!").setEphemeral(true).queue();
                return;
            }

            List<Traits> traits = dbService.getTraitsByGameId(gameId);

            Traits targetTrait = traits.stream()
                    .filter(trait -> trait.getTraitName().equals(targetPassiveName))
                    .findFirst()
                    .orElse(null);

            if (targetTrait == null) {
                event.reply("Не удалось найти пассивку еж еблан!").setEphemeral(true).queue();
                return;
            }

            int rolls = PassiveRoller.rollUntilPassive(traits, targetPassiveName);

            String traitInfo = "**Название**: " + targetTrait.getTraitName() + "\n" +
                    "**Редкость**: " + targetTrait.getTraitRarity() + "\n" +
                    "**Шанс**: " + String.format("%.2f%%", targetTrait.getTraitChance()) + "\n\n" +
                    "**Описание**: " + targetTrait.getTraitDescription();

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Вы выбили **" + targetPassiveName + "** за **" + rolls + "** роллов! 🎉")
                    .setDescription(traitInfo)
                    .setColor(0x00ff00);

            event.editMessageEmbeds(embed.build())
                    .setActionRow(
                            Button.primary("one_roll", "Один ролл"),
                            Button.success("n_roll", "Определённое количество роллов"),
                            Button.danger("reroll_until_passive", "До выбранной пассивки"),
                            Button.secondary("reset_rolls", "🔄 Reset")
                    ).queue();
        }
    }

    private void sendRollOptions(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Выбор типа ролла")
                .setDescription("Выберите тип ролла, который вас интересует:")
                .setColor(0x00ff00);
        if (event.isAcknowledged()) {
            event.getHook().editOriginalEmbeds(embed.build())
                    .setActionRow(
                            Button.primary("one_roll", "Один ролл"),
                            Button.success("n_roll", "Определённое количество роллов"),
                            Button.danger("reroll_until_passive", "До выбранной пассивки"),
                            Button.secondary("reset_rolls", "🔄 Reset")
                    ).queue(
                            success -> System.out.println("Сообщение обновлено успешно через hook!"),
                            failure -> System.out.println("Ошибка при обновлении через hook: " + failure.getMessage())
                    );
        } else {
            event.editMessageEmbeds(embed.build())
                    .setActionRow(
                            Button.primary("one_roll", "Один ролл"),
                            Button.success("n_roll", "Определённое количество роллов"),
                            Button.danger("reroll_until_passive", "До выбранной пассивки"),
                            Button.secondary("reset_rolls", "🔄 Reset")
                    ).queue(
                            success -> System.out.println("Сообщение обновлено успешно!"),
                            failure -> System.out.println("Ошибка при обновлении: " + failure.getMessage())
                    );
        }
    }
}