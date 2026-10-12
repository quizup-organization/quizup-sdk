# AGENTS.md — quizup-sdk

> **SDK Maven** : BOM parent + starters Spring Boot (microservice, Axon). Repo **library** (ordre
> de build 1) — doit être installé **avant** tous les services. Pas de code applicatif : c'est une
> bibliothèque d'auto-configuration et de types partagés.
> Pour les règles de patterns : [
`../../best-practices/.backend/folder-structure.md`](../../best-practices/.backend/folder-structure.md).

---

## 1. Rôle

Fournir à tous les microservices QuizUp :

- les **versions** de dépendances (Spring Boot, Axon, Java, Lombok) via le BOM parent
- une **auto-configuration Spring Boot** (CORS, Swagger, Security, WebSocket, Actuator,
  **Observabilité** Prometheus/Micrometer, PasswordEncoder, exception handler)
- des **types domaine partagés** (`microservice-core`) : exceptions (`BaseProblem`,
  `ProblemCategory`), DTO de recherche/transport (`SearchRequest`, `FilterRequest`,
  `SortRequest`, `PageRequest` → `SearchResponse<T>`), etc.
- un **starter Axon distribué** (RabbitMQ, deadlines, distributed query)

**Package racine** : `io.github.quizup`

---

## 2. Structure Maven (3 modules)

| Module                | Package                         | Rôle                                                                                                                     |
|-----------------------|---------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `quizup-parent`       | —                               | **BOM Maven parent** (versions Spring Boot 3.5, Axon 4.13, Java 25, Lombok…). Gère aussi les versions des artifacts SDK. |
| `quizup-microservice` | `io.github.quizup.microservice` | Starter Spring Boot : CORS, Swagger, Security, WebSocket, Actuator, PasswordEncoder, Exception handler.                  |
| `quizup-axon`         | `io.github.quizup.axon`         | Starter Axon distribué : RabbitMQ, deadlines, distributed query.                                                         |

### Sous-modules de `quizup-microservice`

- `quizup-microservice-core` — **types domaine partagés** (`io.github.quizup.microservice.core.domain.*`)
- `quizup-microservice-autoconfigure` — **auto-configurations Spring** (`io.github.quizup.microservice.autoconfigure.*`)
- `quizup-microservice-starter` — **point d'entrée** : apporte autoconfigure + `quizup-axon-starter` + starters Spring

### Sous-modules de `quizup-axon`

- `quizup-axon-starter` — point d'entrée
- `quizup-axon-autoconfigure` — auto-configurations
- `quizup-axon-deadline` — deadlines distribues
- `quizup-axon-query` — distributed query

---

## 3. Dépendances à déclarer dans les services

```xml
<!-- versions gérées par le BOM quizup-parent, SANS tag de version -->
<dependency>
    <groupId>io.github.quizup</groupId>
    <artifactId>quizup-microservice-core</artifactId>
</dependency>
        <!-- dans quizup-{service}-domain -->

<dependency>
<groupId>io.github.quizup</groupId>
<artifactId>quizup-microservice-starter</artifactId>
</dependency>
        <!-- dans quizup-{service}-infrastructure -->
```

---

## 4. Auto-configurations (microservice)

Préfixe de contrôle : `microservice:` (classe `MicroserviceProperties`).

