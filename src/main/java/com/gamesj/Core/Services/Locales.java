package com.gamesj.Core.Services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gamesj.Core.Models.Localization;
import com.gamesj.Core.Repositories.LocalizationRepository;

@Service
public class Locales {
  private final LocalizationRepository localizationRepository;

  public Locales(LocalizationRepository localizationRepository){
    this.localizationRepository = localizationRepository;
  }

  public List<Localization> getLocales(){
    return localizationRepository.findAll();  
  }
}
