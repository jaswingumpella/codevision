package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Exports a {@link KnowledgeGraph} as a PNG image by first rendering SVG
 * via {@link SvgExporter} and then transcoding to PNG using Apache Batik.
 */
@Component
public class PngExporter implements GraphExporter {

    private final SvgExporter svgExporter;

    public PngExporter(SvgExporter svgExporter) {
        this.svgExporter = svgExporter;
    }

    @Override
    public String formatName() {
        return "png";
    }

    @Override
    public String fileExtension() {
        return ".png";
    }

    @Override
    public String contentType() {
        return "image/png";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        var svgBytes = svgExporter.export(graph);
        // Batik does not support the SVG2 orient="auto-start-reverse" attribute;
        // downgrade it to the widely supported orient="auto".
        var svgString = new String(svgBytes, StandardCharsets.UTF_8)
                .replace("auto-start-reverse", "auto");
        var transcoder = new PNGTranscoder();
        // Disable external entity resolution for security - SVG is from our own SvgExporter
        transcoder.addTranscodingHint(PNGTranscoder.KEY_ALLOWED_SCRIPT_TYPES, "");
        transcoder.addTranscodingHint(PNGTranscoder.KEY_CONSTRAIN_SCRIPT_ORIGIN, Boolean.TRUE);
        var input = new TranscoderInput(new ByteArrayInputStream(
                svgString.getBytes(StandardCharsets.UTF_8)));
        var outputStream = new ByteArrayOutputStream();
        var output = new TranscoderOutput(outputStream);

        try {
            transcoder.transcode(input, output);
            outputStream.flush();
        } catch (TranscoderException e) {
            throw new UncheckedIOException(new IOException("Failed to transcode SVG to PNG", e));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return outputStream.toByteArray();
    }
}
