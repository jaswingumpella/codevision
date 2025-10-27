package com.codevision.codevisionbackend.analyze.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codevision.codevisionbackend.analyze.MetadataDump;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlScannerTest {

    private final YamlScanner yamlScanner = new YamlScanner(new WsdlInspector());

    @Test
    void scanCapturesOpenApiSpecifications(@TempDir Path repoRoot) throws Exception {
        Files.writeString(
                repoRoot.resolve("openapi-spec.yml"),
                """
                openapi: 3.0.0
                info:
                  title: Demo
                  version: 1.0.0
                """);

        Files.writeString(
                repoRoot.resolve("application.yml"),
                "spring:\\n  application:\\n    name: demo");

        MetadataDump dump = yamlScanner.scan(repoRoot);

        assertEquals(1, dump.openApiSpecs().size());
        MetadataDump.OpenApiSpec spec = dump.openApiSpecs().get(0);
        assertEquals("openapi-spec.yml", spec.fileName());
        assertTrue(spec.content().contains("openapi: 3.0.0"));
    }

    @Test
    void scanCapturesWsdlArtifacts(@TempDir Path repoRoot) throws Exception {
        Files.writeString(
                repoRoot.resolve("DemoService.wsdl"),
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://schemas.xmlsoap.org/wsdl/"
                             xmlns:tns="http://example.com/demo"
                             name="DemoService">
                  <portType name="DemoPortType">
                    <operation name="sayHello"/>
                  </portType>
                  <binding name="DemoBinding" type="tns:DemoPortType">
                    <operation name="sayHello"/>
                  </binding>
                  <service name="DemoService">
                    <port name="DemoPort" binding="tns:DemoBinding">
                      <address location="http://localhost/"/>
                    </port>
                  </service>
                </definitions>
                """);

        MetadataDump dump = yamlScanner.scan(repoRoot);

        assertEquals(1, dump.wsdlDocuments().size());
        assertEquals("DemoService.wsdl", dump.wsdlDocuments().get(0).fileName());
        assertEquals(1, dump.soapServices().size());
        MetadataDump.SoapServiceSummary summary = dump.soapServices().get(0);
        assertEquals("DemoService", summary.serviceName());
        assertEquals(1, summary.ports().size());
        MetadataDump.SoapPortSummary portSummary = summary.ports().get(0);
        assertEquals("DemoPort", portSummary.portName());
        assertTrue(portSummary.operations().contains("sayHello"));
    }
}
