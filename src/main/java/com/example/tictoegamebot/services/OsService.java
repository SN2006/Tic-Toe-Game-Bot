package com.example.tictoegamebot.services;

import com.example.tictoegamebot.entity.O;
import com.example.tictoegamebot.entity.User;
import com.example.tictoegamebot.exception.NotEnoughMoneyException;
import com.example.tictoegamebot.repositories.ORepository;
import com.example.tictoegamebot.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OsService {

    private final ORepository oRepository;
    private final UserRepository userRepository;

    @Autowired
    public OsService(ORepository oRepository, UserRepository userRepository) {
        this.oRepository = oRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<O> getShopForUser(Long userId) {
        List<O> shop = new ArrayList<>();
        List<O> osFromDb = oRepository.findAll();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return shop;
        }
        for (O o : osFromDb) {
            if (user.getOSkins().contains(o)) {
                continue;
            }
            shop.add(o);
        }
        return shop;
    }

    @Transactional
    public void buyOForUser(Long userId, Long oId) throws NotEnoughMoneyException {
        User user = userRepository.findById(userId).orElse(null);
        O o = oRepository.findById(oId).orElse(null);
        if (user == null || o == null) {
            return;
        }
        if (user.getOSkins().contains(o)) {
            return;
        }
        if (user.getMoney() < o.getPrice()) {
            throw new NotEnoughMoneyException("You don't have enough money(%d \uD83D\uDCB5)".formatted(
                    o.getPrice() - user.getMoney()
            ));
        }
        user.takeMoney(o.getPrice());
        user.addOSkins(o);
    }
}
