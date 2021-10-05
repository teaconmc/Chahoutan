package org.teacon.chahoutan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.sqlite.hibernate.dialect.SQLiteDialect;
import org.teacon.chahoutan.lucene.SmartCNAnalysisConfigurer;

import java.util.Map;

@SpringBootApplication
public class ChahoutanApplication
{
    public static void main(String[] args)
    {
        var app = new SpringApplication(ChahoutanApplication.class);
        app.setDefaultProperties(Map.ofEntries(
                Map.entry("server.port", 48175),
                Map.entry("spring.datasource.url", "jdbc:sqlite:chahoutan.sqlite3.db"),
                Map.entry("spring.jpa.generate-ddl", true),
                Map.entry("spring.jpa.hibernate.ddl-auto", "update"),
                Map.entry("spring.jpa.properties.hibernate.connection.characterEncoding", "utf-8"),
                Map.entry("spring.jpa.properties.hibernate.connection.useUnicode", true),
                Map.entry("spring.jpa.properties.hibernate.dialect", SQLiteDialect.class.getName()),
                Map.entry("spring.jpa.properties.hibernate.enable_lazy_load_no_trans", true),
                Map.entry("spring.jpa.properties.hibernate.search.backend.analysis.configurer", "class:" + SmartCNAnalysisConfigurer.class.getName()),
                Map.entry("spring.jpa.properties.hibernate.search.backend.lucene_version", "LATEST")));
        app.run(args);
    }
}
