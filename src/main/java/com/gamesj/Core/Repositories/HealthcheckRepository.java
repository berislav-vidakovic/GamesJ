package com.gamesj.Core.Repositories;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.gamesj.Core.Models.Healthcheck;

public interface HealthcheckRepository extends JpaRepository<Healthcheck, Long> {
  // SELECT * FROM healthcheck ORDER BY id ASC LIMIT 1;
  Optional<Healthcheck> findTopByOrderByIdAsc(); 
}
