package com.mathdep.service;

import com.mathdep.dto.MathDto.*;
import com.mathdep.model.ConceptGraph;
import com.mathdep.model.DependencyRelation;
import com.mathdep.model.MathConcept;
import com.mathdep.model.MathConcept.ConceptType;
import com.mathdep.repository.ConceptGraphRepository;
import com.mathdep.repository.MathConceptRepository;
import com.mathdep.service.ai.MathTextAnalysisService;
import com.mathdep.service.graph.CycleDetectionService;
import com.mathdep.service.graph.CycleDetectionService.CycleDetectionResult;
import com.mathdep.service.graph.GraphLayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ConceptGraphService
 *
 * Orchestrates the full pipeline:
 *   1. Call AI to extract concepts + dependencies from text
 *   2. Run cycle detection before persisting
 *   3. Map DTOs → Neo4j entities and save
 *   4. Retrieve graph and convert to React Flow format
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptGraphService {

    private final MathTextAnalysisService aiService;
    private final CycleDetectionService cycleDetectionService;
    private final GraphLayoutService graphLayoutService;
    private final MathConceptRepository conceptRepository;
    private final ConceptGraphRepository graphRepository;

    // ── Analyze & Persist ──────────────────────────────────────────────────────

    /**
     * Full pipeline: text → AI → cycle check → Neo4j → response.
     */
    @Transactional
    public AnalysisResponse analyzeAndPersist(AnalysisRequest request) {
        String graphId = UUID.randomUUID().toString();
        log.info("Starting analysis pipeline for graphId: {}", graphId);

        // 1. Save ConceptGraph metadata
        ConceptGraph graph = ConceptGraph.builder()
            .graphId(graphId)
            .title(request.getTitle())
            .sourceText(request.getText())
            .createdAt(LocalDateTime.now())
            .status(ConceptGraph.GraphStatus.PROCESSING)
            .build();
        graphRepository.save(graph);

        // 2. AI Extraction
        ExtractionResult extracted = aiService.extractConcepts(request.getText());

        // 3. Cycle Detection
        List<String> conceptIds = extracted.getConcepts().stream()
            .map(ConceptDto::getId)
            .collect(Collectors.toList());

        CycleDetectionResult cycleResult = cycleDetectionService.detect(
            conceptIds, extracted.getDependencies());

        if (cycleResult.hasCycle()) {
            log.warn("Circular dependency detected for graphId: {}. Path: {}",
                graphId, cycleResult.cyclePath());
        }

        // 4. Persist concepts to Neo4j
        Map<String, MathConcept> savedConcepts = persistConcepts(extracted, graphId);

        // 5. Wire up DEPENDS_ON relationships
        wireDependencies(extracted.getDependencies(), savedConcepts);

        // 6. Update graph metadata
        graph.setConceptCount(savedConcepts.size());
        graph.setStatus(ConceptGraph.GraphStatus.COMPLETED);
        graphRepository.save(graph);

        return AnalysisResponse.builder()
            .graphId(graphId)
            .title(request.getTitle())
            .conceptCount(savedConcepts.size())
            .dependencyCount(extracted.getDependencies().size())
            .hasCycle(cycleResult.hasCycle())
            .message(cycleResult.hasCycle()
                ? "Analysis complete. WARNING: Circular dependency detected: " + cycleResult.cyclePath()
                : "Analysis complete. Graph saved successfully.")
            .warningCount(extracted.getValidationWarnings() != null ? extracted.getValidationWarnings().size() : 0)
            .warnings(extracted.getValidationWarnings())
            .build();
    }

    // ── Retrieve Graph ─────────────────────────────────────────────────────────

    /**
     * Load graph from Neo4j and convert to React Flow format.
     */
    @Transactional(readOnly = true)
    public GraphResponse getGraphForFrontend(String graphId) {
        log.info("Loading graph for React Flow: {}", graphId);

        ConceptGraph graph = graphRepository.findByGraphId(graphId)
            .orElseThrow(() -> new NoSuchElementException("Graph not found: " + graphId));

        List<MathConcept> concepts = conceptRepository
            .findAllWithPrerequisitesByGraphId(graphId);

        // Re-run cycle detection for display purposes
        List<String> conceptIds = concepts.stream()
            .map(MathConcept::getConceptId)
            .collect(Collectors.toList());

        // Build dependency DTOs from loaded entities
        List<DependencyDto> edges = concepts.stream()
            .flatMap(c -> dependencyRelations(c).stream()
                .filter(r -> r.getTarget() != null)
                .map(r -> DependencyDto.builder()
                    .from(c.getConceptId())
                    .to(r.getTarget().getConceptId())
                    .reason(r.getReason())
                    .build()))
            .collect(Collectors.toList());

        CycleDetectionResult cycleResult = cycleDetectionService.detect(conceptIds, edges);

        return graphLayoutService.buildReactFlowGraph(
            graphId, graph.getTitle(), concepts,
            cycleResult.hasCycle(), cycleResult.cyclePath());
    }

    @Transactional(readOnly = true)
    public GraphExportResponse exportGraph(String graphId) {
        ConceptGraph graph = graphRepository.findByGraphId(graphId)
            .orElseThrow(() -> new NoSuchElementException("Graph not found: " + graphId));

        List<MathConcept> concepts = conceptRepository
            .findAllWithPrerequisitesByGraphId(graphId);

        List<String> conceptIds = concepts.stream()
            .map(MathConcept::getConceptId)
            .collect(Collectors.toList());

        List<DependencyDto> dependencyDtos = concepts.stream()
            .flatMap(concept -> dependencyRelations(concept).stream()
                .filter(relation -> relation.getTarget() != null)
                .map(relation -> DependencyDto.builder()
                    .from(concept.getConceptId())
                    .to(relation.getTarget().getConceptId())
                    .reason(relation.getReason())
                    .build()))
            .collect(Collectors.toList());

        CycleDetectionResult cycleResult = cycleDetectionService.detect(conceptIds, dependencyDtos);

        return GraphExportResponse.builder()
            .graphId(graphId)
            .title(graph.getTitle())
            .createdAt(graph.getCreatedAt())
            .concepts(concepts.stream()
                .map(concept -> ConceptExportDto.builder()
                    .id(concept.getConceptId())
                    .name(concept.getName())
                    .type(concept.getType().name())
                    .statement(concept.getStatement())
                    .proof(concept.getProof())
                    .build())
                .collect(Collectors.toList()))
            .dependencies(concepts.stream()
                .flatMap(concept -> dependencyRelations(concept).stream()
                    .filter(relation -> relation.getTarget() != null)
                    .map(relation -> DependencyExportDto.builder()
                        .from(concept.getConceptId())
                        .to(relation.getTarget().getConceptId())
                        .reason(relation.getReason())
                        .relationType(relation.getRelationType() != null ? relation.getRelationType() : "DEPENDS_ON")
                        .confidence(relation.getConfidence())
                        .build()))
                .collect(Collectors.toList()))
            .hasCycle(cycleResult.hasCycle())
            .cycleNodes(cycleResult.cyclePath())
            .build();
    }

    // ── List All Graphs ────────────────────────────────────────────────────────

    public List<Map<String, Object>> listAllGraphs() {
        return graphRepository.findAll().stream()
            .map(g -> Map.<String, Object>of(
                "graphId", g.getGraphId(),
                "title", g.getTitle() != null ? g.getTitle() : "Untitled",
                "conceptCount", g.getConceptCount(),
                "createdAt", g.getCreatedAt().toString(),
                "status", g.getStatus().name()
            ))
            .collect(Collectors.toList());
    }

    // ── Private Helpers ────────────────────────────────────────────────────────

    private Map<String, MathConcept> persistConcepts(ExtractionResult extracted, String graphId) {
        Map<String, MathConcept> conceptMap = new HashMap<>();

        for (ConceptDto dto : extracted.getConcepts()) {
            MathConcept concept = MathConcept.builder()
                .conceptId(dto.getId())
                .name(dto.getName())
                .type(parseConceptType(dto.getType()))
                .statement(dto.getStatement())
                .proof(dto.getProof())
                .graphId(graphId)
                .dependencies(new ArrayList<>())
                .build();

            MathConcept saved = conceptRepository.save(concept);
            conceptMap.put(dto.getId(), saved);
            log.debug("Saved concept: {} ({})", saved.getName(), saved.getType());
        }

        return conceptMap;
    }

    private void wireDependencies(
            List<DependencyDto> dependencies,
            Map<String, MathConcept> conceptMap) {

        if (dependencies == null) return;

        for (DependencyDto dep : dependencies) {
            MathConcept from = conceptMap.get(dep.getFrom());
            MathConcept to = conceptMap.get(dep.getTo());

            if (from != null && to != null) {
                if (from.getDependencies() == null) {
                    from.setDependencies(new ArrayList<>());
                }
                from.getDependencies().add(DependencyRelation.builder()
                    .target(to)
                    .reason(dep.getReason())
                    .relationType("DEPENDS_ON")
                    .build());
                conceptRepository.save(from);
                log.debug("Wired: {} -[DEPENDS_ON]-> {} ({})",
                    dep.getFrom(), dep.getTo(), dep.getReason());
            } else {
                log.warn("Dependency references unknown concept: {} -> {}", dep.getFrom(), dep.getTo());
            }
        }
    }

    private ConceptType parseConceptType(String type) {
        try {
            return ConceptType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Unknown concept type '{}', defaulting to DEFINITION", type);
            return ConceptType.DEFINITION;
        }
    }

    private List<DependencyRelation> dependencyRelations(MathConcept concept) {
        return concept.getDependencies() != null
            ? concept.getDependencies()
            : Collections.emptyList();
    }
}
