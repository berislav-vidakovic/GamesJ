package com.gamesj.Core.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "localization")
public class Localization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "paramkey", nullable = false)
    private String paramKey;

    @Column(name = "paramvalue", nullable = false)
    private String paramValue;

    @Column(name = "lang", nullable = false, length = 2)
    private String language;

    // --- Getters and Setters ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getParamKey() { return paramKey; }
    public void setParamKey(String paramKey) { this.paramKey = paramKey; }

    public String getParamValue() { return paramValue; }
    public void setParamValue(String paramValue) { this.paramValue = paramValue; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
