package org.teacon.chahoutan.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.teacon.chahoutan.ChahoutanConfig;
import org.teacon.chahoutan.repo.ImageRepository;

import javax.persistence.*;
import javax.ws.rs.BadRequestException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "chahoutan_posts")
@Indexed(index = "chahoutan-indexes")
public class Post
{
    private static final Duration POST_DELAY = Duration.ofMillis(302_400_000L);

    private static final ZoneOffset ZONE_OFFSET = ZoneOffset.ofHours(+8);

    private static final long POST_EPOCH = 1_611_920_400_000L;

    private static final long POST_INTERVAL = 201_600_000L;

    @Id
    @DocumentId
    @GenericField(name = "id")
    @Column(name = "id", nullable = false)
    public Integer id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "revision_id")
    @FullTextField(name = "text", analyzer = "smartcn", valueBridge = @ValueBridgeRef(type = Revision.Bridge.class))
    public Revision revision = null;

    @ElementCollection
    @Column(columnDefinition = "text", nullable = false)
    @CollectionTable(name = "chahoutan_editors")
    public Set<String> editor = new HashSet<>();

    public static Post from(Request request, ImageRepository imageRepo)
    {
        var post = new Post();
        post.id = request.id;
        post.editor = new HashSet<>(request.editors);
        post.revision = Revision.from(post, request.text);
        post.revision.image = request.images().stream()
                .map(r -> Image.from(r, imageRepo).orElseThrow(BadRequestException::new)).toList();
        return post;
    }

    public static int getLastPublicPostId(Integer until)
    {
        var id = Math.floorDiv(Instant.now().minus(POST_DELAY).toEpochMilli() - POST_EPOCH, POST_INTERVAL);
        return Math.toIntExact(until == null ? id : Math.min(id, until));
    }

    public static OffsetDateTime getPublishTime(Post post)
    {
        return Instant.ofEpochMilli(POST_EPOCH + post.id * POST_INTERVAL).atOffset(ZONE_OFFSET);
    }

    public static record Request(@JsonProperty(value = "id", required = true) int id,
                                 @JsonProperty(value = "text", required = true) String text,
                                 @JsonProperty(value = "editors", required = true) List<String> editors,
                                 @JsonProperty(value = "images", required = true) List<Image.Request> images)
    {
    }

    public static record Response(@JsonProperty(value = "id") int id,
                                  @JsonProperty(value = "type") String type,
                                  @JsonProperty(value = "title") String title,
                                  @JsonProperty(value = "text") String text,
                                  @JsonProperty(value = "revision") UUID revision,
                                  @JsonInclude(JsonInclude.Include.NON_NULL)
                                  @JsonProperty(value = "editors") List<String> editors,
                                  @JsonProperty(value = "images") List<Image.Response> images,
                                  @JsonProperty(value = "publish_time") OffsetDateTime publishTime)
    {
        public static Response from(Post post)
        {
            var revision = post.revision;
            var publishTime = getPublishTime(post);
            var type = post.id <= Post.getLastPublicPostId(null) ? "post" : "draft";
            var name = MessageFormat.format(ChahoutanConfig.NAME_PATTERN, post.id, publishTime.toLocalDate());
            var editors = post.editor.stream().sorted().toList();
            var images = revision.image.stream().map(Image.Response::from).toList();
            return new Response(post.id, type, name, revision.text, revision.id, editors, images, publishTime);
        }

        public static Response from(Revision revision)
        {
            var post = revision.post;
            var publishTime = getPublishTime(post);
            var isPost = post.revision != null && post.revision.id.equals(revision.id);
            var type = isPost ? post.id <= Post.getLastPublicPostId(null) ? "post" : "draft" : "revision";
            var name = MessageFormat.format(ChahoutanConfig.NAME_PATTERN, post.id, publishTime.toLocalDate());
            var editors = isPost ? post.editor.stream().sorted().toList() : null;
            var images = revision.image.stream().map(Image.Response::from).toList();
            return new Response(post.id, type, name, revision.text, revision.id, editors, images, publishTime);
        }
    }
}
