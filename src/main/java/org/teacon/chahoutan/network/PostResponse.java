package org.teacon.chahoutan.network;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.teacon.chahoutan.ChahoutanConfig;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.entity.Revision;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public record PostResponse(@JsonProperty(value = "id") int id,
                           @JsonProperty(value = "url") URI url,
                           @JsonProperty(value = "type") String type,
                           @JsonProperty(value = "title") String title,
                           @JsonProperty(value = "text") String text,
                           @JsonProperty(value = "revision") UUID revision,
                           @JsonProperty(value = "revision_url") URI revisionUrl,
                           @JsonProperty(value = "editors") List<String> editors,
                           @JsonProperty(value = "anchors") List<String> anchors,
                           @JsonProperty(value = "anchor_urls") List<String> anchorUrls,
                           @JsonProperty(value = "images") List<ImageResponse> postImages,
                           @JsonProperty(value = "footnotes") Map<String, String> footnotes,
                           @JsonProperty(value = "footnote_urls") Map<String, String> footnoteUrls,
                           @JsonProperty(value = "publish_time") OffsetDateTime publishTime)
{
    public static PostResponse from(Post post)
    {
        var revision = post.getRevision();
        var revisionName = revision.getTitle();
        var type = post.getId() <= Post.getLastPublicPostId(null) ? "post" : "draft";
        var editors = post.getEditors().stream().sorted().toList();
        var images = revision.getImages().stream().map(ImageResponse::from).toList();
        return new PostResponse(post.getId(), post.getBackendUrl(),
                type, revisionName, revision.getText(), revision.getId(),
                revision.getBackendUrl(), editors, revision.getAnchors(), revision.getAnchorUrls(),
                images, revision.getFootnotes(), revision.getFootnoteUrls(), post.getPublishTime());
    }

    public static PostResponse from(Revision revision)
    {
        var post = revision.getPost();
        var revisionName = revision.getTitle();
        var isPost = post.getRevision() != null && post.getRevision().getId().equals(revision.getId());
        var type = isPost ? post.getId() <= Post.getLastPublicPostId(null) ? "post" : "draft" : "revision";
        var editors = isPost ? post.getEditors().stream().sorted().toList() : null;
        var images = revision.getImages().stream().map(ImageResponse::from).toList();
        return new PostResponse(post.getId(),
                isPost ? post.getBackendUrl() : null,
                type, revisionName, revision.getText(), revision.getId(),
                revision.getBackendUrl(), editors, revision.getAnchors(), revision.getAnchorUrls(),
                images, revision.getFootnotes(), revision.getFootnoteUrls(), post.getPublishTime());
    }
}
