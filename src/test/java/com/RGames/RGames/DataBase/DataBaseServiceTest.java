package com.RGames.RGames.DataBase;

import com.RGames.RGames.DataBase.Entity.Game;
import com.RGames.RGames.DataBase.Entity.Info;
import com.RGames.RGames.DataBase.Entity.Traits;
import com.RGames.RGames.DataBase.RepoInterface.CategoryRepository;
import com.RGames.RGames.DataBase.RepoInterface.GameRepository;
import com.RGames.RGames.DataBase.RepoInterface.InfoRepository;
import com.RGames.RGames.DataBase.RepoInterface.TraitsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataBaseServiceTest {
    @Mock
    private InfoRepository infoRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TraitsRepository traitsRepository;
    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private DataBaseService dataBaseService;

    private Game testGame;

    @BeforeEach
    void setUp() {
        testGame = new Game();
        testGame.setId(1L);
        testGame.setName("Test Game");
    }

    @Test
    void testGetGamesByCategoryId_ShouldReturnGames() {
        List<Game> games = List.of(testGame);
        when(gameRepository.findByCategory_Id(1L)).thenReturn(games);

        List<Game> result = dataBaseService.getGamesByCategoryId(1L);

        assertEquals(1, result.size());
        assertEquals("Test Game", result.get(0).getName());
        verify(gameRepository, times(1)).findByCategory_Id(1L);
    }
    
    @Test
    void testGetGamesByCategoryId_ShouldReturnEmptyList_WhenNoGamesFound() {
        when(gameRepository.findByCategory_Id(1L)).thenReturn(List.of());

        List<Game> result = dataBaseService.getGamesByCategoryId(1L);

        assertTrue(result.isEmpty());
        verify(gameRepository, times(1)).findByCategory_Id(1L);
    }

    @Test
    void testGetContent_ShouldReturnContent_WhenInfoExists() {
        when(infoRepository.findByGameIdAndType(testGame.getId(), "wiki"))
            .thenReturn(Optional.of(new Info(testGame, "WIKI", "https://wiki.com")));

        String result = dataBaseService.getContent(testGame, "wiki");

        assertEquals("https://wiki.com", result);
        verify(infoRepository, times(1)).findByGameIdAndType(testGame.getId(), "wiki");
    }

    @Test
    void testGetContent_ShouldReturnNotFound_WhenInfoDoesNotExist() {
        when(infoRepository.findByGameIdAndType(testGame.getId(), "wiki")).thenReturn(Optional.empty());

        String result = dataBaseService.getContent(testGame, "wiki");

        assertEquals("Информация не найдена.", result);
        verify(infoRepository, times(1)).findByGameIdAndType(testGame.getId(), "wiki");
    }

    @Test
    void testGetTraitsByGameId_ShouldReturnTraits() {
        Traits trait = new Traits();
        trait.setTraitName("Monarch");
        List<Traits> traitsList = List.of(trait);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        when(traitsRepository.findByGame(testGame)).thenReturn(traitsList);

        List<Traits> result = dataBaseService.getTraitsByGameId(1L);

        assertEquals(1, result.size());
        assertEquals("Monarch", result.get(0).getTraitName());
        verify(traitsRepository, times(1)).findByGame(testGame);
    }

    @Test
    void testGetTraitsByGameId_ShouldThrowException_WhenGameNotFound() {
        when(gameRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> dataBaseService.getTraitsByGameId(1L));
    
        assertTrue(exception.getMessage().contains("Game not found"));
    
        verify(traitsRepository, never()).findByGame(any());
    }
}
