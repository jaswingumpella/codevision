GitLab Duo-Compatible Metadata JSON Schema

Below is the structure of the ParsedDataResponse JSON – a single JSON object that encapsulates all code insights. This format is designed to be easily ingestible by external tools and AI assistants (like GitLab Duo). It includes high-level project info and various sections for detailed data. (Note: Strings like "..." indicate example or omitted content for brevity.)

{
  "projectId": 123,
  "projectName": "my-java-service",
  "repoUrl": "https://github.com/org/my-java-service.git",
  "analyzedAt": "2025-10-27T02:40:28.123Z",
  "buildInfo": {
    "groupId": "com.myorg",
    "artifactId": "my-java-service",
    "version": "1.0.0-SNAPSHOT",
    "javaVersion": "17"
  },
  "classes": [
    {
      "fullyQualifiedName": "com.myorg.service.OrderService",
      "packageName": "com.myorg.service",
      "stereotype": "SERVICE",
      "sourceSet": "MAIN",
      "userCode": true,
      "annotations": ["Service"],
      "implementedInterfaces": ["com.myorg.api.OrderOperations"]
    },
    {
      "fullyQualifiedName": "com.myorg.controller.OrderController",
      "packageName": "com.myorg.controller",
      "stereotype": "CONTROLLER",
      "sourceSet": "MAIN",
      "userCode": true,
      "annotations": ["RestController", "RequestMapping(/api/orders)"],
      "implementedInterfaces": []
    },
    {
      "fullyQualifiedName": "org.springframework.util.StringUtils",
      "packageName": "org.springframework.util",
      "stereotype": "OTHER",
      "sourceSet": "EXTERNAL",
      "userCode": false,
      "annotations": [],
      "implementedInterfaces": []
    }
    // ... more class entries for all classes in the project (and selected libs)
  ],
  "apiEndpoints": [
    {
      "protocol": "REST",
      "httpMethod": "POST",
      "path": "/api/orders",
      "controllerClass": "com.myorg.controller.OrderController",
      "controllerMethod": "createOrder(OrderDto)"
    },
    {
      "protocol": "REST",
      "httpMethod": "GET",
      "path": "/api/orders/{id}",
      "controllerClass": "com.myorg.controller.OrderController",
      "controllerMethod": "getOrder(String)"
    },
    {
      "protocol": "SOAP",
      "httpMethod": null,
      "path": "placeOrder",  // SOAP operation name
      "controllerClass": "com.myorg.soap.OrderEndpoint",
      "controllerMethod": "placeOrder(OrderRequest)"
    }
    // ... (including any servlet or JAX-RS endpoints if present)
  ],
  "dbAnalysis": {
    "classesByEntity": {
      "OrderEntity": ["com.myorg.repo.OrderRepository", "com.myorg.dao.LegacyOrderDao"],
      "CustomerEntity": ["com.myorg.repo.CustomerRepository"]
      // ...
    },
    "operationsByClass": {
      "com.myorg.repo.OrderRepository": [
        { "methodName": "findById", "operationType": "SELECT", "entityOrTable": "OrderEntity", "rawQuerySnippet": null },
        { "methodName": "save", "operationType": "INSERT", "entityOrTable": "OrderEntity", "rawQuerySnippet": null }
      ],
      "com.myorg.dao.LegacyOrderDao": [
        { "methodName": "getAllOrders", "operationType": "SELECT", "entityOrTable": "OrderEntity", "rawQuerySnippet": "SELECT * FROM orders" }
      ]
      // ...
    }
  },
  "loggerInsights": [
    {
      "className": "com.myorg.service.PaymentService",
      "logLevel": "INFO",
      "lineNumber": 42,
      "messageTemplate": "Processing payment for order {}",
      "variables": ["orderId"],
      "piiRisk": false,
      "pciRisk": false
    },
    {
      "className": "com.myorg.service.PaymentService",
      "logLevel": "ERROR",
      "lineNumber": 87,
      "messageTemplate": "Card number {} is invalid",
      "variables": ["cardNumber"],
      "piiRisk": false,
      "pciRisk": true
    }
    // ... all log statements found
  ],
  "piiPciFindings": [
    {
      "filePath": "src/main/resources/application.yml",
      "lineNumber": 12,
      "snippet": "authToken: sk_test_51H8***",
      "matchType": "PCI",
      "severity": "HIGH",
      "ignored": false
    },
    {
      "filePath": "src/main/java/com/myorg/service/UserService.java",
      "lineNumber": 55,
      "snippet": "logger.info(\"User SSN: \" + userSsn)",
      "matchType": "PII",
      "severity": "HIGH",
      "ignored": false
    }
    // ... all regex pattern matches in files
  ],
  "gherkinFeatures": [
    {
      "featureFile": "src/test/resources/features/order_processing.feature",
      "featureTitle": "Order Processing",
      "scenarios": [
        "Successful order placement",
        "Order failure with invalid payment"
      ]
    }
    // ... additional feature files
  ],
  "callFlows": {
    // Graph of method calls (adjacency list mapping callers to callees):
    "com.myorg.controller.OrderController.createOrder(OrderDto)": [
      "com.myorg.service.OrderService.validateOrder(OrderDto)",
      "com.myorg.service.OrderService.saveOrder(OrderDto)"
    ],
    "com.myorg.service.OrderService.saveOrder(OrderDto)": [
      "com.myorg.repo.OrderRepository.save(OrderEntity)",
      "com.myorg.service.EmailService.sendOrderConfirmation(Order)"
    ]
    // ... and so on for key methods (could include external calls too)
  },
  "diagrams": [
    {
      "diagramType": "CLASS",
      "plantUmlSource": "@startuml\nclass OrderService <<Service>> {\n  +validateOrder()\n  +saveOrder()\n}\nclass OrderRepository <<Repository>>\nOrderService --> OrderRepository : uses\n@enduml",
      "mermaidSource": "classDiagram\n    class OrderService\n    OrderService : <<Service>>\n    class OrderRepository\n    OrderRepository : <<Repository>>\n    OrderService --> OrderRepository : uses"
    },
    {
      "diagramType": "COMPONENT",
      "plantUmlSource": "@startuml\n[User] -> (OrderController)\n(OrderController) -> (OrderService)\n(OrderService) -> (OrderRepository)\n(OrderRepository) -> (Database)\n@enduml",
      "mermaidSource": "flowchart LR\n    User-->OrderController\n    OrderController-->OrderService\n    OrderService-->OrderRepository\n    OrderRepository-->DB[(Database)]"
    },
    {
      "diagramType": "SEQUENCE",
      "plantUmlSource": "@startuml\nactor User\nUser -> OrderController: createOrder\nactivate OrderController\nOrderController -> OrderService: validateOrder\nactivate OrderService\nOrderService -> OrderRepository: save\nactivate OrderRepository\nOrderRepository --> OrderRepository: (DB insert)\ndeactivate OrderRepository\nOrderService -> EmailService: sendConfirmation\nactivate EmailService\nEmailService -> ExternalSMTP: sendEmail\n... (cyclic or external calls if any) ...\n@enduml",
      "mermaidSource": "sequenceDiagram\n    participant User\n    participant OrderController\n    participant OrderService\n    participant OrderRepository\n    User->>OrderController: createOrder\n    OrderController-->>OrderService: validateOrder\n    OrderService-->>OrderRepository: save()\n    OrderRepository-->>OrderRepository: (write to DB)\n    OrderService-->>EmailService: sendConfirmation\n    EmailService-->>ExternalSMTP: sendEmail"
    }
    // ... Use Case and ERD diagrams similarly
  ],
  "metadataDump": {
    "openApiSpecs": [
      { "fileName": "openapi-order.yaml", "content": "openapi: '3.0.0'\ninfo: {...}\npaths: {...}" }
    ],
    "wsdlFiles": [
      { "fileName": "legacy-service.wsdl", "content": "<?xml version=\"1.0\"?>\n<definitions name=\"LegacyOrderService\">...</definitions>" }
    ],
    "applicationConfigs": [
      { "fileName": "application.yml", "content": "server:\n  port: 8080\n..." }
    ]
  }
}


