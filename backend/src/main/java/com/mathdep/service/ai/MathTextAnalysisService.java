package com.mathdep.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathdep.dto.MathDto.ConceptDto;
import com.mathdep.dto.MathDto.DependencyDto;
import com.mathdep.dto.MathDto.ExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MathTextAnalysisService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final ExtractionValidationService validationService;
    private final Environment environment;

    private static final String SYSTEM_PROMPT = """
        You are an expert mathematician and knowledge graph engineer.
                                                                      
          Your task is to analyze mathematical texts and extract structured information as a knowledge graph.
          
          ---
          
          ## TASK
          
          Extract ALL mathematical concepts from the given text. For each concept, identify:
          
          1. TYPE: DEFINITION, LEMMA, THEOREM, COROLLARY, or PROPOSITION \s
          2. NAME: formal name of the concept \s
          3. STATEMENT: full mathematical statement (preserve LaTeX if present) \s
          4. PROOF: proof if present, otherwise null \s
          5. DEPENDENCIES: which other concepts (by ID) it depends on directly \s
          
          ---
          
          ## DEPENDENCY RULES (VERY IMPORTANT)
          
          You MUST build a COMPLETE dependency graph.
          
          ### 1. Core rules
          - A DEFINITION may depend on other DEFINITIONS it references
          - A LEMMA depends on DEFINITIONS and LEMMAS it uses
          - A THEOREM depends on DEFINITIONS and LEMMAS used in its statement OR proof
          - A COROLLARY depends on the THEOREM it follows from
          
          ---
          
          ### 2. CRITICAL OVERRIDE RULE (HIGH PRIORITY)
          
          Do NOT require explicit wording only.
          
          If a dependency is:
          - standard in mathematics
          - logically required for the proof
          - commonly used structure (e.g. IVT uses sign change lemma style argument)
          - implied by theorem-lemma structure
          
          👉 THEN you MUST include it as a dependency.
          
          Treat these as EXPLICIT dependencies even if not directly stated.
          
          ---
          
          ### 3. COMPLETENESS RULE
          
          - Prefer COMPLETE dependency coverage over minimal graphs
          - If a concept is used in reasoning, it MUST be included
          - Missing dependencies are worse than extra dependencies
          
          ---
          
          ### 4. TRANSITIVE AWARENESS (IMPORTANT)
          
          Do NOT over-avoid indirect links.
          
          If:
          A lemma is used to prove a theorem \s
          AND that lemma depends on definitions \s
          
          👉 You should still include BOTH:
          - theorem → lemma
          - theorem → definitions (if logically required)
          
          ---
          
          ## ID GENERATION RULES
          
          Generate IDs using:
          
          {type_prefix}_{short_name}
          
          Rules:
          - lowercase snake_case only
          - deterministic
          - remove generic suffix words:
            "Graph", "Theorem", "Lemma", "Proposition", "Corollary"
          
          Examples:
          - "Intermediate Value Theorem" → thm_intermediate_value
          - "Bridge Characterization" → lem_bridge_characterization
          - "2-Edge-Connected Graph" → def_2_edge_connected
          - "Bolzano-Weierstrass Theorem" → lem_bolzano_weierstrass
          
          ---
          
          ## OUTPUT FORMAT
          
          Return ONLY valid JSON:
          
          {
            "concepts": [
              {
                "id": "unique_snake_case_id",
                "name": "Human Readable Name",
                "type": "DEFINITION|LEMMA|THEOREM|COROLLARY|PROPOSITION",
                "statement": "Mathematical statement",
                "proof": "Proof or null"
              }
            ],
            "dependencies": [
              {
                "from": "concept_that_depends",
                "to": "concept_that_is_prerequisite",
                "reason": "short explanation"
              }
            ],
            "summary": "one sentence summary"
          }
          
          ---
          
          ## CRITICAL OUTPUT RULES
          
          - Output ONLY JSON
          - No markdown, no explanation, no backticks
          - Extract ALL concepts
          - Build a DENSE and COMPLETE dependency graph
          - Do NOT miss lemma → theorem relationships
          - Prefer correctness of structure over strict sparsity
    """;


    private static final String USER_PROMPT_TEMPLATE = """
        Analyze the following mathematical text and extract all concepts with their dependencies.

        === MATHEMATICAL TEXT ===
        %s
        === END OF TEXT ===

        Remember: Output ONLY valid JSON, nothing else.
        """;

    private static final String REPAIR_PROMPT_TEMPLATE = """
        The previous response was not valid JSON for the required extraction schema.
        Return ONLY one valid JSON object with fields concepts, dependencies, and summary.
        Do not include markdown, comments, explanations, or backticks.

        Previous invalid response:
        %s
        """;

    public ExtractionResult extractConcepts(String mathematicalText) {
        String text = mathematicalText != null ? mathematicalText : "";
        log.info("Starting AI extraction for text of length: {} chars", text.length());

        if (isMockAiEnabled()) {
            log.info("MOCK_AI=true. Returning deterministic demo extraction without calling OpenAI.");
            return validationService.validate(mockExtraction());
        }

        ChatClient chatClient = chatClientBuilder.build();
        String rawResponse;

        try {
            rawResponse = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(USER_PROMPT_TEMPLATE.formatted(text))
                .call()
                .content();

            log.debug("Raw AI response:\n{}", rawResponse);
        } catch (Exception e) {
            log.error("AI extraction call failed", e);
            throw new AiExtractionException("The AI extraction service could not be reached.", e);
        }

        try {
            return parseExtractionResult(rawResponse);
        } catch (JsonProcessingException firstParseError) {
            log.warn("Initial AI response was invalid JSON. Attempting one repair request.");
            return repairAndParse(chatClient, rawResponse, firstParseError);
        }
    }

    private ExtractionResult repairAndParse(
            ChatClient chatClient,
            String invalidResponse,
            JsonProcessingException firstParseError) {

        String repairedResponse;
        try {
            repairedResponse = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(REPAIR_PROMPT_TEMPLATE.formatted(invalidResponse))
                .call()
                .content();
            log.debug("Raw AI repair response:\n{}", repairedResponse);
        } catch (Exception e) {
            log.error("AI JSON repair call failed", e);
            throw new AiExtractionException("The AI response was invalid JSON and the repair request failed.", e);
        }

        try {
            return parseExtractionResult(repairedResponse);
        } catch (JsonProcessingException repairParseError) {
            log.error("AI repair response was still invalid JSON", repairParseError);
            throw new AiExtractionException(
                "The AI returned invalid JSON after one repair attempt. Please retry or enable MOCK_AI=true for demo mode.",
                firstParseError);
        }
    }

    ExtractionResult parseExtractionResult(String rawJson) throws JsonProcessingException {
        String cleaned = stripMarkdownFences(rawJson);
        try {
            ExtractionResult result = objectMapper.readValue(cleaned, ExtractionResult.class);
            ExtractionResult validated = validationService.validate(result);
            log.info("Successfully extracted {} concepts and {} dependencies with {} validation warnings",
                validated.getConcepts().size(),
                validated.getDependencies().size(),
                validated.getValidationWarnings().size());
            return validated;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse AI response as JSON: {}", cleaned);
            throw e;
        }
    }

    private String stripMarkdownFences(String rawJson) {
        if (rawJson == null) {
            return "";
        }

        String cleaned = rawJson.trim()
            .replaceAll("(?is)^```(?:json)?\\s*", "")
            .replaceAll("(?is)\\s*```$", "")
            .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1).trim();
        }

        return cleaned;
    }

    private boolean isMockAiEnabled() {
        return Boolean.parseBoolean(environment.getProperty(
            "MOCK_AI",
            environment.getProperty("mock.ai", "false")));
    }

    private ExtractionResult mockExtraction() {
        return ExtractionResult.builder()
            .summary("Continuity and the Intermediate Value Theorem.")
            .concepts(List.of(
                ConceptDto.builder()
                    .id("def_continuity")
                    .name("Continuity")
                    .type("DEFINITION")
                    .statement("A function f is continuous at c if for every epsilon > 0 there exists delta > 0 such that |x - c| < delta implies |f(x) - f(c)| < epsilon.")
                    .proof(null)
                    .build(),
                ConceptDto.builder()
                    .id("def_interval")
                    .name("Interval")
                    .type("DEFINITION")
                    .statement("An interval is a connected subset of the real line.")
                    .proof(null)
                    .build(),
                ConceptDto.builder()
                    .id("thm_intermediate_value")
                    .name("Intermediate Value Theorem")
                    .type("THEOREM")
                    .statement("If f is continuous on [a,b] and N lies between f(a) and f(b), then there exists c in [a,b] such that f(c) = N.")
                    .proof(null)
                    .build()
            ))
            .dependencies(List.of(
                DependencyDto.builder()
                    .from("thm_intermediate_value")
                    .to("def_continuity")
                    .reason("The theorem assumes continuity on a closed interval.")
                    .build(),
                DependencyDto.builder()
                    .from("thm_intermediate_value")
                    .to("def_interval")
                    .reason("The theorem is stated over the interval [a,b].")
                    .build()
            ))
            .build();
    }
}
