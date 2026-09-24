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
