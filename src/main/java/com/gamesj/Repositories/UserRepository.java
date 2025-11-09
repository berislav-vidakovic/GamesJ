package com.gamesj.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.gamesj.Models.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByLoginOrFullName(String login, String fullName);
}
