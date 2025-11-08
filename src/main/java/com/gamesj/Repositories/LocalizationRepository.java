package com.gamesj.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.gamesj.Models.Localization;
import java.util.List;

public interface LocalizationRepository extends JpaRepository<Localization, Integer> {
    List<Localization> findByLanguage(String language);
}
