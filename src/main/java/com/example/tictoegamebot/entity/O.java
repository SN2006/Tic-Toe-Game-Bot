package com.example.tictoegamebot.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "o")
public class O {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "skin")
    private String skin;

    public O() {

    }
}
