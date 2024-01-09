package com.serguni.entities;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Film {
    private int kinopoiskId;
    private String kinopoiskHDId;
    private String imdbId;
    private String nameRu;
    private String nameEn;
    private String nameOriginal;
    private String posterUrl;
    private String posterUrlPreview;
    private String coverUrl;
    private String logoUrl;
    private int reviewsCount;
    private Double ratingGoodReview;
    private int ratingGoodReviewVoteCount;
    private Double ratingKinopoisk;
    private int ratingKinopoiskVoteCount;
    private Double ratingImdb;
    private int ratingImdbVoteCount;
    private Double ratingFilmCritics;
    private int ratingFilmCriticsVoteCount;
    private Double ratingAwait;
    private int ratingAwaitCount;
    private Double ratingRfCritics;
    private int ratingRfCriticsVoteCount;
    private String webUrl;
    private int year;
    private int filmLength;
    private String slogan;
    private String description;
    private String shortDescription;
    private String editorAnnotation;
    private boolean isTicketsAvailable;
    private ProductionStatus productionStatus;
    private Type type;
    private String ratingMpaa;
    private String ratingAgeLimits;
    private boolean hasImax;
    private boolean has3D;
    private String lastSync;
    private List<Country> countries;
    private List<Genre> genres;
    private int startYear;
    private int endYear;
    private boolean serial;
    private boolean shortFilm;
    private boolean completed;

    public enum ProductionStatus {
        FILMING, PRE_PRODUCTION, COMPLETED, ANNOUNCED, UNKNOWN, POST_PRODUCTION
    }

    public enum Type {
        FILM, VIDEO, TV_SERIES, MINI_SERIES, TV_SHOW
    }

    @Data
    public static class Country {
        private String country;
    }

    @Data
    public static class Genre {
        private String genre;
    }
}
