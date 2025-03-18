package com.RGames.RGames.DataBase.RepoInterface;

import com.RGames.RGames.DataBase.Entity.Game;
import com.RGames.RGames.DataBase.Entity.Info;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InfoRepository extends JpaRepository<Info, Long> {
    Optional<Info> findByGameIdAndType(Game game, String type);
}
