package org.teacon.chahoutan.entity;

import org.teacon.chahoutan.ChahoutanConfig;

import javax.persistence.*;
import java.net.URI;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
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
    @Column(name = "id", columnDefinition = "uuid", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post = new Post();

    @Column(name = "creation_time", columnDefinition = "timestamptz", nullable = false)
    private ZonedDateTime creationTime = Instant.EPOCH.atZone(ChahoutanConfig.POST_ZONE_ID);

    @Column(name = "text_content", columnDefinition = "text", nullable = false)
    private String text = "";

    @ManyToMany
    @OrderColumn(name = "image_ordinal", columnDefinition = "int")
    @JoinTable(name = "chahoutan_post_images", joinColumns = @JoinColumn(name = "revision_id"), inverseJoinColumns = @JoinColumn(name = "image_id"))
    private List<Image> images = new ArrayList<>();

    @OneToMany(mappedBy = "revision")
    @OrderBy("uploadDate ASC")
    private List<Correction> corrections = new ArrayList<>();

    @ElementCollection
    @OrderColumn(name = "footnote_ordinal", columnDefinition = "int")
    @Column(name = "footnote", columnDefinition = "text", nullable = false)
    @CollectionTable(name = "chahoutan_post_footnotes", joinColumns = @JoinColumn(name = "revision_id"))
    private List<String> footnotes = new ArrayList<>();

    @ElementCollection
    @MapKeyColumn(name = "anchor", columnDefinition = "text")
    @Column(name = "link", columnDefinition = "text", nullable = false)
    @CollectionTable(name = "chahoutan_post_anchors", joinColumns = @JoinColumn(name = "revision_id"))
    private Map<String, String> anchors = new HashMap<>();

    private static void escape(String rawInput, int start, int end, StringBuilder htmlBuilder)
    {
        for (var i = start; i < end; ++i)
        {
            var c = rawInput.charAt(i);
            switch (c)
            {
                case '\u00a0' -> htmlBuilder.append("\u0020");
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

    public URI getBackendUrl() {
        var urlPrefix = URI.create(ChahoutanConfig.BACKEND_URL_PREFIX);
        return urlPrefix.resolve("v1/posts/" + this.id);
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
        return List.copyOf(this.images);
    }

    public Map<String, String> getFootnotes()
    {
        var footnoteCount = this.footnotes.size();
        var map = new LinkedHashMap<String, String>(footnoteCount);
        for (int i = 0; i < footnoteCount; ++i) {
            map.put(String.format("[%d]", i + 1), this.footnotes.get(i));
        }
        return Map.copyOf(map);
    }

    public Map<String, String> getFootnoteUrls()
    {
        var footnoteCount = this.footnotes.size();
        var map = new LinkedHashMap<String, String>(footnoteCount);
        var urlPrefix = URI.create(ChahoutanConfig.FRONTEND_URL_PREFIX);
        for (int i = 0; i < footnoteCount; ++i) {
            var url = urlPrefix.resolve(this.post.getId() + "#footnote-" + i);
            map.put(String.format("[%d]", i + 1), url.toASCIIString());
        }
        return Map.copyOf(map);
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
        var anchorNodeStyle = "word-break: break-all;";
        var textNodeStyle = "overflow-wrap: break-word; text-align: justify; white-space: pre-wrap;";
        var htmlBuilder = new StringBuilder().append("<p style=\"").append(textNodeStyle).append("\">");
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
                htmlBuilder.append("<a style=\"").append(anchorNodeStyle).append("\" href=\"");
                escape(link, 0, link.length(), htmlBuilder);
                htmlBuilder.append("\">");
                escape(plainText, anchorIndexChosen, index = anchorIndexChosen + anchorChosen.length(), htmlBuilder);
                htmlBuilder.append("</a>");
            }
        }
        if (!this.images.isEmpty())
        {
            htmlBuilder.append("</p><p>");
            var imageNodeStyle = "width: 100%;";
            var urlPrefix = URI.create(ChahoutanConfig.BACKEND_URL_PREFIX);
            for (var image : this.images)
            {
                var file = image.getId() + ".png";
                var src = urlPrefix.resolve("v1/images/" + file).toASCIIString();
                htmlBuilder.append("<img style=\"").append(imageNodeStyle).append("\" src=\"");
                escape(src, 0, src.length(), htmlBuilder);
                htmlBuilder.append("\" alt=\"");
                escape(file, 0, file.length(), htmlBuilder);
                var width = image.getWidth();
                var height = image.getHeight();
                if (width * height > 0)
                {
                    htmlBuilder.append("\" width=\"").append(width).append("\" height=\"").append(height);
                }
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
        this.creationTime = time.atZone(ChahoutanConfig.POST_ZONE_ID);
    }

    public void setFootnotes(Map<String, String> footnotes) {
        var array = new String[footnotes.size()];
        for (var i = 0; i < array.length; ++i) {
            array[i] = Objects.requireNonNull(footnotes.get(String.format("[%d]", i + 1)));
        }
        this.footnotes = List.of(array);
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

    public void setImages(List<Image> images)
    {
        this.images = List.copyOf(images);
    }
}