| Auto-configuration                     | Propriété                                | Rôle                                                                                                       |
|----------------------------------------|------------------------------------------|------------------------------------------------------------------------------------------------------------|
| `CorsAutoConfiguration`                | `microservice.cors.enabled`              | CORS configurable                                                                                          |
| `SwaggerAutoConfiguration`             | `microservice.swagger.enabled`           | OpenAPI / Swagger UI                                                                                       |
| `SwaggerRootRedirectAutoConfiguration` | —                                        | Redirection racine → Swagger                                                                               |
| `ExceptionAutoConfiguration`           | `microservice.exception-handler.enabled` | Handler global + intercepteurs Axon (`ProblemCommandHandlerInterceptor`, `ProblemQueryHandlerInterceptor`) |
| `ResourceServerAutoConfiguration`      | `microservice.resource-server.enabled`   | OAuth2 JWT (issuer + JWK)                                                                                  |
| `WebSocketAutoConfiguration`           | `microservice.websocket.enabled`         | STOMP / SockJS + auth JWT de la trame `CONNECT` (`microservice.websocket.require-auth`, défaut `false`) + heartbeats broker (`heartbeat-outgoing`/`heartbeat-incoming`, 10 s, scheduler du broker) |
| `ActuatorAutoConfiguration`            | `microservice.actuator.enabled`          | Spring Boot Actuator                                                                                       |
| `ObservabilityAutoConfiguration`       | `microservice.observability.enabled`     | Tags communs des métriques Micrometer (`application`, `environment`, `version`) + registre Prometheus     |
| `PasswordEncoderAutoConfiguration`     | —                                        | Bean `BCryptPasswordEncoder`                                                                               |
| `MicroserviceAutoConfiguration`        | —                                        | Configuration de base                                                                                      |

> **Important** : le bean `RestTemplate` de `ResourceServerAutoConfiguration` est annoté
> **`@Primary`** (intercepteur OAuth2 avec le client `server-client`). C'est **obligatoire**
> pour que le **distributed command bus** d'Axon Spring Cloud
> (`org.axonframework.extensions.springcloud.autoconfig.SpringCloudAutoConfiguration`)
> utilise ce bean **au lieu** de son fallback `@ConditionalOnMissingBean`
> (`new RestTemplate()` sans token). Sans `@Primary`, le distributed command bus ne peut pas
> authentifier ses appels au `/command-capabilities` des services distants et échoue avec
> `NoHandlerForCommandException: No node known to accept command [...]`.

---

## 4bis. Buses distribués Axon (Spring Cloud) — pattern

Les services QuizUp utilisent **deux** buses distribués, **tous deux** fournis par le SDK
(`quizup-axon-autoconfigure`) + le starter `axon-springcloud` :

| Bus                       | Auto-configuration SDK                                                                                                                                                 | Routing                                                                                                                                        |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| **Query bus distribué**   | `AxonDistributedQueryAutoConfiguration` → `SpringCloudDistributedQueryBus` + `SpringCloudQueryRouter` + `HttpQueryBusConnector`                                        | Discovery Spring Cloud (`spring.cloud.discovery.client.simple.instances`) + endpoint `/query-capabilities` (SDK `QueryCapabilitiesController`) |
| **Command bus distribué** | `axon-springcloud-spring-boot-autoconfigure` → `SpringCloudAutoConfiguration` → `DistributedCommandBus` + `SpringCloudCommandRouter` + `SpringHttpCommandBusConnector` | Discovery Spring Cloud + endpoint `/command-capabilities` (axon-springcloud `MemberCapabilitiesController`)                                    |

**Règles** :

- **Processing groups explicites** : chaque classe handler (`@EventHandler`/`@SagaEventHandler`)
  doit porter `@ProcessingGroup("<nom>")` en **kebab-case** (ex. `game-projection`). Le SDK ne déduit
  plus de groupe par défaut (`AxonDistributedKafkaAutoConfiguration`) et **échoue au démarrage** si un
  handler n'est pas annoté (le partage d'un même groupe par plusieurs classes est autorisé :
  voir la règle « grouper par write-model »). La source par défaut est câblée
  sur `streamableKafkaMessageSource` : tout groupe déclaré est un `TrackingEventProcessor` Kafka, sans
  property par groupe. Replay ciblé = reset du token d'un seul groupe
  (`DELETE FROM token_entry WHERE processor_name = '<groupe>'`).
