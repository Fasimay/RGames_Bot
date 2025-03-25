package com.RGames.RGames;

import com.RGames.RGames.DataBase.DataBaseService;
import com.RGames.RGames.DataBase.Entity.Traits;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RerollCommandListenerTest {

    private RerollCommandListener rerollCommandListener;
    private DataBaseService mockDbService;
    private SlashCommandInteractionEvent mockSlashCommandEvent;
    private ButtonInteractionEvent mockButtonEvent;

    private List<Traits> mockTraits;

    @BeforeEach
    void setUp() {
        // Мокаются зависимости
        mockDbService = mock(DataBaseService.class);
        mockSlashCommandEvent = mock(SlashCommandInteractionEvent.class);
        mockButtonEvent = mock(ButtonInteractionEvent.class);
        rerollCommandListener = new RerollCommandListener(mockDbService);

        // Мокаем пользователя
        User mockUser = mock(User.class);
        when(mockUser.getIdLong()).thenReturn(12345L);
        when(mockButtonEvent.getUser()).thenReturn(mockUser);

        // Создаем игры с категориями
        mockTraits = List.of(
                new Traits(null, "Common", new BigDecimal("50.0"), "Простая пассивка", null, "Обычная"),
                new Traits(null, "Rare", new BigDecimal("30.0"), "Редкая пассивка", null, "Редкая"),
                new Traits(null, "Epic", new BigDecimal("10.0"), "Эпическая пассивка", null, "Эпическая"),
                new Traits(null, "Legendary", new BigDecimal("5.0"), "Легендарная пассивка", null, "Легендарная"),
                new Traits(null, "Mythic", new BigDecimal("1.0"), "Мифическая пассивка", null, "Мифическая")
        );
    }

    // Проверяет как бот реагирует на slash комманду
    @Test
    void testOnSlashCommandInteractionEvent() {
        // Создаем мок для ReplyCallbackAction (replyEmbeds().addActionRow().queue())
        ReplyCallbackAction mockReplyAction = mock(ReplyCallbackAction.class);

        // Настраиваем мок, чтобы replyEmbeds() возвращал mockReplyAction
        when(mockSlashCommandEvent.replyEmbeds(any(MessageEmbed.class)))
                .thenReturn(mockReplyAction);

        // Учитываем, что метод replyEmbeds(...) может вызываться с массивом
        when(mockSlashCommandEvent.replyEmbeds(any(MessageEmbed.class), any(MessageEmbed[].class)))
                .thenReturn(mockReplyAction);

        // Настраиваем addActionRow, чтобы он тоже возвращал mockReplyAction
        when(mockReplyAction.setActionRow(any(Button.class), any(Button.class), any(Button.class)))
                .thenReturn(mockReplyAction);

        // Отключаем выполнение queue(), чтобы тест не падал
        doNothing().when(mockReplyAction).queue();

        // Настраиваем команду
        when(mockSlashCommandEvent.getName()).thenReturn("reroll");

        // Вызываем обработчик
        rerollCommandListener.onSlashCommandInteraction(mockSlashCommandEvent);

        // Проверяем, что вызван replyEmbeds()
        verify(mockSlashCommandEvent, times(1)).replyEmbeds(any(MessageEmbed.class));
    }

    // Проверяет как бот реагирует на нажатие любой кнопки выбора игры
    @Test
    void testOnButtonInteractionEvent_anyGame() {
        // Создаем мок для цепочки вызовов editMessageEmbeds
        MessageEditCallbackAction mockEditAction = mock(MessageEditCallbackAction.class);

        // Замокаем вызов editMessageEmbeds для varargs (т.е. MessageEmbed...)
        when(mockButtonEvent.editMessageEmbeds((MessageEmbed[]) any()))
                .thenReturn(mockEditAction);

        // Настраиваем addActionRow, чтобы он тоже возвращал mockReplyAction
        when(mockEditAction.setActionRow(any(Button.class), any(Button.class), any(Button.class), any(Button.class)))
                .thenReturn(mockEditAction);

        // Отключаем выполнение queue(), чтобы тест не падал
        doNothing().when(mockEditAction).queue();

        when(mockButtonEvent.getComponentId()).thenReturn("reroll_AV");
        when(mockButtonEvent.getMessageId()).thenReturn("test_message_1");
        when(mockButtonEvent.isAcknowledged()).thenReturn(false);

        // Регистрируем владельца сообщения
        rerollCommandListener.registerMessageOwner("test_message_1", 12345L);

        // Вызываем обработчик
        rerollCommandListener.onButtonInteraction(mockButtonEvent);

        verify(mockButtonEvent, times(1)).editMessageEmbeds(any(MessageEmbed.class));
    }

    // Проверяет как бот реагирует на выбор one_roll
    @Test
    void testOnButtonInteractionEvent_oneRoll() {
        // Мокируем цепочку вызовов editMessageEmbeds -> setActionRow -> queue
        MessageEditCallbackAction mockEditAction = mock(MessageEditCallbackAction.class);

        // Убедимся, что editMessageEmbeds() не возвращает null
        when(mockButtonEvent.editMessageEmbeds(any(MessageEmbed[].class))).thenReturn(mockEditAction);

        // Убедимся, что setActionRow() возвращает mockEditAction
        when(mockEditAction.setActionRow(any(ItemComponent[].class))).thenReturn(mockEditAction);

        // Заглушаем вызов queue()
        doNothing().when(mockEditAction).queue(any(), any());

        String messageId = "test_message_1";
        when(mockButtonEvent.getMessageId()).thenReturn(messageId);
        // Устанавливаем isAcknowledged, например, в false, чтобы использовать ветку editMessageEmbeds
        when(mockButtonEvent.isAcknowledged()).thenReturn(false);

        // Регистрируем владельца сообщения
        rerollCommandListener.registerMessageOwner(messageId, 12345L);

        // Задаем выбор игры для данного пользователя, чтобы пропустить ветку выбора игры
        Map<String, Long> gameChoice = new HashMap<>();
        gameChoice.put(messageId, 1L);
        rerollCommandListener.userChoice.put(12345L, gameChoice);

        when(mockDbService.getTraitsByGameId(anyLong())).thenReturn(mockTraits);

        // Симулируем выбор "одного ролла"
        when(mockButtonEvent.getComponentId()).thenReturn("one_roll");

        // Вызываем onButtonInteraction для обработки одного ролла
        rerollCommandListener.onButtonInteraction(mockButtonEvent);

        // Проверяем, что был вызван метод editMessageEmbeds
        verify(mockButtonEvent, times(1)).editMessageEmbeds(any(MessageEmbed[].class));
    }

    // Проверяет как бот реагирует на выбор n_roll
    @Test
    void testOnButtonInteractionEvent_nRoll() {
        // Создаем мок для цепочки вызовов editMessageEmbeds -> setActionRow -> queue
        MessageEditCallbackAction mockEditAction = mock(MessageEditCallbackAction.class);

        // Убедимся, что editMessageEmbeds() не возвращает null
        when(mockButtonEvent.editMessageEmbeds(any(MessageEmbed[].class))).thenReturn(mockEditAction);

        // Убедимся, что setActionRow() тоже возвращает mockEditAction
        when(mockEditAction.setActionRow(any(ItemComponent[].class))).thenReturn(mockEditAction);

        // Добавляем мокирование queue() с любыми параметрами (или без)
        doNothing().when(mockEditAction).queue(any(), any());

        // Создаем мок для объекта, возвращаемого replyModal()
        ModalCallbackAction mockModalReply = mock(ModalCallbackAction.class);

        // Настраиваем, чтобы replyModal(any()) возвращал этот мок
        when(mockButtonEvent.replyModal(any())).thenReturn(mockModalReply);

        // Для метода queue() этого мока задаем doNothing()
        doNothing().when(mockModalReply).queue();

        // Создаем мок для ReplyCallbackAction
        ReplyCallbackAction mockReplyAction = mock(ReplyCallbackAction.class);

        // Настраиваем, чтобы event.reply(String) возвращал mockReplyAction
        when(mockButtonEvent.reply(anyString())).thenReturn(mockReplyAction);

        // Настраиваем цепочку вызовов для mockReplyAction
        when(mockReplyAction.setEphemeral(anyBoolean())).thenReturn(mockReplyAction);
        doNothing().when(mockReplyAction).queue();

        String messageId = "test_message_2";
        when(mockButtonEvent.getMessageId()).thenReturn(messageId);
        // Устанавливаем isAcknowledged, например, в false, чтобы использовать ветку editMessageEmbeds
        when(mockButtonEvent.isAcknowledged()).thenReturn(false);

        // Регистрируем владельца сообщения
        rerollCommandListener.registerMessageOwner(messageId, 12345L);

        // Задаем выбор игры для данного пользователя, чтобы пропустить ветку выбора игры
        Map<String, Long> gameChoice = new HashMap<>();
        gameChoice.put(messageId, 1L);
        rerollCommandListener.userChoice.put(12345L, gameChoice);

        when(mockDbService.getTraitsByGameId(anyLong())).thenReturn(mockTraits);

        when(mockButtonEvent.getComponentId()).thenReturn("n_roll");

        // Вызываем onButtonInteraction для обработки нескольких роллов
        rerollCommandListener.onButtonInteraction(mockButtonEvent);

        // Проверяем, что был вызван метод replyModal
        verify(mockButtonEvent, times(1)).replyModal(any());
    }

    // Проверяет как бот реагирует на выбор toPassiveRoll
    @Test
    void testOnButtonInteractionEvent_toPassiveRoll() {
        // Создаем общий mock-пользователь
        User sharedUser = mock(User.class);
        when(sharedUser.getIdLong()).thenReturn(12345L);

        // Настраиваем mock для кнопочного события
        MessageEditCallbackAction mockEditAction = mock(MessageEditCallbackAction.class);
        when(mockButtonEvent.editMessageEmbeds(any(MessageEmbed[].class))).thenReturn(mockEditAction);
        when(mockEditAction.setActionRow(any(ItemComponent[].class))).thenReturn(mockEditAction);
        doNothing().when(mockEditAction).queue(any(), any());

        ReplyCallbackAction mockReplyAction = mock(ReplyCallbackAction.class);
        when(mockButtonEvent.reply(anyString())).thenReturn(mockReplyAction);
        when(mockReplyAction.setEphemeral(anyBoolean())).thenReturn(mockReplyAction);
        doNothing().when(mockReplyAction).queue();

        // Настраиваем mock для кнопочного события
        String messageId = "test_message_3";
        when(mockButtonEvent.getMessageId()).thenReturn(messageId);
        when(mockButtonEvent.isAcknowledged()).thenReturn(false);
        when(mockButtonEvent.getComponentId()).thenReturn("reroll_until_passive");
        when(mockButtonEvent.getUser()).thenReturn(sharedUser);

        // Настраиваем mock для события выбора из меню
        StringSelectInteractionEvent mockSelectEvent = mock(StringSelectInteractionEvent.class);
        when(mockSelectEvent.getComponentId()).thenReturn("passive_select_menu");
        when(mockSelectEvent.getMessageId()).thenReturn(messageId);
        when(mockSelectEvent.getUser()).thenReturn(sharedUser);

        // Мокаем поведение метода editMessageEmbeds для события выбора
        MessageEditCallbackAction mockSelectEditAction = mock(MessageEditCallbackAction.class);
        when(mockSelectEvent.editMessageEmbeds(any(MessageEmbed.class))).thenReturn(mockSelectEditAction);
        when(mockSelectEditAction.setActionRow(any(ItemComponent[].class))).thenReturn(mockSelectEditAction);
        when(mockSelectEvent.getSelectedOptions()).thenReturn(List.of(SelectOption.of("Rare", "roll_target_Rare")));
        doNothing().when(mockSelectEditAction).queue();

        // Регистрируем владельца сообщения и выбор игры для пользователя
        rerollCommandListener.registerMessageOwner(messageId, 12345L);
        Map<String, Long> gameChoice = new HashMap<>();
        gameChoice.put(messageId, 1L);
        rerollCommandListener.userChoice.put(12345L, gameChoice);

        when(mockDbService.getTraitsByGameId(anyLong())).thenReturn(mockTraits);

        // Вызываем обработчик для нажатия кнопки "До выбранной пассивки"
        rerollCommandListener.onButtonInteraction(mockButtonEvent);
        verify(mockButtonEvent, times(1)).editMessageEmbeds(any(MessageEmbed[].class));

        // Вызываем обработчик выбора пассивки
        rerollCommandListener.onStringSelectInteraction(mockSelectEvent);
        verify(mockSelectEvent, times(1)).editMessageEmbeds(any(MessageEmbed.class));
    }

    @Test
    void testOnButtonInteractionEvent_notMessageOwner() {
        String messageId = "test_message_4";
        when(mockButtonEvent.getMessageId()).thenReturn(messageId);
        when(mockButtonEvent.getComponentId()).thenReturn("AV_reroll");
        when(mockButtonEvent.isAcknowledged()).thenReturn(false);


        rerollCommandListener.onButtonInteraction(mockButtonEvent);

        // В зависимости от логики можно проверить, что, например, editMessageEmbeds не вызывается
        verify(mockButtonEvent, never()).editMessageEmbeds(any(MessageEmbed[].class));
    }
}