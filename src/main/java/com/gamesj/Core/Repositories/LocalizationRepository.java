package com.gamesj.Core.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.gamesj.Core.Models.Localization;
import java.util.List;

public interface LocalizationRepository extends JpaRepository<Localization, Integer> {
    List<Localization> findByLanguage(String language);
}
