package com.RGames.RGames.DataBase.RepoInterface;

import com.RGames.RGames.DataBase.Entity.Game;
import com.RGames.RGames.DataBase.Entity.Traits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class TraitsRepositoryTest {

    @Autowired
    private TraitsRepository traitsRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Game game;

    @BeforeEach
    void setUp() {
        // Создаём и сохраняем игру
        game = new Game();
        game.setName("Test Game");
        entityManager.persist(game);
        entityManager.flush();

        // Создаём и сохраняем Traits, привязанные к игре
        Traits trait1 = new Traits(null, "Common", new BigDecimal("50.0"), "Простая пассивка", game, "Обычная");
        Traits trait2 = new Traits(null, "Rare", new BigDecimal("30.0"), "Редкая пассивка", game, "Редкая");
        Traits trait3 = new Traits(null, "Epic", new BigDecimal("10.0"), "Эпическая пассивка", game, "Эпическая");
        Traits trait4 = new Traits(null, "Legendary", new BigDecimal("5.0"), "Легендарная пассивка", game, "Легендарная");
        Traits trait5 = new Traits(null, "Mythic", new BigDecimal("1.0"), "Мифическая пассивка", game, "Мифическая");

        // Сохраняем в БД
        entityManager.persist(trait1);
        entityManager.persist(trait2);
        entityManager.persist(trait3);
        entityManager.persist(trait4);
        entityManager.persist(trait5);
        entityManager.flush();
    }

    @Test
    void testFindByGame() {
        List<Traits> foundTraits = traitsRepository.findByGame(game);

        // Проверяем, что вернулось 5 записей
        assertEquals(5, foundTraits.size(), "Ожидалось 5 Traits, но получено другое количество");

        // Проверяем, что все ожидаемые Traits есть в списке
        assertTrue(foundTraits.stream().anyMatch(t -> "Common".equals(t.getTraitName())), "Common отсутствует");
        assertTrue(foundTraits.stream().anyMatch(t -> "Rare".equals(t.getTraitName())), "Rare отсутствует");
        assertTrue(foundTraits.stream().anyMatch(t -> "Epic".equals(t.getTraitName())), "Epic отсутствует");
        assertTrue(foundTraits.stream().anyMatch(t -> "Legendary".equals(t.getTraitName())), "Legendary отсутствует");
        assertTrue(foundTraits.stream().anyMatch(t -> "Mythic".equals(t.getTraitName())), "Mythic отсутствует");
    }

    @Test
    void testFindByGame_EmptyResult() {
        // Создаём новую игру, но не добавляем к ней Traits
        Game otherGame = new Game();
        otherGame.setName("Another Game");
        entityManager.persist(otherGame);
        entityManager.flush();

        List<Traits> foundTraits = traitsRepository.findByGame(otherGame);
        assertTrue(foundTraits.isEmpty(), "Для другой игры не должно быть Traits");
    }
}
