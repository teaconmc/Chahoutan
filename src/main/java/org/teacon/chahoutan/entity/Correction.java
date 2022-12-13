package org.teacon.chahoutan.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Access(AccessType.FIELD)
@Table(name = "chahoutan_corrections")
public class Correction
{
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", columnDefinition = "uuid", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "revision_id", nullable = false)
    private Revision revision = new Revision();

    @Column(name = "upload_date", columnDefinition = "date", nullable = false)
    private LocalDate uploadDate = LocalDate.EPOCH;

    @Column(name = "text_content", columnDefinition = "text", nullable = false)
    private String text = "";
}
