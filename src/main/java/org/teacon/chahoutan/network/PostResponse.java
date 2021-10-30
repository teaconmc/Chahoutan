package org.teacon.chahoutan.network;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.teacon.chahoutan.ChahoutanConfig;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.entity.Revision;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
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
                           @JsonProperty(value = "publish_time") OffsetDateTime publishTime)
{
    public static PostResponse from(Post post)
    {
        var revision = post.getRevision();
        var revisionName = revision.getTitle();
        var publishTime = post.getPublishTime();
        var urlPrefix = URI.create(ChahoutanConfig.BACKEND_URL_PREFIX);
        var url = urlPrefix.resolve("v1/posts/" + post.getId());
        var revisionUrl = urlPrefix.resolve("v1/posts/" + revision.getId());
        var type = post.getId() <= Post.getLastPublicPostId(null) ? "post" : "draft";
        var editors = post.getEditors().stream().sorted().toList();
        var images = revision.getImages().stream().map(ImageResponse::from).toList();
        return new PostResponse(post.getId(), url, type, revisionName, revision.getText(), revision.getId(),
                revisionUrl, editors, revision.getAnchors(), revision.getAnchorUrls(), images, publishTime);
    }

    public static PostResponse from(Revision revision)
    {
        var post = revision.getPost();
        var revisionName = revision.getTitle();
        var publishTime = post.getPublishTime();
        var isPost = post.getRevision() != null && post.getRevision().getId().equals(revision.getId());
        var urlPrefix = URI.create(ChahoutanConfig.BACKEND_URL_PREFIX);
        var url = isPost ? urlPrefix.resolve("v1/posts/" + post.getId()) : null;
        var revisionUrl = urlPrefix.resolve("v1/posts/" + revision.getId());
        var type = isPost ? post.getId() <= Post.getLastPublicPostId(null) ? "post" : "draft" : "revision";
        var editors = isPost ? post.getEditors().stream().sorted().toList() : null;
        var images = revision.getImages().stream().map(ImageResponse::from).toList();
        return new PostResponse(post.getId(), url, type, revisionName, revision.getText(), revision.getId(),
                revisionUrl, editors, revision.getAnchors(), revision.getAnchorUrls(), images, publishTime);
    }
}
