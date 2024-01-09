package com.serguni.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.serguni.entities.Film;
import com.serguni.entities.Person;
import com.serguni.entities.Vote;
import com.serguni.utils.JsonFileHandler;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PersonApi {
    @NonNull
    final private KinopoiskApiClient kinopoiskApiClient;
    @NonNull
    final private Gson gson;
    @NonNull
    final private FilmApi filmsRepository;

    JsonArray personsPreviews;

    Set<Person> persons;

    Set<Vote> votes;

    private final String personsPreviewCachePath = "./cache/persons_preview.json";
    private final String personsCachePath = "./cache/persons.json";
    private final String votesCachePath = "./cache/votes.json";

    private JsonArray readCache(String cachePath) {
        try {
            return gson.fromJson(Files.newBufferedReader(Path.of(cachePath)), JsonArray.class);
        } catch (IOException e) {
            return null;
        }
    }

    public JsonArray getAllStaffsPreviews() throws IOException {
        if (personsPreviews != null) {
            return personsPreviews;
        }

        personsPreviews = readCache(personsPreviewCachePath);

        if (personsPreviews != null) {
            return personsPreviews;
        }

        var allFilmsIds = filmsRepository.getAllFilms().stream().map(Film::getKinopoiskId);

        JsonArray loadedPersonPreviews = new JsonArray();

        allFilmsIds.forEach(filmId -> {

            // Introduce a delay to stay within the rate limit (20 requests per second)
            try {
                Thread.sleep(50); // Adjust the delay duration as needed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            try {
                loadedPersonPreviews.addAll(kinopoiskApiClient.getStaffPreviewsByFilmsId(filmId));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        personsPreviews = loadedPersonPreviews;

        Files.writeString(Path.of(personsPreviewCachePath), gson.toJson(personsPreviews));

        return personsPreviews;
    }

    private Set<Person> toPeronsSet(JsonArray arr) {
        if (arr.isEmpty()) {
            return null;
        }

        var res = new HashSet<Person>();

        for (JsonElement el : arr) {
            res.add(gson.fromJson(el.getAsJsonObject(), Person.class));
        }
        return res;
    }

    public Set<Person> getAllPersons() throws IOException {
        if (persons != null) {
            return persons;
        }

        var cachedPesronJson = readCache(personsCachePath);

        if (cachedPesronJson != null) {
            persons = toPeronsSet(readCache(personsCachePath));

            if (persons != null) {
                return persons;
            }
        }

        var staffsPreviews = readCache(personsPreviewCachePath);

        var loadedPersons = new HashSet<Person>();

        for (JsonElement el : staffsPreviews) {
            JsonObject obj = el.getAsJsonObject();

            // Introduce a delay to stay within the rate limit (20 requests per second)
            try {
                Thread.sleep(50); // Adjust the delay duration as needed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            var staffId = obj.get("staffId").getAsInt();
            try {
                loadedPersons.add(kinopoiskApiClient.getStaff(staffId));
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        persons = loadedPersons;

        Files.writeString(Path.of(personsCachePath), gson.toJson(persons.stream().collect(Collectors.toList())));

        return persons;
    }

    public Set<Vote> getVotes(Collection<Person> persons) {

        var jsonFileHandler = new JsonFileHandler<Vote>(gson, votesCachePath, Vote.class);

        if (votes != null) {
            return votes;
        }

        var cachedVotes = jsonFileHandler.readFromJsonFile();

        if (cachedVotes != null) {
            votes = new HashSet<>(cachedVotes);
            return votes;
        }

        votes = new HashSet<>();

        for (Person person : persons) {
            try {
                votes.addAll(kinopoiskApiClient.getPersonVotes(person));
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        jsonFileHandler.writeToJsonFile(votes);

        return votes;
    }
}
