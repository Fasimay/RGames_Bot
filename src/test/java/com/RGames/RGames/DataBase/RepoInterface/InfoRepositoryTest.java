package com.RGames.RGames.DataBase.RepoInterface;

import com.RGames.RGames.DataBase.Entity.Game;
import com.RGames.RGames.DataBase.Entity.Info;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class InfoRepositoryTest {

    @Autowired
    private InfoRepository infoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Game game1;
    private Game game2;

    @BeforeEach
    void setUp() {
        // Создаём и сохраняем игру
        game1 = new Game();
        game1.setName("Dota 2");
        entityManager.persist(game1);

        game2 = new Game();
        game2.setName("League of Legends");
        entityManager.persist(game2);

        entityManager.flush();
    }

    @Test
    void testfindByGameIdAndType() {
        // Создаём и сохраняем Info для game1
        Info info = new Info();
        info.setGame(game1);
        info.setType("WIKI");
        info.setContent("Wiki Link");
        entityManager.persist(info);
        entityManager.flush();

        // Ищем запись
        Optional<Info> foundInfo = infoRepository.findByGameIdAndType(game1.getId(), "WIKI");

        assertTrue(foundInfo.isPresent(), "Info не найдена");
        assertEquals("Wiki Link", foundInfo.get().getContent());
    }

    @Test
    void testfindByGameIdAndType_NotFound() {
        // Для game2 информации нет
        Optional<Info> foundInfo = infoRepository.findByGameIdAndType(game2.getId(), "WIKI");

        assertTrue(foundInfo.isEmpty(), "Для данной игры и типа не должно быть записей");
    }

    @Test
    void testfindByGameIdAndType_WrongType() {
        // Создаём и сохраняем Info с другим типом
        Info info = new Info();
        info.setGame(game1);
        info.setType("CODES");
        info.setContent("Code 1, Code 2");
        entityManager.persist(info);
        entityManager.flush();

        // Ищем запись по другому типу
        Optional<Info> foundInfo = infoRepository.findByGameIdAndType(game1.getId(), "WIKI");

        assertTrue(foundInfo.isEmpty(),"Неверный тип не должен возвращать запись");
    }
}
