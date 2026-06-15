package com.mathdep.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathdep.dto.MathDto.ExtractionResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class MathTextAnalysisServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExtractionValidationService validationService = new ExtractionValidationService();

    @Test
    void parsesFencedJsonAndValidatesIt() throws Exception {
        MathTextAnalysisService service = new MathTextAnalysisService(
            null,
            objectMapper,
            validationService,
            new MockEnvironment());

        ExtractionResult result = service.parseExtractionResult("""
            ```json
            {
              "concepts": [
                {
                  "id": "def_continuity",
                  "name": "Continuity",
                  "type": "DEFINITION",
                  "statement": "A function is continuous when limits agree."
                }
              ],
              "dependencies": null,
              "summary": "Continuity."
            }
            ```
            """);

        assertThat(result.getConcepts()).hasSize(1);
        assertThat(result.getDependencies()).isEmpty();
        assertThat(result.getConcepts().get(0).getType()).isEqualTo("DEFINITION");
    }

    @Test
    void mockAiModeReturnsStableResult() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("MOCK_AI", "true");
        MathTextAnalysisService service = new MathTextAnalysisService(
            null,
            objectMapper,
            validationService,
            environment);

        ExtractionResult result = service.extractConcepts("Any demo text.");

        assertThat(result.getConcepts()).extracting("id")
            .containsExactly("def_continuity", "def_interval", "thm_intermediate_value");
        assertThat(result.getDependencies()).hasSize(2);
        assertThat(result.getValidationWarnings()).isEmpty();
    }
}
