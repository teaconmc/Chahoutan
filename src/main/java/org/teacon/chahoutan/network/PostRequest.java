package org.teacon.chahoutan.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.entity.Revision;
import org.teacon.chahoutan.repo.ImageRepository;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PostRequest(@JsonProperty(value = "id", required = true) int id,
                          @JsonProperty(value = "text", required = true) String text,
                          @JsonProperty(value = "editors") List<String> editors,
                          @JsonProperty(value = "anchors") List<String> anchors,
                          @JsonProperty(value = "images") List<ImageRequest> images)
{
    private static <T> List<T> nullToEmpty(List<T> input)
    {
        return input == null ? List.of() : input;
    }

    public Post toPost(ImageRepository repo)
    {
        var post = new Post();
        var revision = new Revision();

        post.setId(this.id);
        post.setRevision(revision);
        post.setEditors(nullToEmpty(this.editors));

        revision.setPost(post);
        revision.setText(this.text);
        revision.setCreationTime(Instant.now());
        revision.setAnchors(nullToEmpty(this.anchors));
        revision.setImages(nullToEmpty(this.images).stream().map(request -> request.toImage(repo)).toList());

        return post;
    }
}
