package com.gamesj.API.GraphQL;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
@Controller
public class PingControllerGraphQL {
  // Method name ping() corresponds to the Query field in schema.graphqls
  @QueryMapping
  public String ping() {
    return "pong";
  }
}
