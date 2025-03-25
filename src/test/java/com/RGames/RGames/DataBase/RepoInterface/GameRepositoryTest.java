package com.RGames.RGames.DataBase.RepoInterface;

import com.RGames.RGames.DataBase.Entity.Category;
import com.RGames.RGames.DataBase.Entity.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class GameRepositoryTest {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Category category1;
    private Category category2;

    @BeforeEach
    void setUp() {
        // Создаём и сохраняем две категории
        category1 = new Category();
        category1.setName("Category 1");
        entityManager.persist(category1);

        category2 = new Category();
        category2.setName("Category 2");
        entityManager.persist(category2);

        entityManager.flush();
    }

    @Test
    void testFindByCategory_Id() {
        // Создаём и сохраняем игры, привязывая их к category1
        Game game1 = new Game();
        game1.setName("Game 1");
        game1.setCategory(category1);
        entityManager.persist(game1);

        Game game2 = new Game();
        game2.setName("Game 2");
        game2.setCategory(category1);
        entityManager.persist(game2);


        entityManager.flush();

        // Проверяем поиск по category1
        List<Game> foundGames = gameRepository.findByCategory_Id(category1.getId());
        assertEquals(2, foundGames.size(), "Ожидалось 2 игры в Category 1");

        assertTrue(foundGames.stream().anyMatch(game -> "Game 1".equals(game.getName())), "Game 1 не найдена");
        assertTrue(foundGames.stream().anyMatch(game -> "Game 2".equals(game.getName())), "Game 2 не найдена");
    }

    @Test
    void testFindByCategory_Id_EmptyResult() {
        // В category2 пока нет игр
        List<Game> foundGames = gameRepository.findByCategory_Id(category2.getId());

        assertTrue(foundGames.isEmpty(), "Для пустой категории должен быть пустой список");
    }
}
