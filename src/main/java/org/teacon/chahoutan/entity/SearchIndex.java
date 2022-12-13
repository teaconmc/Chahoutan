package org.teacon.chahoutan.entity;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.teacon.chahoutan.repo.SearchIndexRepository;

import javax.annotation.PostConstruct;
import javax.persistence.*;
import java.util.UUID;

@Entity
@Access(AccessType.FIELD)
@Table(name = "chahoutan_search_indexes")
public class SearchIndex
{
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", columnDefinition = "uuid", nullable = false)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post = new Post();

    @Column(name = "search_index_vector", columnDefinition = "tsvector", nullable = false)
    private String searchIndexVector = "";

    public Post getPost()
    {
        return this.post;
    }
    @Component
    public static class Startup
    {
        private final PlatformTransactionManager transactionManager;
        private final SearchIndexRepository searchIndexRepo;

        public Startup(PlatformTransactionManager transactionManager,
                       SearchIndexRepository searchIndexRepo)
        {
            this.transactionManager = transactionManager;
            this.searchIndexRepo = searchIndexRepo;
        }

        @PostConstruct
        public void createIndex()
        {
            var template = new TransactionTemplate(this.transactionManager);
            template.executeWithoutResult(status -> this.searchIndexRepo.createSearchIndex());
        }
    }
}
