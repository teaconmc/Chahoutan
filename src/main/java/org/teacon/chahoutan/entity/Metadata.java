package org.teacon.chahoutan.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "chahoutan_metadata")
public class Metadata
{
    @Id
    @Column(name = "id", nullable = false, length = 64)
    public String id;

    @Column(name = "text", columnDefinition = "text", nullable = false)
    public String text = "";

    public static Metadata from(String id, String text)
    {
        var result = new Metadata();
        result.text = text;
        result.id = id;
        return result;
    }
}
