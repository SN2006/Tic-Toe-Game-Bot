package com.example.tictoegamebot.repositories;

import com.example.tictoegamebot.entity.User;
import com.example.tictoegamebot.entity.X;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}
