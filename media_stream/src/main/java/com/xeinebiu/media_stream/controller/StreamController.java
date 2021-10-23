package com.xeinebiu.media_stream.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xeinebiu.media_stream.MediaStreamService;
import com.xeinebiu.media_stream.model.Stream;
import com.xeinebiu.media_stream.model.VttSubtitle;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PathVariable;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.framework.body.StreamBody;
import com.yanzhenjie.andserver.framework.body.StringBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.util.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings({"WeakerAccess", "unused"})
@RestController
public class StreamController {

    @GetMapping("/stream/{streamId}")
    public void index(
            @PathVariable("streamId") String id,
            HttpRequest request,
            HttpResponse response
    ) {
        Stream streamItem = getStream(id, request);
        if (streamItem == null) return;

        String subtitlesHtml = createSubtitlesHtml(streamItem);

        String html = "<style>body {margin:0px}</style>\n" +
                "<link href=\"https://unpkg.com/video.js/dist/video-js.min.css\" rel=\"stylesheet\">\n" +
                "<script src=\"https://unpkg.com/video.js/dist/video.min.js\"></script>\n" +
                "<video\n" +
                "    id=\"my-player\"\n" +
                "    class=\"video-js\"\n" +
                "    controls\n" +
                "    preload=\"auto\"\n" +
                "    data-setup='{\"fluid\": true}'>\n" +
                "  <source src=\"http://192.168.1.3:9001/stream/" + id + "/video\" type=\"video/mp4\"></source>\n" +
                subtitlesHtml +
                "  <p class=\"vjs-no-js\">\n" +
                "    To view this video please enable JavaScript, and consider upgrading to a\n" +
                "    web browser that\n" +
                "    <a href=\"https://videojs.com/html5-video-support/\" target=\"_blank\">\n" +
                "      supports HTML5 video\n" +
                "    </a>\n" +
                "  </p>\n" +
                "</video>";

        response.setBody(
                new StringBody(html, MediaType.TEXT_HTML)
        );
    }

    @NonNull
    private String createSubtitlesHtml(@NonNull Stream streamItem) {
        StringBuilder sb = new StringBuilder();

        for (VttSubtitle subtitle : streamItem.getSubtitles()) {

            sb.append("<track kind='captions' src='/stream/")
                    .append(streamItem.getId())
                    .append("/subtitle/")
                    .append(subtitle.getLanguage())
                    .append("' srclang='")
                    .append(subtitle.getLanguage())
                    .append("' label='")
                    .append(subtitle.getDisplayLanguage())
                    .append("' default />")
                    .append("\n");
        }

        return sb.toString();
    }

    @GetMapping("/stream/{streamId}/subtitle/{language}")
    public String getSubtitle(
            @PathVariable("streamId") String id,
            @PathVariable("language") String language,
            HttpRequest request,
            HttpResponse response
    ) {
        Stream streamItem = getStream(id, request);
        if (streamItem == null) return "";

        for (VttSubtitle subtitle : streamItem.getSubtitles()) {
            if (subtitle.getLanguage().equals(language)) {
                return subtitle.getContent();
            }
        }

        return "";
    }

    @GetMapping("/stream/{streamId}/video")
    public void cast(
            @PathVariable("streamId") String id,
            HttpRequest request,
            HttpResponse response
    ) {
        Stream streamItem = getStream(id, request);
        if (streamItem == null) return;

        long contentLength = streamItem.getLength();
        long start = 0, end = 0;

        String range = request.getHeader("Range");
        if (range == null) {
            end = contentLength;
        } else {
            if (range.startsWith("bytes=")) {
                String[] values = range.split("=")[1].split("-");
                start = Integer.parseInt(values[0]);
                if (values.length > 1) {
                    end = Integer.parseInt(values[1]);
                }
            }
            if (end != 0 && end > start) {
                end = end - start + 1;
            } else {
                end = contentLength - 1;
            }
            response.setStatus(206);
            if (start > 0) {
                response.setHeader("Content-Range", " bytes " + start + "-" + end + "/" + contentLength);
            } else {
                response.setHeader("Content-Range", " bytes " + start + "-" + (contentLength - 1) + "/" + contentLength);
            }
        }

        InputStream inputStream = streamItem.getInputStream().invoke();
        VideoStreamBody responseBody = new VideoStreamBody(inputStream, contentLength, start, end);
        response.setBody(responseBody);
    }

    @Nullable
    private Stream getStream(
            String id,
            HttpRequest request
    ) {
        String host = request.getHeader("Host");
        assert host != null;
        int port = Integer.parseInt(host.split(":")[1]);
        return MediaStreamService.Companion.findStream(id, port);
    }

    static class VideoStreamBody extends StreamBody {
        private final long start, end;
        private final InputStream inputStream;

        VideoStreamBody(InputStream stream, long length, long start, long end) {
            super(stream, length, MediaType.parseMediaType("video/mp4"));
            this.inputStream = stream;
            this.start = start;
            this.end = end;
        }

        @Override
        public void writeTo(@NonNull OutputStream output) throws IOException {
            byte[] buffer = new byte[4096];
            long skipped = this.inputStream.skip(this.start);

            int len;
            int total = 0;
            while ((this.end > total) && (len = this.inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, len);
                total += len;
            }
            output.flush();
            close();
        }

        private void close() {
            try {
                this.inputStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