- **Ne pas** redéclarer les props `axon.axonserver.*`, `axon.kafka.*`, `axon.distributed.*` dans les
  `application-*.yml` des services — le SDK les définit par défaut via
  `AxonDistributedEnvironmentPostProcessor` (property source `axonDistributedDefaultProperties`,
  `addLast` = priorité la plus basse). Les `axon.eventhandling.processors.*` ne sont que des **overrides
  ciblés** d'un groupe existant, jamais une déclaration de groupe.
- **Groupes instance-scopés** (`quizup.axon.instance-scoped-groups`) : pour un service à **fan-out
  temps réel multi-réplique** (BFF : chaque réplique ne connaît que ses clients STOMP), un processor
  segmenté ne suffit pas — chaque instance doit consommer **tous** les événements.
  `InstanceScopedProcessingGroupsAutoConfiguration` suffixe les groupes listés par l'id d'instance
  (`<groupe>-<instanceId>`, défaut `HOSTNAME` = nom du pod) : chaque instance a son propre token et
  démarre à la **queue** du flux (aucun replay Kafka pour un nouveau pod ; les clients complètent
  l'historique via les folds REST). Les noms de processors sont donc suffixés (métriques,
  `token_entry` : un replay ciblé doit supprimer toutes les lignes suffixées du groupe).
  **Piège** : pour la source Kafka, la queue du flux s'obtient avec `createHeadToken()` —
  `createTailToken()` renvoie `null` (= **début** du topic) et ferait rejouer tout le flux à
  chaque nouveau pod (notifications/événements re-poussés aux clients connectés).
- Le **distributed command bus** n'est **pas** défini par le SDK (pas d'autoconfig SDK) — il vient
  du starter `axon-springcloud-spring-boot-autoconfigure` (jar Axon). Le bean `RestTemplate` du
  SDK (`@Primary`, avec intercepteur OAuth2 `server-client`) est **injecté** dans
  `RestCapabilityDiscoveryMode` + `SpringHttpCommandBusConnector` de ce starter.
- **Découverte par environnement** :
  - **local** : `spring.cloud.discovery.client.simple.instances` dans chaque `application-local.yml`
    (**tous** les services + leur `uri`, ports locaux). Découverte Kubernetes **désactivée**
    (`spring.cloud.kubernetes.discovery.enabled=false` par défaut dans le SDK).
  - **prod** : `DiscoveryClient` Kubernetes (`spring-cloud-starter-kubernetes-client`), activé par
    `SPRING_CLOUD_KUBERNETES_ENABLED=true` + `SPRING_CLOUD_KUBERNETES_DISCOVERY_ENABLED=true`.
    Un `Role`/`RoleBinding` (SA `default`) est requis pour `list/watch` `services`/`endpoints`.
    Le SDK filtre sur `app.kubernetes.io/component=axon` (les non-Axon ne sont pas découverts).
  - **Adresse du `Registration` local** : `AxonDistributedFallbackRegistrationAutoConfiguration` crée
    le `Registration` (Spring Cloud Kubernetes n'en fournit **pas**, le fallback est donc actif **en
    prod aussi**). Son adresse doit **matcher** l'instance découverte, sinon le routeur traite le nœud
    local comme distant (capacités récupérées par HTTP → `No node known to accept command` au
    démarrage). Règle : `server.address` s'il est défini, sinon l'IP locale du pod
    (`spring.cloud.client.ip-address`) quand `spring.cloud.kubernetes.discovery.enabled=true`,
    sinon `localhost`. Port = `server.port`. (Ne pas détecter via la présence d'un
    `SimpleDiscoveryClient` : Spring Cloud Commons en crée toujours un, même en prod.)
  - **Endpoints non-`Ready`** : `spring.cloud.kubernetes.discovery.include-not-ready-addresses=true`
    (défaut SDK) pour que le pod local soit découvert **dès sa création**. Sinon, pendant le
    démarrage, le pod n'est pas dans les Endpoints, un `HeartbeatEvent` reconstruit le
    `ConsistentHash` sans le membre local → le seeder/saga local échoue en
    `No node known to accept command`. Verrouillé par
    `SpringCloudCommandRouterLocalMemberTest`.
