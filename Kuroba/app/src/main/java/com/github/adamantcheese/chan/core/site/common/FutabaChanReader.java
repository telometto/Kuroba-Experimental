package com.github.adamantcheese.chan.core.site.common;

import android.util.JsonReader;

import com.github.adamantcheese.chan.core.model.Post;
import com.github.adamantcheese.chan.core.model.PostHttpIcon;
import com.github.adamantcheese.chan.core.model.PostImage;
import com.github.adamantcheese.chan.core.site.SiteEndpoints;
import com.github.adamantcheese.chan.core.site.parser.ChanReader;
import com.github.adamantcheese.chan.core.site.parser.ChanReaderProcessor;
import com.github.adamantcheese.chan.core.site.parser.CommentParser;
import com.github.adamantcheese.chan.core.site.parser.PostParser;

import org.jsoup.parser.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;

import static com.github.adamantcheese.chan.core.site.SiteEndpoints.makeArgument;

public class FutabaChanReader
        implements ChanReader {
    private final PostParser postParser;

    public FutabaChanReader() {
        CommentParser commentParser = new CommentParser().addDefaultRules();
        this.postParser = new DefaultPostParser(commentParser);
    }

    @Override
    public PostParser getParser() {
        return postParser;
    }

    @Override
    public void loadThread(JsonReader reader, ChanReaderProcessor chanReaderProcessor)
            throws Exception {
        reader.beginObject();
        // Page object
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (key.equals("posts")) {
                reader.beginArray();
                // Thread array
                while (reader.hasNext()) {
                    // Thread object
                    readPostObject(reader, chanReaderProcessor);
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    @Override
    public void loadCatalog(JsonReader reader, ChanReaderProcessor chanReaderProcessor)
            throws Exception {
        reader.beginArray(); // Array of pages

        while (reader.hasNext()) {
            reader.beginObject(); // Page object

            while (reader.hasNext()) {
                if (reader.nextName().equals("threads")) {
                    reader.beginArray(); // Threads array

                    while (reader.hasNext()) {
                        readPostObject(reader, chanReaderProcessor);
                    }

                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }

            reader.endObject();
        }

        reader.endArray();
    }

    public void readPostObject(JsonReader reader, ChanReaderProcessor chanReaderProcessor)
            throws Exception {
        Post.Builder builder = new Post.Builder();
        builder.board(chanReaderProcessor.getLoadable().board);

        SiteEndpoints endpoints = chanReaderProcessor.getLoadable().getSite().endpoints();

        // File
        String fileId = null;
        String fileExt = null;
        int fileWidth = 0;
        int fileHeight = 0;
        long fileSize = 0;
        boolean fileSpoiler = false;
        String fileName = null;
        String fileHash = null;
        boolean fileDeleted = false;

        List<PostImage> files = new ArrayList<>();

        // Country flag
        String countryCode = null;
        String trollCountryCode = null;
        String countryName = null;

        // 4chan pass leaf
        int since4pass = 0;

        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();

            switch (key) {
                case "no":
                    builder.id(reader.nextInt());
                    break;
                /*case "now":
                    post.date = reader.nextString();
                    break;*/
                case "sub":
                    builder.subject(reader.nextString());
                    break;
                case "name":
                    builder.name(reader.nextString());
                    break;
                case "com":
                    builder.comment(reader.nextString());
                    break;
                case "tim":
                    fileId = reader.nextString();
                    break;
                case "time":
                    builder.setUnixTimestampSeconds(reader.nextLong());
                    break;
                case "ext":
                    fileExt = reader.nextString().replace(".", "");
                    break;
                case "w":
                    fileWidth = reader.nextInt();
                    break;
                case "h":
                    fileHeight = reader.nextInt();
                    break;
                case "fsize":
                    fileSize = reader.nextLong();
                    break;
                case "filename":
                    fileName = reader.nextString();
                    break;
                case "trip":
                    builder.tripcode(reader.nextString());
                    break;
                case "country":
                    countryCode = reader.nextString();
                    break;
                case "troll_country":
                    trollCountryCode = reader.nextString();
                    break;
                case "country_name":
                    countryName = reader.nextString();
                    break;
                case "spoiler":
                    fileSpoiler = reader.nextInt() == 1;
                    break;
                case "resto":
                    int opId = reader.nextInt();
                    builder.op(opId == 0);
                    builder.opId(opId);
                    break;
                case "filedeleted":
                    fileDeleted = reader.nextInt() == 1;
                    break;
                case "sticky":
                    builder.sticky(reader.nextInt() == 1);
                    break;
                case "closed":
                    builder.closed(reader.nextInt() == 1);
                    break;
                case "archived":
                    builder.archived(reader.nextInt() == 1);
                    break;
                case "replies":
                    builder.replies(reader.nextInt());
                    break;
                case "images":
                    builder.threadImagesCount(reader.nextInt());
                    break;
                case "unique_ips":
                    builder.uniqueIps(reader.nextInt());
                    break;
                case "last_modified":
                    builder.lastModified(reader.nextLong());
                    break;
                case "id":
                    builder.posterId(reader.nextString());
                    break;
                case "capcode":
                    builder.moderatorCapcode(reader.nextString());
                    break;
                case "since4pass":
                    since4pass = reader.nextInt();
                    break;
                case "extra_files":
                    reader.beginArray();

                    while (reader.hasNext()) {
                        PostImage postImage = readPostImage(reader, builder, endpoints);
                        if (postImage != null) {
                            files.add(postImage);
                        }
                    }

                    reader.endArray();
                    break;
                case "md5":
                    fileHash = reader.nextString();
                    break;
                default:
                    // Unknown/ignored key
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();

        // The file from between the other values.
        if (fileId != null && fileName != null && fileExt != null && !fileDeleted) {
            Map<String, String> args = makeArgument("tim", fileId, "ext", fileExt);
            PostImage image = new PostImage.Builder()
                    .serverFilename(fileId)
                    .thumbnailUrl(endpoints.thumbnailUrl(builder, false, args))
                    .spoilerThumbnailUrl(endpoints.thumbnailUrl(builder, true, args))
                    .imageUrl(endpoints.imageUrl(builder, args))
                    .filename(Parser.unescapeEntities(fileName, false))
                    .extension(fileExt)
                    .imageWidth(fileWidth)
                    .imageHeight(fileHeight)
                    .spoiler(fileSpoiler)
                    .size(fileSize)
                    .fileHash(fileHash, true)
                    .build();
            // Insert it at the beginning.
            files.add(0, image);
        }

        builder.postImages(files);

        if (builder.op) {
            // Update OP fields later on the main thread
            Post.Builder op = new Post.Builder();
            op.closed(builder.closed);
            op.archived(builder.archived);
            op.sticky(builder.sticky);
            op.replies(builder.replies);
            op.threadImagesCount(builder.threadImagesCount);
            op.uniqueIps(builder.uniqueIps);
            op.lastModified(builder.lastModified);
            chanReaderProcessor.setOp(op);
        }

        if (countryCode != null && countryName != null) {
            HttpUrl countryUrl = endpoints.icon("country", makeArgument("country_code", countryCode));
            builder.addHttpIcon(new PostHttpIcon(countryUrl, countryName + "/" + countryCode));
        }

        if (trollCountryCode != null && countryName != null) {
            HttpUrl countryUrl = endpoints.icon("troll_country", makeArgument("troll_country_code", trollCountryCode));
            builder.addHttpIcon(new PostHttpIcon(countryUrl, countryName + "/t_" + trollCountryCode));
        }

        if (since4pass != 0) {
            HttpUrl iconUrl = endpoints.icon("since4pass", null);
            builder.addHttpIcon(new PostHttpIcon(iconUrl, String.valueOf(since4pass)));
        }

        if (chanReaderProcessor.containsPostNo(builder.id).unwrap()) {
            chanReaderProcessor.addForUpdateInDatabase(builder);
        } else {
            chanReaderProcessor.addForParse(builder);
        }
    }

    private PostImage readPostImage(JsonReader reader, Post.Builder builder, SiteEndpoints endpoints)
            throws IOException {
        reader.beginObject();

        String fileId = null;
        long fileSize = 0;

        String fileExt = null;
        int fileWidth = 0;
        int fileHeight = 0;
        boolean fileSpoiler = false;
        String fileName = null;
        String fileHash = null;

        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "tim":
                    fileId = reader.nextString();
                    break;
                case "fsize":
                    fileSize = reader.nextLong();
                    break;
                case "w":
                    fileWidth = reader.nextInt();
                    break;
                case "h":
                    fileHeight = reader.nextInt();
                    break;
                case "spoiler":
                    fileSpoiler = reader.nextInt() == 1;
                    break;
                case "ext":
                    fileExt = reader.nextString().replace(".", "");
                    break;
                case "filename":
                    fileName = reader.nextString();
                    break;
                case "md5":
                    fileHash = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        if (fileId != null && fileName != null && fileExt != null) {
            Map<String, String> args = makeArgument("tim", fileId, "ext", fileExt);
            return new PostImage.Builder().serverFilename(fileId)
                    .thumbnailUrl(endpoints.thumbnailUrl(builder, false, args))
                    .spoilerThumbnailUrl(endpoints.thumbnailUrl(builder, true, args))
                    .imageUrl(endpoints.imageUrl(builder, args))
                    .filename(Parser.unescapeEntities(fileName, false))
                    .extension(fileExt)
                    .imageWidth(fileWidth)
                    .imageHeight(fileHeight)
                    .spoiler(fileSpoiler)
                    .size(fileSize)
                    .fileHash(fileHash, true)
                    .build();
        }
        return null;
    }
}
