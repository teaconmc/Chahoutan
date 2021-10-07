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
@Table(name = "chahoutan_revisions")
public class Revision
{
    private static Parser MD_PARSER = Parser.builder().enabledBlockTypes(Set.of()).build();
    private static TextContentRenderer MD_PLAIN_RENDERER = TextContentRenderer.builder().stripNewlines(true).build();
    private static HtmlRenderer MD_HTML_RENDERER = HtmlRenderer.builder().softbreak(" ").percentEncodeUrls(true).build();

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "post", nullable = false)
    public Post post = new Post();

    @Column(name = "creation_time", nullable = false)
    public Instant creationTime = Instant.EPOCH;

    @Column(name = "text", columnDefinition = "text", nullable = false)
    public String text = "";

    @ManyToMany
    @OrderColumn(name = "image_ordinal")
    @CollectionTable(name = "chahoutan_post_images", joinColumns = @JoinColumn(name = "revision_id", referencedColumnName = "id"))
    public List<Image> image = new ArrayList<>();

    public static Revision from(Post post, String text)
    {
        var revision = new Revision();
        revision.post = post;
        revision.creationTime = Instant.now();
        revision.text = text;
        return revision;
    }

    public static String toRssHtmlText(Revision revision)
    {
        var editors = revision.post.editor;
        var editorSignText = editors.isEmpty() ? "" : ChahoutanConfig.EDITOR_SIGN_PREFIX +
                String.join(ChahoutanConfig.EDITOR_SIGN_SEPARATOR, editors) + ChahoutanConfig.EDITOR_SIGN_SUFFIX;
        var node = MD_PARSER.parse(revision.text + editorSignText);
        var stringBuilder = new StringBuilder();
        var html = new HtmlWriter(stringBuilder);
        html.tag("div");
        MD_HTML_RENDERER.render(node, stringBuilder);
        if (!revision.image.isEmpty())
        {
            html.tag("p");
            var urlPrefix = URI.create(ChahoutanConfig.BACKEND_URL_PREFIX);
            for (Image image : revision.image)
            {
                var path = "v1/images/" + image.id + ".png";
                html.tag("img", Map.of("src", urlPrefix.resolve(path).toASCIIString()), true);
            }
            html.tag("/p");
        }
        html.tag("/div");
        return stringBuilder.toString();
    }

    public static String toRssPlainText(Revision revision)
    {
        var editors = revision.post.editor;
        var editorSignText = editors.isEmpty() ? "" : ChahoutanConfig.EDITOR_SIGN_PREFIX +
                String.join(ChahoutanConfig.EDITOR_SIGN_SEPARATOR, editors) + ChahoutanConfig.EDITOR_SIGN_SUFFIX;
        var node = MD_PARSER.parse(revision.text + editorSignText);
        return MD_PLAIN_RENDERER.render(node).strip();
    }

    public static class Bridge implements ValueBridge<Revision, String>
    {
        @Override
        public String toIndexedValue(Revision value, ValueBridgeToIndexedValueContext context)
        {
            return value == null ? null : value.text;
        }
    }
}
