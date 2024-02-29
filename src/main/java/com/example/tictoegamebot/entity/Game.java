package com.example.tictoegamebot.entity;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Game {

    @Getter
    private final int[][] board;
    @Getter
    private final int inLine;
    @Getter
    private User xPlayer;
    @Getter
    private User oPlayer;
    @Getter
    private String currentStep;
    private int countStep = 0;
    @Getter
    private boolean gameOver = false;
    @Getter
    private List<List<Integer>> markPosition = new ArrayList<>();

    public Game(int type, User xPlayer, User oPlayer){
        this.xPlayer = xPlayer;
        this.oPlayer = oPlayer;
        this.currentStep = "X";
        switch(type){
            case 1:{
                board = new int[5][5];
                inLine = 4;
                break;
            }
            case 0:
            default:{
                board = new int[3][3];
                inLine = 3;
                break;
            }
        }
    }

    public User nowStepUser(){
        if (currentStep.equals("X")){
            return xPlayer;
        }else{
            return oPlayer;
        }
    }

    public int nextStep(int y, int x){
        if (gameOver){
            return -2;
        }
        if (currentStep.equals("X")){
            board[y][x] = 1;
            currentStep = "O";
        }else {
            board[y][x] = 2;
            currentStep = "X";
        }
        countStep++;
        int gameStatus = checkGameOver();
        if (gameStatus != 0){
            gameOver = true;
            return gameStatus;
        }
        if (countStep == board.length * board[0].length){
            gameOver = true;
            return 0;
        }
        return -1;
    }

    private int checkGameOver(){
        int horizontal = checkHorizontal();
        if (horizontal != 0) return horizontal;
        int vertical = checkVertical();
        if (vertical != 0) return vertical;
        int diagonal1 = checkDiagonal1();
        if (diagonal1 != 0) return diagonal1;
        return checkDiagonal2();
    }

    private int checkHorizontal(){
        for (int y = 0; y < board.length; y++){
            int countX = 0;
            int countO = 0;
            for (int x = 0; x < board[0].length; x++){
                if (board[y][x] == 0){
                    countX = 0;
                    countO = 0;
                }else if (board[y][x] == 1){
                    countX++;
                }else if (board[y][x] == 2){
                    countO++;
                }
                if (countX == inLine){
                    for (int i = 0; i < inLine; i++){
                        markPosition.add(Arrays.asList(y, x - i));
                    }
                    return 1;
                }
                if (countO == inLine){
                    for (int i = 0; i < inLine; i++){
                        markPosition.add(Arrays.asList(y, x - i));
                    }
                    return 2;
                }
            }
        }
        return 0;
    }

    private int checkVertical(){
        for (int x = 0; x < board[0].length; x++){
            int countX = 0;
            int countO = 0;
            for (int y = 0; y < board.length; y++){
                if (board[y][x] == 0){
                    countX = 0;
                    countO = 0;
                }else if (board[y][x] == 1){
                    countX++;
                }else if (board[y][x] == 2){
                    countO++;
                }
                if (countX == inLine){
                    for (int i = 0; i < inLine; i++){
                        markPosition.add(Arrays.asList(y - i, x));
                    }
                    return 1;
                }
                if (countO == inLine){
                    for (int i = 0; i < inLine; i++){
                        markPosition.add(Arrays.asList(y - i, x));
                    }
                    return 2;
                }
            }
        }
        return 0;
    }

    private int checkDiagonal1(){
        for (int y = 0; y < board.length - inLine + 1; y++){
            for (int x = 0; x < board[0].length - inLine + 1; x++){
                int countX = 0;
                int countO = 0;
                for (int k = 0; k < inLine; k++){
                    if (board[y + k][x + k] == 0){
                        countX = 0;
                        countO = 0;
                    }else if (board[y + k][x + k] == 1){
                        countX++;
                    }else if (board[y + k][x + k] == 2){
                        countO++;
                    }
                }
                if (countX == inLine){
                    for (int i = 0; i < inLine; i++){
                        markPosition.add(Arrays.asList(y + i, x + i));
                    }
                    return 1;
                }
                if (countO == inLine){
                    for (int i = 0; i < inLine; i++){
                        markPosition.add(Arrays.asList(y + i, x + i));
                    }
                    return 2;
                }
            }
        }
        return 0;
    }

    private int checkDiagonal2(){
        for (int y = board.length - 1; y >= inLine - 1; y--){
            for (int x = 0; x < board[0].length - inLine + 1; x++){
                int countX = 0;
                int countO = 0;
                for (int k = 0; k < inLine; k++){
                    if (board[y - k][x + k] == 0){
                        countX = 0;
                        countO = 0;
                    }else if (board[y - k][x + k] == 1){
                        countX++;
                    }else if (board[y - k][x + k] == 2){
                        countO++;
                    }
                }
                if (countX == inLine){
                    for (int i = 0; i < inLine; i++){
                        markPosition.add(Arrays.asList(y - i, x + i));
                    }
                    return 1;
                }
                if (countO == inLine){
                    for (int i = 0; i < inLine; i++){
                        markPosition.add(Arrays.asList(y - i, x + i));
                    }
                    return 2;
                }
            }
        }
        return 0;
    }

    public int getWidth(){
        return board[0].length;
    }

    public int getHeight(){
        return board.length;
    }
}