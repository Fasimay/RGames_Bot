package com.RGames.RGames;

import com.RGames.RGames.DataBase.DataBaseService;
import com.RGames.RGames.DataBase.Entity.Category;
import com.RGames.RGames.DataBase.Entity.Game;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import net.dv8tion.jda.api.utils.FileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GameCommandListenerTest {

    private GameCommandListener gameCommandListener;
    private DataBaseService mockDbService;
    private SlashCommandInteractionEvent mockSlashCommandEvent;
    private ButtonInteractionEvent mockButtonEvent;

    private List<Game> mockGames;

    @BeforeEach
    void setUp() {
        // Мокаются зависимости
        mockDbService = mock(DataBaseService.class);
        mockSlashCommandEvent = mock(SlashCommandInteractionEvent.class);
        mockButtonEvent = mock(ButtonInteractionEvent.class);
        gameCommandListener = new GameCommandListener(mockDbService);

        // Мокаем пользователя
        User mockUser = mock(User.class);
        when(mockUser.getIdLong()).thenReturn(12345L);
        when(mockButtonEvent.getUser()).thenReturn(mockUser);


        // Создаем категории
        Category tdCategory = new Category(1L, "Tower Defense");
        Category funCategory = new Category(2L, "Fun Games");
        Category webCategory = new Category(3L, "Web Games");
        
        // Создаем игры с категориями
        mockGames = List.of(
            new Game(1L, "TD Game 1", "TD Desc 1", "wiki1.com", "codes1.com", tdCategory, "tier1.com", "https://tdgame1.com"),
            new Game(2L, "TD Game 2", "TD Desc 2", "wiki2.com", "codes2.com", tdCategory, "tier2.com", "https://tdgame2.com"),
            new Game(3L, "Fun Game 1", "Fun Desc 1", "wiki3.com", "codes3.com", funCategory, "tier3.com", "https://fungame1.com"),
            new Game(4L, "Fun Game 2", "Fun Desc 2", "wiki4.com", "codes4.com", funCategory, "tier4.com", "https://fungame2.com"),
            new Game(5L, "Web Game 1", "Web Desc 1", "wiki5.com", "codes5.com", webCategory, "tier5.com", "https://webgame1.com"),
            new Game(6L, "Web Game 2", "Web Desc 2", "wiki6.com", "codes6.com", webCategory, "tier6.com", "https://webgame2.com")
        );
    }

    // Проверяет как бот реагирует на slash комманду
    @Test
    void testOnSlashCommandInteraction() {
        // Создаем мок для ReplyCallbackAction (replyEmbeds().addActionRow().queue())
        ReplyCallbackAction mockReplyAction = mock(ReplyCallbackAction.class);

        // Настраиваем мок, чтобы replyEmbeds() возвращал mockReplyAction
        when(mockSlashCommandEvent.replyEmbeds(any(MessageEmbed.class)))
                .thenReturn(mockReplyAction);

        // Учитываем, что метод replyEmbeds(...) может вызываться с массивом
        when(mockSlashCommandEvent.replyEmbeds(any(MessageEmbed.class), any(MessageEmbed[].class)))
                .thenReturn(mockReplyAction);

        // Настраиваем addActionRow, чтобы он тоже возвращал mockReplyAction
        when(mockReplyAction.addActionRow(any(Button.class), any(Button.class), any(Button.class)))
                .thenReturn(mockReplyAction);

        // Отключаем выполнение queue(), чтобы тест не падал
        doNothing().when(mockReplyAction).queue();

        // Настраиваем команду
        when(mockSlashCommandEvent.getName()).thenReturn("games");

        // Вызываем обработчик
        gameCommandListener.onSlashCommandInteraction(mockSlashCommandEvent);

        // Проверяем, что вызван replyEmbeds()
        verify(mockSlashCommandEvent, times(1)).replyEmbeds(any(MessageEmbed.class));
    }

    // Проверяет как бот реагирует на нажатие кнопки tdGames
    @Test
    void testOnButtonInteraction_tdGames() {
        // Создаем мок для цепочки вызовов editMessageEmbeds
        MessageEditCallbackAction mockEditAction = mock(MessageEditCallbackAction.class);

        // Замокаем вызов editMessageEmbeds для varargs (т.е. MessageEmbed...)
        when(mockButtonEvent.editMessageEmbeds((MessageEmbed[]) any()))
                .thenReturn(mockEditAction);

        // При вызове setFiles нужно указать, что аргумент – коллекция (чтобы убрать неоднозначность)
        when(mockEditAction.setFiles((Collection<? extends FileUpload>) any()))
                .thenReturn(mockEditAction);
        doNothing().when(mockEditAction).queue();

        when(mockButtonEvent.getComponentId()).thenReturn("td_games");
        when(mockButtonEvent.getMessageId()).thenReturn("test_message_1");
        when(mockButtonEvent.isAcknowledged()).thenReturn(false);
        when(mockDbService.getGamesByCategoryId(1L))
                .thenReturn(mockGames.stream().filter(g -> g.getCategory().getId() == 1L).toList());

        // Регистрируем владельца сообщения, чтобы условие в onButtonInteraction прошло
        gameCommandListener.registerMessageOwner("test_message_1", 12345L);

        gameCommandListener.onButtonInteraction(mockButtonEvent);

        // Проверяем, что был вызван метод editMessageEmbeds с любыми MessageEmbed (varargs)
        verify(mockButtonEvent, times(1)).editMessageEmbeds((MessageEmbed[]) any());
    }

    // Проверяет как бот реагирует на нажатие кнопки funGames
    @Test
    void testOnButtonInteraction_funGames() {
        // Создаем мок для цепочки вызовов editMessageEmbeds
        MessageEditCallbackAction mockEditAction = mock(MessageEditCallbackAction.class);

        // Настраиваем мок editMessageEmbeds
        when(mockButtonEvent.editMessageEmbeds((MessageEmbed[]) any()))
                .thenReturn(mockEditAction);

        // Новый мок для setActionRow!
        when(mockEditAction.setActionRow(any(ItemComponent[].class)))
                .thenReturn(mockEditAction);

        // Настройка мока кнопки и базы данных
        when(mockButtonEvent.getComponentId()).thenReturn("fun_games");
        when(mockButtonEvent.getMessageId()).thenReturn("test_message_2");
        when(mockButtonEvent.isAcknowledged()).thenReturn(false);
        when(mockDbService.getGamesByCategoryId(2L))
                .thenReturn(mockGames.stream().filter(g -> g.getCategory().getId() == 2L).toList());

        // Регистрируем владельца сообщения
        gameCommandListener.registerMessageOwner("test_message_2", 12345L);

        // Вызываем обработчик
        gameCommandListener.onButtonInteraction(mockButtonEvent);

        // Проверяем, что editMessageEmbeds действительно был вызван
        verify(mockButtonEvent, times(1)).editMessageEmbeds((MessageEmbed[]) any());
    }

    // Проверяет как бот реагирует на нажатие кнопки webGames
    @Test
    void testOnButtonInteraction_webGames() {
        // Создаем мок для цепочки вызовов editMessageEmbeds
        MessageEditCallbackAction mockEditAction = mock(MessageEditCallbackAction.class);

        // Настраиваем мок editMessageEmbeds
        when(mockButtonEvent.editMessageEmbeds((MessageEmbed[]) any()))
                .thenReturn(mockEditAction);

        // Настройка мока кнопки и базы данных
        when(mockButtonEvent.getComponentId()).thenReturn("web_games");
        when(mockButtonEvent.getMessageId()).thenReturn("test_message_3");
        when(mockButtonEvent.isAcknowledged()).thenReturn(false);
        when(mockDbService.getGamesByCategoryId(3L))
                .thenReturn(mockGames.stream().filter(g -> g.getCategory().getId() == 3L).toList());

        // Регистрируем владельца сообщения
        gameCommandListener.registerMessageOwner("test_message_3", 12345L);

        // Вызываем обработчик
        gameCommandListener.onButtonInteraction(mockButtonEvent);

        // Проверяем, что editMessageEmbeds действительно был вызван
        verify(mockButtonEvent, times(1)).editMessageEmbeds((MessageEmbed[]) any());
    }

    // Проверяет как бот реагирует на то если передать ему пустой лист
    @Test
    void testOnButtonInteraction_tdGames_noGames() {
        // Создаем мок для ReplyCallbackAction (цепочка reply().setEphemeral().queue())
        ReplyCallbackAction mockReplyAction = mock(ReplyCallbackAction.class);

        // Настраиваем мок, чтобы reply() возвращал mockReplyAction
        when(mockButtonEvent.reply(anyString()))
                .thenReturn(mockReplyAction);

        // Настраиваем setEphemeral, чтобы он возвращал mockReplyAction
        when(mockReplyAction.setEphemeral(anyBoolean()))
                .thenReturn(mockReplyAction);

        // Отключаем выполнение queue(), чтобы тест не падал
        doNothing().when(mockReplyAction).queue();

        // Настройка данных для теста
        when(mockButtonEvent.getComponentId()).thenReturn("td_games");
        when(mockButtonEvent.getMessageId()).thenReturn("test_message_3");
        when(mockButtonEvent.isAcknowledged()).thenReturn(false);

        // ❗ Важно: пустой список игр!
        when(mockDbService.getGamesByCategoryId(1L)).thenReturn(List.of());

        // Регистрируем владельца сообщения
        gameCommandListener.registerMessageOwner("test_message_3", 12345L);

        // Вызываем обработчик
        gameCommandListener.onButtonInteraction(mockButtonEvent);

        // Проверяем, что reply("Игры в этой категории не добавлены!") был вызван
        verify(mockButtonEvent, times(1)).reply("Игры в этой категории не добавлены!");
    }

    // Проверяет как бот реагирует если с сообщением взаимодействует не его создатель
    @Test
    void testOnButtonInteraction_notOwner() {
        // Создаем мок для ReplyCallbackAction (цепочка reply().setEphemeral().queue())
        ReplyCallbackAction mockReplyAction = mock(ReplyCallbackAction.class);

        // Настраиваем мок, чтобы reply() возвращал mockReplyAction
        when(mockButtonEvent.reply(anyString()))
                .thenReturn(mockReplyAction);

        // Настраиваем setEphemeral, чтобы он возвращал mockReplyAction
        when(mockReplyAction.setEphemeral(anyBoolean()))
                .thenReturn(mockReplyAction);

        // Отключаем выполнение queue(), чтобы тест не падал
        doNothing().when(mockReplyAction).queue();

        // Настраиваем ID кнопки
        when(mockButtonEvent.getComponentId()).thenReturn("td_games");

        // ❗ Важно: в тесте пользователь НЕ должен быть владельцем сообщения
        when(mockButtonEvent.getUser().getIdLong()).thenReturn(99999L); // Другой пользователь
        when(mockButtonEvent.getMessageId()).thenReturn("test_message_4");

        // Регистрируем владельца сообщения (но это ДРУГОЙ пользователь)
        gameCommandListener.registerMessageOwner("test_message_4", 12345L);

        // Вызываем обработчик
        gameCommandListener.onButtonInteraction(mockButtonEvent);

        // Проверяем, что бот ответил "Иди в пизду не твоё"
        verify(mockButtonEvent, times(1)).reply("Иди в пизду не твоё");
    }

    // Проверяет что будет если взаимодействовать с кнопкой которой нету
    @Test
    void testOnButtonInteraction_unknownButton() {
        // Создаем мок для ReplyCallbackAction (reply().setEphemeral().queue())
        ReplyCallbackAction mockReplyAction = mock(ReplyCallbackAction.class);

        // Настраиваем мок, чтобы reply() возвращал mockReplyAction
        when(mockButtonEvent.reply(anyString()))
                .thenReturn(mockReplyAction);

        // Настраиваем setEphemeral, чтобы он возвращал mockReplyAction
        when(mockReplyAction.setEphemeral(anyBoolean()))
                .thenReturn(mockReplyAction);

        // Отключаем выполнение queue(), чтобы тест не падал
        doNothing().when(mockReplyAction).queue();

        // Настраиваем кнопку, которой нет в обработчике
        when(mockButtonEvent.getComponentId()).thenReturn("unknown_button");
        when(mockButtonEvent.getMessageId()).thenReturn("test_message_5");
        when(mockButtonEvent.isAcknowledged()).thenReturn(false);

        // Регистрируем владельца сообщения
        gameCommandListener.registerMessageOwner("test_message_5", 12345L);

        // Вызываем обработчик
        gameCommandListener.onButtonInteraction(mockButtonEvent);

        // Проверяем, что бот ответил "Unknown game type button!"
        verify(mockButtonEvent, times(1)).reply("Unknown game type button!");
    }
}
    