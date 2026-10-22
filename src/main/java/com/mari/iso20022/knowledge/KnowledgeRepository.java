package com.mari.iso20022.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface KnowledgeRepository extends JpaRepository<KnowledgeChunk, UUID> {
    @Query("select c from KnowledgeChunk c join fetch c.document d " +
            "where lower(c.topic) like lower(concat('%', :term, '%')) " +
            "or lower(c.keywords) like lower(concat('%', :term, '%')) " +
            "or lower(c.content) like lower(concat('%', :term, '%')) " +
            "or lower(d.documentCode) like lower(concat('%', :term, '%')) " +
            "or lower(d.title) like lower(concat('%', :term, '%')) " +
            "or lower(d.source) like lower(concat('%', :term, '%')) " +
            "order by d.documentCode, c.chunkCode")
    List<KnowledgeChunk> search(@Param("term") String term);
}
