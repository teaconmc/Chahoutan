package org.teacon.chahoutan.network;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.teacon.chahoutan.ChahoutanConfig;
import org.teacon.chahoutan.entity.Image;

import java.net.URI;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public record ImageResponse(@JsonProperty(value = "id") String id,
                            @JsonProperty(value = "png") String png,
                            @JsonProperty(value = "png_url") URI pngUrl,
                            @JsonProperty(value = "webp") String webp,
                            @JsonProperty(value = "webp_url") URI webpUrl)
{
    public static ImageResponse from(Image image)
    {
        var urlPrefix = URI.create(ChahoutanConfig.BACKEND_URL_PREFIX);
        var pngFile = image.getId() + ".png";
        var pngUrl = urlPrefix.resolve("v1/images/" + pngFile);
        var webpFile = image.getId() + ".webp";
        var webpUrl = urlPrefix.resolve("v1/images/" + webpFile);
        return new ImageResponse(image.getId(), pngFile, pngUrl, webpFile, webpUrl);
    }
}
