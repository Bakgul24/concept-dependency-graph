package com.mathdep.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Objects for the Mathematical Concept Dependency Mapper.
 * These classes form the contract between the AI extraction layer,
 * the graph persistence layer, and the REST API.
 */
public class MathDto {

    // ─────────────────────────────────────────────────────────
    // AI Extraction Result (what Spring AI returns as JSON)
    // ─────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtractionResult {
        private List<ConceptDto> concepts;
        private List<DependencyDto> dependencies;
        private String summary;
        private List<String> validationWarnings;
        private ValidationReport validationReport;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ValidationReport {
        private List<String> warnings;
        private int removedDependencies;
        private int repairedConcepts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConceptDto {
        /** Unique slug, e.g. "def_continuity" */
        private String id;
        private String name;
        /** DEFINITION | LEMMA | THEOREM | COROLLARY | PROPOSITION */
        private String type;
        private String statement;
        private String proof;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DependencyDto {
        /** The concept that HAS a prerequisite */
        private String from;
        /** The prerequisite concept */
        private String to;
        /** Optional explanation of why this dependency exists */
        private String reason;
    }

    // ─────────────────────────────────────────────────────────
    // API Request / Response
    // ─────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisRequest {
        private String title;
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResponse {
        private String graphId;
        private String title;
        private int conceptCount;
        private int dependencyCount;
        private boolean hasCycle;
        private String message;
        private int warningCount;
        private List<String> warnings;
    }

    // ─────────────────────────────────────────────────────────
    // Graph Response for React Flow
    // ─────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphResponse {
        private String graphId;
        private String title;
        private List<ReactFlowNode> nodes;
        private List<ReactFlowEdge> edges;
        private boolean hasCycle;
        private List<String> cycleNodes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactFlowNode {
        private String id;
        private String type;   // "definitionNode" | "lemmaNode" | "theoremNode"
        private NodeData data;
        private Position position;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeData {
        private String label;
        private String conceptType;
        private String statement;
        private String proof;
        private List<DependencySummary> dependsOn;
        private List<DependencySummary> usedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private double x;
        private double y;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactFlowEdge {
        private String id;
        private String source;
        private String target;
        private String label;
        private String reason;
        private String relationType;
        private EdgeData data;
        private boolean animated;
        private EdgeStyle style;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeData {
        private String reason;
        private String relationType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeStyle {
        private String stroke;
        private int strokeWidth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependencySummary {
        private String conceptId;
        private String label;
        private String conceptType;
        private String reason;
        private String relationType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphExportResponse {
        private String graphId;
        private String title;
        private LocalDateTime createdAt;
        private List<ConceptExportDto> concepts;
        private List<DependencyExportDto> dependencies;
        private boolean hasCycle;
        private List<String> cycleNodes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConceptExportDto {
        private String id;
        private String name;
        private String type;
        private String statement;
        private String proof;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependencyExportDto {
        private String from;
        private String to;
        private String reason;
        private String relationType;
        private Double confidence;
    }
}
