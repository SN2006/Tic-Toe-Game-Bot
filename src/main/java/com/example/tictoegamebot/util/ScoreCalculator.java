package com.example.tictoegamebot.util;

import com.example.tictoegamebot.entity.User;

public class ScoreCalculator {

    public static int calculateScore(User winner, User loser) {
        int different = winner.getScore() - loser.getScore();
        if (different > 1000){
            return 2;
        }
        if (different > 500){
            return 5;
        }
        if (different > 250){
            return 8;
        }
        if (different > 100){
            return 10;
        }
        if (different > 50){
            return 12;
        }
        if (different > 25){
            return 14;
        }
        if (different > 10){
            return 16;
        }
        if (different > -10){
            return 18;
        }
        if (different > -25){
            return 20;
        }
        if (different > -50){
            return 25;
        }
        if (different > -100){
            return 32;
        }
        if (different > -250){
            return 40;
        }
        if (different > -500){
            return 48;
        }
        return different / 10;
    }

}
