package com.example.tictoegamebot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Objects;

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
    @Column(name = "price")
    private Integer price;

    public O() {

    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        O o = (O) object;
        return Objects.equals(id, o.id) && Objects.equals(skin, o.skin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, skin);
    }
}
