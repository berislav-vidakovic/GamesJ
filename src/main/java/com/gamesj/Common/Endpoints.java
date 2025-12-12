package com.gamesj.Common;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class Endpoints {

    public Endpoints() {
      this.endpointsPublic.addAll( List.of(
        "/api/auth/refresh"
      ));
      this.endpointsProtected.addAll(List.of(""));
    } 

    private List<String> endpointsPublic = new ArrayList<>();
    private List<String> endpointsProtected = new ArrayList<>();

    public List<String> getPublicEndpoints(){
      return endpointsPublic;
    }
}
