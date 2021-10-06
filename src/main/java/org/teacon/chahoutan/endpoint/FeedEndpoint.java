package org.teacon.chahoutan.endpoint;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.springframework.stereotype.Component;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.entity.Revision;
import org.teacon.chahoutan.repo.MetadataRepository;
import org.teacon.chahoutan.repo.PostRepository;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Path("/feed")
@Produces("application/rss+xml; charset=utf-8")
public class FeedEndpoint
{
    private static final String DEFAULT_URL_PREFIX = "https://www.teacon.cn/chahoutan";

    private final PostRepository postRepo;
    private final MetadataRepository metadataRepo;

    public FeedEndpoint(PostRepository postRepo, MetadataRepository metadataRepo)
    {
        this.postRepo = postRepo;
        this.metadataRepo = metadataRepo;
    }

    @GET
    public String rss()
    {
        try
        {
            var feed = this.getFeed("rss_2.0");
            var output = new SyndFeedOutput();
            return output.outputString(feed);
        }
        catch (FeedException e)
        {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/atom")
    public String atom()
    {
        try
        {
            var feed = this.getFeed("atom_1.0");
            var output = new SyndFeedOutput();
            return output.outputString(feed);
        }
        catch (FeedException e)
        {
            throw new InternalServerErrorException(e);
        }
    }

    private SyndFeed getFeed(String type)
    {
        var feed = new SyndFeedImpl();
        var author = this.metadataRepo.findById("author").map(m -> m.text).orElse("TeaCon");
        var title = this.metadataRepo.findById("title").map(m -> m.text).orElse("TeaCon Chahoutan");
        var urlPrefix = URI.create(this.metadataRepo.findById("url_prefix").map(m -> m.text).orElse(DEFAULT_URL_PREFIX));

        feed.setFeedType(type);

        var manager = new SyndPersonImpl();
        manager.setName(author);
        this.metadataRepo.findById("email").map(m -> m.text).ifPresent(manager::setEmail);

        feed.setTitle(title);
        feed.setAuthor(author);
        feed.setAuthors(List.of(manager));
        feed.setLink(urlPrefix.toASCIIString());
        feed.setDescription(this.metadataRepo.findById("description").map(m -> m.text).orElse(title));

        var items = new ArrayList<SyndEntry>(20);
        var lastId = Post.getLastPublicPostId(null);
        for (Post post : this.postRepo.findFirst20PostsByIdLessThanEqualAndRevisionNotNullOrderByIdDesc(lastId))
        {
            var entry = new SyndEntryImpl();
            var publishTIme = Post.getPublishTime(post);
            var name = Post.getTitle(post, publishTIme.toLocalDate());

            entry.setTitle(name);
            entry.setUri(post.revision.id.toString());
            entry.setLink(urlPrefix.resolve(Integer.toString(post.id)).toASCIIString());

            var content = new SyndContentImpl();
            content.setType(MediaType.TEXT_HTML);
            content.setValue(Revision.toHtmlText(post.revision)); // TODO: images and editors
            entry.setContents(List.of(content));

            var description = new SyndContentImpl();
            description.setType(MediaType.TEXT_PLAIN);
            description.setValue(Revision.toPlainText(post.revision));
            entry.setDescription(description);

            entry.setPublishedDate(Date.from(publishTIme.toInstant()));

            var editors = new ArrayList<SyndPerson>();
            for (String editor : post.editor)
            {
                var person = new SyndPersonImpl();
                person.setName(editor);
                editors.add(person);
            }
            editors.add(manager);
            entry.setAuthor(author);
            entry.setAuthors(editors);

            items.add(entry);
        }
        feed.setEntries(items);

        return feed;
    }
}
