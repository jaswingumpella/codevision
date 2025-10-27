Diagram Source Templates (PlantUML & Mermaid)

Below are simplified templates/examples showing how CodeDocGen represents various diagrams in PlantUML and Mermaid format. These templates illustrate the structure and syntax used for each diagram type.

Class Diagram: Shows classes with relationships (inheritance, associations).
PlantUML (Class Diagram):

@startuml
class OrderService <<Service>> {
  - OrderRepository repo  // member field
  + createOrder(order: Order)
}
class OrderRepository <<Repository>>
OrderService --> OrderRepository : uses  // association (has a repo)
class BaseService
OrderService --|> BaseService  // inheritance (extends BaseService)
@enduml


Mermaid (Class Diagram):

classDiagram
    class OrderService {
        <<Service>>
        OrderRepository repo
        createOrder(Order)
    }
    class OrderRepository {
        <<Repository>>
    }
    OrderService --> OrderRepository : uses
    class BaseService
    OrderService --|> BaseService


(In these, <<Service>> and <<Repository>> are stereotypes indicating the class type, and we use appropriate UML arrows for associations (-->) and inheritance (--|>).)

Component Diagram: High-level view of components/modules and their interactions.
PlantUML (Component Diagram):

@startuml
[Browser] --> ControllerComponent : HTTP Request
ControllerComponent --> ServiceComponent : calls
ServiceComponent --> RepositoryComponent : uses
RepositoryComponent --> Database : JDBC/ORM
ServiceComponent --> ExternalAPI : REST call
@enduml


(Here, [Browser] and Database are depicted as external actors. ControllerComponent, ServiceComponent, RepositoryComponent are abstractions grouping classes; they could also be labeled with package or stereotype names. Arrows indicate invocation or dependency flows.)
Mermaid (using Flowchart for Component Diagram):

flowchart LR
    Browser --> Controller[[Controller Layer]]
    Controller --> Service[[Service Layer]]
    Service --> Repository[[Repository Layer]]
    Repository --> DB[(Database)]
    Service --> ExternalAPI[[External API]]


(Mermaid’s flowchart is used to illustrate components: we use double brackets for components and parentheses for the database. Arrows show direction of interaction.)

Use Case Diagram: Actors and use cases (system functionality).
PlantUML (Use Case Diagram):

@startuml
actor User
actor "Payment System" as ExternalSystem
User -> (Place Order)
User -> (View Order Status)
ExternalSystem -> (Place Order) : triggers via SOAP
@enduml


(This shows a User actor executing two use cases, and an external system (perhaps a partner system) also triggering the “Place Order” use case via a SOAP interface. PlantUML uses (Use Case) for use cases and actor name for actors.)
Mermaid: (Mermaid doesn’t have a native use case diagram, but we could approximate it or omit Mermaid for this type. If needed, a flowchart or sequence could illustrate the interactions. For brevity, we might not generate a Mermaid use case diagram, or we represent it textually.)

Entity-Relationship Diagram (ERD): Tables (entities) and relationships.
PlantUML (ERD/Class style):

@startuml
entity Order {
  * orderId : Long
  -- 
  customerId : Long
  status : String
}
entity Customer {
  * customerId : Long
  name : String
}
Order }o--|| Customer : "many-to-one"
@enduml


(Using PlantUML’s entity stereotype to list fields. Primary keys are prefixed with *. The relationship is shown with crow’s foot: Order to Customer is many-to-one (}o--|| syntax).)
Mermaid (ER Diagram):

erDiagram
    Order {
        int order_id PK
        int customer_id FK
        string status
    }
    Customer {
        int customer_id PK
        string name
    }
    Order }o--|| Customer : places


(Mermaid’s erDiagram uses a similar notation; PK/FK are marked after field types. Relationship is }o--|| with a label "places" (optional textual label for relationship).)

Sequence Diagram: Dynamic call flow between objects/actors over time.
PlantUML (Sequence Diagram):

@startuml
participant User
participant OrderController
participant OrderService
participant OrderRepository
User -> OrderController: createOrder(request)
activate OrderController
OrderController -> OrderService: validateOrder(request)
activate OrderService
OrderService -> OrderRepository: save(order)
activate OrderRepository
OrderRepository --> OrderRepository: insert into DB
deactivate OrderRepository
OrderService -> EmailService: sendConfirmation(order)
activate EmailService
EmailService -> ExternalSMTP: sendEmail()
deactivate EmailService
deactivate OrderService
OrderController <-- OrderService: (return)
deactivate OrderController
@enduml


(This shows a user calling the controller, which calls service, which calls repository, does a DB insert, then service calls an EmailService which calls an external SMTP service. Notes: activate/deactivate manage lifelines activation bar. --> to self indicates an internal operation like DB action. The sequence ends with returns.)
Mermaid (Sequence Diagram):

sequenceDiagram
    participant User
    participant OrderController
    participant OrderService
    participant OrderRepository
    User->>OrderController: createOrder(request)
    OrderController->>OrderService: validateOrder()
    OrderService->>OrderRepository: save(order)
    OrderRepository-->>OrderRepository: (write to DB)
    OrderService-->>EmailService: sendConfirmation()
    EmailService-->>ExternalSMTP: sendEmail()
    OrderController<<--OrderService: response


(Mermaid uses a similar participant list and arrow syntax. Note that ->> is used for normal calls and -->> for responses or internal calls in this context. Mermaid auto-deactivates lifelines, so explicit deactivate is not needed.)

These examples demonstrate the general format. In the actual output, class and method names will be specific to the project, and there may be many more elements. Also, the tool will ensure any special characters are escaped (especially in PlantUML). The diagrams are kept relatively simple and cycle-free: if a cycle is detected, PlantUML will show a note or a self-call indicating the cycle rather than infinitely expanding. Both PlantUML and Mermaid notations are provided for each diagram type so users can choose their preferred format or use whichever integrates better with their documentation pipeline.