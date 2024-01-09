package com.serguni.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.serguni.api.PersonApi;
import com.serguni.entities.Person;
import com.serguni.entities.Person.PersonFilm;
import com.serguni.entities.Person.ProfessionKey;
import com.serguni.graph_repositories.PersonGraphRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PeronService {
    private final PersonGraphRepository personGraphRepository;
    private final PersonApi personApi;
    private final Gson gson;

    public void loadPersonsToGraph() throws IOException {
        personGraphRepository.savePersons(personApi.getAllPersons());
    }

    public void loadPersonsPerviewsToGraph() throws IOException {
        Set<Person> persons = new HashSet<>();
        var personPreviews = personApi.getAllStaffsPreviews();

        for (JsonElement jsonElement : personPreviews) {
            var obj = jsonElement.getAsJsonObject();

            Person person = gson.fromJson(obj, Person.class);
            person.setPersonId(obj.get("staffId").getAsInt());

            var description = obj.get("description");

            PersonFilm personFilm = new PersonFilm();

            personFilm.setFilmId(obj.get("filmId").getAsInt());

            if (description != null) {
                personFilm.setDescription(description.getAsString());
            }

            personFilm.setProfessionKey(ProfessionKey.valueOf(obj.get("professionKey").getAsString()));

            var films = new ArrayList<PersonFilm>();
            films.add(personFilm);

            person.setFilms(films);

            persons.add(person);
        }

        personGraphRepository.savePersons(persons);
    }

    public void loadRatedRelationships() throws IOException {
        personGraphRepository.createPersonRatedRelations(
                personApi.getVotes(personApi.getAllPersons()));
    }
}
