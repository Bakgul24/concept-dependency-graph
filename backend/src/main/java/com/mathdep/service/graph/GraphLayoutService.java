package com.mathdep.service.graph;

import com.mathdep.dto.MathDto.*;
import com.mathdep.model.DependencyRelation;
import com.mathdep.model.MathConcept;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GraphLayoutService
 *
 * Assigns (x, y) positions to nodes for the React Flow frontend.
 * Uses a layered layout strategy:
 *
 *   Layer 0 (bottom): DEFINITION nodes (no prerequisites)
 *   Layer 1 (middle): LEMMA nodes
 *   Layer 2 (top):    THEOREM / COROLLARY / PROPOSITION nodes
 *
 * Within each layer, nodes are evenly spaced horizontally.
 */
@Slf4j
@Service
public class GraphLayoutService {

    private static final int LAYER_HEIGHT = 200;
    private static final int NODE_WIDTH = 280;
    private static final int HORIZONTAL_PADDING = 60;

    /**
     * Converts persisted MathConcept nodes into React Flow nodes and edges.
     */
    public GraphResponse buildReactFlowGraph(
            String graphId,
            String title,
            List<MathConcept> concepts,
            boolean hasCycle,
            List<String> cyclePath) {

        Map<Integer, List<MathConcept>> layers = groupByLayer(concepts);
        List<ReactFlowNode> nodes = buildNodes(layers, concepts);
        List<ReactFlowEdge> edges = buildEdges(concepts);

        return GraphResponse.builder()
            .graphId(graphId)
            .title(title)
            .nodes(nodes)
            .edges(edges)
            .hasCycle(hasCycle)
            .cycleNodes(cyclePath)
            .build();
    }

    // ── Node Building ──────────────────────────────────────────────────────────

    private List<ReactFlowNode> buildNodes(
            Map<Integer, List<MathConcept>> layers,
            List<MathConcept> concepts) {
        List<ReactFlowNode> nodes = new ArrayList<>();

        layers.forEach((layerIndex, layerConcepts) -> {
            int count = layerConcepts.size();
            int totalWidth = count * NODE_WIDTH + (count - 1) * HORIZONTAL_PADDING;
            int startX = -totalWidth / 2;

            for (int i = 0; i < count; i++) {
                MathConcept concept = layerConcepts.get(i);
                double x = startX + i * (NODE_WIDTH + HORIZONTAL_PADDING);
                double y = -layerIndex * LAYER_HEIGHT; // Higher layers go up

                nodes.add(ReactFlowNode.builder()
                    .id(concept.getConceptId())
                    .type(resolveNodeType(concept.getType()))
                    .data(NodeData.builder()
                        .label(concept.getName())
                        .conceptType(concept.getType().name())
                        .statement(concept.getStatement())
                        .proof(concept.getProof())
                        .dependsOn(buildDependsOn(concept))
                        .usedBy(buildUsedBy(concept, concepts))
                        .build())
                    .position(new Position(x, y))
                    .build());
            }
        });

        return nodes;
    }

    // ── Edge Building ──────────────────────────────────────────────────────────

    private List<ReactFlowEdge> buildEdges(List<MathConcept> concepts) {
        List<ReactFlowEdge> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (MathConcept concept : concepts) {
            for (DependencyRelation relation : dependencyRelations(concept)) {
                MathConcept prerequisite = relation.getTarget();
                if (prerequisite == null) {
                    continue;
                }
                String edgeId = "e_%s_%s".formatted(concept.getConceptId(), prerequisite.getConceptId());
                if (seen.add(edgeId)) {
                    String relationType = relation.getRelationType() != null
                        ? relation.getRelationType()
                        : "DEPENDS_ON";
                    edges.add(ReactFlowEdge.builder()
                        .id(edgeId)
                        .source(concept.getConceptId())
                        .target(prerequisite.getConceptId())
                        .label("depends on")
                        .reason(relation.getReason())
                        .relationType(relationType)
                        .data(EdgeData.builder()
                            .reason(relation.getReason())
                            .relationType(relationType)
                            .build())
                        .animated(false)
                        .style(EdgeStyle.builder()
                            .stroke(resolveEdgeColor(concept.getType(), relationType))
                            .strokeWidth(2)
                            .build())
                        .build());
                }
            }
        }

        return edges;
    }

    private List<DependencySummary> buildDependsOn(MathConcept concept) {
        return dependencyRelations(concept).stream()
            .filter(relation -> relation.getTarget() != null)
            .map(relation -> {
                MathConcept target = relation.getTarget();
                String relationType = relation.getRelationType() != null
                    ? relation.getRelationType()
                    : "DEPENDS_ON";
                return DependencySummary.builder()
                    .conceptId(target.getConceptId())
                    .label(target.getName())
                    .conceptType(target.getType().name())
                    .reason(relation.getReason())
                    .relationType(relationType)
                    .build();
            })
            .collect(Collectors.toList());
    }

    private List<DependencySummary> buildUsedBy(MathConcept concept, List<MathConcept> allConcepts) {
        return allConcepts.stream()
            .flatMap(source -> dependencyRelations(source).stream()
                .filter(relation -> relation.getTarget() != null)
                .filter(relation -> concept.getConceptId().equals(relation.getTarget().getConceptId()))
                .map(relation -> {
                    String relationType = relation.getRelationType() != null
                        ? relation.getRelationType()
                        : "DEPENDS_ON";
                    return DependencySummary.builder()
                        .conceptId(source.getConceptId())
                        .label(source.getName())
                        .conceptType(source.getType().name())
                        .reason(relation.getReason())
                        .relationType(relationType)
                        .build();
                }))
            .collect(Collectors.toList());
    }

    private List<DependencyRelation> dependencyRelations(MathConcept concept) {
        return concept.getDependencies() != null
            ? concept.getDependencies()
            : Collections.emptyList();
    }

    // ── Layer Assignment ───────────────────────────────────────────────────────

    /**
     * Groups concepts into layers based on type hierarchy:
     * Layer 0: DEFINITION
     * Layer 1: LEMMA
     * Layer 2: THEOREM, COROLLARY, PROPOSITION
     */
    private Map<Integer, List<MathConcept>> groupByLayer(List<MathConcept> concepts) {
        return concepts.stream().collect(Collectors.groupingBy(c -> switch (c.getType()) {
            case DEFINITION -> 0;
            case LEMMA -> 1;
            case THEOREM, COROLLARY, PROPOSITION -> 2;
        }));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String resolveNodeType(MathConcept.ConceptType type) {
        return switch (type) {
            case DEFINITION -> "definitionNode";
            case LEMMA -> "lemmaNode";
            case THEOREM -> "theoremNode";
            case COROLLARY -> "corollaryNode";
            case PROPOSITION -> "propositionNode";
        };
    }

    private String resolveEdgeColor(MathConcept.ConceptType fromType, String relationType) {
        if (!"DEPENDS_ON".equals(relationType)) {
            return "#a0aec0";
        }

        return switch (fromType) {
            case THEOREM -> "#e74c3c";
            case LEMMA -> "#e67e22";
            case COROLLARY -> "#9b59b6";
            case PROPOSITION -> "#3498db";
            default -> "#95a5a6";
        };
    }
}
