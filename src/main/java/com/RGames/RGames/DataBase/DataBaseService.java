package com.RGames.RGames.DataBase;


import com.RGames.RGames.DataBase.Entity.Game;
import com.RGames.RGames.DataBase.Entity.Traits;
import com.RGames.RGames.DataBase.RepoInterface.CategoryRepository;
import com.RGames.RGames.DataBase.RepoInterface.GameRepository;
import com.RGames.RGames.DataBase.RepoInterface.InfoRepository;

import java.util.*;

import com.RGames.RGames.DataBase.RepoInterface.TraitsRepository;
import org.springframework.stereotype.Service;

@Service // Аннотация указывает, что этот класс является сервисным компонентом Spring
public class DataBaseService {

    // Репозитории для взаимодействия с базой данных
    private final GameRepository gameRepository;
    private final InfoRepository infoRepository;
    private final CategoryRepository categoryRepository;
    private final TraitsRepository traitsRepository;


    // Конструктор для внедрения зависимостей (Dependency Injection)
    public DataBaseService(GameRepository gameRepository, InfoRepository infoRepository, CategoryRepository categoryRepository, TraitsRepository traitsRepository) {
        this.gameRepository = gameRepository;
        this.infoRepository = infoRepository;
        this.categoryRepository = categoryRepository;
        this.traitsRepository = traitsRepository;
    }

    //Получаю все игры по категории
    public List<Game> getGamesByCategoryId(Long categoryId) {
        List<Game> games = gameRepository.findByCategory_Id(categoryId);
        System.out.println("Найдено игр: " + games.size());
        games.forEach(game -> System.out.println("Игра " + game.getName()));
        return games;
    }

    //Получаю информацию по типу и id
    public String getContent(Game game, String type) {
        return infoRepository.findByGameIdAndType(game.getId(), type)
                .map(info -> info.getContent()) // Берём саму ссылку/путь из найденной записи
                .orElse("Информация не найдена.");
    }

    //Получение всех пассивок в определённой игре
    public List<Traits> getTraitsByGameId(Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new IllegalArgumentException("Game not found"));
        List<Traits> traits = traitsRepository.findByGame(game);
        System.out.println("Найдено пассивок: " + traits.size());
        traits.forEach(trait -> System.out.println("Пассивка " + trait.getTraitName()));
        return traits;
    }
}
