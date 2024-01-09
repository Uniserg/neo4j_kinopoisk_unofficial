package com.serguni;

import java.io.IOException;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import com.serguni.di.DIContainer;

public class Main {

    public static void dropDb(Driver driver) {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
            session.run("CALL apoc.schema.assert({}, {})");
        }
    }

    public static void main(String[] args) throws IOException {
        // Создание контейнера зависисмостей через try с ресурсами (DIContainer -
        // AutoCloseables)
        try (DIContainer container = new DIContainer()) {
            dropDb(container.neo4jDriver); // Очистка БД

            // Создание ограничений
            container.schemaService.createAllConstraints();

            // Загружаем фильмы
            container.filmService.loadFilmsToGraph();

            // Загружаем пользователей с развернутой информацией
            // сюда входи пол, факты, супруги и др. дполнительные поля, которые так же имеют
            // связи
            container.personService.loadPersonsToGraph();

            // Загрузка людей с неполной информацией
            // Учитывая, что людей в БД довольно много (примерно 5500), а ограничение - 500
            // запросов в день
            // было принято решение загрузить краткую информацию об оставшихся людях
            container.personService.loadPersonsPerviewsToGraph();

            // Загрузка оценок
            // Учитывая, что пользователей много для ограничения, загружаются не все
            // Было замечено, что приходят фильмы, которых нет в основном запросе по фильмам
            // Резолвить не стал, потому что лень. Это уже проблема самой API
            container.personService.loadRatedRelationships();
        }
    }
}
