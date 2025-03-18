package com.RGames.RGames.DataBase.RepoInterface;


import com.RGames.RGames.DataBase.Entity.Game;
import com.RGames.RGames.DataBase.Entity.Traits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TraitsRepository extends JpaRepository<Traits, Long> {
    List<Traits> findByGame(Game game);

    Optional<Traits> findByTraitNameAndGame_Id(String traitName, Long gameId);
}
