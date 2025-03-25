package com.RGames.RGames.DataBase.Entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(name = "description", length = 300)
    private String description;
    @Column(name = "wiki_url")
    private String wikiUrl;
    @Column(name = "codes_url")
    private String codesUrl;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    @Column(name = "tier_list_image_path")
    private String tierListLink;
    @Column(name = "game_url")
    private String gameUrl;
}
