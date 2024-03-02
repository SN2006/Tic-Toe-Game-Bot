package com.example.tictoegamebot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Table(name = "usr")
public class User {
    @Id
    @Column(name = "id")
    private Long id;
    @Column(name = "first_name")
    private String username;
    @Column(name = "wins")
    private int wins;
    @Column(name = "loses")
    private int loses;
    @Column(name = "draws")
    private int draws;
    @Column(name = "score")
    private int score;
    @Column(name = "money")
    private int money;
    @Column(name = "game_mode")
    private int gameMode;

    @ManyToOne
    @JoinColumn(name = "x_skin_id", referencedColumnName = "id")
    private X xSkin;

    @ManyToOne
    @JoinColumn(name = "o_skin_id", referencedColumnName = "id")
    private O oSkin;

    @OneToOne
    @JoinColumn(name = "friend", referencedColumnName = "id")
    private User friend;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name="x_user",
            joinColumns=  @JoinColumn(name="user_id", referencedColumnName="id"),
            inverseJoinColumns= @JoinColumn(name="x_id", referencedColumnName="id") )
    private List<X> xSkins;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name="o_user",
            joinColumns=  @JoinColumn(name="user_id", referencedColumnName="id"),
            inverseJoinColumns= @JoinColumn(name="o_id", referencedColumnName="id") )
    private List<O> oSkins;

    @Transient
    private Game game;

    public User() {}

    public User(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public void setMyFriend(User friend) {
        this.friend = friend;
        friend.setFriend(this);
    }

    public void unsetMyFriend() {
        this.friend.setFriend(null);
        this.friend = null;
    }

    public void addXSkin(X xSkin) {
        if (this.xSkins == null) {
            this.xSkins = new ArrayList<>();
        }
        this.xSkins.add(xSkin);
    }

    public void addOSkins(O oSkin) {
        if (this.oSkins == null) {
            this.oSkins = new ArrayList<>();
        }
        this.oSkins.add(oSkin);
    }

    public void increaseWins(){
        this.wins++;
    }

    public void increaseLoses(){
        this.loses++;
    }

    public void increaseDraws(){
        this.draws++;
    }

    public void addMoney(int money) {
        this.money += money;
    }

    public void takeMoney(int money) {
        this.money -= money;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
