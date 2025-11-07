package com.gamesj.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.gamesj.Models.Healthcheck;

public interface HealthcheckRepository extends JpaRepository<Healthcheck, Long> {
}
