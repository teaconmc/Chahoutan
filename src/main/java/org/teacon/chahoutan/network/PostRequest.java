package org.teacon.chahoutan.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.entity.Revision;
import org.teacon.chahoutan.repo.ImageRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PostRequest(@JsonProperty(value = "id", required = true) int id,
                          @JsonProperty(value = "text", required = true) String text,
                          @JsonProperty(value = "editors") List<String> editors,
                          @JsonProperty(value = "anchors") List<String> anchors,
                          @JsonProperty(value = "footnotes") Map<String, String> footnotes,
                          @JsonProperty(value = "images") List<ImageRequest> images)
{
    private static <T> Stream<T> nullToEmpty(List<T> input)
    {
        return input == null ? Stream.empty() : input.stream();
    }

    private static <S, T> Stream<Map.Entry<T, S>> nullToEmpty(Map<T, S> input)
    {
        return input == null ? Stream.empty() : input.entrySet().stream();
    }

    private static String normalize(String text)
    {
        return String.join("\u00a0", text.split("\\s"));
    }

    public Post toPost(ImageRepository repo)
    {
        var post = new Post();
        var revision = new Revision();

        post.setId(this.id);
        post.setRevision(revision);
        post.setEditors(nullToEmpty(this.editors).toList());

        revision.setPost(post);
        revision.setText(normalize(this.text));
        revision.setCreationTime(Instant.now());
        revision.setAnchors(nullToEmpty(this.anchors).map(PostRequest::normalize).toList());
        revision.setFootnotes(nullToEmpty(this.footnotes)
                .filter(e -> e.getKey().startsWith("[") && e.getKey().endsWith("]"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        revision.setImages(nullToEmpty(this.images).map(request -> request.toImage(repo)).toList());

        return post;
    }
}
