package com.RGames.RGames;

import com.RGames.RGames.DataBase.Entity.Traits;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PassiveRoller {
    private static final Random random = new Random();

    public static Traits rollOnePassive(List<Traits> traitsList) {
        if (traitsList == null || traitsList.isEmpty()) {
            System.out.println("Список пассивок пуст");
            return null; // Если список пуст, ничего не выпадает
        }

        // 1. Считаем суммарный вес всех пассивок (BigDecimal)
        BigDecimal totalWeight = traitsList.stream()
                .map(Traits::getTraitChance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Генерируем случайное число в диапазоне [0, totalWeight]
        double randomValue = random.nextDouble() * totalWeight.doubleValue();

        // 3. Определяем, какая пассивка выпала
        BigDecimal cumulativeWeight = BigDecimal.ZERO;
        for (Traits trait : traitsList) {
            cumulativeWeight = cumulativeWeight.add(trait.getTraitChance());
            if (randomValue <= cumulativeWeight.doubleValue()) {
                return trait; // Возвращаем выпавшую пассивку
            }
        }

        return null;
    }

    public static List<Traits> rollNPassives(List<Traits> traitsList, int n) {
        if (traitsList == null || traitsList.isEmpty()) {
            System.out.println("Список пассивок пуст");
            return null; // Если список пуст, ничего не выпадает
        }

        List<Traits> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            results.add(rollOnePassive(traitsList)); // Вызываем существующий метод rollOnePassive
        }

        return results;
    }

    public static int rollUntilPassive(List<Traits> traitsList, String targetPassiveName) {
        if (traitsList == null || traitsList.isEmpty()) {
            System.out.println("Список пассивок пуст");
            return -1;
        }

        int rolls = 0;

        while (true) {
            Traits rolledTrait = rollOnePassive(traitsList);
            rolls++;

            if (rolledTrait != null && rolledTrait.getTraitName().equals(targetPassiveName)) {
                return rolls;
            }
        }
    }
}
