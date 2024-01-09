package com.serguni.graph_repositories;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SchemaRepository {

    private final Driver neo4jDriver;

    public void createAllConstraints() {
        try (Session session = neo4jDriver.session()) {
            session.run("CREATE CONSTRAINT FOR (film:Film) REQUIRE film.kinopoiskId IS UNIQUE");

            session.run("CREATE CONSTRAINT FOR (country:Country) REQUIRE country.name IS UNIQUE");

            session.run("CREATE CONSTRAINT FOR (person:Person) REQUIRE person.personId IS UNIQUE");

            session.run("CREATE CONSTRAINT FOR (genre:Genre) REQUIRE genre.genre IS UNIQUE");
        }
    }
}
