package com.serguni.services;

import java.io.IOException;

import com.serguni.api.FilmApi;
import com.serguni.graph_repositories.FilmGraphRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FilmService {
    private final FilmApi kinopoiskApiRepository;
    private final FilmGraphRepository filmRepository;

    public void loadFilmsToGraph() throws IOException {
        filmRepository.saveFilms(kinopoiskApiRepository.getAllFilms());
    }
}
