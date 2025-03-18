package com.RGames.RGames.DataBase.Entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "traits")
public class Traits {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trait_name")
    private String traitName;
    @Column(name = "trait_chance")
    private BigDecimal TraitChance;
    @Column(name = "trait_description")
    private String traitDescription;

    @ManyToOne
    @JoinColumn(name = "games_id")
    private Game game;

    @Column(name = "trait_rarity")
    private String traitRarity;
}
