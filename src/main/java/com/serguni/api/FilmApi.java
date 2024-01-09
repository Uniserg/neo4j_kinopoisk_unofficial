package com.serguni.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.serguni.entities.Film;
import com.serguni.utils.JsonFileHandler;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FilmApi {
    @NonNull
    final private KinopoiskApiClient kinopoiskApiClient;
    @NonNull
    final private Gson gson;

    Set<Film> cachedInMemoryFilms;

    private List<Film> parseFimlsJson(JsonArray itemsArray) {
        List<Film> films = new ArrayList<>(itemsArray.size());

        for (int i = 0; i < itemsArray.size(); i++) {
            JsonObject filmObject = itemsArray.get(i).getAsJsonObject();
            Film film = gson.fromJson(filmObject, Film.class);
            films.add(film);
        }
        return films;
    }

    private Set<Film> getUniqueFilms() throws IOException {
        var films = new HashSet<Film>();
        int totalPages = kinopoiskApiClient.getFilmsPagesTotal();

        for (int i = 1; i <= totalPages; i++) {
            var loaded = kinopoiskApiClient.getFilms(i);

            if (loaded != null) {
                films.addAll(parseFimlsJson(loaded));
            }
        }

        System.out.println("Количество фильмов: " + films.size());

        return films;
    }

    public JsonArray getAllFullFilmsJson() throws IOException {
        var jsonArray = new JsonArray();

        for (Film film : getUniqueFilms()) {

            // Introduce a delay to stay within the rate limit (20 requests per second)
            try {
                Thread.sleep(50); // Adjust the delay duration as needed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            jsonArray.add(kinopoiskApiClient.getFilmByKinopoiskId(film.getKinopoiskId()));
        }

        return jsonArray;
    }

    public Set<Film> getAllFilms() throws IOException {

        if (cachedInMemoryFilms != null) {
            return cachedInMemoryFilms;
        }

        var cahcheHandler = new JsonFileHandler<Film>(gson, "./cache/films.json", Film.class);

        var cachedFilms = cahcheHandler.readFromJsonFile();
        if (cachedFilms != null) {
            cachedInMemoryFilms = cachedFilms.stream().collect(Collectors.toSet());
            return cachedInMemoryFilms;
        }

        cachedInMemoryFilms = getUniqueFilms().stream().map(film -> {
            // Introduce a delay to stay within the rate limit (20 requests per second)
            try {
                Thread.sleep(50); // Adjust the delay duration as needed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            try {
                return gson.fromJson(kinopoiskApiClient.getFilmByKinopoiskId(film.getKinopoiskId()), Film.class);
            } catch (JsonSyntaxException | IOException e) {
                e.printStackTrace();
                return null;
            }

        }).filter(e -> e != null).collect(Collectors.toSet());

        cahcheHandler.writeToJsonFile(cachedInMemoryFilms.stream().collect(Collectors.toList()));

        return cachedInMemoryFilms;
    }
}
