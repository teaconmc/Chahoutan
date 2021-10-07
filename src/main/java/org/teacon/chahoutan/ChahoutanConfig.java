package org.teacon.chahoutan;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;
import org.teacon.chahoutan.auth.RequireAuthFilter;
import org.teacon.chahoutan.endpoint.FeedEndpoint;
import org.teacon.chahoutan.endpoint.VersionEndpoint;
import org.teacon.chahoutan.endpoint.v1.ImageEndpoint;
import org.teacon.chahoutan.endpoint.v1.PostEndpoint;
import org.teacon.chahoutan.endpoint.v1.RefreshEndpoint;
import org.teacon.chahoutan.provider.ErrorExceptionMapper;
import org.teacon.chahoutan.provider.JacksonContextResolver;

@Component
public class ChahoutanConfig extends ResourceConfig
{
    public static final String AUTHOR = "TeaCon";
    public static final String TITLE = "TeaCon 茶后谈";
    public static final String EMAIL = "contact@teacon.org";
    public static final String DESCRIPTION = "茶后谈是 TeaCon（Mod 开发茶会）旗下的原创性专栏，" +
            "主要对 Minecraft Mod 社区的趣闻轶事进行辑录，并于每周一三五以 Bilibili 动态的形式刊载。" +
            "茶后谈目前由 TeaCon 执行委员会独立维护，所有已刊载内容均使用 CC-BY 4.0 协议授权。";

    public static final String NAME_PATTERN = "#{0}（{1}）";
    public static final String BACKEND_URL_PREFIX = "https://chahoutan.teacon.cn/";
    public static final String FRONTEND_URL_PREFIX = "https://www.teacon.cn/chahoutan/";

    public static final String EDITOR_SIGN_SUFFIX = "】";
    public static final String EDITOR_SIGN_SEPARATOR = "，";
    public static final String EDITOR_SIGN_PREFIX = "【本期编辑：";

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
        this.register(new ErrorExceptionMapper());
        this.register(new JacksonContextResolver());
        this.register(new JacksonJaxbJsonProvider());
    }
}
