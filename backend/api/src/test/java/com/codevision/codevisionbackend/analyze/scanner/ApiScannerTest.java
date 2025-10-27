package com.codevision.codevisionbackend.analyze.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.codevision.codevisionbackend.analyze.MetadataDump;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiScannerTest {

    private final ApiScanner apiScanner = new ApiScanner();

    @Test
    void detectsJaxWsWebServiceMethods(@TempDir Path repoRoot) throws Exception {
        Path module = repoRoot.resolve("service-module");
        Path sourceDir = module.resolve("src/main/java/example/ws");
        Files.createDirectories(sourceDir);
        Path javaFile = sourceDir.resolve("GreetingEndpoint.java");
        Files.writeString(
                javaFile,
                """
                package example.ws;

                import jakarta.jws.WebMethod;
                import jakarta.jws.WebService;

                @WebService(serviceName = \"GreetingService\")
                public class GreetingEndpoint {

                    @WebMethod
                    public String sayHello(String name) {
                        return \"Hello \" + name;
                    }
                }
                """);

        List<ApiEndpointRecord> endpoints =
                apiScanner.scan(repoRoot, List.of(module), MetadataDump.empty());

        assertFalse(endpoints.isEmpty(), "Expected SOAP endpoints to be discovered");
        ApiEndpointRecord endpoint = endpoints.get(0);
        assertEquals("SOAP", endpoint.protocol());
        assertEquals("sayHello", endpoint.pathOrOperation());
        assertEquals("example.ws.GreetingEndpoint", endpoint.controllerClass());
    }
}
