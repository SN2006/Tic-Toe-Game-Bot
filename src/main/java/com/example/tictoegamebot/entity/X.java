package com.example.tictoegamebot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Objects;

@Data
@Entity
@Table(name = "x")
public class X {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "skin")
    private String skin;
    @Column(name = "price")
    private Integer price;

    public X() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        X x = (X) o;
        return Objects.equals(id, x.id) && Objects.equals(skin, x.skin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, skin);
    }
}
