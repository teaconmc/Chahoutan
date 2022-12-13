package org.teacon.chahoutan.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.teacon.chahoutan.entity.SearchIndex;

import java.util.List;
import java.util.UUID;

public interface SearchIndexRepository extends CrudRepository<SearchIndex, UUID>
{
    String CREATE_SEARCH_INDEX = """
            CREATE INDEX IF NOT EXISTS chahoutan_search_index_vector_idx
            ON chahoutan_search_indexes USING GIN (search_index_vector)
            """;
    String REFRESH_ALL_BY_POST_ID = """
            INSERT INTO chahoutan_search_indexes (id, post_id, search_index_vector)
            SELECT gen_random_uuid(), pairs.id, to_tsvector(cast(:config AS regconfig), pairs.text_content) FROM
            (
                SELECT p.id, cast(p.id AS text) AS text_content
                FROM chahoutan_posts p WHERE p.id = :id
                UNION ALL SELECT p.id, r.text_content
                FROM chahoutan_posts p JOIN chahoutan_revisions r ON p.revision_id = r.id AND p.id = :id
                UNION ALL SELECT p.id, c.text_content
                FROM chahoutan_posts p JOIN chahoutan_corrections c ON p.revision_id = c.revision_id AND p.id = :id
            )
            AS pairs
            """;
    String SELECT_ALL_POST_IDS = """
            SELECT p.id FROM Post p
            """;
    String SELECT_BY_QUERY = """
            SELECT si.post.id FROM SearchIndex si WHERE si.post.id <= :until
            AND ts_match_vq(si.searchIndexVector, plainto_tsquery(:query)) = TRUE GROUP BY si.post.id
            ORDER BY max(ts_rank(si.searchIndexVector, plainto_tsquery(:query))) DESC, si.post.id DESC
            """;

    @Modifying
    @Query(value = CREATE_SEARCH_INDEX, nativeQuery = true)
    void createSearchIndex();

    @Modifying
    @Query(value = REFRESH_ALL_BY_POST_ID, nativeQuery = true)
    void refreshAllByPostId(String config, Integer id);

    void deleteAllByPostId(Integer id);

    @Query(value = SELECT_ALL_POST_IDS)
    Iterable<Integer> selectPostIds();

    @Query(value = SELECT_BY_QUERY)
    List<Integer> selectByQuery(String query, int until, Pageable pageable);
}