- **Rafraîchissement des capacités** : `SimpleDiscoveryClient` n'émet pas de `HeartbeatEvent`,
  donc `AxonDistributedFallbackRegistrationAutoConfiguration` ré-émet un heartbeat
  périodique (`axon.distributed.spring-cloud.heartbeat-interval`, 10 s) pour que le
  `SpringCloudCommandRouter` rafraîchisse ses capacités dès que les pairs sont disponibles (sans cela :
  `No node known to accept command`).
- **`axon.distributed.spring-cloud.enable-accept-all-commands=false`** : chaque nœud
  n'annonce **que** ses propres handlers (`CommandNameFilter`). À `true`, tous les nœuds
  se déclarent capables de tout et les commandes sont routées vers un nœud sans handler
  (`NoHandlerForCommandException`).

---

## 4ter. Console d'exploitation Axon & DLQ (`AxonConsole*AutoConfiguration`)

Équivalent fonctionnel open source de la Axoniq Platform — référence de parité :
`../../product/axon-platform-parity.md`.

- **DLQ activée par défaut** (`AxonConsoleDlqAutoConfiguration`) : un événement dont le handler
  échoue est **enqueued** au lieu de bloquer indéfiniment le segment du processor. Le bean Axon
  `DeadLetterQueueProviderConfigurerModule` (no-op de `JpaAutoConfiguration`) est **remplacé** par
  `QuizupDeadLetterQueueProviderConfigurerModule` (auto-config `afterName=HibernateJpaAutoConfiguration`,
  `@AutoConfigureBefore(JpaAutoConfiguration)` — verrouillé par test). Sont **exclus** : groupes
  instance-scopés (`quizup.axon.instance-scoped-groups`), **sagas** (Axon ne les wrappe pas : groupes
  FQCN exclus par le provider, tous les groupes de sagas exclus côté état/API via
  `sagaConfigurations()`), et `quizup.axon.dlq.excluded-groups`. Bornes :
  `quizup.axon.dlq.{enabled,max-sequences,max-sequence-size,claim-duration}`.
- **`build-info` hérité** (pluginManagement du parent) : chaque service embarque
  `META-INF/build-info.properties` → `BuildProperties` alimente le tag métrique `version` **et** les
  snapshots de l'agent (plus de `unknown`).
- **État & API interne** (`AxonConsoleAutoConfiguration`) : `ConsoleState` (processors, segments,
  erreurs, stats DLQ) ; endpoint interne `/internal/console/**` (JWT M2M requis, jamais ingressed) —
  lecture DLQ paginée, retry/delete/clear de séquence, pause/start, reset (tail/head/instant,
  processor **à l'arrêt**), split/merge de segment ; **event store** (stats, événements/snapshots par
  agrégat, recherche bornée par type) et **scheduled events** (db-scheduler, lecture seule) —
  `ConsoleEventStoreService` (JdbcTemplate read-only + `EventStore.readEvents`).
- **Agent push** : `quizup.axon.console.enabled=true` (défaut `false`), `url` (défaut
  `http://quizup-console.quizup-prod.svc.cluster.local`, Service k8s port 80), `interval` (15 s) —
  snapshot borné versionné `v1` poussé en M2M via le `RestTemplate` OAuth2 `server-client`.
  Snapshot enrichi (contenu natif Axon Server) : `canPause/canPlay/canSplit/canMerge`,
  `tokenStoreIdentifier`, `activeThreads`, `capacity`, `warnings`, `onePartOf` par segment.
