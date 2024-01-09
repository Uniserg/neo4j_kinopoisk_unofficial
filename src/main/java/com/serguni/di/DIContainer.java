package com.serguni.di;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serguni.api.KinopoiskApiClient;
import com.serguni.api.PersonApi;
import com.serguni.api.FilmApi;
import com.serguni.graph_repositories.FilmGraphRepository;
import com.serguni.graph_repositories.PersonGraphRepository;
import com.serguni.graph_repositories.SchemaRepository;
import com.serguni.services.FilmService;
import com.serguni.services.PeronService;
import com.serguni.services.SchemaService;
import com.serguni.utils.EnvLoader;

public class DIContainer implements AutoCloseable {
    // Утилиты
    private final EnvLoader envLoader = new EnvLoader(".env");
    public final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Переменные окружения
    private final String neo4jUri = envLoader.getEnvVar("NEO4J_URI");
    private final String neo4jUsername = envLoader.getEnvVar("NEO4J_USERNAME");
    private final String neo4jPassword = envLoader.getEnvVar("NEO4J_PASSWORD");

    // Драйвер к neo4j
    public final Driver neo4jDriver;

    // Клиент с запросами на kinopoisk unofficial
    public final KinopoiskApiClient kinopoiskApiClient;

    // Репозитории API
    public final FilmApi filmApi;
    public final PersonApi personApi;

    // Репозитории для neo4j
    public final FilmGraphRepository filmGraphRepository;
    public final PersonGraphRepository personGraphRepository;
    public final SchemaRepository schemaRepository;

    // Сервисы
    public final FilmService filmService;
    public final PeronService personService;
    public final SchemaService schemaService;

    public DIContainer() {
        neo4jDriver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUsername, neo4jPassword));

        kinopoiskApiClient = new KinopoiskApiClient(envLoader.getEnvVar("KINOPOISK_HOST"),
                envLoader.getEnvVar("KINOPOISK_API_KEY"), gson);

        filmGraphRepository = new FilmGraphRepository(neo4jDriver);

        filmApi = new FilmApi(kinopoiskApiClient, gson);

        filmService = new FilmService(filmApi, filmGraphRepository);

        personApi = new PersonApi(kinopoiskApiClient, gson, filmApi);

        personGraphRepository = new PersonGraphRepository(neo4jDriver);

        personService = new PeronService(personGraphRepository, personApi, gson);

        schemaRepository = new SchemaRepository(neo4jDriver);

        schemaService = new SchemaService(schemaRepository);
    }

    @Override
    public void close() {
        neo4jDriver.close();
    }
}
