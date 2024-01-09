package com.serguni.graph_repositories;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import com.serguni.entities.Film;
import com.serguni.entities.Film.Country;
import com.serguni.entities.Film.Genre;

public class FilmGraphRepository {
    private final Driver neo4jDriver;

    public FilmGraphRepository(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    public void saveFilms(Iterable<Film> films) {
        try (Session session = neo4jDriver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                for (Film film : films) {
                    saveFilmWithRelations(tx, film);
                }
                tx.commit();
            }
        }
    }

    private void saveFilmWithRelations(Transaction tx, Film film) {
        Class<?> filmClass = Film.class;

        Set<String> labels = new HashSet<>();
        Map<String, Object> properties = new HashMap<>();

        try {
            for (Field field : filmClass.getDeclaredFields()) {
                field.setAccessible(true);

                String fieldName = field.getName();
                Object value = field.get(film);

                if (value != null) {
                    if (field.getType().isEnum()) {
                        labels.add(((Enum<?>) value).name());
                    } else if (!fieldName.equals("genres") && !fieldName.equals("countries")) {
                        properties.put(fieldName, value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            // Handle the exception as needed
        }

        StringBuilder cypherQuery = new StringBuilder("MERGE (f:Film");

        // Append labels
        cypherQuery.append(labels.stream().map(label -> ":" + label).collect(Collectors.joining()));

        cypherQuery.append(" {");

        // Append properties
        cypherQuery.append(properties.entrySet().stream()
                .map(entry -> entry.getKey() + ": $" + entry.getKey())
                .collect(Collectors.joining(", ")));

        cypherQuery.append("})");

        // Create the main film node
        tx.run(cypherQuery.toString(), properties);

        // Create nodes and relationships for countries
        if (film.getCountries() != null) {
            for (Country country : film.getCountries()) {
                String countryNodeQuery = "MERGE (c:Country {name: $country})";
                tx.run(countryNodeQuery, Collections.singletonMap("country", country.getCountry()));

                String relationshipQuery = "MATCH (f:Film), (c:Country) " +
                        "WHERE f.kinopoiskId = $kinopoiskId AND c.name = $country " +
                        "MERGE (f)-[:IN_COUNTRY]->(c)";
                tx.run(relationshipQuery,
                        Map.of("kinopoiskId", film.getKinopoiskId(), "country", country.getCountry()));
            }
        }

        // Create nodes and relationships for genres
        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                String genreNodeQuery = "MERGE (g:Genre {genre: $genre})";
                tx.run(genreNodeQuery, Collections.singletonMap("genre", genre.getGenre()));

                String relationshipQuery = "MATCH (f:Film), (g:Genre) " +
                        "WHERE f.kinopoiskId = $kinopoiskId AND g.genre = $genre " +
                        "MERGE (f)-[:HAS_GENRE]->(g)";
                tx.run(relationshipQuery, Map.of("kinopoiskId", film.getKinopoiskId(), "genre", genre.getGenre()));
            }
        }
    }
}
