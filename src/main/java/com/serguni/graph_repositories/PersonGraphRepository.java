package com.serguni.graph_repositories;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import com.serguni.entities.Person;
import com.serguni.entities.Vote;
import com.serguni.entities.Person.PersonFilm;
import com.serguni.entities.Person.ProfessionKey;
import com.serguni.entities.Person.Spouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PersonGraphRepository {
    @NonNull
    private final Driver neo4jDriver;

    private boolean isPersonExists(Session session, Person person) {
        String checkSpouseQuery = "MATCH (s:Person {personId: $spouseId}) RETURN s";
        Result result = session.run(checkSpouseQuery, Map.of("spouseId", person.getPersonId()));
        return result.hasNext();
    }

    public void savePersons(Iterable<Person> persons) {
        try (Session session = neo4jDriver.session()) {
            for (Person person : persons) {
                createPersonWithSpousesAndFilmsRelations(session, person);
            }
        }
    }

    private void createPerson(Session session, Person person) {

        if (isPersonExists(session, person)) {
            return;
        }

        Class<?> personClass = Person.class;

        Set<String> labels = new HashSet<>();
        Map<String, Object> properties = new HashMap<>();

        try {
            for (Field field : personClass.getDeclaredFields()) {
                field.setAccessible(true);

                String fieldName = field.getName();
                Object value = field.get(person);

                if (value != null) {
                    if (field.getType().isEnum() && field.getType() != ProfessionKey.class) {
                        labels.add(((Enum<?>) value).name());
                    } else if (!fieldName.equals("spouses") && !fieldName.equals("films")) {
                        properties.put(fieldName, value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            // Handle the exception as needed
        }

        StringBuilder cypherQuery = new StringBuilder("CREATE (p:Person");

        // Append labels
        cypherQuery
                .append(labels.stream().map(label -> ":" +
                        label).collect(Collectors.joining()))
                // Append properties
                .append("{")
                .append(properties.entrySet().stream()
                        .map(entry -> entry.getKey() + ": $" + entry.getKey())
                        .collect(Collectors.joining(", ")))
                .append("})");

        // Create person node

        try {
            session.run(cypherQuery.toString(), properties);
        } catch (Exception e) {
            System.out.println(person);
            e.printStackTrace();
        }

        createBornInAndDeathInCountryRelation(session, person);
    }

    private void createBornInAndDeathInCountryRelation(Session session, Person person) {
        // Извлекаем информацию о странах из места рождения и смерти
        String bornCountryStr = extractCountryFromPlace(person.getBirthplace());
        String deathCountryStr = extractCountryFromPlace(person.getDeathplace());

        // Создаем или обновляем связь BORN_IN
        createOrUpdateCountryRelation(session, person, bornCountryStr, "BORN_IN", person.getBirthday());

        // Создаем или обновляем связь DEATH_IN
        createOrUpdateCountryRelation(session, person, deathCountryStr, "DEATH_IN", person.getDeath());
    }

    private String extractCountryFromPlace(String place) {
        String countryStr = null;
        try {
            // Извлекаем страну из строки места (предполагаем, что страна находится в
            // четвертом элементе, разделенном запятой)
            var birthplace = place.split(", ");
            countryStr = birthplace[birthplace.length - 1];
        } catch (Exception e) {
            // Обработка возможных ошибок при извлечении
        }
        return countryStr;
    }

    private void createOrUpdateCountryRelation(Session session, Person person, String countryStr,
            String relationshipType, String date) {
        if (countryStr != null) {
            StringBuilder cypherQuery = new StringBuilder("MATCH (p:Person {personId: $personId}) ")
                    .append("MERGE (c:Country {name: $countryName}) ")
                    .append("MERGE (p)-[r:").append(relationshipType).append("]->(c) ");

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("personId", person.getPersonId());
            parameters.put("countryName", countryStr);

            // Добавляем SET только если date не равно null
            if (date != null) {
                cypherQuery.append("SET r.date = $date");
                parameters.put("date", date);
            }

            session.run(cypherQuery.toString(), parameters);
        }
    }

    // private void createOrUpdateCountryRelation(Session session, Person person,
    // String countryStr,
    // String relationshipType, String date) {
    // if (countryStr != null) {
    // // Создаем Cypher запрос для создания или обновления связи между Person и
    // // Country
    // String cypherQuery = "MATCH (p:Person {personId: $personId}) "
    // + "MERGE (c:Country {name: $countryName}) "
    // + "MERGE (p)-[r:" + relationshipType + "]->(c)";

    // // Параметры для передачи в запрос
    // Map<String, Object> parameters = new HashMap<>();
    // parameters.put("personId", person.getPersonId());
    // parameters.put("countryName", countryStr);

    // // Выполняем запрос
    // session.run(cypherQuery, parameters);
    // }
    // }

    private void createSpousesRelation(Session session, Person p1, Person p2, Spouse spouse) {
        // Check if the spouse node already exists
        String checkSpouseQuery = "MATCH (s:Person {personId: $spouseId}) RETURN s";
        Result result = session.run(checkSpouseQuery, Map.of("spouseId", p2.getPersonId()));

        if (!result.hasNext()) {
            createPerson(session, p2);
        }

        String relationshipQuery = "MATCH (p:Person {personId: $personId}), (s:Person {personId: $spouseId}) " +
                "MERGE (p)-[:SPOURCED {divorced: $divorced, divorcedReason: $divorcedReason," +
                "children: $children, relation: $relation}]->(s)";

        session.run(relationshipQuery,
                Map.of("personId", p1.getPersonId(),
                        "spouseId", p2.getPersonId(),
                        "divorced", spouse.isDivorced(),
                        "divorcedReason", spouse.getDivorcedReason(),
                        "children", spouse.getChildren(),
                        "relation", spouse.getRelation()));
    }

    private void createPersonFilmsRelation(Session session, Person person) {
        if (person.getFilms() != null) {
            for (PersonFilm film : person.getFilms()) {
                // Create relationship based on ProfessionKey
                if (film.getProfessionKey() != null) {
                    String professionRelationshipQuery = "MATCH (p:Person {personId: $personId}), (f:Film {kinopoiskId: $filmId}) "
                            +
                            "MERGE (p)-[:" + film.getProfessionKey().name() +
                            (film.getDescription() != null ? " {description: $description}" : "") +
                            "]->(f)";

                    Map<String, Object> params = new HashMap<>();

                    params.put("personId", person.getPersonId());
                    params.put("filmId", film.getFilmId());

                    var description = film.getDescription();

                    if (description != null) {
                        params.put("description", description);
                    }

                    session.run(professionRelationshipQuery, params);
                }
            }
        }
    }

    public void createPersonRatedRelations(Iterable<Vote> votes) {
        try (Session session = neo4jDriver.session()) {
            for (Vote vote : votes) {
                // Создаем Cypher запрос для создания связи между фильмом и персоной
                String cypherQuery = "MATCH (p:Person {personId: $personId}), (f:Film {kinopoiskId: $filmId}) "
                        + "MERGE (p)-[r:RATED]->(f) "
                        + "SET r.rating = $rating";

                // Параметры для передачи в запрос
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("personId", vote.getPersonId());
                parameters.put("filmId", vote.getFilmId());
                parameters.put("rating", vote.getRating());

                // Выполняем запрос
                session.run(cypherQuery, parameters);
            }
        }
    }

    private void createPersonWithSpousesAndFilmsRelations(Session session, Person person) {

        createPerson(session, person);

        // Create nodes and relationships for spouses
        if (person.getSpouses() != null) {

            for (Spouse spouse : person.getSpouses()) {
                Person spousePerson = Person.builder()
                        .personId(spouse.getPersonId())
                        .nameRu(spouse.getName())
                        .sex(spouse.getSex())
                        .webUrl(spouse.getWebUrl())
                        .build();

                createSpousesRelation(session, person, spousePerson, spouse);
            }
        }

        // Create nodes and relationships for films
        createPersonFilmsRelation(session, person);
    }

}
