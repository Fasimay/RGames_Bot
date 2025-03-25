package com.RGames.RGames.DataBase.Entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "game")
@Table(name = "traits")
public class Traits {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trait_name", nullable = false)
    private String traitName;

    @Column(name = "trait_chance", nullable = false, precision = 5, scale = 2)
    private BigDecimal traitChance;

    @Column(name = "trait_description", length = 500)
    private String traitDescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "games_id", nullable = false)
    private Game game;

    @Column(name = "trait_rarity", nullable = false)
    private String traitRarity;
}
