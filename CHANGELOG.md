## [5.0.0](https://github.com/quizup-organization/quizup-sdk/compare/v4.8.0...v5.0.0) (2026-10-22)

### ⚠ BREAKING CHANGES

* **sdk:** retire les contrats métier inter-services du SDK

### Code Refactoring

* **sdk:** retire les contrats métier inter-services du SDK ([0e28fda](https://github.com/quizup-organization/quizup-sdk/commit/0e28fdaa4f5294fff4fe65fd38eed11900db943e))

## [4.8.0](https://github.com/quizup-organization/quizup-sdk/compare/v4.7.1...v4.8.0) (2026-10-22)

### Features

* **account:** événements transverses de suppression et effacement de l'event store ([01409cc](https://github.com/quizup-organization/quizup-sdk/commit/01409cc023459bfaa850f336d88bdc523254cdbd))

## [4.7.1](https://github.com/quizup-organization/quizup-sdk/compare/v4.7.0...v4.7.1) (2026-10-12)

### Bug Fixes

* **sdk:** suivi des commandes en vol enregistré en @PostConstruct (cycle de beans Configuration/bus distribué au démarrage des services à projection CommandGateway) ([6de1331](https://github.com/quizup-organization/quizup-sdk/commit/6de133132c39183a954e72a6e94eda1b537f85bb))

## [4.7.0](https://github.com/quizup-organization/quizup-sdk/compare/v4.6.0...v4.7.0) (2026-10-12)

### Features

* **console:** lag chiffré, commandes en vol, payload snapshots, annulation scheduled, adresse d'instance ([01dcf26](https://github.com/quizup-organization/quizup-sdk/commit/01dcf26aab96f3e0ce7245ca3a9a70f583ece384))

## [4.6.0](https://github.com/quizup-organization/quizup-sdk/compare/v4.5.2...v4.6.0) (2026-10-12)

### Features

* **console:** build-info, snapshot enrichi et API interne event store/scheduled ([c6ae47e](https://github.com/quizup-organization/quizup-sdk/commit/c6ae47ec50a6a5344657799eb511cd4654d75e12))

## [4.5.2](https://github.com/quizup-organization/quizup-sdk/compare/v4.5.1...v4.5.2) (2026-10-12)

### Bug Fixes

* **axon:** groupes instance-scopés démarrés à la queue du flux (fin du replay complet par pod) ([615ab1f](https://github.com/quizup-organization/quizup-sdk/commit/615ab1f5458cc3d8b59e91e2c727ba48379d4d4e))

## [4.5.1](https://github.com/quizup-organization/quizup-sdk/compare/v4.5.0...v4.5.1) (2026-10-12)

### Bug Fixes

* **console:** URL par défaut de l'agent = Service k8s quizup-console (port 80) ([f30be89](https://github.com/quizup-organization/quizup-sdk/commit/f30be895a855a9adfafdde6337d69b6f49e96417))

## [4.5.0](https://github.com/quizup-organization/quizup-sdk/compare/v4.4.1...v4.5.0) (2026-10-12)

### Features

* **console:** DLQ Axon par défaut, agent console et API interne d'exploitation ([98e1868](https://github.com/quizup-organization/quizup-sdk/commit/98e186899c22f3452dee77e0a5b26c4ccadb2f87))

## [4.4.1](https://github.com/quizup-organization/quizup-sdk/compare/v4.4.0...v4.4.1) (2026-10-11)

### Bug Fixes

* **axon:** groupes instance-scopés sur la source Kafka du SDK (pas l'event store local) ([919aaa5](https://github.com/quizup-organization/quizup-sdk/commit/919aaa56a78a07f7cf12105e979385c66f8cbb12))

## [4.4.0](https://github.com/quizup-organization/quizup-sdk/compare/v4.3.0...v4.4.0) (2026-10-11)

### Features

* **axon:** processing groups instance-scopés (fan-out temps réel multi-réplique) ([f5efacd](https://github.com/quizup-organization/quizup-sdk/commit/f5efacd441538185bcf09bc35d7ed99b3e8aaf5b))

## [4.3.0](https://github.com/quizup-organization/quizup-sdk/compare/v4.2.0...v4.3.0) (2026-10-08)

### Features

* **websocket:** heartbeats STOMP du broker (outgoing/incoming) ([7c268e3](https://github.com/quizup-organization/quizup-sdk/commit/7c268e3c3df1cb98c4a172c1775ef6509bca38c6))

## [4.2.0](https://github.com/quizup-organization/quizup-sdk/compare/v4.1.0...v4.2.0) (2026-10-04)

### Features

* **config:** add quizup-notification to local bus discovery ([e6138af](https://github.com/quizup-organization/quizup-sdk/commit/e6138af443cc6d98662d6dfcb98b1bf5bebe682a))

## [4.1.0](https://github.com/quizup-organization/quizup-sdk/compare/v4.0.0...v4.1.0) (2026-09-30)

### Features

* **i18n:** shared Language type and language Problem ([7f46280](https://github.com/quizup-organization/quizup-sdk/commit/7f4628027e4a296a533adffdd38b0178b7bdf124))

## [4.0.0](https://github.com/quizup-organization/quizup-sdk/compare/v3.0.1...v4.0.0) (2026-09-27)

### ⚠ BREAKING CHANGES

* **sdk:** EventEnvelope transport for the distributed query bus

### Features

* **sdk:** EventEnvelope transport for the distributed query bus ([889f41f](https://github.com/quizup-organization/quizup-sdk/commit/889f41f177738031a76ae8d6cf21a91d979d278f))

## [3.0.1](https://github.com/quizup-organization/quizup-sdk/compare/v3.0.0...v3.0.1) (2026-09-25)

### Bug Fixes

* **sdk:** keep Optional responses over the query bus and map remote problems ([985d589](https://github.com/quizup-organization/quizup-sdk/commit/985d589d179aebeb4c86e21165e0cca94f324b9a)), closes [OptionalResponseType#forSerialization](https://github.com/quizup-organization/OptionalResponseType/issues/forSerialization)

## [3.0.0](https://github.com/quizup-organization/quizup-sdk/compare/v2.4.5...v3.0.0) (2026-09-25)

### ⚠ BREAKING CHANGES

* **sdk:** replace custom pagination model with core SearchRequest/SearchResponse DTOs

### Features

* **sdk:** replace custom pagination model with core SearchRequest/SearchResponse DTOs ([07c9487](https://github.com/quizup-organization/quizup-sdk/commit/07c94876738ceffb25e49238da8d6c4e19ce5e84))

## [2.4.5](https://github.com/quizup-organization/quizup-sdk/compare/v2.4.4...v2.4.5) (2026-09-24)

### Reverts

* Revert "fix(axiom): preserve PageResult element types over the distributed query transport" ([9b2299b](https://github.com/quizup-organization/quizup-sdk/commit/9b2299b9d5eb712027f77add883b3e79c141126f))

## [2.4.4](https://github.com/quizup-organization/quizup-sdk/compare/v2.4.3...v2.4.4) (2026-09-24)

### Bug Fixes

* **axiom:** preserve PageResult element types over the distributed query transport ([e28f3d1](https://github.com/quizup-organization/quizup-sdk/commit/e28f3d14653cff057161314916544bb8cc6dcc2a))

## [2.4.3](https://github.com/quizup-organization/quizup-sdk/compare/v2.4.2...v2.4.3) (2026-09-24)

### Bug Fixes

* bus-only Jackson type info for search criteria (keep REST DTOs typeless) ([9d4a715](https://github.com/quizup-organization/quizup-sdk/commit/9d4a715332ad781316f78908c43d4702c470a6d8))

## [2.4.2](https://github.com/quizup-organization/quizup-sdk/compare/v2.4.1...v2.4.2) (2026-09-24)

### Bug Fixes

* add Jackson type info to search criteria interfaces for distributed queries ([f0ab7c2](https://github.com/quizup-organization/quizup-sdk/commit/f0ab7c208f58509d1f120dc3a0c9d9ba70160e1f))

## [2.4.1](https://github.com/quizup-organization/quizup-sdk/compare/v2.4.0...v2.4.1) (2026-09-23)

### Bug Fixes

* **sdk:** bump PostgreSQL JDBC driver to 42.7.12 (CVE-2026-54291) ([eff48a0](https://github.com/quizup-organization/quizup-sdk/commit/eff48a0eac7f61a8e64af56d7323285abba0a55d))

## [2.4.0](https://github.com/quizup-organization/quizup-sdk/compare/v2.3.4...v2.4.0) (2026-09-23)

### Features

* **sdk:** add noContent() 204 helper to ResponseEntityBuilder ([3fd2af1](https://github.com/quizup-organization/quizup-sdk/commit/3fd2af1469fcbfac44c015a627175da418a67eea))

## [2.3.4](https://github.com/quizup-organization/quizup-sdk/compare/v2.3.3...v2.3.4) (2026-09-22)

### Bug Fixes

* **sdk:** discover not-ready endpoints so the local Axon member survives startup ([63f28e5](https://github.com/quizup-organization/quizup-sdk/commit/63f28e57a3c4647d75b6bc7ee230faba68251c00))

## [2.3.3](https://github.com/quizup-organization/quizup-sdk/compare/v2.3.2...v2.3.3) (2026-09-22)

### Bug Fixes

* **sdk:** pick local pod IP for fallback registration under kubernetes discovery ([76b17f7](https://github.com/quizup-organization/quizup-sdk/commit/76b17f7df3d6715e01bd2df0ad8eccde74780873))

## [2.3.2](https://github.com/quizup-organization/quizup-sdk/compare/v2.3.1...v2.3.2) (2026-09-22)

### Bug Fixes

* **sdk:** allow intentional processing-group sharing (drop duplicate guard) ([e2dbe49](https://github.com/quizup-organization/quizup-sdk/commit/e2dbe4932962d4f09fd99e1a3153e4962f5dbaab))

## [2.3.1](https://github.com/quizup-organization/quizup-sdk/compare/v2.3.0...v2.3.1) (2026-09-22)

### Bug Fixes

* **sdk:** align fallback registration address with kubernetes discovery ([547fe26](https://github.com/quizup-organization/quizup-sdk/commit/547fe26e70d24ba3f22838fa056b9ea15ac46037))

## [2.3.0](https://github.com/quizup-organization/quizup-sdk/compare/v2.2.0...v2.3.0) (2026-09-22)

### Features

* **sdk:** explicit Axon processing groups and query bus metrics ([c105ea4](https://github.com/quizup-organization/quizup-sdk/commit/c105ea4a56aec4817052a6238331a6aaaf8cb247))

## [2.2.0](https://github.com/quizup-organization/quizup-sdk/compare/v2.1.1...v2.2.0) (2026-09-21)

### Features

* **config:** ship shared quizup config fragments ([8c40abb](https://github.com/quizup-organization/quizup-sdk/commit/8c40abbd3a8b8e18f05d2d9d643e938d6a75a683))

### Bug Fixes

* **observability:** emit ECS structured logs only in prod ([67e26fe](https://github.com/quizup-organization/quizup-sdk/commit/67e26fee796141fbdebc1209939b608127214ac7))

## [2.1.1](https://github.com/quizup-organization/quizup-sdk/compare/v2.1.0...v2.1.1) (2026-09-20)

### Bug Fixes

* **discovery:** scope kubernetes discovery to axon services and disable by default ([604b68f](https://github.com/quizup-organization/quizup-sdk/commit/604b68f19f9c6b6bd2edf3a5cf1214ae8653614b))

## [2.1.0](https://github.com/quizup-organization/quizup-sdk/compare/v2.0.0...v2.1.0) (2026-09-20)

### Features

* **discovery:** support kubernetes discovery client for prod ([666eb73](https://github.com/quizup-organization/quizup-sdk/commit/666eb73886f7a0c89bb615554ce745387caf4a96))

## [2.0.0](https://github.com/quizup-organization/quizup-sdk/compare/v1.5.0...v2.0.0) (2026-09-20)

### ⚠ BREAKING CHANGES

* **constants:** QuizUpConstants.ADMIN_USER_* and BOT_USER_* are removed; use SYSTEM_USER_ID/SYSTEM_USER_NAME/SYSTEM_USER_EMAIL.

### Code Refactoring

* **constants:** remove legacy admin/bot aliases ([8fc70ba](https://github.com/quizup-organization/quizup-sdk/commit/8fc70bab5410415e7c541dc4f86324e21d3df11f))

## [1.5.0](https://github.com/quizup-organization/quizup-sdk/compare/v1.4.3...v1.5.0) (2026-09-20)

### Features

* **constants:** unify admin and bot into a single system account ([20ec08c](https://github.com/quizup-organization/quizup-sdk/commit/20ec08cf373ba64bc7652072987b63e58f49cc1c))

## [1.4.3](https://github.com/quizup-organization/quizup-sdk/compare/v1.4.2...v1.4.3) (2026-09-20)

### Bug Fixes

* **observability:** remove WebSocket STOMP metrics ([44f053d](https://github.com/quizup-organization/quizup-sdk/commit/44f053d620168f0e5d342131ca4a0cdee29092dd))

## [1.4.2](https://github.com/quizup-organization/quizup-sdk/compare/v1.4.1...v1.4.2) (2026-09-20)

### Bug Fixes

* **swagger:** configurable OpenAPI server URL ([0808fee](https://github.com/quizup-organization/quizup-sdk/commit/0808fee563c2010796b3b0ecaea0e504142cd492))

## [1.4.1](https://github.com/quizup-organization/quizup-sdk/compare/v1.4.0...v1.4.1) (2026-09-20)

### Bug Fixes

* **observability:** register Axon activity metrics after startup ([be691fa](https://github.com/quizup-organization/quizup-sdk/commit/be691fa2ffb5d9dc21b55b304cc6effc80ddceff))

## [1.4.0](https://github.com/quizup-organization/quizup-sdk/compare/v1.3.0...v1.4.0) (2026-09-20)

### Features

* **observability:** Axon activity metrics (commands, events, processors) ([b1e888d](https://github.com/quizup-organization/quizup-sdk/commit/b1e888df1b7dad4518ba2b81915c5ba9488deb4a))

## [1.3.0](https://github.com/quizup-organization/quizup-sdk/compare/v1.2.0...v1.3.0) (2026-09-20)

### Features

* **observability:** WebSocket STOMP metrics ([4dfa6f1](https://github.com/quizup-organization/quizup-sdk/commit/4dfa6f1edd443eb62464094a8e77f779449e840b))

## [1.2.0](https://github.com/quizup-organization/quizup-sdk/compare/v1.1.0...v1.2.0) (2026-09-20)

### Features

* **observability:** structured JSON logs (ECS) and OpenTelemetry tracing ([351eb3f](https://github.com/quizup-organization/quizup-sdk/commit/351eb3f57f1deca5b5ec2cbbdb3fff9b21455e45))

## [1.1.0](https://github.com/quizup-organization/quizup-sdk/compare/v1.0.0...v1.1.0) (2026-09-20)

### Features

* **observability:** expose Prometheus metrics and Axon metrics ([caf7034](https://github.com/quizup-organization/quizup-sdk/commit/caf7034c6917e507322d751b5dd3564e56088883))

## 1.0.0 (2026-09-19)

### Features

* first commit ([ba12156](https://github.com/quizup-organization/quizup-sdk/commit/ba121562beef7cdc81f65d2e174e5ae813b1cabb))

### Bug Fixes

* **quizup-sdk:** update infra ([49204f3](https://github.com/quizup-organization/quizup-sdk/commit/49204f3964f5ba4a4f282277c8a30aeb49a40025))
* update AGENTS.md file ([8193d12](https://github.com/quizup-organization/quizup-sdk/commit/8193d129aee026d62348125313aa2bc7c809a1a5))
* update AGENTS.md file ([b57012a](https://github.com/quizup-organization/quizup-sdk/commit/b57012a00b9b4b96c2d4b4af499877d05eaf100c))
