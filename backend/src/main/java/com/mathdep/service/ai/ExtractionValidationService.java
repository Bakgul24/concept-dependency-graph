package com.mathdep.service.ai;

import com.mathdep.dto.MathDto.ConceptDto;
import com.mathdep.dto.MathDto.DependencyDto;
import com.mathdep.dto.MathDto.ExtractionResult;
import com.mathdep.dto.MathDto.ValidationReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ExtractionValidationService {

    private static final Set<String> VALID_TYPES = Set.of(
        "DEFINITION", "LEMMA", "THEOREM", "COROLLARY", "PROPOSITION"
    );

    public ExtractionResult validate(ExtractionResult raw) {
        ExtractionResult result = raw != null ? raw : new ExtractionResult();
        List<String> warnings = new ArrayList<>();
        int[] repairedConcepts = {0};

        List<ConceptDto> inputConcepts = result.getConcepts();
        if (inputConcepts == null) {
            inputConcepts = new ArrayList<>();
            warnings.add("Concept list was missing and was normalized to an empty list.");
        }

        Map<String, ConceptDto> conceptsById = new LinkedHashMap<>();
        for (int i = 0; i < inputConcepts.size(); i++) {
            ConceptDto concept = inputConcepts.get(i);
            if (concept == null) {
                warnings.add("Null concept at index %d was removed.".formatted(i));
                repairedConcepts[0]++;
                continue;
            }

            repairRequiredFields(concept, i, warnings, repairedConcepts);

            String baseId = safeId(concept.getId(), concept.getType(), concept.getName(), i);
            String uniqueId = uniqueId(baseId, conceptsById.keySet());
            if (!uniqueId.equals(concept.getId())) {
                warnings.add("Concept id '%s' was repaired to '%s'.".formatted(concept.getId(), uniqueId));
                concept.setId(uniqueId);
                repairedConcepts[0]++;
            }

            conceptsById.put(concept.getId(), concept);
        }

        List<DependencyDto> inputDependencies = result.getDependencies();
        if (inputDependencies == null) {
            inputDependencies = new ArrayList<>();
            warnings.add("Dependency list was missing and was normalized to an empty list.");
        }

        List<DependencyDto> validDependencies = new ArrayList<>();
        Set<String> seenDependencies = new LinkedHashSet<>();
        int removedDependencies = 0;

        for (int i = 0; i < inputDependencies.size(); i++) {
            DependencyDto dependency = inputDependencies.get(i);
            if (dependency == null) {
                warnings.add("Null dependency at index %d was removed.".formatted(i));
                removedDependencies++;
                continue;
            }

            String from = trimToNull(dependency.getFrom());
            String to = trimToNull(dependency.getTo());
            if (from == null || to == null) {
                warnings.add("Dependency at index %d was removed because from/to was missing.".formatted(i));
                removedDependencies++;
                continue;
            }

            if (!conceptsById.containsKey(from) || !conceptsById.containsKey(to)) {
                warnings.add("Dependency '%s -> %s' was removed because it references an unknown concept.".formatted(from, to));
                removedDependencies++;
                continue;
            }

            if (from.equals(to)) {
                warnings.add("Dependency '%s -> %s' was removed because self-loops are not allowed.".formatted(from, to));
                removedDependencies++;
                continue;
            }

            String dependencyKey = from + "->" + to;
            if (!seenDependencies.add(dependencyKey)) {
                warnings.add("Duplicate dependency '%s' was removed.".formatted(dependencyKey));
                removedDependencies++;
                continue;
            }

            dependency.setFrom(from);
            dependency.setTo(to);
            validDependencies.add(dependency);
        }

        result.setConcepts(new ArrayList<>(conceptsById.values()));
        result.setDependencies(validDependencies);
        result.setValidationWarnings(warnings);
        result.setValidationReport(ValidationReport.builder()
            .warnings(warnings)
            .removedDependencies(removedDependencies)
            .repairedConcepts(repairedConcepts[0])
            .build());

        warnings.forEach(warning -> log.warn("Extraction validation: {}", warning));
        return result;
    }

    private void repairRequiredFields(
            ConceptDto concept,
            int index,
            List<String> warnings,
            int[] repairedConcepts) {

        if (isBlank(concept.getName())) {
            concept.setName("Concept %d".formatted(index + 1));
            warnings.add("Concept at index %d had no name and was assigned '%s'.".formatted(index, concept.getName()));
            repairedConcepts[0]++;
        } else {
            concept.setName(concept.getName().trim());
        }

        String normalizedType = normalizeType(concept.getType());
        if (!normalizedType.equals(concept.getType())) {
            warnings.add("Concept '%s' type '%s' was normalized to '%s'.".formatted(
                concept.getName(), concept.getType(), normalizedType));
            concept.setType(normalizedType);
            repairedConcepts[0]++;
        }

        if (isBlank(concept.getStatement())) {
            concept.setStatement(concept.getName());
            warnings.add("Concept '%s' had no statement and reused its name as a safe statement.".formatted(concept.getName()));
            repairedConcepts[0]++;
        } else {
            concept.setStatement(concept.getStatement().trim());
        }
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "DEFINITION";
        }

        String normalized = type.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return VALID_TYPES.contains(normalized) ? normalized : "DEFINITION";
    }

    private String safeId(String currentId, String type, String name, int index) {
        String seed = trimToNull(currentId);
        if (seed == null) {
            seed = "%s_%s".formatted(typePrefix(type), name);
        }

        String slug = Normalizer.normalize(seed, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");

        if (slug.isBlank()) {
            slug = "concept_%d".formatted(index + 1);
        }

        if (Character.isDigit(slug.charAt(0))) {
            slug = "concept_" + slug;
        }

        return slug;
    }

    private String uniqueId(String baseId, Set<String> existingIds) {
        if (!existingIds.contains(baseId)) {
            return baseId;
        }

        int suffix = 2;
        String candidate = baseId + "_" + suffix;
        while (existingIds.contains(candidate)) {
            suffix++;
            candidate = baseId + "_" + suffix;
        }
        return candidate;
    }

    private String typePrefix(String type) {
        return switch (normalizeType(type)) {
            case "LEMMA" -> "lem";
            case "THEOREM" -> "thm";
            case "COROLLARY" -> "cor";
            case "PROPOSITION" -> "prop";
            default -> "def";
        };
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
