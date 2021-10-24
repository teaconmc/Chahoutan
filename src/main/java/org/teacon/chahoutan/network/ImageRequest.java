package org.teacon.chahoutan.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.teacon.chahoutan.entity.Image;
import org.teacon.chahoutan.repo.ImageRepository;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageRequest(@JsonProperty(value = "id") @DefaultValue("") String id,
                           @JsonProperty(value = "png") @DefaultValue("") String png,
                           @JsonProperty(value = "webp") @DefaultValue("") String webp)
{
    public Image toImage(ImageRepository repo)
    {
        var id = this.id.toLowerCase(Locale.ROOT);
        if (!id.isEmpty())
        {
            return repo.findById(id).orElseThrow(BadRequestException::new);
        }
        var png = this.png.toLowerCase(Locale.ROOT);
        if (!png.endsWith(".png"))
        {
            return repo.findById(png.substring(0, png.length() - 4)).orElseThrow(BadRequestException::new);
        }
        var webp = this.webp.toLowerCase(Locale.ROOT);
        if (!webp.endsWith(".webp"))
        {
            return repo.findById(webp.substring(0, webp.length() - 5)).orElseThrow(BadRequestException::new);
        }
        throw new BadRequestException();
    }
}
