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
                          @JsonProperty(value = "editors", required = true) List<String> editors,
                          @JsonProperty(value = "anchors", required = true) List<String> anchors,
                          @JsonProperty(value = "images", required = true) List<ImageRequest> images)
{
    public Post toPost(ImageRepository repo)
    {
        var post = new Post();
        var revision = new Revision();

        post.setId(this.id);
        post.setRevision(revision);
        post.setEditors(this.editors);

        revision.setPost(post);
        revision.setText(this.text);
        revision.setAnchors(this.anchors);
        revision.setCreationTime(Instant.now());
        revision.setImages(this.images.stream().map(request -> request.toImage(repo)).toList());

        return post;
    }
}
