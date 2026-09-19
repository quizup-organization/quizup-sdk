# quizup-sdk

Agrégateur des librairies transverses QuizUp : `quizup-parent`, `quizup-microservice`, `quizup-axon`.

## Modules

| Module                | Description                                                                       |
|-----------------------|-----------------------------------------------------------------------------------|
| `quizup-parent`       | Parent Maven des services et librairies QuizUp (BOM, properties, plugins)         |
| `quizup-microservice` | Starter des microservices QuizUp (CORS, Swagger, sécurité, WebSocket, exceptions) |
| `quizup-axon`         | Starter Axon distribué (RabbitMQ, deadlines, query bus)                           |

## Build

```bash
mvn -B verify
```

## Publish

Publié sur [GitHub Packages](https://github.com/orgs/quizup-organization/packages) sous `io.github.quizup`.
