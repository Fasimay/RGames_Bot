package com.RGames.RGames.DataBase.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "info")
public class Info {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String type; // "TIERLIST", "CODES", "WIKI"
    private String content; // Содержимое (ссылка или текст)

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    public Info(Game game, String type, String content) {
        this.game = game;
        this.type = type;
        this.content = content;
    }
}
