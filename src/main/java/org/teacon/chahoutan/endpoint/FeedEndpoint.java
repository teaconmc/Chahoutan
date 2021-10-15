package org.teacon.chahoutan.endpoint;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.springframework.stereotype.Component;
import org.teacon.chahoutan.ChahoutanConfig;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.entity.Revision;
import org.teacon.chahoutan.repo.PostRepository;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Path("/feed")
@Produces("application/rss+xml; charset=utf-8")
public class FeedEndpoint
{
    private final PostRepository postRepo;

    public FeedEndpoint(PostRepository postRepo)
    {
        this.postRepo = postRepo;
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
        var urlPrefix = URI.create(ChahoutanConfig.FRONTEND_URL_PREFIX);

        feed.setFeedType(type);

        var chahoutanAuthor = new SyndPersonImpl();
        chahoutanAuthor.setName(ChahoutanConfig.AUTHOR);
        chahoutanAuthor.setEmail(ChahoutanConfig.EMAIL);

        var icon = new SyndImageImpl();
        icon.setTitle(ChahoutanConfig.TITLE);
        icon.setDescription(ChahoutanConfig.DESCRIPTION);
        icon.setUrl(URI.create(ChahoutanConfig.BACKEND_URL_PREFIX).resolve("favicon.ico").toASCIIString());

        feed.setImage(icon);
        feed.setTitle(ChahoutanConfig.TITLE);
        feed.setAuthor(ChahoutanConfig.AUTHOR);
        feed.setAuthors(List.of(chahoutanAuthor));
        feed.setDescription(ChahoutanConfig.DESCRIPTION);
        feed.setLink(ChahoutanConfig.FRONTEND_URL_PREFIX);

        var items = new ArrayList<SyndEntry>(20);
        var lastId = Post.getLastPublicPostId(null);
        for (Post post : this.postRepo.findFirst20PostsByIdLessThanEqualAndRevisionNotNullOrderByIdDesc(lastId))
        {
            var entry = new SyndEntryImpl();
            var publishTIme = Post.getPublishTime(post);
            var name = MessageFormat.format(ChahoutanConfig.NAME_PATTERN, post.id, publishTIme.toLocalDate());

            entry.setTitle(name);
            entry.setUri(post.revision.id.toString());
            entry.setLink(urlPrefix.resolve(Integer.toString(post.id)).toASCIIString());

            var content = new SyndContentImpl();
            content.setType(MediaType.TEXT_HTML);
            content.setValue(Revision.toRssHtmlText(post.revision));
            entry.setContents(List.of(content));

            var description = new SyndContentImpl();
            description.setType(MediaType.TEXT_PLAIN);
            description.setValue(Revision.toRssPlainText(post.revision));
            entry.setDescription(description);

            var editors = new ArrayList<SyndPerson>();
            for (String editor : Post.getSortedEditors(post))
            {
                var person = new SyndPersonImpl();
                person.setName(editor);
                editors.add(person);
            }
            editors.add(chahoutanAuthor);
            entry.setAuthors(editors);

            entry.setAuthor(ChahoutanConfig.AUTHOR);
            entry.setPublishedDate(Date.from(publishTIme.toInstant()));

            items.add(entry);
        }
        feed.setEntries(items);

        return feed;
    }
}
