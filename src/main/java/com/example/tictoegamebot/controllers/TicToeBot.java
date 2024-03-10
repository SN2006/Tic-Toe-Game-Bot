package com.example.tictoegamebot.controllers;

import com.example.tictoegamebot.config.BotConfig;
import com.example.tictoegamebot.entity.*;
import com.example.tictoegamebot.exception.AlreadyConnectedToFriendException;
import com.example.tictoegamebot.exception.NotEnoughMoneyException;
import com.example.tictoegamebot.exception.UserIsAlreadyYourFriendException;
import com.example.tictoegamebot.exception.UserNotFoundException;
import com.example.tictoegamebot.services.OsService;
import com.example.tictoegamebot.services.UsersService;
import com.example.tictoegamebot.services.XsService;
import com.example.tictoegamebot.util.Constants;
import com.example.tictoegamebot.util.ScoreCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TicToeBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final UsersService usersService;
    private final XsService xsService;
    private final OsService osService;
    private Map<User, Game> games = new HashMap<>();
    private Map<User, Integer> messageIdMap = new HashMap<>();

    @Autowired
    public TicToeBot(BotConfig botConfig, UsersService usersService, XsService xsService, OsService osService) {
        super(botConfig.getBotToken());
        this.botConfig = botConfig;
        this.usersService = usersService;
        this.xsService = xsService;
        this.osService = osService;
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
            }else if(textFromUser.startsWith("/addMoney") && user.getId().equals(botConfig.getBotAdminId())){
                String[] split = textFromUser.split(" ");
                Long userToGetMoneyId = Long.parseLong(split[1]);
                User userToGetMoney = usersService.addMoney(userToGetMoneyId, 500);
                if (userToGetMoney != null){
                    sendMessage(String.format("You gave %d\uD83D\uDCB5 to %s", 500, userToGetMoney.getUsername()), user.getId().toString());
                    sendMessage(String.format("You get %d\uD83D\uDCB5", 500), userToGetMoneyId.toString());
                }
            }else if (textFromUser.equals("/get_id")){
                getIdMessage(user);
            }else if (textFromUser.startsWith("/addFriend")){
                String[] split = textFromUser.split(" ");
                try{
                    Long friendId = Long.parseLong(split[1]);
                    User friend = usersService.addFriend(user.getId(), friendId);
                    sendMessage(String.format("%s has been added to your friend list",
                            friend.getUsername()), user.getId().toString());
                    sendMessage(String.format("%s has been added to your friend list",
                            user.getUsername()), friend.getId().toString());
                } catch (UserNotFoundException | UserIsAlreadyYourFriendException e) {
                    sendMessage(e.getMessage(), user.getId().toString());
                }catch (Exception e){
                    sendMessage("Please enter a valid user ID (for example: /addFriend 1103395784)",
                            userId.toString());
                }
            }else if (textFromUser.startsWith("/disconnect")) {
                try{
                    User friend = user.getFriend();
                    usersService.unsetFriend(user);
                    disconnectMessage(user);
                    disconnectMessage(friend);
                } catch (UserNotFoundException e) {
                    sendMessage(e.getMessage(), userId.toString());
                }
            } else if (textFromUser.equals("/menu")){
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
            }else if(call_data.equals("settings")){
                settingsMessage(user);
            }else if(call_data.equals("rating")){
                List<User> rating = usersService.getRating();
                ratingMessage(user, rating);
            }else if(call_data.equals("shop")){
                shopMessage(user);
            }else if(call_data.equals("xShop")){
                List<X> shop = xsService.getShopForUser(user.getId());
                xShopMessage(user, shop);
            }else if(call_data.equals("oShop")){
                List<O> shop = osService.getShopForUser(user.getId());
                oShopMessage(user, shop);
            }else if(call_data.equals("xSkins")){
                List<X> xSkins = usersService.getUserXSkins(user.getId());
                xSkinMessage(user, xSkins);
            }else if(call_data.equals("oSkins")){
                List<O> oSkins = usersService.getUserOSkins(user.getId());
                oSkinMessage(user, oSkins);
            }else if (call_data.equals("statistics")){
                Statistic statistic = usersService.getUserStatistic(user.getId());
                statisticMessage(user, statistic);
            }else if (call_data.equals("drawAgree")){
                User friend = user.getFriend();
                Game game = games.get(user);
                user.setGame(game);
                friend.setGame(game);
                game.changeAgreeMap(user.getId());
                if (game.isGameOver()){
                    int score = 0;
                    int money = Constants.RANDOM.nextInt(20) + 5;
                    usersService.addResult(user.getId(), 0, money);
                    usersService.addResult(friend.getId(), 0, money);
                    if (game.nowStep().getId().equals(user.getId())){
                        drawMessage(user, score);
                    }else {
                        drawMessage(friend, score);
                    }
                    moneyInfoMessage(user, money);
                    moneyInfoMessage(friend, money);

                    user.setGame(null);
                    friend.setGame(null);
                    games.remove(user);
                    games.remove(friend);
                    messageIdMap.remove(user);
                    messageIdMap.remove(friend);
                    try {
                        usersService.unsetFriend(user);
                    } catch (UserNotFoundException e) {
                        System.out.println(e.getMessage());
                    }
                }else{
                    if (game.nowStep().getId().equals(user.getId())){
                        nextStepMessage(user);
                    }else{
                        nextStepMessage(friend);
                    }
                }
            }else if (call_data.equals("start game")){
                if (user.getFriend() == null){
                    sendMessage("You are not connected to a friend. " +
                            "To connect, use the /invite command, passing it your friend’s id " +
                            "(for example: /invite 1103395784)", userId.toString());
                }else {
                    int randomNumber = (int) (Math.random() * 100);
                    boolean isUserX = Constants.RANDOM.nextBoolean();
                    User friend = user.getFriend();
                    Game game;
                    if (randomNumber < 50){
                        game = new Game(friend.getGameMode(), user, friend);
                    }else{
                        game = new Game(user.getGameMode(), friend, user);
                    }
                    user.setGame(game);
                    friend.setGame(game);
                    games.put(user, game);
                    games.put(friend, game);
                    startGameMessage(game.nowStepUser());
                }
            }else if (call_data.equals("selectFriendToStart")){
                chooseFriendToStartMessage(user);
            }else if (call_data.startsWith("playWith")){
                Long friendId = Long.parseLong(call_data.split(" ")[1]);
                try {
                    usersService.setFriend(user, friendId);
                    user = usersService.getUserById(user.getId());
                    User friend = user.getFriend();
                    int randomNumber = (int) (Math.random() * 100);
                    Game game;
                    if (randomNumber < 50){
                        game = new Game(friend.getGameMode(), user, friend);
                    }else{
                        game = new Game(user.getGameMode(), friend, user);
                    }
                    user.setGame(game);
                    friend.setGame(game);
                    games.put(user, game);
                    games.put(friend, game);
                    startGameMessage(game.nowStepUser());
                }catch (UserNotFoundException | AlreadyConnectedToFriendException e){
                    sendMessage(e.getMessage(), userId.toString());
                }
            }else if(call_data.startsWith("setGameMode")){
                int mode = Integer.parseInt(call_data.split(" ")[1]);
                int messageId = Integer.parseInt(call_data.split(" ")[2]);
                user = usersService.setGameMode(user.getId(), mode);
                editMessageMarkup(user.getId().toString(), messageId, settingMarkup(user, messageId));
            }else if(call_data.startsWith("buyX")){
                String[] splitData = call_data.split(" ");
                Long xId = Long.parseLong(splitData[1]);
                Integer messageId = Integer.parseInt(splitData[2]);
                try{
                    xsService.buyXForUser(user.getId(), xId);
                    deleteMessage(user.getId().toString(), messageId);
                    sendMessage("The skin has been added to your collection",
                            user.getId().toString());
                }catch (NotEnoughMoneyException e){
                    deleteMessage(user.getId().toString(), messageId);
                    sendMessage(e.getMessage(), userId.toString());
                }
            }else if(call_data.startsWith("buyO")){
                String[] splitData = call_data.split(" ");
                Long oId = Long.parseLong(splitData[1]);
                Integer messageId = Integer.parseInt(splitData[2]);
                try{
                    osService.buyOForUser(user.getId(), oId);
                    deleteMessage(user.getId().toString(), messageId);
                    sendMessage("The skin has been added to your collection",
                            user.getId().toString());
                }catch (NotEnoughMoneyException e){
                    deleteMessage(user.getId().toString(), messageId);
                    sendMessage(e.getMessage(), userId.toString());
                }
            }else if(call_data.startsWith("setXSkin")){
                String[] splitData = call_data.split(" ");
                Long xSkinId = Long.parseLong(splitData[1]);
                Integer messageId = Integer.parseInt(splitData[2]);
                usersService.setXSkin(user.getId(), xSkinId);
                List<X> xSkins = usersService.getUserXSkins(user.getId());
                editMessageMarkup(user.getId().toString(), messageId, xSkinsMarkup(xSkinId, xSkins, messageId));

//                deleteMessage(user.getId().toString(), messageId);
//                sendMessage("Skin successfully changed!", userId.toString());
            }else if(call_data.startsWith("setOSkin")){
                String[] splitData = call_data.split(" ");
                Long oSkinId = Long.parseLong(splitData[1]);
                Integer messageId = Integer.parseInt(splitData[2]);
                usersService.setOSkin(user.getId(), oSkinId);
                List<O> oSkins = usersService.getUserOSkins(user.getId());
                editMessageMarkup(user.getId().toString(), messageId, oSkinsMarkup(oSkinId, oSkins, messageId));

//                deleteMessage(user.getId().toString(), messageId);
//                sendMessage("Skin successfully changed!", userId.toString());
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
                    friend.setGame(null);
                    games.remove(user);
                    games.remove(friend);
                    messageIdMap.remove(user);
                    messageIdMap.remove(friend);
                    try {
                        usersService.unsetFriend(user);
                    } catch (UserNotFoundException e) {
                        System.out.println(e.getMessage());
                    }
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
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(id);
        sendMessage.setText(text);
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

    private void editMessageMarkup(String id, Integer message_id, InlineKeyboardMarkup inlineKeyboardMarkup){
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setReplyMarkup(inlineKeyboardMarkup);
        editMessageReplyMarkup.setChatId(id);
        editMessageReplyMarkup.setMessageId(message_id);
        try {
            this.sendApiMethod(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void deleteMessage(String id, Integer message_id){
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(id);
        deleteMessage.setMessageId(message_id);
        try {
            this.sendApiMethod(deleteMessage);
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
//        sendMessage("Hello, %s! Welcome to Tic-Toe Bot!".formatted(user.getUsername()),
//                user.getId().toString());
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
        ), friend.getId().toString(), offerADrawMarkup(friend));
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
            ), friend.getId().toString(), messageIdMap.get(friend), offerADrawMarkup(friend));
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
            ), friend.getId().toString(), messageIdMap.get(friend), offerADrawMarkup(friend));
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

    private void xSkinMessage(User user, List<X> xSkins){
        Message message = sendAndGetMessage("%s, Choose a skin: ".formatted(user.getUsername()), user.getId().toString());
        editMessageMarkup(user.getId().toString(), message.getMessageId(), xSkinsMarkup(user.getXSkin().getId(), xSkins, message.getMessageId()));
    }

    private void oSkinMessage(User user, List<O> oSkins){
        Message message = sendAndGetMessage("%s, Choose a skin: ".formatted(user.getUsername()), user.getId().toString());
        editMessageMarkup(user.getId().toString(), message.getMessageId(), oSkinsMarkup(user.getOSkin().getId(), oSkins, message.getMessageId()));
    }

    private void shopMessage(User user){
        sendMessage("\uD83D\uDECD️ Shop \uD83D\uDECD️", user.getId().toString(), shopMarkup());
    }

    private void xShopMessage(User user, List<X> shop){
        if (shop.isEmpty()){
            sendMessage("You have already purchased all skins for X", user.getId().toString());
            return;
        }
        Message message = sendAndGetMessage(String.format("Your money: %d \uD83D\uDCB5\nAvailable X skins:", user.getMoney()), user.getId().toString());
        editMessageMarkup(user.getId().toString(), message.getMessageId(), xShopMarkUp(shop, message.getMessageId()));
    }

    private void oShopMessage(User user, List<O> shop){
        if (shop.isEmpty()){
            sendMessage("You have already purchased all skins for O", user.getId().toString());
            return;
        }
        Message message = sendAndGetMessage(String.format("Your money: %d \uD83D\uDCB5\nAvailable O skins:", user.getMoney()), user.getId().toString());
        editMessageMarkup(user.getId().toString(), message.getMessageId(), oShopMarkUp(shop, message.getMessageId()));
    }

    private void ratingMessage(User user, List<User> rating){
        AtomicInteger counter = new AtomicInteger();
        StringBuilder text = new StringBuilder();
        for (User ratingUser : rating){
            int place = counter.incrementAndGet();
            if (place == 1){
                text.append("\uD83E\uDD47: ");
            }else if (place == 2){
                text.append("\uD83E\uDD48: ");
            }else if (place == 3){
                text.append("\uD83E\uDD49: ");
            }else {
                for (Character digit : String.valueOf(place).toCharArray()){
                    text.append(Constants.digits.get(Integer.parseInt(digit.toString())));
                }
                text.append(": ");
            }
            if (ratingUser.getId().equals(user.getId())){
                text.append("\uD83D\uDC49");
            }
            text.append(ratingUser.getUsername());
            text.append(" - ");
            text.append(ratingUser.getScore());
            text.append("\uD83C\uDF96\n");
        }
        sendMessage(text.toString(), user.getId().toString());
    }

    private void chooseFriendToStartMessage(User user){
        if (user.getFriends().isEmpty()){
            sendMessage("You have no friends to start the game!\n" +
                    "To add another friend, enter the command /addFriend (for example: /addFriend 1103395784)",
                    user.getId().toString());
            return;
        }
        sendMessage("Choose a friend to start the game\uD83D\uDC47\uD83C\uDFFD\n" +
                "To add another friend, enter the command /addFriend (for example: /addFriend 1103395784)",
                user.getId().toString(), selectedFriendToPlayMarkup(user));
    }

    private InlineKeyboardMarkup shopMarkup(){
        InlineKeyboardMarkup shopMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton xShopButton = new InlineKeyboardButton();
        xShopButton.setText("❌ skins");
        xShopButton.setCallbackData("xShop");
        row1.add(xShopButton);

        InlineKeyboardButton oShopButton = new InlineKeyboardButton();
        oShopButton.setText("⭕ skins");
        oShopButton.setCallbackData("oShop");
        row1.add(oShopButton);

        rows.add(row1);
        shopMarkup.setKeyboard(rows);
        return shopMarkup;
    }

    private InlineKeyboardMarkup menuMarkup(){
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        List<InlineKeyboardButton> row6 = new ArrayList<>();
        List<InlineKeyboardButton> row7 = new ArrayList<>();

        InlineKeyboardButton startButton = new InlineKeyboardButton();
        startButton.setText("\uD83D\uDC49Play");
        startButton.setCallbackData("selectFriendToStart");
        row1.add(startButton);
        rows.add(row1);

        InlineKeyboardButton ratingButton = new InlineKeyboardButton();
        ratingButton.setText("\uD83C\uDFC5Rating");
        ratingButton.setCallbackData("rating");
        row3.add(ratingButton);
        rows.add(row3);

        InlineKeyboardButton statisticsButton = new InlineKeyboardButton();
        statisticsButton.setText("\uD83C\uDFC6My statistics");
        statisticsButton.setCallbackData("statistics");
        row4.add(statisticsButton);
        rows.add(row4);

        InlineKeyboardButton xSkinsButton = new InlineKeyboardButton();
        xSkinsButton.setText("❌My skins on X");
        xSkinsButton.setCallbackData("xSkins");
        row5.add(xSkinsButton);
        rows.add(row5);

        InlineKeyboardButton oSkinsButton = new InlineKeyboardButton();
        oSkinsButton.setText("⭕My skins on 0");
        oSkinsButton.setCallbackData("oSkins");
        row5.add(oSkinsButton);

        InlineKeyboardButton shopButton = new InlineKeyboardButton();
        shopButton.setText("\uD83D\uDED2Shop");
        shopButton.setCallbackData("shop");
        row6.add(shopButton);
        rows.add(row6);

        InlineKeyboardButton settingButton = new InlineKeyboardButton();
        settingButton.setText("⚙️Settings");
        settingButton.setCallbackData("settings");
        row7.add(settingButton);
        rows.add(row7);

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

    private void settingsMessage(User user){
        Message message = sendAndGetMessage("What type of field do you prefer?", user.getId().toString());

        editMessageMarkup(user.getId().toString(), message.getMessageId(), settingMarkup(user, message.getMessageId()));
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
        buttons.add(new KeyboardButton("/menu"));
        buttons.add(new KeyboardButton("/get_id"));
        //создаем и заполняем список кнопок
        return buttons;
    }

    private InlineKeyboardMarkup boardMarkup(User user, int[][] board) {
        Game game = user.getGame();
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

        List<InlineKeyboardButton> drawRows = new ArrayList<>();
        InlineKeyboardButton drawButton = new InlineKeyboardButton();
        StringBuilder textBuilder = new StringBuilder();
        if (game.getDrawAgreeMap().get(game.getXPlayer().getId())) {
            textBuilder.append("✅");
        }
        textBuilder.append("Offer a draw");
        if (game.getDrawAgreeMap().get(game.getOPlayer().getId())) {
            textBuilder.append("✅");
        }
        drawButton.setText(textBuilder.toString());
        drawButton.setCallbackData("drawAgree");
        drawRows.add(drawButton);
        rows.add(drawRows);

        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup offerADrawMarkup(User user) {
        Game game = user.getGame();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton drawButton = new InlineKeyboardButton();
        StringBuilder textBuilder = new StringBuilder();
        if (game.getDrawAgreeMap().get(game.getXPlayer().getId())) {
            textBuilder.append("✅");
        }
        textBuilder.append("Offer a draw");
        if (game.getDrawAgreeMap().get(game.getOPlayer().getId())) {
            textBuilder.append("✅");
        }
        drawButton.setText(textBuilder.toString());
        drawButton.setCallbackData("drawAgree");
        row.add(drawButton);
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup xShopMarkUp(List<X> shop, Integer messageId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (X skin : shop){
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("%s - %d\uD83D\uDCB5".formatted(skin.getSkin(), skin.getPrice()));
            button.setCallbackData("buyX %d %d".formatted(skin.getId(), messageId));
            row.add(button);
            rows.add(row);
        }
        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup oShopMarkUp(List<O> shop, Integer messageId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (O skin : shop){
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("%s - %d\uD83D\uDCB5".formatted(skin.getSkin(), skin.getPrice()));
            button.setCallbackData("buyO %d %d".formatted(skin.getId(), messageId));
            row.add(button);
            rows.add(row);
        }
        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup xSkinsMarkup(Long xSkinId, List<X> xSkins, Integer messageId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        int counter = 0;
        for (X x : xSkins) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            if (x.getId().equals(xSkinId)) {
                button.setCallbackData("currentSkinX");
                button.setText("✳" + x.getSkin() + "✳");
            }else{
                button.setCallbackData("setXSkin " + x.getId() + " " + messageId);
                button.setText(x.getSkin());
            }
            counter++;
            row.add(button);
            if (counter == 3) {
                rows.add(row);
                counter = 0;
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()){
            rows.add(row);
        }
        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup oSkinsMarkup(Long oSkinId, List<O> oSkins, Integer messageId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        int counter = 0;
        for (O o : oSkins) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            if (o.getId().equals(oSkinId)) {
                button.setCallbackData("currentSkinO");
                button.setText("✳" + o.getSkin() + "✳");
            }else{
                button.setCallbackData("setOSkin " + o.getId() + " " + messageId);
                button.setText(o.getSkin());
            }
            counter++;
            row.add(button);
            if (counter == 3) {
                rows.add(row);
                counter = 0;
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()){
            rows.add(row);
        }
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup settingMarkup(User user, Integer messageId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton board3On3Button = new InlineKeyboardButton();
        board3On3Button.setCallbackData("setGameMode 0 " + messageId);
        if (user.getGameMode() == 0){
            board3On3Button.setCallbackData("blocked");
            board3On3Button.setText("✳3×3✳");
        }else {
            board3On3Button.setText("3×3");
        }
        row1.add(board3On3Button);

        InlineKeyboardButton board5On5Button = new InlineKeyboardButton();
        board5On5Button.setCallbackData("setGameMode 1 " + messageId);
        if (user.getGameMode() == 1){
            board5On5Button.setCallbackData("blocked");
            board5On5Button.setText("✳5×5✳");
        }else {
            board5On5Button.setText("5×5");
        }
        row1.add(board5On5Button);

        InlineKeyboardButton board8On8Button = new InlineKeyboardButton();
        board8On8Button.setCallbackData("setGameMode 2 " + messageId);
        if (user.getGameMode() == 2){
            board8On8Button.setCallbackData("blocked");
            board8On8Button.setText("✳8×8✳");
        }else {
            board8On8Button.setText("8×8");
        }
        row1.add(board8On8Button);

        rows.add(row1);
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup selectedFriendToPlayMarkup(User user){
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (User friend : user.getFriends()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(friend.getUsername());
            button.setCallbackData("playWith " + friend.getId());
            rows.add(Collections.singletonList(button));
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
