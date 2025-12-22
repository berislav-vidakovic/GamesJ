package com.gamesj.API.REST;

import java.util.ArrayList;
import java.util.List;


public class Endpoints {

    public Endpoints() {
      Endpoints.Public.addAll( List.of(
        "/api/auth/refresh"
      ));
      Endpoints.Protected.addAll(List.of(""));
    } 

    private static List<String> Public = new ArrayList<>();
    private static List<String> Protected = new ArrayList<>();

    public static List<String> getPublicEndpoints(){
      return Public;
    }
}
