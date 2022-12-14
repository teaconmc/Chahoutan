package org.teacon.chahoutan;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.teacon.chahoutan.auth.RequireAuthFilter;
import org.teacon.chahoutan.endpoint.FeedEndpoint;
import org.teacon.chahoutan.endpoint.VersionEndpoint;
import org.teacon.chahoutan.endpoint.v1.ImageEndpoint;
import org.teacon.chahoutan.endpoint.v1.PostEndpoint;
import org.teacon.chahoutan.endpoint.v1.RefreshEndpoint;
import org.teacon.chahoutan.provider.CorsResponseFilter;
import org.teacon.chahoutan.provider.ErrorExceptionMapper;
import org.teacon.chahoutan.provider.JacksonContextResolver;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Objects;

@Component
public class ChahoutanConfig extends ResourceConfig
{
    public static final String PG_FTS_CONFIG = Objects.requireNonNullElse(System.getenv("CHAHOUTAN_PG_FTS_CONFIG"), "english");
    public static final String PG_DATASOURCE = Objects.requireNonNullElse(System.getenv("CHAHOUTAN_PG_DATASOURCE"), "jdbc:postgresql://localhost:5432/chahoutan?user=postgres");

    public static final String AUTHOR = "TeaCon";
    public static final String TITLE = "TeaCon 茶后谈";
    public static final String EMAIL = "contact@teacon.org";
    public static final String DESCRIPTION = "茶后谈是 TeaCon（Mod 开发茶会）旗下的原创性专栏，" +
            "主要对 Minecraft Mod 社区的趣闻轶事进行辑录，并于每周一三五以 Bilibili 动态的形式刊载。" +
            "茶后谈目前由 TeaCon 执行委员会独立维护，所有已刊载内容均使用 CC-BY 4.0 协议授权。";

    public static final String NAME_PATTERN = "#{0}（{1}）";
    public static final String BACKEND_URL_PREFIX = "https://chahoutan.teacon.cn/";
    public static final String FRONTEND_URL_PREFIX = "https://www.teacon.cn/chahoutan/";

    public static final String ANCHOR_PREFIX = "茶后谈#";
    public static final String ANCHOR_REDDIT_PREFIX = "/r/";
    public static final String ANCHOR_FORGE_PREFIX = "MinecraftForge#";
    public static final String ANCHOR_REDDIT_URL_PREFIX = "https://www.reddit.com/r/";
    public static final String ANCHOR_FORGE_URL_PREFIX = "https://github.com/MinecraftForge/MinecraftForge/issues/";

    public static final String EDITOR_SIGN_SUFFIX = "】";
    public static final String EDITOR_SIGN_SEPARATOR = "，";
    public static final String EDITOR_SIGN_PREFIX = "【本期编辑：";

    public static final ZoneId POST_ZONE_ID = ZoneId.of("UTC+8");
    public static final Duration POST_DELAY = Duration.ofHours(28);
    public static final Duration POST_INTERVAL = Duration.ofHours(56);
    public static final Instant POST_EPOCH = Instant.parse("2021-01-29T19:40:00+08:00");
    public static final Pageable POST_QUERY_DEFAULT_PAGE = Pageable.ofSize(20);

