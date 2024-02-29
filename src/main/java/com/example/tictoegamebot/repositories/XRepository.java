package com.example.tictoegamebot.repositories;

import com.example.tictoegamebot.entity.X;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface XRepository extends JpaRepository<X, Long> {
}
