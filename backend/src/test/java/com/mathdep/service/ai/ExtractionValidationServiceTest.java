package com.mathdep.service.ai;

import com.mathdep.dto.MathDto.ConceptDto;
import com.mathdep.dto.MathDto.DependencyDto;
import com.mathdep.dto.MathDto.ExtractionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionValidationServiceTest {

    private final ExtractionValidationService validationService = new ExtractionValidationService();

    @Test
    void validExtractionPasses() {
        ExtractionResult result = validationService.validate(ExtractionResult.builder()
            .concepts(List.of(
                concept("def_continuity", "Continuity", "DEFINITION"),
                concept("thm_ivt", "Intermediate Value Theorem", "THEOREM")
            ))
            .dependencies(List.of(DependencyDto.builder()
                .from("thm_ivt")
                .to("def_continuity")
                .reason("Uses continuity.")
                .build()))
            .summary("Continuity demo.")
            .build());

        assertThat(result.getConcepts()).hasSize(2);
        assertThat(result.getDependencies()).hasSize(1);
        assertThat(result.getValidationWarnings()).isEmpty();
        assertThat(result.getValidationReport().getRemovedDependencies()).isZero();
        assertThat(result.getValidationReport().getRepairedConcepts()).isZero();
    }

    @Test
    void missingDependencyTargetIsRemoved() {
        ExtractionResult result = validationService.validate(ExtractionResult.builder()
            .concepts(List.of(concept("thm_ivt", "Intermediate Value Theorem", "THEOREM")))
            .dependencies(List.of(DependencyDto.builder()
                .from("thm_ivt")
                .to("missing_definition")
                .reason("Model hallucinated this target.")
                .build()))
            .build());

        assertThat(result.getDependencies()).isEmpty();
        assertThat(result.getValidationReport().getRemovedDependencies()).isEqualTo(1);
        assertThat(result.getValidationWarnings()).anyMatch(w -> w.contains("unknown concept"));
    }

    @Test
    void duplicateIdsAreRepairedDeterministically() {
        ExtractionResult result = validationService.validate(ExtractionResult.builder()
            .concepts(List.of(
                concept("def_continuity", "Continuity", "DEFINITION"),
                concept("def_continuity", "Continuity Again", "LEMMA")
            ))
            .dependencies(List.of())
            .build());

        assertThat(result.getConcepts()).extracting(ConceptDto::getId)
            .containsExactly("def_continuity", "def_continuity_2");
        assertThat(result.getValidationReport().getRepairedConcepts()).isEqualTo(1);
    }

    @Test
    void nullDependenciesBecomeEmptyList() {
        ExtractionResult result = validationService.validate(ExtractionResult.builder()
            .concepts(List.of(concept("def_continuity", "Continuity", "DEFINITION")))
            .dependencies(null)
            .build());

        assertThat(result.getDependencies()).isEmpty();
        assertThat(result.getValidationWarnings()).anyMatch(w -> w.contains("Dependency list was missing"));
    }

    private ConceptDto concept(String id, String name, String type) {
        return ConceptDto.builder()
            .id(id)
            .name(name)
            .type(type)
            .statement(name + " statement.")
            .build();
    }
}
