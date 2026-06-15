package com.mathdep.service;

import com.mathdep.dto.MathDto.DependencyDto;
import com.mathdep.model.DependencyRelation;
import com.mathdep.model.MathConcept;
import com.mathdep.repository.MathConceptRepository;
import com.mathdep.service.ai.MathTextAnalysisService;
import com.mathdep.service.graph.CycleDetectionService;
import com.mathdep.service.graph.GraphLayoutService;
import com.mathdep.repository.ConceptGraphRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConceptGraphServiceTest {

    @Test
    void dependencyReasonPersistsOnRelationshipProperties() throws Exception {
        MathConceptRepository conceptRepository = mock(MathConceptRepository.class);
        ConceptGraphService service = new ConceptGraphService(
            mock(MathTextAnalysisService.class),
            new CycleDetectionService(),
            new GraphLayoutService(),
            conceptRepository,
            mock(ConceptGraphRepository.class));

        MathConcept theorem = concept("thm_ivt", "Intermediate Value Theorem", MathConcept.ConceptType.THEOREM);
        MathConcept definition = concept("def_continuity", "Continuity", MathConcept.ConceptType.DEFINITION);

        Method method = ConceptGraphService.class.getDeclaredMethod(
            "wireDependencies",
            List.class,
            Map.class);
        method.setAccessible(true);
        method.invoke(service,
            List.of(DependencyDto.builder()
                .from("thm_ivt")
                .to("def_continuity")
                .reason("The theorem assumes continuity.")
                .build()),
            Map.of(
                "thm_ivt", theorem,
                "def_continuity", definition));

        assertThat(theorem.getDependencies()).hasSize(1);
        DependencyRelation relation = theorem.getDependencies().get(0);
        assertThat(relation.getTarget()).isSameAs(definition);
        assertThat(relation.getReason()).isEqualTo("The theorem assumes continuity.");
        assertThat(relation.getRelationType()).isEqualTo("DEPENDS_ON");
        verify(conceptRepository).save(theorem);
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
