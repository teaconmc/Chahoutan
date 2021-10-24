package org.teacon.chahoutan.entity;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;
import org.commonmark.renderer.text.TextContentRenderer;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.teacon.chahoutan.ChahoutanConfig;

import javax.persistence.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;

@Entity
@Access(AccessType.FIELD)
@Table(name = "chahoutan_revisions")
public class Revision
{
    private static Parser MD_PARSER = Parser.builder().enabledBlockTypes(Set.of()).build();
    private static TextContentRenderer MD_PLAIN_RENDERER = TextContentRenderer.builder().stripNewlines(true).build();
    private static HtmlRenderer MD_HTML_RENDERER = HtmlRenderer.builder().softbreak(" ").percentEncodeUrls(true).build();

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "post", nullable = false)
    private Post post = new Post();

    @Column(name = "creation_time", nullable = false)
    private Instant creationTime = Instant.EPOCH;

    @Column(name = "text", columnDefinition = "text", nullable = false)
    private String text = "";

    @ManyToMany
    @OrderColumn(name = "image_ordinal")
    @CollectionTable(name = "chahoutan_post_images", joinColumns = @JoinColumn(name = "revision_id", referencedColumnName = "id"))
    private List<Image> image = new ArrayList<>();

    public UUID getId()
    {
        return this.id;
    }

    public Post getPost()
    {
        return this.post;
    }

    public String getText()
    {
        return this.text;
    }

    public List<Image> getImages()
    {
        return List.copyOf(this.image);
    }

    public String getRssPlainText()
    {
        var editors = this.post.getEditors();
        var editorSignText = editors.isEmpty() ? "" : ChahoutanConfig.EDITOR_SIGN_PREFIX +
                String.join(ChahoutanConfig.EDITOR_SIGN_SEPARATOR, editors) + ChahoutanConfig.EDITOR_SIGN_SUFFIX;
        var node = MD_PARSER.parse(this.text + editorSignText);
        return MD_PLAIN_RENDERER.render(node).strip();
    }

    public String getRssHtmlText()
    {
        var editors = this.post.getEditors();
        var editorSignText = editors.isEmpty() ? "" : ChahoutanConfig.EDITOR_SIGN_PREFIX +
                String.join(ChahoutanConfig.EDITOR_SIGN_SEPARATOR, editors) + ChahoutanConfig.EDITOR_SIGN_SUFFIX;
        var node = MD_PARSER.parse(this.text + editorSignText);
        var stringBuilder = new StringBuilder();
        var html = new HtmlWriter(stringBuilder);
        MD_HTML_RENDERER.render(node, stringBuilder);
        if (!this.image.isEmpty())
        {
            html.tag("p");
            var urlPrefix = URI.create(ChahoutanConfig.BACKEND_URL_PREFIX);
            for (Image image : this.image)
            {
                var path = "v1/images/" + image.getId() + ".png";
                var src = urlPrefix.resolve(path).toASCIIString();
                html.tag("img", Map.of("src", src, "alt", image.getId() + ".png"), true);
            }
            html.tag("/p");
        }
        return stringBuilder.toString();
    }

    public void setPost(Post post)
    {
        this.post = post;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setCreationTime(Instant time)
    {
        this.creationTime = time;
    }

    public void setImages(List<Image> image)
    {
        this.image = List.copyOf(image);
    }

    public static class Bridge implements ValueBridge<Revision, String>
    {
        @Override
        public String toIndexedValue(Revision value, ValueBridgeToIndexedValueContext context)
        {
            return value == null ? null : value.getText();
        }
    }
}
