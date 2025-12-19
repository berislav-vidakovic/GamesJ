package com.gamesj.Core.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.gamesj.Core.Models.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByLoginOrFullName(String login, String fullName);
}
