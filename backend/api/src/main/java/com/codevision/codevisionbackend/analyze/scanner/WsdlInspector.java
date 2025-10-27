package com.codevision.codevisionbackend.analyze.scanner;

import com.codevision.codevisionbackend.analyze.MetadataDump;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class WsdlInspector {

    private static final Logger log = LoggerFactory.getLogger(WsdlInspector.class);
    private final DocumentBuilderFactory documentBuilderFactory;

    public WsdlInspector() {
        this.documentBuilderFactory = createFactory();
    }

    public List<MetadataDump.SoapServiceSummary> inspect(String content, String fileName) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            document.getDocumentElement().normalize();
            return extractSummaries(document, fileName);
        } catch (Exception ex) {
            log.debug("Failed to inspect WSDL {}: {}", fileName, ex.getMessage());
            return List.of();
        }
    }

    private List<MetadataDump.SoapServiceSummary> extractSummaries(Document document, String fileName) {
        Map<String, List<String>> portTypeOperations = extractPortTypeOperations(document);
        Map<String, BindingDescriptor> bindings = extractBindings(document, portTypeOperations);

        List<MetadataDump.SoapServiceSummary> summaries = new ArrayList<>();
        NodeList serviceNodes = document.getElementsByTagNameNS("*", "service");
        if (serviceNodes != null && serviceNodes.getLength() > 0) {
            for (int i = 0; i < serviceNodes.getLength(); i++) {
                Node node = serviceNodes.item(i);
                if (!(node instanceof Element serviceElement)) {
                    continue;
                }
                String serviceName = safeAttr(serviceElement, "name", fileName);
                List<MetadataDump.SoapPortSummary> portSummaries =
                        extractPortSummaries(serviceElement, bindings);
                summaries.add(new MetadataDump.SoapServiceSummary(fileName, serviceName, portSummaries));
            }
            return summaries;
        }

        if (!bindings.isEmpty()) {
            // No explicit service element; synthesize a summary based on bindings.
            Set<String> portNames = new HashSet<>();
            List<MetadataDump.SoapPortSummary> portSummaries = bindings.values().stream()
                    .map(binding -> {
                        String portName = binding.name();
                        if (!portNames.add(portName)) {
                            portName = portName + "_" + portNames.size();
                        }
                        return new MetadataDump.SoapPortSummary(portName, binding.operations());
                    })
                    .toList();
            summaries.add(new MetadataDump.SoapServiceSummary(fileName, deriveFallbackServiceName(document, fileName), portSummaries));
        }

        return summaries;
    }

    private List<MetadataDump.SoapPortSummary> extractPortSummaries(
            Element serviceElement, Map<String, BindingDescriptor> bindings) {
        NodeList portNodes = serviceElement.getElementsByTagNameNS("*", "port");
        if (portNodes == null || portNodes.getLength() == 0) {
            return List.of();
        }
        List<MetadataDump.SoapPortSummary> ports = new ArrayList<>();
        for (int idx = 0; idx < portNodes.getLength(); idx++) {
            Node node = portNodes.item(idx);
            if (!(node instanceof Element portElement)) {
                continue;
            }
            String portName = safeAttr(portElement, "name", "port-" + idx);
            String bindingRef = stripPrefix(safeAttr(portElement, "binding", ""));
            BindingDescriptor binding = bindings.get(bindingRef);
            List<String> operations = binding != null ? binding.operations() : List.of();
            ports.add(new MetadataDump.SoapPortSummary(portName, operations));
        }
        return ports;
    }

    private Map<String, BindingDescriptor> extractBindings(
            Document document, Map<String, List<String>> portTypeOperations) {
        Map<String, BindingDescriptor> bindings = new HashMap<>();
        NodeList bindingNodes = document.getElementsByTagNameNS("*", "binding");
        if (bindingNodes == null) {
            return bindings;
        }
        for (int i = 0; i < bindingNodes.getLength(); i++) {
            Node node = bindingNodes.item(i);
            if (!(node instanceof Element bindingElement)) {
                continue;
            }
            String bindingName = safeAttr(bindingElement, "name", "binding-" + i);
            String portTypeName = stripPrefix(safeAttr(bindingElement, "type", ""));

            List<String> operations = extractBindingOperations(bindingElement);
            if (operations.isEmpty() && portTypeOperations.containsKey(portTypeName)) {
                operations = portTypeOperations.get(portTypeName);
            }
            bindings.put(bindingName, new BindingDescriptor(bindingName, portTypeName, operations));
        }
        return bindings;
    }

    private Map<String, List<String>> extractPortTypeOperations(Document document) {
        Map<String, List<String>> portTypeOperations = new HashMap<>();
        NodeList portTypeNodes = document.getElementsByTagNameNS("*", "portType");
        if (portTypeNodes == null) {
            return portTypeOperations;
        }

        for (int i = 0; i < portTypeNodes.getLength(); i++) {
            Node node = portTypeNodes.item(i);
            if (!(node instanceof Element portTypeElement)) {
                continue;
            }
            String portTypeName = safeAttr(portTypeElement, "name", "portType-" + i);
            List<String> operations = extractOperationNames(portTypeElement.getChildNodes());
            if (!operations.isEmpty()) {
                portTypeOperations.put(portTypeName, operations);
            }
        }
        return portTypeOperations;
    }

    private List<String> extractBindingOperations(Element bindingElement) {
        return extractOperationNames(bindingElement.getChildNodes());
    }

    private List<String> extractOperationNames(NodeList nodes) {
        if (nodes == null) {
            return List.of();
        }
        List<String> operations = new ArrayList<>();
        for (int j = 0; j < nodes.getLength(); j++) {
            Node node = nodes.item(j);
            if (!(node instanceof Element element)) {
                continue;
            }
            String localName = localName(element);
            if (!"operation".equals(localName)) {
                continue;
            }
            String operationName = safeAttr(element, "name", "operation-" + j);
            operations.add(operationName);
        }
        return operations;
    }

    private String deriveFallbackServiceName(Document document, String fileName) {
        Element root = document.getDocumentElement();
        if (root != null) {
            String attr = root.getAttribute("name");
            if (attr != null && !attr.isBlank()) {
                return attr;
            }
        }
        return fileName;
    }

    private String safeAttr(Element element, String attribute, String defaultValue) {
        if (element == null) {
            return defaultValue;
        }
        String value = element.getAttribute(attribute);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String stripPrefix(String value) {
        if (value == null) {
            return null;
        }
        int idx = value.indexOf(':');
        if (idx >= 0 && idx + 1 < value.length()) {
            return value.substring(idx + 1);
        }
        return value;
    }

    private String localName(Node node) {
        String localName = node.getLocalName();
        if (localName != null && !localName.isBlank()) {
            return localName;
        }
        String name = node.getNodeName();
        if (name == null) {
            return "";
        }
        int idx = name.indexOf(':');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }

    private DocumentBuilderFactory createFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ex) {
            log.debug("Failed to configure secure XML parsing: {}", ex.getMessage());
        }
        return factory;
    }

    private static final class BindingDescriptor {

        private final String name;
        private final String portType;
        private final List<String> operations;

        BindingDescriptor(String name, String portType, List<String> operations) {
            this.name = name;
            this.portType = portType;
            this.operations = operations == null
                    ? List.of()
                    : Collections.unmodifiableList(new ArrayList<>(operations));
        }

        String name() {
            return name;
        }

        String portType() {
            return portType;
        }

        List<String> operations() {
            return operations;
        }
    }
}