    public static final byte[] ICO_BINARY_WITH_GZIP_COMPRESSED = Base64.getDecoder().decode("H4sICHTyaWECA3RlYWNv" +
            "bi1mYXZpY29uLmljbwDtWn1oFEcUn8Roq9XaqkixepcIihYFP0JRErmokVosrWgDCkqsX/hJ/1EMVnP5Q0zwKyBIENuqbRW0rSKi" +
            "UBWJxNbalkYFqR9gPKK2Yls9xXrW643v3f6OHtt172P3crN1XvjdIztv37w3O/PmzdsVooD+KioE/RaLIROE6CuEGEKgS6JOGNc1" +
            "adKkSZMmTRlTEaEfYWASRhBG5xGjTPYwfIRXCYUu+d2VsInwkCA9hL8IpwjjHfjei3AW+m4SDhH2A7sI2xXAp0k2JXCY8DPsjhHq" +
            "s/C9gHAcOlhndw+u2XLCNfhQm+G9U3HfCUInD8et/oQbhCihNIP7Pof/KwiVJlSlgcWEVcAKYHEGmEOYTXjX1PeELOLkR/Dlkwz8" +
            "P+SxeJcOvsvA/zcI6wkNJtQkPVczOE78jb6uI3Z+S2hOwmnCD4TWFLhA+IVwB/ruEnZa2NOUQaycmsPcYBvsvE+Yi/jpBnUmNCKO" +
            "PyLMVzAv2gffLxGG5qif9wlh9LNOEd95X9gDm3iuv5Lj/rikE0J/Tcg/X8qj/xtgy08d4HuCBmCeJWLaP4SLWN8TO3Cvno41yflF" +
            "7w4e9xLCbfj/O+JCYjyuIv4U5rB/H9biI5xB8kFlyGlCGP/JWItPMA4/Yr3kgvaij6V5jj8bYcdW09z4GtcfEKa43OcYzPvzCuTF" +
            "3QhtyDlKTG3VhAjhMeaGW/QlxnayUIPmw56PLdregv+cO/ld6Ksv9F1xMb9xIz9qg119LNqXY3z2uNDXIuiqUSwHWwO7lli08T5w" +
            "GePTy2E/X6GfYYr5PxAxqTlFnJzkMNf7g/CrQnM/mS6j5lVk0bYE/lc70F8KHV8oWuf4DPaNsGhb5oL/S6FjgaL+18C+6RZtW9A2" +
            "zoH+euioVNT/D2xi4Em0vebC/BqqqP/vwL6g6XoAsfGsQ/2JnLK/ov6Pg32b8X8BcrTfcEascKg/UeMoVtT/4bCvDc/6btL5eLkL" +
            "+hN1jkGK+v9e0hk4hnMh71VjXdLfBN1vKup/nQt7nB1VQz/Xb8012FVZgvflhS5gEZ73kxzWYroQDgq16/urczzHClHvqbQA59ZV" +
            "WWJmFs98hklHqdCkSVM+idfg28+x/+cRh7k+/IIL+dxN7K9dPOI/f2tzC2Ow26EurjE9gK5vnlHXUJH4G7EbKc6KnKv9SZiVQhfn" +
            "2heha62H1sFR2FxmU0uJQWZlmnWnFo/4zmuVvwHgdw/dbOQ4Tj7E+Wykjdw0oXbdTVjUG9jeY2nIrofshzYyOyAzzyP+V8HexjRk" +
            "E/V0u3d0rZDxecT/SbD3SAq5ItQo+N3dyzZy56DvdY/4/yJie1TY1woHi/S+ydoJuWUeiv+14t/vRu32Npb5PoWuMshxXtHDI/5z" +
            "3D+cYm/j90l3ME9KUug7gDEo/5/lyw1J+bId9RDO3t2pSvzNFNen25/jMxPva8X66KxJkyZNmjR1LMlsqS6exsoY1EQNHowEuDEa" +
            "iASZx/wR4/p/uJR0b08WoZ96voUuNcdvlfI6XWMF9zoR6FqkQMowt82RMsR8TFC2873lAdnC3BeQZ7g/n9/gfn9cp2z0y0Y7viVg" +
            "8AbwuiC4hBz87AnONsbLHDYD0wreIo0xIB4XD4WjcR6+FzP+j9TGeXvUxHE9FDHkwpGY3+DBOJfRYADc6CgWlNnTUzeNa2s+QgAA");

    public static final int VERSION = 1;
    public ChahoutanConfig()
    {
        // endpoints
        this.register(FeedEndpoint.class);
        this.register(VersionEndpoint.class);
        this.register(ImageEndpoint.class);
        this.register(PostEndpoint.class);
        this.register(RefreshEndpoint.class);
        // providers
        this.register(new RequireAuthFilter());
        this.register(new CorsResponseFilter());
        this.register(new ErrorExceptionMapper());
        this.register(new JacksonContextResolver());
        this.register(new JacksonJaxbJsonProvider());
    }
}
