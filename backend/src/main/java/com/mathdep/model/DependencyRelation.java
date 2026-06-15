package com.mathdep.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyRelation {

    @RelationshipId
    private Long id;

    @TargetNode
    private MathConcept target;

    @Property("reason")
    private String reason;

    @Property("confidence")
    private Double confidence;

    @Property("relationType")
    @Builder.Default
    private String relationType = "DEPENDS_ON";
}
