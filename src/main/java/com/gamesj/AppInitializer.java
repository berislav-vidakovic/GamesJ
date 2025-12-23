package com.gamesj;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.gamesj.Core.Repositories.UserRepository;

@Component
public class AppInitializer implements ApplicationRunner {

    private final UserRepository userRepository;

    public AppInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println(" =========== Cleaning up hanging user(s) =======");

        userRepository.setAllUsersOffline();
    }
}