- **Métriques** : `quizup.axon.console.dlq.size|sequences`, `...processor.caught_up|replaying`
  (jauges enregistrées sur `ApplicationReadyEvent`, jamais pendant l'initialisation Axon).

---

## 5. Types partagés (`microservice-core`)

Packages sous `io.github.quizup.microservice.core.domain.*` :

- **`exception`** : `BaseProblem`, `ProblemCategory` — base de toutes les exceptions métier ;
  familles de `Problem` par domaine (`SearchableProblems`, `SearchValidationProblems`,
  `LanguageProblems`…). **Règle** : jamais de `throw new RuntimeException`/`IllegalArgumentException`
  dans le code métier — toujours un `Problem`.
- **`constant`** : `QuizUpConstants` — identifiant et email du compte système unique (`SYSTEM_USER_ID`, `SYSTEM_USER_EMAIL`, `SYSTEM_USER_NAME`)
- **`model.i18n`** : `Language` (`fr`/`en`, code ISO 639-1, sérialisé/désérialisé strictement via
  `@JsonValue`/`@JsonCreator`) — langue partagée entre profil (préférence) et contenu (questions).
  Code inconnu ⇒ `LanguageProblems.UnsupportedLanguageProblem` (400 côté REST).
- **`model.search`** (metadata technique) : `FilterOperator`, `SortDirection`, `FieldType`,
  `Searchable`/`SearchableEntity`/`SearchableField`. **Aucun modèle de pagination custom** : les DTO
  de recherche (`SearchRequest`/`FilterRequest`/`SortRequest`/`PageRequest` → `SearchResponse<T>`)
  vivent sous `infrastructure.in.api.request`/`response` et sont l'unique représentation
  échangée (REST **et** bus). Ils n'implémentent plus d'interface domaine et ne portent aucune
  annotation Jackson technique.
- **`model.notification`** : `EventEnvelope` — enveloppe d'événement **auto-descriptive** partagée
  par l'historique REST et le push WebSocket/query bus : `aggregateId`, `sequenceNumber`,
  `timestamp`, `eventType` (FQCN du payload, dérivé par `EventEnvelope.of(...)`), `payload`
  (`Object`, sans annotation framework). Les modules `domain/` des services restent purs.
  - Le codec `EventEnvelopeModule` (`quizup-axon-query`) sérialise/désérialise le payload avec son
    **type concret** à partir d'`eventType`, restreint à l'allowlist `io.github.quizup.`
    (fail-closed). Il n'est enregistré que dans le **serializer dédié du query bus distribué**
    (`distributedQuerySerializer`, cf. `AxonDistributedQueryAutoConfiguration`) ; les serializers
    Axon général/message/event et le format de l'event store sont inchangés.
  - Le contrat exposé au web n'est **pas** cette enveloppe : le BFF possède son propre
    `EventEnvelopeResponse` (annotations Jackson du contrat web, `eventType` = type web).
  - Format wire du query bus non rétro-compatible : déploiement coordonné requis.
- **`infrastructure.axon`** : `QueryResponseTypes` — **factory unique** des `ResponseType` de queries (`instanceOf`,
  `optionalInstanceOf`, `multipleInstancesOf`, `searchResponseOf`). **Règle** : les services ne doivent
  plus utiliser `ResponseTypes` d'Axon
  directement. `multipleInstancesOf` s'appuie sur `RawMultipleInstancesResponseType`, qui matche le **type brut** de
  l'élément de liste et corrige le cas `List<EventEnvelope>` (qu'Axon
  natif ne résout pas → `NoHandlerForQueryException`)
- **`model.security`** : `QuizUpPrincipal` (contexte JWT)
- **`port.out`** : ports sortants partagés
- **`query`** : queries partagées
- **`validator`** : validateurs partagés

**`SecurityHelper`** (package `io.github.quizup.microservice.security`, module autoconfigure) —
extraction du contexte JWT : `getUserId()`, `getUserEmail()`, `findUserId()`, `getPrincipal()`,
`isAuthenticated()`. **Uniquement** utilisable dans les controllers.