Notes on Schema: This JSON structure provides a complete snapshot of the project analysis:

The top-level keys like classes, apiEndpoints, etc., correspond to major sections of the analysis.

In classes[], each entry represents a class with key metadata. (If needed, additional fields like extendsClass could be added to denote inheritance, but those can also be deduced from relationships or annotations.)

apiEndpoints[] covers all detected API entry points and their metadata.

dbAnalysis contains two nested maps summarizing data access.

loggerInsights[] lists every log statement (with risk flags).

piiPciFindings[] lists every sensitive data occurrence found.

gherkinFeatures[] outlines BDD scenarios.

callFlows is presented as an adjacency list of call relations (it could also be an array of edges or some other representation; here it’s shown as a mapping from caller to callees for clarity).

diagrams[] provides the source of each generated diagram. The actual content strings have newlines and UML syntax which are shown here in condensed form for example. (In real JSON, these would be escaped strings or perhaps we would omit them to reduce size, relying on diagram table for storage. But for a GitLab Duo context, including them could allow an AI to regenerate or modify diagrams if needed.)

metadataDump holds raw text of important files like OpenAPI specs, WSDLs, configs, etc., that might be relevant for deep analysis or regenerating documentation. This ensures the assistant has access to those specifications if needed for answers.

This JSON schema is GitLab Duo-compatible in the sense that it’s a single self-contained object that an AI assistant can consume to answer questions about the codebase. It’s organized logically by topic, and the naming is human-readable. An external tool could parse this JSON to, for example, populate a knowledge base or respond to queries (e.g., “List all REST endpoints and their controllers” or “Show me any logs that contain credit card numbers” can be answered by traversing this JSON).