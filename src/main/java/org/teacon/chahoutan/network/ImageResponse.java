package org.teacon.chahoutan.network;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.teacon.chahoutan.ChahoutanConfig;
import org.teacon.chahoutan.entity.Image;

import java.net.URI;
import java.util.List;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public record ImageResponse(@JsonProperty(value = "id") String id,
                            @JsonProperty(value = "size") List<Integer> size,
                            @JsonProperty(value = "detached") Boolean detached,
                            @JsonProperty(value = "png") String png,
                            @JsonProperty(value = "png_url") URI pngUrl,
                            @JsonProperty(value = "webp") String webp,
                            @JsonProperty(value = "webp_url") URI webpUrl)
{
    public static ImageResponse from(Image image)
    {
        var width = image.getWidth();
        var height = image.getHeight();
        var detached = image.getRevisions().isEmpty();
        var size = width * height > 0 ? List.of(width, height) : null;
        var urlPrefix = URI.create(ChahoutanConfig.BACKEND_URL_PREFIX);
        var pngFile = image.getId() + ".png";
        var pngUrl = urlPrefix.resolve("v1/images/" + pngFile);
        var webpFile = image.getId() + ".webp";
        var webpUrl = urlPrefix.resolve("v1/images/" + webpFile);
        return new ImageResponse(image.getId(), size, detached, pngFile, pngUrl, webpFile, webpUrl);
    }
}