---

## 6. Observabilité (métriques Prometheus / Micrometer)

Le starter embarque **`micrometer-registry-prometheus`** : chaque service expose
`GET /actuator/prometheus` (déjà `permitAll` côté sécurité). Prometheus scrape ce endpoint
**in-cluster** sur le port du Service nommé `http` (80 → 8080) — jamais via l'Ingress.

**Défauts injectés par `ObservabilityEnvironmentPostProcessor`** (property source
`quizupObservabilityDefaultProperties`, `addLast` = priorité la plus basse, même modèle que
`AxonDistributedEnvironmentPostProcessor`) :

| Propriété | Valeur par défaut |
|---|---|
| `management.endpoints.web.exposure.include` | `health,info,metrics,prometheus` |
| `management.endpoint.health.probes.enabled` | `true` |
| `management.prometheus.metrics.export.enabled` | `true` |
| `management.metrics.distribution.percentiles-histogram.http.{server,client}.requests` | `true` |
| `logging.structured.format.console` | `ecs` (logs JSON, corrélés `trace.id`/`span.id`) |
| `management.tracing.enabled` | `false` (sauf `microservice.observability.tracing.enabled=true`) |

**Traces distribuées** : `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` sont sur
le classpath. Le tracing est **désactivé par défaut** ; l'activer avec
`microservice.observability.tracing.enabled=true` (fait en GitOps via les ConfigMaps de service),
ce qui fixe le sampling (0,1), l'endpoint OTLP in-cluster (`otel-collector.monitoring`) et active
les observations Kafka. Côté Axon, `AxonDistributedTracingAutoConfiguration` expose un `SpanFactory`
(`OpenTelemetrySpanFactory`) adossé à l'`OpenTelemetry` de Spring (sinon Axon utiliserait
`GlobalOpenTelemetry`, no-op).

> **Règle** : ne **pas** redéclarer ces propriétés dans les `application-*.yml` des services :
> une déclaration explicite écrase le défaut. **Exception** : `quizup-gateway` déclare son propre
> `management.endpoints.web.exposure.include` → il doit inclure `prometheus`.

**Tags communs** (`ObservabilityAutoConfiguration`) : tout compteur/timer/histogramme porte
`application` (= `spring.application.name`), `environment` (profil actif) et `version`
(`build-info` ou `info.app.version`).

**Métriques Axon** (module `quizup-axon`) : branchées sur le `MeterRegistry` via `axon-micrometer`
(messages dispatchés/traités/en échec, latence des bus, event processors). Désactivées
automatiquement quand aucun `MeterRegistry` n'est présent (tests Axon in-memory).

Métriques d'**activité** complémentaires (`quizup.axon.*`), car les bus distribués et l'event bus
Kafka échappent partiellement au `MessageMonitor` :
- `AxonMetricsInterceptorConfiguration` (modèle `MessageHandlerConfiguration`) enregistre les
  intercepteurs des bus en `@PostConstruct` (injection paresseuse `@Lazy @Qualifier("distributedCommandBus"/"distributedQueryBus")`) :
  `quizup.axon.commands` / `quizup.axon.command.duration`, `quizup.axon.queries` /
  `quizup.axon.query.duration`, `quizup.axon.events.published`.
- `AxonDistributedActivityMetricsAutoConfiguration` enregistre le traitement d'événements
  (`quizup.axon.events.processed` / `quizup.axon.event.duration`) via `ConfigurerModule`, et les
  jauges d'état (`quizup.axon.event.processor.running|error`) sur `ApplicationReadyEvent`
  (les processors doivent être initialisés).

**KPI métier** : les compteurs métier par service ont été **retirés** (voir `../../observability.md`).
Si un besoin revient, exposer un **port hexagonal** par domaine (ex. `GameMetricsPort`) implémenté
en infrastructure avec `MeterRegistry` (jamais d'import Micrometer dans `domain/`).
