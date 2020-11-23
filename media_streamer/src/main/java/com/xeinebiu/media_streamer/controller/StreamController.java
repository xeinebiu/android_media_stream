package com.xeinebiu.media_streamer.controller;

import androidx.annotation.NonNull;

import com.xeinebiu.media_streamer.MediaStreamerService;
import com.xeinebiu.media_streamer.model.Stream;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PathVariable;
import com.yanzhenjie.andserver.annotation.RequestMapping;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.framework.body.StreamBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.util.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings({"WeakerAccess", "unused"})
@RestController
@RequestMapping(path = "/stream")
public class StreamController {

    @GetMapping(path = {"/video/{streamId}"})
    public void cast(
            @PathVariable("streamId") String id,
            HttpRequest request,
            HttpResponse response
    ) {
        String host = request.getHeader("Host");
        assert host != null;
        int port = Integer.parseInt(host.split(":")[1]);
        Stream streamItem = MediaStreamerService.Companion.findStream(id, port);
        if (streamItem == null) {
            return;
        }

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
