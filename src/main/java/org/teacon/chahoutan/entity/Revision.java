package org.teacon.chahoutan.entity;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.teacon.chahoutan.ChahoutanConfig;

import javax.persistence.*;
import java.net.URI;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;

@Entity
@Access(AccessType.FIELD)
@Table(name = "chahoutan_revisions")
public class Revision
{
    private static final String HTTP_SCHEME = "http:";
    private static final String HTTPS_SCHEME = "https:";

    private static final Map<String, String> ANCHOR_PREFIXES = Map.of(
            HTTP_SCHEME, HTTP_SCHEME, HTTPS_SCHEME, HTTPS_SCHEME,
            ChahoutanConfig.ANCHOR_PREFIX, ChahoutanConfig.FRONTEND_URL_PREFIX,
            ChahoutanConfig.ANCHOR_FORGE_PREFIX, ChahoutanConfig.ANCHOR_FORGE_URL_PREFIX,
            ChahoutanConfig.ANCHOR_REDDIT_PREFIX, ChahoutanConfig.ANCHOR_REDDIT_URL_PREFIX);

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

    @ElementCollection
    @MapKeyColumn(name = "anchor", columnDefinition = "text")
    @Column(name = "link", columnDefinition = "text", nullable = false)
    @CollectionTable(name = "chahoutan_post_anchors", joinColumns = @JoinColumn(name = "revision_id", referencedColumnName = "id"))
    private Map<String, String> anchors = new HashMap<>();

    private static void escape(String rawInput, int start, int end, StringBuilder htmlBuilder)
    {
        for (var i = start; i < end; ++i)
        {
            var c = rawInput.charAt(i);
            switch (c)
            {
                case '"' -> htmlBuilder.append("&quot;");
                case '&' -> htmlBuilder.append("&amp;");
                case '<' -> htmlBuilder.append("&lt;");
                case '>' -> htmlBuilder.append("&gt;");
                default -> htmlBuilder.append(c);
            }
        }
    }

    public UUID getId()
    {
        return this.id;
    }

    public Post getPost()
    {
        return this.post;
    }

    public String getTitle()
    {
        var time = this.post.getPublishTime().toLocalDate();
        return MessageFormat.format(ChahoutanConfig.NAME_PATTERN, this.post.getId(), time);
    }

    public String getText()
    {
        return this.text;
    }

    public List<String> getAnchors()
    {
        return this.anchors.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getKey).toList();
    }

    public List<String> getAnchorUrls()
    {
        return this.anchors.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
    }

    public List<Image> getImages()
    {
        return List.copyOf(this.image);
    }

    public String getRssPlainText()
    {
        var editors = this.post.getEditors();
        if (!editors.isEmpty())
        {
            var textBuilder = new StringBuilder(this.text);
            var prefix = ChahoutanConfig.EDITOR_SIGN_PREFIX;
            for (var editor : editors)
            {
                textBuilder.append(prefix).append(editor);
                prefix = ChahoutanConfig.EDITOR_SIGN_SEPARATOR;
            }
            return textBuilder.append(ChahoutanConfig.EDITOR_SIGN_SUFFIX).toString();
        }
        return this.text;
    }

    public String getRssHtmlText()
    {
        var index = 0;
        var plainText = this.getRssPlainText();
        var htmlBuilder = new StringBuilder().append("<p>");
        while (index < plainText.length())
        {
            var anchorChosen = "";
            var anchorIndexChosen = plainText.length();
            for (var anchor : this.anchors.keySet())
            {
                var anchorIndex = plainText.indexOf(anchor, index);
                if (anchorIndex >= index && anchorIndex <= anchorIndexChosen)
                {
                    if (anchorIndex < anchorIndexChosen || anchor.length() > anchorChosen.length())
                    {
                        anchorChosen = anchor;
                        anchorIndexChosen = anchorIndex;
                    }
                }
            }
            if (anchorChosen.isEmpty())
            {
                escape(plainText, index, index = plainText.length(), htmlBuilder);
            }
            else
            {
                var link = this.anchors.get(anchorChosen);
                escape(plainText, index, anchorIndexChosen, htmlBuilder);
                htmlBuilder.append("<a href=\"");
                escape(link, 0, link.length(), htmlBuilder);
                htmlBuilder.append("\">");
                escape(plainText, anchorIndexChosen, index = anchorIndexChosen + anchorChosen.length(), htmlBuilder);
                htmlBuilder.append("</a>");
            }
        }
        if (!this.image.isEmpty())
        {
            htmlBuilder.append("</p><p>");
            var urlPrefix = URI.create(ChahoutanConfig.BACKEND_URL_PREFIX);
            for (var image : this.image)
            {
                var file = image.getId() + ".png";
                var src = urlPrefix.resolve("v1/images/" + file).toASCIIString();
                htmlBuilder.append("<img src=\"");
                escape(src, 0, src.length(), htmlBuilder);
                htmlBuilder.append("\" alt=\"");
                escape(file, 0, file.length(), htmlBuilder);
                htmlBuilder.append("\">");
            }
        }
        return htmlBuilder.append("</p>").toString();
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

    public void setAnchors(List<String> anchors)
    {
        var entries = new HashMap<String, String>();
        for (var anchor : anchors)
        {
            for (var entry : ANCHOR_PREFIXES.entrySet())
            {
                var prefix = entry.getKey();
                if (anchor.startsWith(prefix))
                {
                    var link = entry.getValue() + anchor.substring(prefix.length());
                    entries.put(anchor, link);
                    break;
                }
            }
        }
        this.anchors = Map.copyOf(entries);
    }

    public void setImages(List<Image> image)
    {
        this.image = List.copyOf(image);
    }

    public static class TextBridge implements ValueBridge<Revision, String>
    {
        @Override
        public String toIndexedValue(Revision value, ValueBridgeToIndexedValueContext context)
        {
            return value == null ? null : value.getText();
        }
    }

    public static class TitleBridge implements ValueBridge<Revision, String>
    {
        @Override
        public String toIndexedValue(Revision value, ValueBridgeToIndexedValueContext context)
        {
            return value == null ? null : value.getTitle();
        }
    }
}
