package com.RGames.RGames.DataBase.RepoInterface;

import com.RGames.RGames.DataBase.Entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Category category1;

    @BeforeEach
    void setUp() {
        // Создаём и сохраняем категорию
        category1 = new Category();
        category1.setName("Test Category 1");
        entityManager.persist(category1);
        entityManager.flush();
    }

    @Test
    void findByName() {
        // Ищем категорию "Test Category 1"
        Category foundCategory = categoryRepository.findByName("Test Category 1");

        assertNotNull(foundCategory, "Категория не найдена");
        assertEquals("Test Category 1", foundCategory.getName(), "Имя категории не совпадает");
    }

    @Test
    void findByName_NotFound() {
        // Ищем категорию, которой нет
        Category foundCategory = categoryRepository.findByName("Fake Category 1");

        assertNull(foundCategory, "Ожидался null, но категория найдена");
    }
}
