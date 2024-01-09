package com.serguni.utils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JsonFileHandler<T> {

    private final Gson gson;
    private final String filePath;
    private final Class<T> itemType;

    public List<T> readFromJsonFile() {
        try (FileReader reader = new FileReader(filePath, StandardCharsets.UTF_8)) {

            // Определение типа данных для десериализации
            Type listType = TypeToken.getParameterized(List.class, itemType).getType();

            // Десериализация из файла в список
            List<T> itemList = gson.fromJson(reader, listType);

            return itemList;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void writeToJsonFile(Collection<T> iterable) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(iterable.toArray(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
