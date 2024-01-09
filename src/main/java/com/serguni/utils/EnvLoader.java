package com.serguni.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnvLoader {
    private Map<String, String> envVars;

    public EnvLoader(String envFilePath) {
        this.envVars = loadEnvFromFile(envFilePath);
    }

    private Map<String, String> loadEnvFromFile(String envFilePath) {
        Map<String, String> envVars = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(envFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Разделение строки по символу '=' для получения имени переменной и её значения
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    envVars.put(key, value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return envVars;
    }

    public String getEnvVar(String varName) {
        return envVars.get(varName);
    }
}
