package com.mathdep.repository;

import com.mathdep.model.ConceptGraph;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConceptGraphRepository extends Neo4jRepository<ConceptGraph, Long> {

    Optional<ConceptGraph> findByGraphId(String graphId);

    void deleteByGraphId(String graphId);
}
