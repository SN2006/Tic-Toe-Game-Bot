package com.example.tictoegamebot.entity;

import lombok.Data;

@Data
public class Statistic {

    private int countOfGame;
    private int countOfWins;
    private int countOfLosses;
    private int countOfDraws;
    private int score;
    private int money;
    private double winPercentage;

    public Statistic(int countOfGame, int countOfWins, int countOfLosses, int countOfDraws, int score, int money, double winPercentage) {
        this.countOfGame = countOfGame;
        this.countOfWins = countOfWins;
        this.countOfLosses = countOfLosses;
        this.countOfDraws = countOfDraws;
        this.score = score;
        this.money = money;
        this.winPercentage = winPercentage;
    }
}
