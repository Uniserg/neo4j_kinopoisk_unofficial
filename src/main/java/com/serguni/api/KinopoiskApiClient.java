package com.serguni.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.serguni.entities.Person;
import com.serguni.entities.Vote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
public class KinopoiskApiClient {

    @Data
    @Builder
    public static class GetAllFilmsParams {
        private String countries;
        private String genres;
        private String order;
        private String type;
        private Double ratingFrom;
        private Double ratingTo;
        private Integer yearFrom;
        private Integer yearTo;
        private String imdbId;
        private String keyword;
        private Integer page;
    }

    public final String kinopoiskHost;
    public final String apiKey;
    public final Gson gson;

    public JsonElement getFilmByKinopoiskId(int kinopoiskId) throws IOException {
        String url = kinopoiskHost + "/api/v2.2/films/" + kinopoiskId;
        Reader json = makeApiRequest(url);
        return gson.fromJson(json, JsonElement.class);
    }

    public JsonArray getFilms(int page) throws IOException {
        return getFilms(GetAllFilmsParams.builder().page(page).build());
    }

    public JsonObject getFilmsJson(GetAllFilmsParams params) throws IOException {
        StringBuilder urlBuilder = new StringBuilder(kinopoiskHost + "/api/v2.2/films?");
        appendQueryParams(urlBuilder, params);

        String url = urlBuilder.toString();
        Reader json = makeApiRequest(url);

        return gson.fromJson(json, JsonObject.class);
    }

    private JsonArray parseToJsonArray(JsonObject jsonObject) {
        if (jsonObject.has("items") && jsonObject.get("items").isJsonArray()) {
            return jsonObject.getAsJsonArray("items");
        }
        return null;
    }

    JsonArray getAllFilms() throws IOException {
        var films = new JsonArray();
        int totalPages = getFilmsPagesTotal();

        for (int i = 1; i <= totalPages; i++) {
            var loaded = getFilms(i);
            if (loaded != null) {
                films.addAll(loaded);
            }
        }

        return films;
    }

    public JsonArray getFilms(GetAllFilmsParams params) throws IOException {
        return parseToJsonArray(getFilmsJson(params));
    }

    private void appendQueryParams(StringBuilder urlBuilder, GetAllFilmsParams params) {
        Class<?> clazz = params.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(params);
                if (value != null) {
                    appendQueryParam(urlBuilder, field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public int getFilmsPagesTotal() throws IOException {
        String url = kinopoiskHost + "/api/v2.2/films";

        Reader json = makeApiRequest(url);

        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        return jsonObject.get("totalPages").getAsInt();
    }

    private Reader makeGetPersonsRequest() throws IOException {
        return makeApiRequest(kinopoiskHost + "/api/v1/persons");
    }

    public int getPersonsTotal() throws IOException {
        return gson
                .fromJson(makeGetPersonsRequest(), JsonObject.class)
                .get("total")
                .getAsInt();
    }

    public Set<Integer> getAllPeronIds(int page) throws IOException {
        var json = gson.fromJson(makeGetPersonsRequest(), JsonObject.class);

        var personIds = new HashSet<Integer>();

        JsonArray items = json.get("items").getAsJsonArray();

        for (JsonElement item : items) {
            personIds.add(item.getAsJsonObject().get("kinopoiskId").getAsInt());
        }

        return personIds;
    }

    public Set<Integer> getAllPeronIds() throws IOException {
        var total = getPersonsTotal();
        var pageSize = 50;
        var personIds = new HashSet<Integer>();

        for (int page = 1; page <= total / pageSize + 1; page++) {

            // Introduce a delay to stay within the rate limit (20 requests per second)
            try {
                Thread.sleep(50); // Adjust the delay duration as needed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            var pageIds = getAllPeronIds(page);
            personIds.addAll(pageIds);
        }

        return personIds;
    }

    public Person getStaff(int id) throws IOException {
        return gson.fromJson(makeApiRequest(kinopoiskHost + "/api/v1/staff/" + id), Person.class);
    }

    public JsonArray getStaffPreviewsByFilmsId(int filmId) throws IOException {

        JsonArray jsonArray = gson.fromJson(makeApiRequest(kinopoiskHost + "/api/v1/staff?filmId=" + filmId),
                JsonArray.class);

        for (JsonElement jsonElement : jsonArray) {
            var obj = jsonElement.getAsJsonObject();
            obj.addProperty("filmId", filmId);
        }

        return jsonArray;
    }

    private Set<Vote> toVotesSet(Person person, JsonArray votesJson) {
        Set<Vote> votes = new HashSet<>();

        for (JsonElement el : votesJson) {

            var obj = el.getAsJsonObject();

            Vote vote = new Vote();

            vote.setFilmId(obj.get("kinopoiskId").getAsInt());
            vote.setPersonId(person.getPersonId());
            vote.setRating(obj.get("userRating").getAsInt());

            votes.add(vote);
        }

        return votes;
    }

    public Set<Vote> getPersonVotes(Person person) throws IOException {

        var json = gson.fromJson(makeApiRequest(kinopoiskHost + "/api/v1/kp_users/" + person.getPersonId() + "/votes"),
                JsonObject.class);

        var total = json.get("totalPages").getAsInt();

        Set<Vote> votes = toVotesSet(person, json.get("items").getAsJsonArray());

        for (int i = 2; i <= total; i++) {
            var json2 = gson.fromJson(
                    makeApiRequest(
                            kinopoiskHost + "/api/v1/kp_users/" + person.getPersonId() + "/votes" + "?page=" + i),
                    JsonObject.class);

            votes.addAll(toVotesSet(person, json2.get("items").getAsJsonArray()));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return votes;
    }

    private void appendQueryParam(StringBuilder urlBuilder, String paramName, Object paramValue) {
        if (paramValue != null) {
            if (urlBuilder.charAt(urlBuilder.length() - 1) != '?') {
                urlBuilder.append('&');
            }
            urlBuilder.append(paramName).append('=').append(paramValue);
        }
    }

    private Reader makeApiRequest(String url) throws IOException {
        URL apiUrl = new URL(url);

        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        connection.setRequestMethod("GET");

        connection.setRequestProperty("X-API-KEY", apiKey);

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            return new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
        } else {
            throw new IOException("Failed to fetch data. HTTP Status: " + responseCode);
        }
    }
}
