package com.example.tictoegamebot.controllers;

import com.example.tictoegamebot.config.BotConfig;
import com.example.tictoegamebot.entity.Game;
import com.example.tictoegamebot.entity.Statistic;
import com.example.tictoegamebot.entity.User;
import com.example.tictoegamebot.exception.AlreadyConnectedToFriendException;
import com.example.tictoegamebot.exception.UserNotFoundException;
import com.example.tictoegamebot.services.UsersService;
import com.example.tictoegamebot.util.Constants;
import com.example.tictoegamebot.util.ScoreCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TicToeBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final UsersService usersService;
    private Map<User, Game> games = new HashMap<>();
    private Map<User, Integer> messageIdMap = new HashMap<>();

    @Autowired
    public TicToeBot(BotConfig botConfig, UsersService usersService) {
        super(botConfig.getBotToken());
        this.botConfig = botConfig;
        this.usersService = usersService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String textFromUser = update.getMessage().getText();
            Long userId = update.getMessage().getFrom().getId();
            String userFirstName = update.getMessage().getFrom().getFirstName();
            User user = usersService.getUserById(userId);
            if (user == null){
                User userToSave = new User(userId, userFirstName);
                user = usersService.save(userToSave);
            }
            user = usersService.updateUsername(user, userFirstName);
            if (textFromUser.equals("/start")){
                startMessage(user);
            }else if (textFromUser.equals("getId")){
                getIdMessage(user);
            }else if (textFromUser.startsWith("/invite")){
                try {
                    Long friendId = Long.parseLong(textFromUser.split(" ")[1]);
                    try {
                        usersService.setFriend(user, friendId);
                        connectMessage(user);
                        connectMessage(user.getFriend());
                    }catch (UserNotFoundException | AlreadyConnectedToFriendException e){
                        sendMessage(e.getMessage(), userId.toString());
                    }
                }catch (Exception e){
                    sendMessage("Please enter a valid user ID (for example: /invite 1103395784)",
                            userId.toString());
                }
            } else if (textFromUser.startsWith("/disconnect")) {
                try{
                    User friend = user.getFriend();
                    usersService.unsetFriend(user);
                    disconnectMessage(user);
                    disconnectMessage(friend);
                } catch (UserNotFoundException e) {
                    sendMessage(e.getMessage(), userId.toString());
                }
            } else if (textFromUser.equals("\uD83C\uDFC6Menu\uD83C\uDFC6")){
                menuMessage(user);
            }else{
                sendMessage("Sorry, but I don`t understand you", userId.toString());
            }
        }else if (update.hasCallbackQuery()){
            String call_data = update.getCallbackQuery().getData();
            Long userId = update.getCallbackQuery().getFrom().getId();
            User user = usersService.getUserById(userId);
            if (call_data.equals("disconnect")){
                try{
                    User friend = user.getFriend();
                    usersService.unsetFriend(user);
                    disconnectMessage(user);
                    disconnectMessage(friend);
                } catch (UserNotFoundException e) {
                    sendMessage(e.getMessage(), userId.toString());
                }
            }else if (call_data.equals("statistics")){
                Statistic statistic = usersService.getUserStatistic(user.getId());
                statisticMessage(user, statistic);
            }else if (call_data.equals("start game")){
                if (user.getFriend() == null){
                    sendMessage("You are not connected to a friend. " +
                            "To connect, use the /invite command, passing it your friend’s id " +
                            "(for example: /invite 1103395784)", userId.toString());
                }else {
                    boolean isUserX = Constants.RANDOM.nextBoolean();
                    User friend = user.getFriend();
                    Game game;
                    if (isUserX){
                        game = new Game(0, user, friend);
                    }else{
                        game = new Game(0, friend, user);
                    }
                    user.setGame(game);
                    friend.setGame(game);
                    games.put(user, game);
                    games.put(friend, game);
                    startGameMessage(game.nowStepUser());
                }
            }else if (call_data.startsWith("step")){
                if (!games.containsKey(user)){
                    sendMessage("The current game has been completed", userId.toString());
                    return;
                }
                User friend = user.getFriend();
                Game game = games.get(user);
                user.setGame(game);
                friend.setGame(game);
                String[] splitData = call_data.split(" ");
                int y = Integer.parseInt(splitData[1]);
                int x = Integer.parseInt(splitData[2]);
                int status = game.nextStep(y, x);
                if (status == -1){
                    nextStepMessage(friend);
                }else if (status == 0){
                    int score = 0;
                    int money = Constants.RANDOM.nextInt(20) + 5;
                    usersService.addResult(user.getId(), 0, money);
                    usersService.addResult(friend.getId(), 0, money);
                    drawMessage(friend, score);
                    moneyInfoMessage(user, money);
                    moneyInfoMessage(friend, money);
                }else if (status == 1){
                    User winner = game.getXPlayer();
                    User loser = game.getOPlayer();
                    int score = ScoreCalculator.calculateScore(winner, loser);
                    int winnerMoney = score * (Constants.RANDOM.nextInt(5) + 1);
                    int loserMoney = Constants.RANDOM.nextInt(15) + 5;
                    usersService.addResult(winner.getId(), score, winnerMoney);
                    usersService.addResult(loser.getId(), -(score / 2), loserMoney);
                    winMessage(game, 1, score);
                    moneyInfoMessage(winner, winnerMoney);
                    moneyInfoMessage(loser, loserMoney);
                }else if (status == 2){
                    User winner = game.getOPlayer();
                    User loser = game.getXPlayer();
                    int score = ScoreCalculator.calculateScore(winner, loser);
                    int winnerMoney = score * (Constants.RANDOM.nextInt(5) + 1);
                    int loserMoney = Constants.RANDOM.nextInt(15) + 5;
                    usersService.addResult(winner.getId(), score, winnerMoney);
                    usersService.addResult(loser.getId(), -(score / 2), loserMoney);
                    winMessage(game, 2, score);
                    moneyInfoMessage(winner, winnerMoney);
                    moneyInfoMessage(loser, loserMoney);
                }
                if (game.isGameOver()){
                    user.setGame(null);
                    friend.setFriend(null);
                    games.remove(user);
                    games.remove(friend);
                    messageIdMap.remove(user);
                    messageIdMap.remove(friend);
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    private void sendMessage(String text, String id){
        SendMessage sendMessage = SendMessage.builder()
                .chatId(id)
                .text(text)
                .build();
        try {
            this.sendApiMethod(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendMessage(String text, String id, ReplyKeyboardMarkup replyKeyboardMarkup){
        SendMessage sendMessage = SendMessage.builder()
                .chatId(id)
                .text(text)
                .build();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        try {
            this.sendApiMethod(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendMessage(String text, String id, InlineKeyboardMarkup inlineKeyboardMarkup){
        SendMessage sendMessage = SendMessage.builder()
                .chatId(id)
                .text(text)
                .build();
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        try {
            this.sendApiMethod(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendPhoto(String text, String id, InlineKeyboardMarkup inlineKeyboardMarkup, InputFile file){
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(id);
        sendPhoto.setPhoto(file);
        sendPhoto.setReplyMarkup(inlineKeyboardMarkup);
        sendPhoto.setCaption(text);
        try {
           execute(sendPhoto);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void editMessage(String text, String id, Integer message_id, InlineKeyboardMarkup inlineKeyboardMarkup){
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(id);
        editMessageText.setText(text);
        editMessageText.setMessageId(message_id);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);
        try {
            this.sendApiMethod(editMessageText);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private Message sendAndGetMessage(String text, String id){
        SendMessage sendMessage = SendMessage.builder()
                .chatId(id)
                .text(text)
                .build();
        try {
            return this.sendApiMethod(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private Message sendAndGetMessage(String text, String id, InlineKeyboardMarkup inlineKeyboardMarkup){
        SendMessage sendMessage = SendMessage.builder()
                .chatId(id)
                .text(text)
                .build();
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        try {
            return this.sendApiMethod(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private void startMessage(User user){
        sendMessage("Hello, %s! Welcome to Tic-Toe Bot!".formatted(user.getUsername()),
                user.getId().toString(), replyKeyboardMarkup());
    }

    private void getIdMessage(User user){
        sendMessage("%d".formatted(user.getId()), user.getId().toString());
    }

    private void connectMessage(User user){
        sendMessage("You are connected to %s".formatted(user.getFriend().getUsername()), user.getId().toString());
    }

    private void disconnectMessage(User user){
        sendMessage("You are disconnected from your friend", user.getId().toString());
    }

    private void menuMessage(User user){
//        sendMessage("\uD83C\uDFC6Menu\uD83C\uDFC6", user.getId().toString(), menuMarkup());
        sendPhoto("\uD83C\uDFC6Menu\uD83C\uDFC6", user.getId().toString(), menuMarkup(), Constants.menuPicture);
    }

    private void startGameMessage(User user){
        String text = """
                %dx%d %d in row
                \uD83D\uDC49%s%s
                %s%s
                """;
        Game game = user.getGame();
        User friend = user.getFriend();
        Message userMessage = sendAndGetMessage(text.formatted(
                game.getWidth(), game.getHeight(), game.getInLine(), user.getXSkin().getSkin(), user.getUsername(),
                user.getOSkin().getSkin(), friend.getUsername()
                ), user.getId().toString(), boardMarkup(user, game.getBoard()));
        Message friendMessage = sendAndGetMessage(text.formatted(
                game.getWidth(), game.getHeight(), game.getInLine(), friend.getXSkin().getSkin(), user.getUsername(),
                friend.getOSkin().getSkin(), friend.getUsername()
        ), friend.getId().toString());
        messageIdMap.put(user, userMessage.getMessageId());
        messageIdMap.put(friend, friendMessage.getMessageId());
    }

    private void nextStepMessage(User user){
        Game game = user.getGame();
        User friend = user.getFriend();
        String text = """
                %dx%d %d in row
                \uD83D\uDC49%s%s
                %s%s
                """;
        if (game.getCurrentStep().equals("X")){
            editMessage(text.formatted(
                    game.getWidth(), game.getHeight(), game.getInLine(),
                    user.getXSkin().getSkin(), user.getUsername(),
                    user.getOSkin().getSkin(), friend.getUsername()
            ), user.getId().toString(), messageIdMap.get(user), boardMarkup(user, game.getBoard()));
            editMessage(text.formatted(
                    game.getWidth(), game.getHeight(), game.getInLine(),
                    friend.getXSkin().getSkin(), user.getUsername(),
                    friend.getOSkin().getSkin(), friend.getUsername()
            ), friend.getId().toString(), messageIdMap.get(friend), null);
        }else{
            editMessage(text.formatted(
                    game.getWidth(), game.getHeight(), game.getInLine(),
                    user.getOSkin().getSkin(), user.getUsername(),
                    user.getXSkin().getSkin(), friend.getUsername()
            ), user.getId().toString(), messageIdMap.get(user), boardMarkup(user, game.getBoard()));
            editMessage(text.formatted(
                    game.getWidth(), game.getHeight(), game.getInLine(),
                    friend.getOSkin().getSkin(), user.getUsername(),
                    friend.getXSkin().getSkin(), friend.getUsername()
            ), friend.getId().toString(), messageIdMap.get(friend), null);
        }
    }

    private void drawMessage(User user, int score){
        Game game = user.getGame();
        User friend = user.getFriend();
        String text = """
                ⚖️%s%s +%d🎖
                ⚖️%s%s +%d🎖
                It's a draw:
                
                """;
        if (game.getCurrentStep().equals("X")){
            editMessage(text.formatted(
                    user.getXSkin().getSkin(), user.getUsername(), score,
                    user.getOSkin().getSkin(), friend.getUsername(), score
            ) + resultBoard(game, 0, user), user.getId().toString(), messageIdMap.get(user), null);
            editMessage(text.formatted(
                    friend.getXSkin().getSkin(), user.getUsername(), score,
                    friend.getOSkin().getSkin(), friend.getUsername(), score
            ) + resultBoard(game, 0, friend), friend.getId().toString(), messageIdMap.get(friend), null);
        }else{
            editMessage(text.formatted(
                    user.getOSkin().getSkin(), user.getUsername(), score,
                    user.getXSkin().getSkin(), friend.getUsername(), score
            ) + resultBoard(game, 0, user), user.getId().toString(), messageIdMap.get(user), null);
            editMessage(text.formatted(
                    friend.getOSkin().getSkin(), user.getUsername(), score,
                    friend.getXSkin().getSkin(), friend.getUsername(), score
            ) + resultBoard(game, 0, friend), friend.getId().toString(), messageIdMap.get(friend), null);
        }
    }

    private void winMessage(Game game, int status, int score){
        User winner, loser;
        if (status == 1){
            String text = """
                🏆%s%s +%d🎖
                🤕%s%s -%d🎖
                %s is winner:
                
                """;
            winner = game.getXPlayer();
            loser = winner.getFriend();
            editMessage(text.formatted(
                    winner.getXSkin().getSkin(), winner.getUsername(), score,
                    winner.getOSkin().getSkin(), loser.getUsername(), score / 2,
                    winner.getUsername()
            ) + resultBoard(game, 1, winner), winner.getId().toString(), messageIdMap.get(winner), null);
            editMessage(text.formatted(
                    loser.getXSkin().getSkin(), winner.getUsername(), score,
                    loser.getOSkin().getSkin(), loser.getUsername(), score / 2,
                    winner.getUsername()
            ) + resultBoard(game, 1, loser), loser.getId().toString(), messageIdMap.get(loser), null);
        }else{
            String text = """
                🤕%s%s -%d🎖
                🏆%s%s +%d🎖
                %s is winner:
                
                """;
            winner = game.getOPlayer();
            loser = winner.getFriend();
            editMessage(text.formatted(
                    winner.getXSkin().getSkin(), loser.getUsername(), score / 2,
                    winner.getOSkin().getSkin(), winner.getUsername(), score,
                    winner.getUsername()
            ) + resultBoard(game, 2, winner), winner.getId().toString(), messageIdMap.get(winner), null);
            editMessage(text.formatted(
                    loser.getXSkin().getSkin(), loser.getUsername(), score / 2,
                    loser.getOSkin().getSkin(), winner.getUsername(), score,
                    winner.getUsername()
            ) + resultBoard(game, 2, loser), loser.getId().toString(), messageIdMap.get(loser), null);
        }
    }

    private void moneyInfoMessage(User user, int money){
        sendMessage("You earned %d\uD83D\uDCB5".formatted(money), user.getId().toString());
    }

    private InlineKeyboardMarkup menuMarkup(){
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        List<InlineKeyboardButton> row3 = new ArrayList<>();

        InlineKeyboardButton startButton = new InlineKeyboardButton();
        startButton.setText("\uD83D\uDC49Play with friend");
        startButton.setCallbackData("start game");
        row1.add(startButton);
        rows.add(row1);

        InlineKeyboardButton disconnectButton = new InlineKeyboardButton();
        disconnectButton.setText("\uD83D\uDEABDisconnect from friend");
        disconnectButton.setCallbackData("disconnect");
        row2.add(disconnectButton);
        rows.add(row2);

        InlineKeyboardButton statisticsButton = new InlineKeyboardButton();
        statisticsButton.setText("\uD83C\uDFC6My statistics");
        statisticsButton.setCallbackData("statistics");
        row3.add(statisticsButton);
        rows.add(row3);

        markupInline.setKeyboard(rows);
        return markupInline;
    }

    private void statisticMessage(User user, Statistic statistic){
        String text = """
                ▶️ Total games played: %d
                📊 Win percentage: %.2f%%
                🎖 Rating: %d
                
                🏆 Wins: %d
                🤕 Defeats: %d
                ⚖️ Draws: %d
                💵 Money: %d
                """;
        sendMessage(text.formatted(
                statistic.getCountOfGame(), statistic.getWinPercentage(), statistic.getScore(),
                statistic.getCountOfWins(), statistic.getCountOfLosses(), statistic.getCountOfDraws(),
                statistic.getMoney()
        ), user.getId().toString());
    }

    private ReplyKeyboardMarkup replyKeyboardMarkup() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        //следующие три строчки могут менять значение аргументов взависимости от ваших задач
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        //добавляем "клавиатуру"
        replyKeyboardMarkup.setKeyboard(keyboardRows());

        return replyKeyboardMarkup;
    }

    private List<KeyboardRow> keyboardRows() {
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(new KeyboardRow(keyboardButtons()));
        //создаем список рядов кнопок из списка кнопок

        return rows;
    }

    private List<KeyboardButton> keyboardButtons() {
        List<KeyboardButton> buttons = new ArrayList<>();
        buttons.add(new KeyboardButton("\uD83C\uDFC6Menu\uD83C\uDFC6"));
        buttons.add(new KeyboardButton("getId"));
        //создаем и заполняем список кнопок
        return buttons;
    }

    private InlineKeyboardMarkup boardMarkup(User user, int[][] board) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int y = 0; y < board.length; y++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int x = 0; x < board[0].length; x++) {
                String text = "⬜️";
                String callBackData = "step %d %d".formatted(y, x);
                switch (board[y][x]) {
                    case 1:{
                        text = user.getXSkin().getSkin();
                        callBackData = "blocked";
                        break;
                    }
                    case 2: {
                        text = user.getOSkin().getSkin();
                        callBackData = "blocked";
                    }
                }
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(text);
                inlineKeyboardButton.setText(text);
                inlineKeyboardButton.setCallbackData(callBackData);
                row.add(inlineKeyboardButton);
            }
            rows.add(row);
        }
        markup.setKeyboard(rows);
        return markup;
    }

    private String resultBoard(Game game, int winner, User user){
        StringBuilder result = new StringBuilder();
        int[][] board = game.getBoard();
        List<List<Integer>> markedPositions = game.getMarkPosition();
        String mark;
        if (winner == 1){
            mark = "❎";
        }else {
            mark = "\uD83D\uDFE2";
        }
        for (List<Integer> markedPosition : markedPositions) {
            board[markedPosition.get(0)][markedPosition.get(1)] = 3;
        }
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board[0].length; x++) {
                if (board[y][x] == 0){
                    result.append("⬜️");
                }else if (board[y][x] == 1){
                    result.append(user.getXSkin().getSkin());
                }else if (board[y][x] == 2){
                    result.append(user.getOSkin().getSkin());
                }else {
                    result.append(mark);
                }
            }
            result.append("\n");
        }
        return result.toString();
    }
}
