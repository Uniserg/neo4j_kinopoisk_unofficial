package com.serguni.entities;

import lombok.Data;

@Data
public class Vote {
    private int personId;
    private int filmId;
    private int rating;
}
