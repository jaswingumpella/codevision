package com.codevision.codevisionbackend.api.generated;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Utility helpers referenced by the generated API interfaces.
 * This is normally emitted by the OpenAPI generator as a supporting file;
 * we keep a lightweight version under source control now that generation
 * targets the main sources directory.
 */
public final class ApiUtil {

    private ApiUtil() {}

    public static void setExampleResponse(NativeWebRequest nativeWebRequest, String mediaType, String example) {
        if (nativeWebRequest == null) {
            return;
        }
        HttpServletResponse response = nativeWebRequest.getNativeResponse(HttpServletResponse.class);
        if (response == null) {
            return;
        }
        response.setCharacterEncoding("UTF-8");
        response.setContentType(mediaType);
        try (PrintWriter writer = response.getWriter()) {
            writer.print(example);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write example response", e);
        }
    }
}

