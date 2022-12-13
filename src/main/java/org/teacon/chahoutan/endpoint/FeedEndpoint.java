package org.teacon.chahoutan.endpoint;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.springframework.stereotype.Component;
import org.teacon.chahoutan.ChahoutanConfig;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.repo.PostRepository;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Path("/feed")
public class FeedEndpoint
{
    private final PostRepository postRepo;

    public FeedEndpoint(PostRepository postRepo)
    {
        this.postRepo = postRepo;
    }

    @GET
    @Produces("application/rss+xml; charset=utf-8")
    public String rss(@QueryParam("until") Integer until)
    {
        try
        {
            var feed = this.getFeed("rss_2.0", until);
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
    @Produces("application/atom+xml; charset=utf-8")
    public String atom(@QueryParam("until") Integer until)
    {
        try
        {
            var feed = this.getFeed("atom_1.0", until);
            var output = new SyndFeedOutput();
            return output.outputString(feed);
        }
        catch (FeedException e)
        {
            throw new InternalServerErrorException(e);
        }
    }

    private SyndFeed getFeed(String type, Integer until)
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
        var lastId = Post.getLastPublicPostId(until);
        var queryDefaultPage = ChahoutanConfig.POST_QUERY_DEFAULT_PAGE;
        for (var post : this.postRepo.findByIdLessThanEqualAndRevisionNotNullOrderByIdDesc(lastId, queryDefaultPage))
        {
            var entry = new SyndEntryImpl();
            var publishTIme = post.getPublishTime();

            entry.setTitle(post.getRevision().getTitle());
            entry.setUri(post.getRevision().getId().toString());
            entry.setLink(urlPrefix.resolve(Integer.toString(post.getId())).toASCIIString());

            var content = new SyndContentImpl();
            content.setType(MediaType.TEXT_HTML);
            content.setValue(post.getRevision().getRssHtmlText());
            entry.setContents(List.of(content));

            var description = new SyndContentImpl();
            description.setType(MediaType.TEXT_PLAIN);
            description.setValue(post.getRevision().getRssPlainText());
            entry.setDescription(description);

            var editors = new ArrayList<SyndPerson>();
            for (var editor : post.getEditors())
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
