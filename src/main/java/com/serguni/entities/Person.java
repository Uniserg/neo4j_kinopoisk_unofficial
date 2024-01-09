package com.serguni.entities;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class Person {
    private int personId;
    private String webUrl;
    private String nameRu;
    private String nameEn;
    private Sex sex;
    private String posterUrl;
    private String growth;
    private String birthday;
    private String death;
    private Integer age;
    private String birthplace;
    private String deathplace;
    private Integer hasAwards;
    private String profession;
    private List<String> facts;
    private List<Spouse> spouses;
    private List<PersonFilm> films;

    public enum Sex {
        MALE, FEMALE
    }

    public enum ProfessionKey {
        WRITER,
        OPERATOR,
        EDITOR,
        COMPOSER,
        PRODUCER_USSR,
        HIMSELF,
        HERSELF,
        HRONO_TITR_MALE,
        HRONO_TITR_FEMALE,
        TRANSLATOR,
        DIRECTOR,
        DESIGN,
        PRODUCER,
        ACTOR,
        VOICE_DIRECTOR,
        UNKNOWN
    }

    @Data
    public static class Spouse {
        private int personId;
        private String name;
        private boolean divorced;
        private String divorcedReason;
        private Sex sex;
        private int children;
        private String webUrl;
        private String relation;
    }

    @Data
    @NoArgsConstructor
    public static class PersonFilm {
        private int filmId;
        private String nameRu;
        private String nameEn;
        private String rating;
        private boolean general;
        private String description;
        private ProfessionKey professionKey;
    }
}
