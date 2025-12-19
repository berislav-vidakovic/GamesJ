package com.gamesj.API.GraphQL;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import com.gamesj.Core.Repositories.HealthcheckRepository;
import com.gamesj.Core.Models.Healthcheck;
import java.util.Optional;

@Controller
public class PingDbControllerGraphQL {

    private final HealthcheckRepository healthcheckRepository;

    public PingDbControllerGraphQL(HealthcheckRepository healthcheckRepository) {
      this.healthcheckRepository = healthcheckRepository;
    }

    @QueryMapping
    public String pingDb() {
      try {
        Optional<Healthcheck> rowOpt = healthcheckRepository.findTopByOrderByIdAsc();
        if (rowOpt.isEmpty()) 
          return null; // GraphQL returns "data": { "pingDb": null }        
        Healthcheck row = rowOpt.get();
        return row.getMsg();
      } 
      catch (Exception ex) {
        ex.printStackTrace();
        throw new RuntimeException("Database connection failed");
      }
    }
}
