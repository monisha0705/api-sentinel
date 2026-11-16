
package com.apisentinel.sentinel.controller;

import com.apisentinel.sentinel.dto.EndpointSummary;
import com.apisentinel.sentinel.service.OpenApiParserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/specifications")
public class SpecificationController {

    private final OpenApiParserService parserService;

    public SpecificationController(OpenApiParserService parserService) {
        this.parserService = parserService;
    }

    @PostMapping(
        value = "/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadSpecification(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please upload a non-empty OpenAPI file.");
        }

        try {
            String specification = new String(
                    file.getBytes(),
                    StandardCharsets.UTF_8
            );

            List<EndpointSummary> endpoints =
                    parserService.parse(specification);

            return ResponseEntity.ok(endpoints);

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(exception.getMessage());

        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to read the uploaded file.");
        }
    }
}
