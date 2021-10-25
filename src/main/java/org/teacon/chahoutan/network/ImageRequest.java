package org.teacon.chahoutan.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.teacon.chahoutan.entity.Image;
import org.teacon.chahoutan.repo.ImageRepository;

import javax.ws.rs.BadRequestException;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageRequest(@JsonProperty(value = "id") String id,
                           @JsonProperty(value = "png") String png,
                           @JsonProperty(value = "webp") String webp)
{
    private static String nullToEmpty(String input)
    {
        return input == null ? "" : input;
    }

    public Image toImage(ImageRepository repo)
    {
        var id = nullToEmpty(this.id).toLowerCase(Locale.ROOT);
        if (!id.isEmpty())
        {
            return repo.findById(id).orElseThrow(BadRequestException::new);
        }
        var png = nullToEmpty(this.png).toLowerCase(Locale.ROOT);
        if (png.endsWith(".png"))
        {
            return repo.findById(png.substring(0, png.length() - 4)).orElseThrow(BadRequestException::new);
        }
        var webp = nullToEmpty(this.webp).toLowerCase(Locale.ROOT);
        if (webp.endsWith(".webp"))
        {
            return repo.findById(webp.substring(0, webp.length() - 5)).orElseThrow(BadRequestException::new);
        }
        throw new BadRequestException();
    }
}
