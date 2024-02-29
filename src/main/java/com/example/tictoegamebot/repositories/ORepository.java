package com.example.tictoegamebot.repositories;

import com.example.tictoegamebot.entity.O;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ORepository extends JpaRepository<O, Long> {
}
