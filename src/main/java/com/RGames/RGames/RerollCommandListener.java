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

        // Если нажата кнопка сброса
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

        // Обработка выбора игры
        if (buttonId.equals("reroll_AV") || buttonId.equals("reroll_AA") || buttonId.equals("reroll_ALS")) {
            Map<String, Long> userGames = userChoice.getOrDefault(userId, new HashMap<>());
            Long gameId;
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
                    System.out.println("Неизвестная кнопка выбора игры: '" + buttonId + "'");
                    event.deferEdit().queue();
                    return;
            }
            userGames.put(messageId, gameId);
            userChoice.put(userId, userGames);
            sendRollOptions(event);
            return;
        }

        // Обработка кнопок выбора типа ролла ("one_roll", "n_roll", "reroll_until_passive")
        Map<String, Long> userGames = userChoice.get(userId);
        if (userGames == null || !userGames.containsKey(messageId)) {
            event.reply("Сначала выберите игру!").setEphemeral(true).queue();
            return;
        }
        Long gameId = userGames.get(messageId);

        Long rollTypeId = switch (buttonId) {
            case "one_roll" -> 1L;
            case "n_roll" -> 2L;
            case "reroll_until_passive" -> 3L;
            default -> null;
        };

        if (rollTypeId == null) {
            System.out.println("Неизвестная кнопка типа ролла: '" + buttonId + "'");
            event.deferEdit().queue();
            return;
        }

        // Получаем список пассивок из БД
        List<Traits> traits = dbService.getTraitsByGameId(gameId);
        if (traits.isEmpty()) {
            event.reply("В этой игре отсутствуют пассивки!").setEphemeral(true).queue();
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
        }

        // Обработка нескольких роллов (n_roll)
        if (rollTypeId == 2L) {
            TextInput rollCountInput = TextInput.create("roll_count", "Количество роллов", TextInputStyle.SHORT)
                    .setPlaceholder("Введите число (например, 10)")
                    .setRequired(true)
                    .setMinLength(1)
                    .setMaxLength(4) // до 4 символов, т.е. число до 1000
                    .build();

            Modal modal = Modal.create("roll_count_modal", "Выбор количества роллов")
                    .addActionRow(rollCountInput)
                    .build();

            event.replyModal(modal).queue();
            return;
        }

        // Обработка роллов до выбранной пассивки
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

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        System.out.println("Получено событие модального окна. Значения: " + event.getValues());

        if (event.getValue("roll_count") == null) {
            System.out.println("Значение 'roll_count' не найдено.");
            event.reply("Не удалось получить значение количества роллов, попробуйте снова.").setEphemeral(true).queue();
            return;
        }

        String rollCountStr = event.getValue("roll_count").getAsString();
        System.out.println("Получено число: " + rollCountStr);

        long userId = event.getUser().getIdLong();

        try {
            int rollsToDo = Integer.parseInt(rollCountStr);
            if (rollsToDo <= 0 || rollsToDo > 1000) {
                event.reply("Число должно быть от 1 до 1000!").setEphemeral(true).queue();
                return;
            }

            Map<String, Long> userGames = userChoice.get(userId);
            if (userGames == null || userGames.isEmpty()) {
                event.reply("Сначала выберите игру!").setEphemeral(true).queue();
                return;
            }
            // Для модального окна используем первое найденное сообщение пользователя
            String messageId = userGames.keySet().iterator().next();
            Long gameId = userGames.get(messageId);

            List<Traits> traits = dbService.getTraitsByGameId(gameId);
            System.out.println("Получено пассивок: " + traits.size());
            if (traits.isEmpty()) {
                event.reply("В этой игре отсутствуют пассивки!").setEphemeral(true).queue();
                return;
            }

            List<Traits> rolledPassives = PassiveRoller.rollNPassives(traits, rollsToDo);

            // Группируем результаты
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
            embed.setTitle("Выпавшие пассивки! 🎉").setColor(0x00ff00);

            for (Map.Entry<String, Integer> entry : sortedPassives) {
                Traits trait = traitDetails.get(entry.getKey());
                int count = entry.getValue();
                embed.addField(trait.getTraitName() + " (" + count + ")",
                        "**Редкость:** " + trait.getTraitRarity() + "\n" +
                                "**Шанс:** " + String.format("%.2f%%", trait.getTraitChance()),
                        false);
            }

            event.editMessageEmbeds(embed.build()).queue();

        } catch (NumberFormatException e) {
            event.reply("Введите корректное число!").setEphemeral(true).queue();
        } catch (Exception e) {
            e.printStackTrace();
            event.reply("Произошла ошибка при обращении к базе данных!").setEphemeral(true).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("passive_select_menu")) {
            String targetPassiveName = event.getSelectedOptions().get(0).getValue().replace("roll_target_", "");
            System.out.println("Выбранная пассивка: " + targetPassiveName);
            long userId = event.getUser().getIdLong();

            Map<String, Long> userGames = userChoice.get(userId);
            if (userGames == null || userGames.isEmpty()) {
                event.reply("Сначала выберите игру!").setEphemeral(true).queue();
                return;
            }
            // Для простоты выбираем первое сообщение пользователя
            String messageId = userGames.keySet().iterator().next();
            Long gameId = userGames.get(messageId);

            List<Traits> traits = dbService.getTraitsByGameId(gameId);

            Traits targetTrait = traits.stream()
                    .filter(trait -> trait.getTraitName().equals(targetPassiveName))
                    .findFirst()
                    .orElse(null);

            if (targetTrait == null) {
                event.reply("Не удалось найти выбранную пассивку!").setEphemeral(true).queue();
                return;
            }

            int rolls = PassiveRoller.rollUntilPassive(traits, targetPassiveName);

            String traitInfo = "**Название**: " + targetTrait.getTraitName() + "\n" +
                    "**Редкость**: " + targetTrait.getTraitRarity() + "\n" +
                    "**Шанс**: " + String.format("%.2f%%", targetTrait.getTraitChance()) + "\n\n" +
                    "**Описание**: " + targetTrait.getTraitDescription();

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Вы выбили " + targetPassiveName + " за " + rolls + " роллов! 🎉")
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


    private void sendRollOptions (ButtonInteractionEvent event){
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