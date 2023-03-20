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
            SELECT p.id FROM chahoutan_posts p
            """;
    String SELECT_BY_QUERY = """
            SELECT post_id FROM
            (
                SELECT si.post_id AS post_id, ts_rank_cd(si.search_index_vector, query) AS rank
                FROM chahoutan_search_indexes si, to_tsquery(cast(:config AS regconfig), :query) query
                WHERE si.post_id <= :until ORDER BY rank DESC, post_id DESC
            )
            AS results WHERE rank > 0
            """;

    @Modifying
    @Query(value = CREATE_SEARCH_INDEX, nativeQuery = true)
    void createSearchIndex();

    @Modifying
    @Query(value = REFRESH_ALL_BY_POST_ID, nativeQuery = true)
    void refreshAllByPostId(String config, Integer id);

    void deleteAllByPostId(Integer id);

    @Query(value = SELECT_ALL_POST_IDS, nativeQuery = true)
    Iterable<Integer> selectPostIds();

    @Query(value = SELECT_BY_QUERY, nativeQuery = true)
    List<Integer> selectByQuery(String config, String query, int until, Pageable pageable);
}
