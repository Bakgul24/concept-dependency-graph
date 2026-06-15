package com.mathdep.service.graph;

import com.mathdep.dto.MathDto.GraphResponse;
import com.mathdep.model.DependencyRelation;
import com.mathdep.model.MathConcept;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GraphLayoutServiceTest {

    private final GraphLayoutService service = new GraphLayoutService();

    @Test
    void graphResponseEdgeContainsDependencyReason() {
        MathConcept definition = concept("def_continuity", "Continuity", MathConcept.ConceptType.DEFINITION);
        MathConcept theorem = concept("thm_ivt", "Intermediate Value Theorem", MathConcept.ConceptType.THEOREM);
        theorem.getDependencies().add(DependencyRelation.builder()
            .target(definition)
            .reason("The theorem assumes continuity on the interval.")
            .relationType("DEPENDS_ON")
            .build());

        GraphResponse response = service.buildReactFlowGraph(
            "graph-1",
            "Demo",
            List.of(definition, theorem),
            false,
            List.of());

        assertThat(response.getEdges()).hasSize(1);
        assertThat(response.getEdges().get(0).getLabel()).isEqualTo("depends on");
        assertThat(response.getEdges().get(0).getReason())
            .isEqualTo("The theorem assumes continuity on the interval.");
        assertThat(response.getEdges().get(0).getRelationType()).isEqualTo("DEPENDS_ON");
        assertThat(response.getEdges().get(0).getData().getReason())
            .isEqualTo("The theorem assumes continuity on the interval.");
        assertThat(response.getNodes().stream()
            .filter(node -> node.getId().equals("thm_ivt"))
            .findFirst()
            .orElseThrow()
            .getData()
            .getDependsOn()).hasSize(1);
    }

    private MathConcept concept(String id, String name, MathConcept.ConceptType type) {
        return MathConcept.builder()
            .conceptId(id)
            .name(name)
            .type(type)
            .statement(name + " statement.")
            .graphId("graph-1")
            .dependencies(new ArrayList<>())
            .build();
    }
}
