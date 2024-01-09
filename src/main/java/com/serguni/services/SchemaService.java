package com.serguni.services;

import com.serguni.graph_repositories.SchemaRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SchemaService {
    private final SchemaRepository schemaRepository;

    public void createAllConstraints() {
        schemaRepository.createAllConstraints();
    }
}
