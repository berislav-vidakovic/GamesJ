package com.gamesj.API.GraphQL;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.gamesj.API.REST.RequestChecker;
import com.gamesj.Core.DTO.LocalizationResponse;
import com.gamesj.Core.Models.Localization;
import com.gamesj.Core.Services.Locales;

@Controller
public class LocalesQueryController {

  private final Locales localesService;

  public LocalesQueryController(Locales localesService) {
      this.localesService = localesService;
  }

  @QueryMapping
  public LocalizationResponse localizations(@Argument String clientId) {

      System.out.println("RESOLVER STARTED  GraphQL  *******************");

      UUID parsedClientId = RequestChecker.parseIdParameter(clientId);
      if (parsedClientId == null) 
          return new LocalizationResponse(List.of());

      List<Localization> locales = localesService.getLocales();      
      if (locales.isEmpty()) {
          System.out.println("EMPTY LIST GraphQL Response *******************");

          return new LocalizationResponse(List.of());
      }
      System.out.println("Returning GraphQL Response *******************");
      return new LocalizationResponse(locales);
  }
}

