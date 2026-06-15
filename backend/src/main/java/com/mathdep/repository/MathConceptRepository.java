package com.mathdep.repository;

import com.mathdep.model.MathConcept;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MathConceptRepository extends Neo4jRepository<MathConcept, Long> {

    List<MathConcept> findByGraphId(String graphId);

    Optional<MathConcept> findByConceptIdAndGraphId(String conceptId, String graphId);

    @Query("""
        MATCH (c:MathConcept {graphId: $graphId})
        OPTIONAL MATCH (c)-[r:DEPENDS_ON]->(dep:MathConcept)
        RETURN c, collect(r), collect(dep)
        """)
    List<MathConcept> findAllWithPrerequisitesByGraphId(@Param("graphId") String graphId);

    @Query("""
        MATCH (c:MathConcept {graphId: $graphId})
        DETACH DELETE c
        """)
    void deleteAllByGraphId(@Param("graphId") String graphId);

    @Query("""
        MATCH (a:MathConcept {graphId: $graphId})-[:DEPENDS_ON*]->(b:MathConcept {graphId: $graphId})
        WHERE a.conceptId = b.conceptId
        RETURN a.conceptId
        LIMIT 1
        """)
    Optional<String> findCycleConceptId(@Param("graphId") String graphId);
}
