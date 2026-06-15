package com.mathdep.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j Graph Node representing a mathematical concept.
 * Concepts can be of type DEFINITION, LEMMA, or THEOREM.
 *
 * Graph structure:
 *   (Theorem) -[:DEPENDS_ON]-> (Lemma) -[:DEPENDS_ON]-> (Definition)
 */
@Node("MathConcept")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MathConcept {

    @Id
    @GeneratedValue
    private Long id;

    /** Unique identifier used by the frontend and AI (e.g., "def_continuity") */
    @Property("conceptId")
    private String conceptId;

    /** Human-readable name (e.g., "Continuity of a Function") */
    @Property("name")
    private String name;

    /** DEFINITION | LEMMA | THEOREM | COROLLARY | PROPOSITION */
    @Property("type")
    private ConceptType type;

    /** The mathematical statement, possibly with LaTeX */
    @Property("statement")
    private String statement;

    /** Optional proof text */
    @Property("proof")
    private String proof;

    /** Source document / section reference */
    @Property("source")
    private String source;

    /** The graph ID this concept belongs to */
    @Property("graphId")
    private String graphId;

    /**
     * Outgoing DEPENDS_ON relationships.
     * This concept depends on (requires as prerequisite) these concepts.
     */
    @Relationship(type = "DEPENDS_ON", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<DependencyRelation> dependencies = new ArrayList<>();

    public enum ConceptType {
        DEFINITION, LEMMA, THEOREM, COROLLARY, PROPOSITION
    }
}
