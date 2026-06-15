package com.mathdep.service.graph;

import com.mathdep.dto.MathDto.DependencyDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class CycleDetectionService {

    private enum Color { WHITE, GRAY, BLACK }

    /**
     * Result of cycle detection, including the cycle path if found.
     */
    public record CycleDetectionResult(
        boolean hasCycle,
        List<String> cyclePath
    ) {
        public static CycleDetectionResult noCycle() {
            return new CycleDetectionResult(false, Collections.emptyList());
        }

        public static CycleDetectionResult withCycle(List<String> path) {
            return new CycleDetectionResult(true, path);
        }
    }

    /**
     * Detects cycles in the dependency graph represented as edge list.
     *
     * @param conceptIds   all concept IDs (vertices)
     * @param dependencies all dependency edges (from → to means "from" depends on "to")
     * @return CycleDetectionResult
     */
    public CycleDetectionResult detect(
            List<String> conceptIds,
            List<DependencyDto> dependencies) {

        // Build adjacency list: from → list of tos
        Map<String, List<String>> adjacency = buildAdjacencyList(conceptIds, dependencies);

        Map<String, Color> color = new HashMap<>();
        for (String id : conceptIds) {
            color.put(id, Color.WHITE);
        }

        // DFS from each unvisited node
        for (String id : conceptIds) {
            if (color.get(id) == Color.WHITE) {
                List<String> path = new ArrayList<>();
                if (dfs(id, adjacency, color, path)) {
                    log.warn("Cycle detected in dependency graph! Path: {}", path);
                    return CycleDetectionResult.withCycle(path);
                }
            }
        }

        log.info("No cycles detected in the dependency graph.");
        return CycleDetectionResult.noCycle();
    }

    /**
     * Recursive DFS with backtracking.
     *
     * @return true if a cycle is found
     */
    private boolean dfs(
            String node,
            Map<String, List<String>> adjacency,
            Map<String, Color> color,
            List<String> path) {

        color.put(node, Color.GRAY);
        path.add(node);

        List<String> neighbors = adjacency.getOrDefault(node, Collections.emptyList());
        for (String neighbor : neighbors) {
            Color neighborColor = color.getOrDefault(neighbor, Color.WHITE);

            if (neighborColor == Color.GRAY) {
                // Found a back-edge → cycle detected
                path.add(neighbor); // Show where cycle closes
                return true;
            }

            if (neighborColor == Color.WHITE) {
                if (dfs(neighbor, adjacency, color, path)) {
                    return true;
                }
            }
        }

        // Node fully processed — backtrack
        color.put(node, Color.BLACK);
        path.remove(path.size() - 1);
        return false;
    }

    /**
     * Builds an adjacency list from dependency edges.
     * Edge direction: from → to (i.e., "from" requires "to")
     */
    private Map<String, List<String>> buildAdjacencyList(
            List<String> conceptIds,
            List<DependencyDto> dependencies) {

        Map<String, List<String>> adjacency = new HashMap<>();
        for (String id : conceptIds) {
            adjacency.put(id, new ArrayList<>());
        }

        for (DependencyDto dep : dependencies) {
            adjacency.computeIfAbsent(dep.getFrom(), k -> new ArrayList<>())
                     .add(dep.getTo());
        }

        return adjacency;
    }
}
