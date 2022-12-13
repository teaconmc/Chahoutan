package org.teacon.chahoutan;

import org.hibernate.dialect.PostgreSQL10Dialect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class ChahoutanApplication
{
    public static void main(String[] args)
    {
        var app = new SpringApplication(ChahoutanApplication.class);
        app.setDefaultProperties(Map.ofEntries(
                Map.entry("debug", true),
                Map.entry("logging.file.name", "chahoutan-spring.log"),
                Map.entry("server.port", 48175),
                Map.entry("spring.datasource.url", ChahoutanConfig.PG_DATASOURCE),
                Map.entry("spring.jpa.generate-ddl", true),
                Map.entry("spring.jpa.hibernate.ddl-auto", "update"),
                Map.entry("spring.jpa.properties.hibernate.connection.characterEncoding", "utf-8"),
                Map.entry("spring.jpa.properties.hibernate.connection.useUnicode", true),
                Map.entry("spring.jpa.properties.hibernate.dialect", PostgreSQL10Dialect.class.getName()),
                Map.entry("spring.jpa.properties.hibernate.enable_lazy_load_no_trans", true)));
        app.run(args);
    }
}
