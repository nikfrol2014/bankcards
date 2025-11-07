package com.example.bankcards.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class OpenApiController {

    @GetMapping(value = "/docs/openapi.yaml", produces = "application/yaml")
    public ResponseEntity<String> getOpenApiSpec() throws IOException {
        // Читаем файл из корневой директории docs/
        Path filePath = Paths.get("docs/openapi.yaml");
        String yamlContent = Files.readString(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/yaml"))
                .body(yamlContent);
    }

    @GetMapping(value = "/docs/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getOpenApiSpecJson() throws IOException {
        Path filePath = Paths.get("docs/openapi.yaml");
        String yamlContent = Files.readString(filePath);

        return ResponseEntity.ok()
                .body(yamlContent);
    }
}