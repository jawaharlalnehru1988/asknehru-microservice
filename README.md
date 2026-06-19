# AskNehru Spring Boot Backend

This workspace is the Spring Boot replacement for the current Django backend.

## Shape

This is a modular monolith, not a set of separately deployed microservices.

There is one runtime Spring Boot application:

- `asknehru-backend` - the only deployable application

And these internal modules:

- `auth-service` - identity, JWT, refresh, roles, sessions
- `user-service` - account profiles and user-facing metadata
- `conversation-service` - interview conversations and AI session data
- `roadmap-service` - roadmap planning and progress tracking
- `shared-contracts` - shared DTOs and API contracts

## Migration rule

Migrate one module at a time through Nginx route cutover. Keep Django live for any path that has not moved yet.
