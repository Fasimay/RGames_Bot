package com.RGames.RGames;

import com.RGames.RGames.DataBase.Entity.Traits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PassiveRollerTest {

    private List<Traits> mockTraits;

    @BeforeEach
    void setUp() {
        // Создаём список фиктивных пассивок для тестов
        mockTraits = List.of(
                new Traits(null, "Common", new BigDecimal("50.0"), "Обычная пассивка", null, "Обычная"),
                new Traits(null, "Rare", new BigDecimal("30.0"), "Редкая пассивка", null, "Редкая"),
                new Traits(null, "Epic", new BigDecimal("10.0"), "Эпическая пассивка", null, "Эпическая"),
                new Traits(null, "Legendary", new BigDecimal("5.0"), "Легендарная пассивка", null, "Легендарная"),
                new Traits(null, "Mythic", new BigDecimal("1.0"), "Мифическая пассивка", null, "Мифическая")
        );
    }

    // Тест на выпадение одной пассивки
    @Test
    void testRollOnePassive() {
        Traits rolledTrait = PassiveRoller.rollOnePassive(mockTraits);

        assertNotNull(rolledTrait, "Результат не должен быть null");
        assertTrue(mockTraits.contains(rolledTrait), "Выпавшая пассивка должна быть из списка");
    }

    // Тест на случай пустого списка пассивок
    @Test
    void testRollOnePassive_EmptyList() {
        Traits rolledTrait = PassiveRoller.rollOnePassive(List.of());

        assertNull(rolledTrait, "Должно вернуть null для пустого списка");
    }

    // Тест на выпадение n пассивок
    @Test
    void testRollNPassives() {
        int rolls = 10;
        List<Traits> rolledPassives = PassiveRoller.rollNPassives(mockTraits, rolls);

        assertNotNull(rolledPassives, "Список не должен быть null");
        assertEquals(rolls, rolledPassives.size(), "Должно выпасть ровно " + rolls + " пассивок");

        // Проверяем, что все выпавшие пассивки из исходного списка
        assertTrue(rolledPassives.stream().allMatch(mockTraits::contains),
                "Все выпавшие пассивки должны быть из исходного списка");
    }

    // Тест на случай пустого списка при n роллах
    @Test
    void testRollNPassives_EmptyList() {
        List<Traits> rolledPassives = PassiveRoller.rollNPassives(List.of(), 5);

        assertNull(rolledPassives, "Должно вернуть null для пустого списка");
    }

    // Тест на rollUntilPassive — должно рано или поздно найти пассивку
    @Test
    void testRollUntilPassive() {
        String targetPassive = "Epic";
        int rolls = PassiveRoller.rollUntilPassive(mockTraits, targetPassive);

        assertTrue(rolls > 0, "Количество роллов должно быть больше 0");
    }

    // Тест на rollUntilPassive с пустым списком
    @Test
    void testRollUntilPassive_EmptyList() {
        int rolls = PassiveRoller.rollUntilPassive(List.of(), "Epic");

        assertEquals(-1, rolls, "Должно вернуть -1 при пустом списке");
    }
}
