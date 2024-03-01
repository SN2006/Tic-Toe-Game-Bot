package com.example.tictoegamebot.services;

import com.example.tictoegamebot.entity.User;
import com.example.tictoegamebot.entity.X;
import com.example.tictoegamebot.exception.NotEnoughMoneyException;
import com.example.tictoegamebot.repositories.UserRepository;
import com.example.tictoegamebot.repositories.XRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class XsService {

    private final XRepository xRepository;
    private final UserRepository userRepository;

    @Autowired
    public XsService(XRepository xRepository, UserRepository userRepository) {
        this.xRepository = xRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<X> getShopForUser(Long userId) {
        List<X> shop = new ArrayList<>();
        List<X> xsFromDb = xRepository.findAll();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return shop;
        }
        for (X x : xsFromDb) {
            if (user.getXSkins().contains(x)) {
                continue;
            }
            shop.add(x);
        }
        return shop;
    }

    @Transactional
    public void buyXForUser(Long userId, Long xId) throws NotEnoughMoneyException {
        User user = userRepository.findById(userId).orElse(null);
        X x = xRepository.findById(xId).orElse(null);
        if (user == null || x == null) {
            return;
        }
        if (user.getXSkins().contains(x)) {
            return;
        }
        if (user.getMoney() < x.getPrice()) {
            throw new NotEnoughMoneyException("You don't have enough money(%d \uD83D\uDCB5)".formatted(
                    x.getPrice() - user.getMoney()
            ));
        }
        user.takeMoney(x.getPrice());
        user.addXSkin(x);
    }
}
