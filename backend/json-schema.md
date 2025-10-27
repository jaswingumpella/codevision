GitLab Duo-Compatible Metadata JSON Schema
==========================================

The backend serialises every analysis into a single `ParsedDataResponse` JSON document. Downstream tools (GitLab Duo, Confluence exports, custom dashboards) can rely on this snapshot without touching the relational tables. The example below is intentionally abbreviated—replace `"..."` with the project-specific values or truncate large arrays as needed.

```json
{
  "projectId": 123,
  "projectName": "my-java-service",
  "repoUrl": "https://github.com/org/my-java-service.git",
  "analyzedAt": "2025-10-27T02:40:28.123Z",
  "buildInfo": {
    "groupId": "com.myorg",
    "artifactId": "my-java-service",
    "version": "1.0.0-SNAPSHOT",
    "javaVersion": "21"
  },
  "classes": [
    {
      "fullyQualifiedName": "com.myorg.service.OrderService",
      "packageName": "com.myorg.service",
      "className": "OrderService",
      "stereotype": "SERVICE",
      "sourceSet": "MAIN",
      "userCode": true,
      "relativePath": "src/main/java/com/myorg/service/OrderService.java",
      "annotations": ["Service"],
      "interfacesImplemented": ["com.myorg.api.OrderOperations"]
    }
    // ...all other discovered classes (main + test)
  ],
  "metadataDump": {
    "openApiSpecs": [
      { "fileName": "openapi-orders.yaml", "content": "openapi: 3.0.0\ninfo: {...}\npaths: {...}" }
    ],
    "wsdlDocuments": [
      { "fileName": "legacy-order.wsdl", "content": "<?xml version=\"1.0\"?><definitions>...</definitions>" }
    ],
    "xsdDocuments": [],
    "soapServices": [
      {
        "fileName": "legacy-order.wsdl",
        "serviceName": "LegacyOrderService",
        "ports": [
          { "portName": "LegacyOrderPort", "operations": ["placeOrder", "cancelOrder"] }
        ]
      }
    ]
  },
  "apiEndpoints": [
    {
      "protocol": "REST",
      "httpMethod": "POST",
      "pathOrOperation": "/api/orders",
      "controllerClass": "com.myorg.controller.OrderController",
      "controllerMethod": "createOrder(OrderDto)",
      "specArtifacts": [
        { "type": "OPENAPI", "name": "Orders API", "reference": "openapi-orders.yaml" }
      ]
    },
    {
      "protocol": "SOAP",
      "httpMethod": null,
      "pathOrOperation": "placeOrder",
      "controllerClass": "com.myorg.soap.LegacyOrderEndpoint",
      "controllerMethod": "placeOrder(OrderRequest)",
      "specArtifacts": [
        { "type": "WSDL", "name": "LegacyOrderService", "reference": "legacy-order.wsdl" }
      ]
    }
    // ...servlets/JAX-RS resources follow the same shape
  ],
  "dbAnalysis": {
    "entities": [
      {
        "entityName": "Order",
        "fullyQualifiedName": "com.myorg.persistence.Order",
        "tableName": "orders",
        "primaryKeys": ["id"],
        "fields": [
          { "name": "id", "type": "Long", "columnName": "order_id" },
          { "name": "createdAt", "type": "OffsetDateTime", "columnName": "created_at" }
        ],
        "relationships": [
          { "fieldName": "customer", "targetType": "Customer", "relationshipType": "MANY_TO_ONE" }
        ]
      }
    ],
    "classesByEntity": {
      "Order": ["com.myorg.repo.OrderRepository"]
    },
    "operationsByClass": {
      "com.myorg.repo.OrderRepository": [
        {
          "methodName": "findById",
          "operationType": "SELECT",
          "target": "Order [orders]",
          "querySnippet": null
        },
        {
          "methodName": "findByStatus",
          "operationType": "SELECT",
          "target": "Order [orders]",
          "querySnippet": "select o from Order o where o.status = :status"
        }
      ]
    }
  },
  "assets": {
    "images": [
      {
        "fileName": "architecture.png",
        "relativePath": "docs/architecture/architecture.png",
        "sizeBytes": 24576,
        "sha256": "3e9c8b..."
      }
    ]
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
    }
  ],
  "piiPciScan": [
    {
      "filePath": "src/main/resources/application.yml",
      "lineNumber": 12,
      "snippet": "authToken: sk_test_****",
      "matchType": "PCI",
      "severity": "HIGH",
      "ignored": false
    }
  ],
  "gherkinFeatures": [
    {
      "featureFile": "src/test/resources/features/order_processing.feature",
      "featureTitle": "Order Processing",
      "scenarios": ["Successful order placement"]
    }
  ],
  "callFlows": {
    "com.myorg.controller.OrderController.createOrder(OrderDto)": [
      "com.myorg.service.OrderService.validateOrder(OrderDto)",
      "com.myorg.service.OrderService.saveOrder(OrderDto)"
    ]
  },
  "diagrams": [
    {
      "diagramType": "CLASS",
      "plantUmlSource": "@startuml\nclass OrderService <<Service>> {\n  +validateOrder()\n}\n@enduml",
      "mermaidSource": "classDiagram\n    class OrderService"
    }
  ]
}
```

### Field Reference

- **Top-level metadata** – `projectId`, `projectName`, `repoUrl`, and `analyzedAt` identify the snapshot. `buildInfo` surfaces Maven coordinates and Java release.
- **`classes`** – complete Java inventory with stereotype heuristics, source set, relative path, annotations, and implemented interfaces.
- **`metadataDump`** – verbatim config artifacts (OpenAPI, WSDL, XSD) plus synthesized SOAP service summaries. These strings can be fed directly into documentation tooling.
- **`apiEndpoints`** – unified REST/SOAP/legacy catalog. `pathOrOperation` stores the HTTP path or SOAP operation. `specArtifacts` references the supporting documents captured in `metadataDump`.
- **`dbAnalysis`** – the new database view:
  - `entities` describes each JPA entity/table (primary keys, fields/columns, relationship hints).
  - `classesByEntity` maps entities to the repository/DAO classes that touch them.
  - `operationsByClass` lists repository methods with inferred CRUD intent and any inline query text.
- **`assets`** – pointer to image assets (diagrams, screenshots) so exports can embed them.
- **`loggerInsights` / `piiPciScan`** – reserved for iterations 5+ (logging + sensitive-data scanning). They remain arrays for tooling compatibility.
- **`callFlows` & `diagrams`** – support diagram generation and sequence visualisation.

Treat the JSON snapshot as the canonical contract between the ingestion pipeline and every consumer. The REST endpoints (`/project/{id}/overview`, `/project/{id}/api-endpoints`, `/project/{id}/db-analysis`, etc.) simply return slices of this document for UI convenience.
