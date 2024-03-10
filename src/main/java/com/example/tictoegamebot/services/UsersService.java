package com.example.tictoegamebot.services;

import com.example.tictoegamebot.entity.O;
import com.example.tictoegamebot.entity.Statistic;
import com.example.tictoegamebot.entity.User;
import com.example.tictoegamebot.entity.X;
import com.example.tictoegamebot.exception.AlreadyConnectedToFriendException;
import com.example.tictoegamebot.exception.UserIsAlreadyYourFriendException;
import com.example.tictoegamebot.exception.UserNotFoundException;
import com.example.tictoegamebot.repositories.ORepository;
import com.example.tictoegamebot.repositories.UserRepository;
import com.example.tictoegamebot.repositories.XRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UsersService {

    private final UserRepository userRepository;
    private final ORepository oRepository;
    private final XRepository xRepository;

    @Autowired
    public UsersService(UserRepository userRepository, ORepository oRepository, XRepository xRepository) {
        this.userRepository = userRepository;
        this.oRepository = oRepository;
        this.xRepository = xRepository;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id){
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public User save(User user){
        X defaultX1 = xRepository.findById(1L).orElse(null);
        X defaultX2 = xRepository.findById(2L).orElse(null);
        O defaultO1 = oRepository.findById(1L).orElse(null);
        O defaultO2 = oRepository.findById(2L).orElse(null);
        user.addXSkin(defaultX1);
        user.addXSkin(defaultX2);
        user.addOSkins(defaultO1);
        user.addOSkins(defaultO2);
        user.setXSkin(defaultX1);
        user.setOSkin(defaultO1);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUsername(User user, String newUsername){
        user.setUsername(newUsername);
        return userRepository.save(user);
    }

    @Transactional
    public void setFriend(User user, Long id) throws UserNotFoundException, AlreadyConnectedToFriendException {
        Optional<User> friendOptional = userRepository.findById(id);
        if (friendOptional.isEmpty()){
            throw new UserNotFoundException("User with id " + id + " not found");
        }
        if (user.getFriend() != null){
            throw new AlreadyConnectedToFriendException(String.format("You are already playing a game with %s"
                    , user.getFriend().getUsername()));
        }
        User friend = friendOptional.get();
        if (friend.getFriend() != null){
            throw new AlreadyConnectedToFriendException(String.format("%s is already playing a game",
                    friend.getUsername()));
        }
        user.setMyFriend(friend);
        userRepository.save(user);
    }

    @Transactional
    public void unsetFriend(User user) throws UserNotFoundException {
        if (user.getFriend() == null){
            throw new UserNotFoundException("You are not connected to other users");
        }
        User friend = user.getFriend();
        user.unsetMyFriend();
        userRepository.save(user);
        userRepository.save(friend);
    }

    @Transactional
    public void addResult(Long id, int score, int money){
        User user = userRepository.findById(id).orElse(null);
        if (user == null){
            return;
        }
        int userScore = user.getScore();
        userScore += score;
        if (userScore < 0) userScore = 0;

        if (score > 0){
            user.increaseWins();
        }else if (score < 0){
            user.increaseLoses();
        }else{
            user.increaseDraws();
        }
        user.setScore(userScore);
        user.addMoney(money);
    }

    @Transactional(readOnly = true)
    public Statistic getUserStatistic(Long id){
        User user = userRepository.findById(id).orElse(null);
        if (user == null){
            return new Statistic(0,0,0,0,0,0,0);
        }
        int countOfGame = user.getDraws() + user.getWins() + user.getLoses();
        double percentage = (double)user.getWins() / countOfGame * 100;
        return new Statistic(countOfGame, user.getWins(), user.getLoses(), user.getDraws(),
                user.getScore(), user.getMoney(), percentage);
    }

    @Transactional(readOnly = true)
    public List<O> getUserOSkins(Long id){
        User user = userRepository.findById(id).orElse(null);
        if (user == null){
            return new ArrayList<>();
        }
        return user.getOSkins();
    }

    @Transactional(readOnly = true)
    public List<X> getUserXSkins(Long id){
        User user = userRepository.findById(id).orElse(null);
        if (user == null){
            return new ArrayList<>();
        }
        return user.getXSkins();
    }

    @Transactional
    public void setOSkin(Long id, Long oId){
        O o = oRepository.findById(oId).orElse(null);
        User user = userRepository.findById(id).orElse(null);
        if (user == null || o == null){
            return;
        }
        user.setOSkin(o);
    }

    @Transactional
    public void setXSkin(Long id, Long xId){
        X x = xRepository.findById(xId).orElse(null);
        User user = userRepository.findById(id).orElse(null);
        if (user == null || x == null){
            return;
        }
        user.setXSkin(x);
    }

    @Transactional(readOnly = true)
    public List<User> getRating(){
        return userRepository.findAll(Sort.by("score").descending());
    }

    @Transactional
    public User setGameMode(Long id, int gameModeId){
        User user = userRepository.findById(id).orElse(null);
        if (user == null){
            return null;
        }
        user.setGameMode(gameModeId);
        return user;
    }

    @Transactional
    public User addMoney(Long id, int money){
        User user = userRepository.findById(id).orElse(null);
        if (user == null){
            return null;
        }
        user.addMoney(money);
        return user;
    }

    @Transactional
    public User addFriend(Long userId, Long friendId) throws UserNotFoundException, UserIsAlreadyYourFriendException {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null){
            return null;
        }
        User friend = userRepository.findById(friendId).orElse(null);
        if (friend == null){
            throw new UserNotFoundException("User with id " + friendId + " not found");
        }
        user.addFriend(friend);
        return friend;
    }
}
