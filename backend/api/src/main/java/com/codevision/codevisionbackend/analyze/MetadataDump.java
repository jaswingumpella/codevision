package com.codevision.codevisionbackend.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetadataDump(
        List<OpenApiSpec> openApiSpecs,
        List<SpecDocument> wsdlDocuments,
        List<SpecDocument> xsdDocuments,
        List<SoapServiceSummary> soapServices) {

    public MetadataDump {
        openApiSpecs = openApiSpecs == null ? List.of() : List.copyOf(openApiSpecs);
        wsdlDocuments = wsdlDocuments == null ? List.of() : List.copyOf(wsdlDocuments);
        xsdDocuments = xsdDocuments == null ? List.of() : List.copyOf(xsdDocuments);
        soapServices = soapServices == null ? List.of() : List.copyOf(soapServices);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OpenApiSpec(String fileName, String content) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SpecDocument(String fileName, String content) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SoapServiceSummary(String fileName, String serviceName, List<SoapPortSummary> ports) {

        public SoapServiceSummary {
            ports = ports == null ? List.of() : List.copyOf(ports);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SoapPortSummary(String portName, List<String> operations) {

        public SoapPortSummary {
            operations = operations == null ? List.of() : List.copyOf(operations);
        }
    }

    public static MetadataDump empty() {
        return new MetadataDump(List.of(), List.of(), List.of(), List.of());
    }
}
