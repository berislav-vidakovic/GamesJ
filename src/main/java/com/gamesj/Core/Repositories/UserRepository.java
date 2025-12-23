package com.gamesj.Core.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.gamesj.Core.Models.User;

import jakarta.transaction.Transactional;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByLoginOrFullName(String login, String fullName);
    Optional<User> findById(Integer  userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isOnline = false")
    void setAllUsersOffline();
}
