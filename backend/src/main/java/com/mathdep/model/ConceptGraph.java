package com.mathdep.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

/**
 * Represents a single analysis session / uploaded document.
 * Acts as a root node connecting all extracted concepts.
 */
@Node("ConceptGraph")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptGraph {

    @Id
    @GeneratedValue
    private Long id;

    @Property("graphId")
    private String graphId;

    @Property("title")
    private String title;

    @Property("sourceText")
    private String sourceText;

    @Property("createdAt")
    private LocalDateTime createdAt;

    @Property("conceptCount")
    private int conceptCount;

    @Property("status")
    private GraphStatus status;

    public enum GraphStatus {
        PROCESSING, COMPLETED, FAILED
    }
}
