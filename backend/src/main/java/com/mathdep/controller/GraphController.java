package com.mathdep.controller;

import com.mathdep.dto.MathDto.*;
import com.mathdep.service.ConceptGraphService;
import com.mathdep.service.document.DocumentTextExtractionService;
import com.mathdep.service.document.DocumentTextExtractionService.ExtractedDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/graphs")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class GraphController {

    private final ConceptGraphService graphService;
    private final DocumentTextExtractionService documentTextExtractionService;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyzeText(@RequestBody AnalysisRequest request) {
        log.info("Received analysis request: title={}", request.getTitle());

        if (request.getText() == null || request.getText().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AnalysisResponse response = graphService.analyzeAndPersist(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/analyze/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResponse> analyzeFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {

        log.info("Received file upload: name={}, size={} bytes",
            file.getOriginalFilename(), file.getSize());

        ExtractedDocument extracted = documentTextExtractionService.extract(file);
        String graphTitle = title != null && !title.isBlank() ? title : extracted.filename();

        AnalysisRequest request = AnalysisRequest.builder()
            .title(graphTitle)
            .text(extracted.text())
            .build();

        AnalysisResponse response = graphService.analyzeAndPersist(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/analyze/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResponse> analyzePdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {

        return analyzeFile(file, title);
    }

    @GetMapping("/{graphId}")
    public ResponseEntity<GraphResponse> getGraph(@PathVariable String graphId) {
        log.info("Fetching graph: {}", graphId);
        GraphResponse response = graphService.getGraphForFrontend(graphId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{graphId}/export")
    public ResponseEntity<GraphExportResponse> exportGraph(@PathVariable String graphId) {
        log.info("Exporting graph: {}", graphId);
        return ResponseEntity.ok(graphService.exportGraph(graphId));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listGraphs() {
        return ResponseEntity.ok(graphService.listAllGraphs());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Mathematical Concept Dependency Mapper"
        ));
    }
}
