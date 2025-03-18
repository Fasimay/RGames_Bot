package com.RGames.RGames.DataBase.RepoInterface;

import com.RGames.RGames.DataBase.Entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByCategory_Id(Long categoryId);

}
