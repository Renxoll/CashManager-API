# cash — MVP backend (SmartCash)

Backend del MVP de finanzas personales: recibe texto plano de notificaciones bancarias,
lo categoriza con un LLM (Structured Outputs) y lo deja listo en Postgres, con push
notification al usuario. La API está protegida por autenticación Bearer token.

## Stack

- **Java 21 / Spring Boot 4.1.0**, arquitectura **DDD en 4 capas** (`domain`,
  `application`, `infrastructure`, `interfaces`) organizada por **bounded contexts**
  (`iam`, `profile`, `subscription`, `transactions`, `workspaces`, `analytics`,
  `groups`, `gmailsync`, `advisor`).
- **Spring Security** (stateless, Bearer token) protege toda la API salvo `/api/v1/iam/**`
  y `/actuator/health`.
- **PostgreSQL** (persistencia, vía Flyway) + **Redis** (cache comercio → categoría).
- **Docker** con Dockerfile multi-stage, listo para Azure Container Apps / App Service.
- **LLM**: dialecto OpenAI Chat Completions con Structured Outputs (`response_format:
  json_schema`, `strict: true`). Compatible con GPT-4o-mini o Gemini Flash en su
  endpoint OpenAI-compatible — solo cambia `LLM_BASE_URL` / `LLM_MODEL`.
- **FCM** (Firebase Cloud Messaging) para push notifications, con fallback mock cuando
  no hay proyecto Firebase configurado.

## Bounded contexts

| Contexto | Responsabilidad |
|---|---|
| **`iam`** | Protección de la API: registro/login, hasheo de contraseñas (BCrypt), emisión y validación del Bearer token. |
| **`profile`** | Registro y guardado de perfiles de usuario: nombre visible, token FCM para notificaciones. |
| **`subscription`** | Suscripciones de la plataforma: alta a un plan, cancelación, invariante de una sola suscripción activa por usuario. Pasarela de pago vía Stripe Checkout (webhook `checkout.session.completed`). |
| **`transactions`** | El dominio core: ingesta y categorización de gastos (ver flujo abajo). Cada transacción vive en un **módulo** (ver `workspaces`). |
| **`workspaces`** | "Módulos": el usuario separa sus gastos en varios contenedores renombrables y personalizables (color/ícono) — p. ej. "Empresa", "Hijo", "Inversiones" — cada uno con su propia lista de categorías, aparte del módulo "General" al que cae la ingesta automática. Ver [Módulos](#módulos-workspaces) abajo. |
| **`analytics`** | Read model para el dashboard: resumen mensual de gasto/ingreso por moneda y desglose por categoría, acotado a un módulo. |
| **`groups`** | Gastos compartidos estilo Splitwise: grupos con otros usuarios reales, división en partes iguales, saldos y simplificación de deudas. |
| **`gmailsync`** | Conexión OAuth2 a la bandeja de Gmail del usuario para leer sus notificaciones bancarias directo (alternativa a reenviar correos a mano). |
| **`advisor`** | Asesor financiero conversacional (LLM) sobre el contexto financiero del mes del usuario. |
| `shared` | Plumbing técnico transversal (no es un bounded context): `ApiError`, el `@RestControllerAdvice` catch-all, rate limiting, observabilidad. |

Los tres primeros nacieron de dividir lo que originalmente era un único contexto
`users` — cada uno tiene hoy su propia tabla y su propio ciclo de vida; solo comparten
el mismo UUID por convención (nunca hay FKs cruzando bounded contexts).

## El flujo (core del MVP)

```
POST /api/v1/transactions/webhook  (requiere Authorization: Bearer <token>)
  { "userId": "...", "rawText": "Consumo de S/24.50 en Starbucks" }
        │
        ▼
1. Se valida que exista un perfil para ese usuario (UserDirectory, ACL hacia Profile).
        │
        ▼
2. Heurística de dominio (regex best-effort) intenta extraer monto+moneda+comercio
   del caso feliz "SÍMBOLO+monto en Comercio".
        │
        ▼
3. Si hay comercio candidato → se busca en Redis (merchant-category:*).
   Si hay hit → se arma la transacción SIN llamar al LLM (ExtractionSource=CACHE).
        │ (miss o heurística no aplicó)
        ▼
4. Se llama al LLM con el prompt estricto (ver abajo). Si devuelve JSON
   inválido, se reintenta UNA vez con un prompt de corrección. Si vuelve a
   fallar (o hay error HTTP), el agregado se marca FAILED (texto original
   intacto, nada se pierde) → HTTP 422.
        │ (éxito)
        ▼
5. El agregado Transaction se categoriza (invariantes propias) y emite el
   evento de dominio TransactionCategorized.
        ▼
6. Se persiste, se cachea comercio→categoría en Redis (TTL 30 días) y la
   política CategorizedExpenseNotificationPolicy reacciona al evento
   disparando el push FCM (al token que Profile tenga guardado) → HTTP 201.
```

## Ingesta de gastos por correo (bancos y billeteras electrónicas)

El webhook JSON de arriba sirve para pruebas/integraciones directas, pero el flujo real
de uso es que el usuario reenvíe las notificaciones que ya le manda su banco o su
billetera (Yape, Plin, etc.) por correo, sin tipear nada a mano.

```
Onboarding (una vez, al registrar el perfil):
  UserProfile.register() genera un inboxAddress determinístico:
  alias-{sha256(userId)[:10]}@{app.inbound-email.domain}
  (se consulta después vía GET /api/v1/profiles/me → inboxAddress)
        │
        ▼
El usuario reenvía (o configura un forward automático de) sus alertas
bancarias/de billetera a ese inboxAddress
        │
        ▼
SendGrid Inbound Parse recibe el correo (MX del dominio apuntando a
SendGrid) y hace POST multipart/form-data a:
  POST /api/v1/transactions/inbound?token=<INBOUND_EMAIL_TOKEN>
  { to, from, subject, text }
        │
        ▼
SendGridInboundWebhookController valida el token (comparación en tiempo
constante) y delega a TransactionCommandService.handle(IngestEmailedTransactionCommand)
        │
        ▼
1. TrustedBankSenderPolicy revisa el dominio del "From" contra el
   allowlist (app.inbound-email.trusted-sender-domains). Dominio no
   confiable → se descarta en silencio (log), pero igual responde 200
   (SendGrid reintenta agresivamente ante cualquier respuesta no-2xx,
   y un remitente no confiable no se arregla reintentando).
        │ (dominio confiable)
        ▼
2. UserDirectory.findUserIdByInboxAddress(to) resuelve el dueño del
   buzón. Buzón inexistente (typo, reenvío mal dirigido) → mismo
   descarte silencioso.
        │ (buzón conocido)
        ▼
3. De acá en más es el mismo camino que el webhook JSON: Transaction.
   receive() → PENDING → evento TransactionReceived → worker async
   categoriza con el LLM (o el atajo de Redis si ya se vio ese
   comercio antes) → PROCESSED/FAILED.
```

### Configuración necesaria

| Variable | Default (dev) | Uso |
|---|---|---|
| `INBOUND_EMAIL_DOMAIN` | `inbox.smartcash.pe` | Dominio de los buzones generados (`alias-xxxx@...`). Debe coincidir con el dominio que SendGrid tiene configurado para Inbound Parse (MX apuntando a `mx.sendgrid.net`). |
| `INBOUND_EMAIL_TOKEN` | placeholder de dev, **cambiar en cualquier despliegue real** | Secreto en el query param `?token=` del webhook — Inbound Parse no soporta headers custom, solo se configura la URL de destino. |
| `INBOUND_EMAIL_TRUSTED_DOMAINS` | `bcp.com.pe,notificaciones.interbank.pe,bbva.pe,netinterbank.com.pe,yape.pe` | Allowlist de dominios de remitente (`AllowlistedBankSenderPolicy`). Cualquier correo de un dominio fuera de esta lista se descarta como no confiable, aunque el buzón destino sí exista. |

### Agregar un banco o billetera nueva (p. ej. Plin)

`netinterbank.com.pe` (remitente `servicioalcliente@netinterbank.com.pe`) ya está en el
allowlist — es el dominio real que usa Interbank para notificar tanto operaciones de la
banca tradicional como de Plin (Plin no tiene dominio propio de notificación: viaja bajo
el dominio del banco emisor). `yape.pe` (remitente `notificaciones@yape.pe`) también está
en el allowlist. Sumar una billetera o banco nuevo no toca código, solo configuración:

1. Conseguir un correo real de notificación de esa billetera (reenviado por un usuario
   de prueba) y anotar el dominio real del remitente `From:`. `AllowlistedBankSenderPolicy`
   compara solo el dominio después de la `@`, tolerando el formato típico
   `"Yape <alertas@dominio.pe>"` (nombre visible + dirección entre `<>`) — pero el
   dominio real hay que verlo en un correo de verdad, no adivinarlo.
2. Sumar ese dominio a `INBOUND_EMAIL_TRUSTED_DOMAINS`, separado por coma
   (`AllowlistedBankSenderPolicy` normaliza a minúsculas y hace trim de cada uno, así
   que espacios extra no rompen nada).
3. Redeploy con la variable actualizada — el allowlist se arma en memoria al arrancar
   (`TransactionDomainConfig.trustedBankSenderPolicy`), no hay que migrar ni tocar Redis.

El prompt de extracción (`ExtractionPrompts`) no asume un formato fijo de texto, así que
el LLM debería poder extraer monto/comercio aunque la redacción de Yape/Plin difiera de
la de un banco tradicional — pero vale la pena probarlo con un correo real antes de
darlo por soportado del todo, sobre todo porque estas billeteras suelen mandar
transferencias P2P ("le enviaste S/ 15 a Juan Pérez") además de pagos a comercios, y el
heurístico de dominio (`BankNotificationHeuristicParser`, que asume "SÍMBOLO+monto en
Comercio") probablemente no matchee ese texto — no es un problema (cae al LLM de
todos modos), solo significa que el atajo de Redis por comercio no aplica para esos
casos.

## Módulos (workspaces)

Un **módulo** es un contenedor de transacciones que el usuario nombra y personaliza
(color + ícono). Todo usuario tiene un módulo **"General"** (`isDefault`) que se crea
solo en el alta de cuenta (`AccountRegisteredEvent` de IAM, mismo hilo/transacción que el
registro del perfil) y **no se puede archivar** — es el destino de la ingesta automática
por correo. El usuario crea los módulos custom que quiera ("Empresa", "Hijo",
"Inversiones", …), cada uno con su **propia lista de categorías** editable.

**Modelo de categorías (híbrido, para no tocar el pipeline del LLM):**

- Módulo **General** → la categoría es un `CategoryCode` del catálogo cerrado (tabla
  `categories`, la misma enumeración que exige el Structured Output del LLM). El flujo de
  extracción, el cache Redis comercio→categoría y la política de notificación quedan
  **intactos**.
- Módulo **custom** → la categoría es una fila de `workspace_categories` (id UUID),
  referenciada desde `transactions.workspace_category_id`. El código (`code`) se deriva en
  el backend del rótulo que escribe el usuario (slug en mayúsculas, sin tildes) y es
  único **dentro** del módulo.
- Al **mover** una transacción de módulo se le reasigna la categoría al vocabulario del
  módulo destino (obligatorio para un gasto; los ingresos no se categorizan).

Sin FK desde `transactions` hacia `workspaces`/`workspace_categories` (misma regla de
autonomía de contexto que el resto del repo); la integridad la garantiza el ACL
`WorkspaceDirectory` en `transactions.application`.

### Endpoints — `/api/v1/workspaces` (todos requieren Bearer; el `userId` sale del principal)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/workspaces` | Módulos activos del usuario (el "General" primero), cada uno con sus categorías. |
| `POST` | `/api/v1/workspaces` | Crea un módulo custom. Body: `{ "name", "colorHex"?, "icon"? }` (`colorHex` `#RRGGBB`, defaults `#8B5CF6` / `wallet`). Se siembra con las 8 categorías base como punto de partida. → `201`. |
| `GET` | `/api/v1/workspaces/{id}` | Un módulo. `404` si no existe o es de otro usuario. |
| `PATCH` | `/api/v1/workspaces/{id}` | Renombra y/o repersonaliza. Body parcial: `{ "name"?, "colorHex"?, "icon"? }`. |
| `DELETE` | `/api/v1/workspaces/{id}` | Archiva el módulo (soft delete; las transacciones no se borran). `409` si es el "General". → `204`. |
| `POST` | `/api/v1/workspaces/{id}/categories` | Agrega una categoría. Body: `{ "displayName", "icon"? }`. `409` si el `code` derivado ya existe. → `201` con el módulo. |
| `PATCH` | `/api/v1/workspaces/{id}/categories/{categoryId}` | Renombra una categoría. Body: `{ "displayName", "icon"? }`. |
| `DELETE` | `/api/v1/workspaces/{id}/categories/{categoryId}` | Archiva una categoría. `409` si es la última activa del módulo. → `200` con el módulo. |

### Cómo el resto de la API pasó a ser "por módulo"

| Endpoint | Cambio |
|---|---|
| `GET /api/v1/transactions` | Nuevo query param opcional `?workspaceId=<uuid>` — filtra al módulo indicado; ausente = todos los módulos del usuario. La respuesta incluye ahora `workspaceId` por transacción, y `category`/`categoryCode` se resuelven contra el catálogo del módulo correspondiente. |
| `PATCH /api/v1/transactions/{id}/category` | El `categoryCode` del body ahora se valida contra el vocabulario del módulo **actual** de la transacción (catálogo cerrado si es el General, categorías del módulo si es custom). Un código inválido → `400`. |
| `PATCH /api/v1/transactions/{id}/workspace` | **Nuevo.** Mueve la transacción a otro módulo. Body: `{ "workspaceId": "<uuid>", "categoryCode"?: "<code>" }` — `categoryCode` obligatorio para un gasto (categoría válida en el módulo destino), ignorado para un ingreso. Módulo destino inexistente o ajeno → `404`. |
| `POST /api/v1/transactions/income` | Body admite `"workspaceId"?` opcional (default: módulo "General"). |
| `GET /api/v1/analytics/monthly-summary` | Nuevo query param opcional `?workspaceId=<uuid>` — el resumen es siempre de **un** módulo (el indicado, o el "General" si se omite). El desglose por categoría se une a `categories` para el General y a `workspace_categories` para un módulo custom. `breakdown[].categoryId` es ahora un string (id bigint del catálogo o UUID de categoría de módulo). |

## Arquitectura DDD

Cada bounded context tiene sus 4 capas. `domain` define **contratos** (agregados, value
objects, comandos/queries, y las interfaces `XxxCommandService`/`XxxQueryService`);
`application` los **implementa** (`XxxCommandServiceImpl`, `XxxQueryServiceImpl`) y
orquesta, incluida la comunicación saliente hacia otros contextos (ACL):

```
pe.smartcash.cash
├── iam/                                   [protección de la API + hasheo de contraseñas]
│   ├── domain/
│   │   ├── model/
│   │   │   ├── aggregates/   Credentials (aggregate root) + CredentialsRepository
│   │   │   ├── valueobjects/ UserId, Email, HashedPassword
│   │   │   └── commands/     SignUpCommand, SignInCommand
│   │   ├── services/         IamCommandService (contrato) + AccessToken (VO) +
│   │   │                     PasswordHasher / TokenService (puertos)
│   │   └── exception/        EmailAlreadyRegisteredException, InvalidCredentialsException
│   ├── application/internal/commandservices/  IamCommandServiceImpl
│   ├── infrastructure/
│   │   ├── persistence/  JPA entity + mapper + adapter (tabla credentials)
│   │   ├── hashing/       BCryptPasswordHasherAdapter
│   │   ├── tokens/        HmacTokenServiceAdapter (firma HMAC-SHA256, no JWT de librería)
│   │   └── security/      SecurityConfig (SecurityFilterChain) + BearerTokenAuthenticationFilter
│   │                       + BearerAuthenticationEntryPoint (401 con el mismo formato ApiError)
│   └── interfaces/rest/   IamController (sign-up, sign-in) + resources/transform
│
├── profile/                               [registro y guardado de perfiles]
│   ├── domain/
│   │   ├── model/aggregates/    UserProfile + UserProfileRepository
│   │   ├── model/valueobjects/  UserId
│   │   ├── model/commands/      RegisterUserProfileCommand, UpdateFcmTokenCommand
│   │   ├── model/queries/       FindUserProfileByIdQuery
│   │   ├── services/            UserProfileCommandService, UserProfileQueryService (+ UserProfileDetail)
│   │   └── exception/           UserProfileNotFoundException
│   ├── application/internal/{commandservices,queryservices}/  *Impl
│   ├── infrastructure/persistence/   JPA entity + mapper + adapter (tabla user_profiles)
│   └── interfaces/rest/   UserProfileController + resources/transform
│
├── subscription/                          [suscripciones de la plataforma]
│   ├── domain/
│   │   ├── model/aggregates/    Subscription + SubscriptionRepository
│   │   ├── model/valueobjects/  SubscriptionId, UserId, PlanCode (FREE/PREMIUM), SubscriptionStatus
│   │   ├── model/commands/      SubscribeCommand, CancelSubscriptionCommand
│   │   ├── model/queries/       FindActiveSubscriptionByUserIdQuery
│   │   ├── services/            SubscriptionCommandService, SubscriptionQueryService (+ SubscriptionDetail)
│   │   └── exception/           ActiveSubscriptionAlreadyExistsException, SubscriptionNotFoundException
│   ├── application/internal/{commandservices,queryservices}/  *Impl
│   ├── infrastructure/
│   │   ├── persistence/   JPA entity + mapper + adapter (tabla subscriptions)
│   │   └── payment/       StripePaymentGatewayAdapter (único que importa com.stripe.*) + config
│   └── interfaces/rest/   SubscriptionController + StripeWebhookController + resources/transform
│
├── workspaces/                            [módulos: contenedores de gasto por usuario]
│   ├── domain/
│   │   ├── model/aggregates/    Workspace (aggregate root, con la colección WorkspaceCategory)
│   │   │                        + WorkspaceRepository
│   │   ├── model/valueobjects/  WorkspaceId, WorkspaceCategoryId, UserId, CategoryTemplate,
│   │   │                        StarterCategories (las 8 base)
│   │   ├── model/commands/      CreateWorkspaceCommand, UpdateWorkspaceCommand,
│   │   │                        ArchiveWorkspaceCommand, {Add,Update,Archive}WorkspaceCategoryCommand
│   │   ├── model/queries/       FindWorkspacesByOwnerQuery, FindWorkspaceByIdQuery
│   │   ├── services/            WorkspaceCommandService, WorkspaceQueryService (+ read-models)
│   │   └── exception/           WorkspaceNotFoundException, DefaultWorkspaceProtectedException,
│   │                            DuplicateWorkspaceCategoryException, LastActiveWorkspaceCategoryException, …
│   ├── application/internal/{commandservices,queryservices}/  *Impl (+ CategoryCodeFactory: rótulo → code)
│   ├── infrastructure/persistence/   JPA entities + mapper + adapter (workspaces + workspace_categories)
│   └── interfaces/
│       ├── rest/     WorkspaceController + resources/transform + exception handler local
│       └── events/   AccountRegisteredWorkspaceProvisioner (crea el módulo "General" en el alta)
│
├── transactions/                          [bounded context core]
│   ├── domain/
│   │   ├── model/
│   │   │   ├── aggregates/   Transaction (aggregate root) + TransactionRepository
│   │   │   ├── valueobjects/ TransactionId, UserId, Money, Merchant, CategoryCode,
│   │   │   │                 TransactionStatus, ExtractionSource, TransactionType,
│   │   │   │                 WorkspaceId, WorkspaceCategoryId
│   │   │   ├── commands/     IngestBankNotificationCommand, UpdateTransactionCategoryCommand,
│   │   │   │                 MoveTransactionToWorkspaceCommand, RecordManualIncomeCommand, …
│   │   │   ├── queries/      FindTransactionByIdQuery, FindTransactionsByUserQuery
│   │   │   └── events/       TransactionCategorized, TransactionReceived
│   │   ├── services/         TransactionCommandService, TransactionQueryService (contratos)
│   │   │                     + TransactionDetail (read-model) + el resto de puertos:
│   │   │                     CategoryCatalog, TransactionExtractionService, MerchantCategoryCache,
│   │   │                     UserDirectory, TransactionNotifier
│   │   ├── policy/           CategorizedExpenseNotificationPolicy (+ impl por defecto)
│   │   ├── service/          BankNotificationHeuristicParser (servicio de dominio puro)
│   │   └── exception/        TransactionExtractionFailedException, UserNotFoundException
│   ├── application/
│   │   └── internal/
│   │       ├── commandservices/   TransactionCommandServiceImpl
│   │       ├── queryservices/     TransactionQueryServiceImpl
│   │       └── outboundservices/
│   │           └── acl/           UserDirectoryAdapter (ACL → Profile),
│   │                              WorkspaceDirectoryAdapter (ACL → Workspaces: módulo por
│   │                              defecto, validación de categoría, resolución de nombres)
│   ├── infrastructure/
│   │   ├── persistence/  JPA entities + mappers + adapters (Transaction, Category)
│   │   ├── llm/           Adaptador OpenAI Chat Completions + prompt + DTOs de cable
│   │   ├── cache/         Adaptador Redis
│   │   ├── notification/  Adaptador FCM real + mock (NoOp) + config Firebase
│   │   └── config/        Wiring de los objetos de dominio como beans de Spring
│   └── interfaces/rest/   TransactionWebhookController + resources/transform + exception handler local
│
└── shared/                                [plumbing técnico transversal, NO es un bounded context]
    └── interfaces/rest/    ApiError, GlobalExceptionHandler (catch-all, @Order LOWEST_PRECEDENCE)
```

**Reglas de dependencia:** `domain` no importa nada de `application`/`infrastructure`/
`interfaces` ni depende de Spring/JPA/Jackson (la única excepción pragmática son los
enums de vocabulario compartido — `TransactionStatus`, `ExtractionSource`, `PlanCode`,
`SubscriptionStatus` — reutilizados tal cual en la entidad JPA por no tener
comportamiento). `application` implementa los contratos de `domain` (Command/QueryService)
y orquesta, incluidas las llamadas salientes a otros contextos. `infrastructure`
implementa los puertos técnicos (persistencia, LLM, cache, push, hasheo, tokens).
`interfaces` solo conoce `application` (a través de las interfaces de dominio, inyectadas
por Spring) y sus propios `resources`/`transform`.

**El ACL vive en `application`, no en `infrastructure`:** llamar a otro bounded context
en el mismo proceso es orquestación (una invocación de método Java a otro Spring bean),
no I/O técnico real como sí lo son la BD, el LLM, Redis o FCM — por eso
`UserDirectoryAdapter` vive en `transactions.application.internal.outboundservices.acl`.
Sigue implementando el puerto de dominio `UserDirectory` (dependency inversion intacta) y
solo invoca la API pública de Profile (`profile.domain.services.UserProfileQueryService`),
nunca `profile.domain.model.*` directamente. `UserDirectory` separa a propósito "¿existe
el usuario?" de "¿tiene destino de notificación?": un perfil puede existir sin token FCM
todavía, y eso no debe rechazar el registro del gasto.

**Comandos y queries en `domain`, sus implementaciones en `application`:** en los cuatro
contextos, `XxxCommandService`/`XxxQueryService` son las interfaces que expone el
dominio; `XxxCommandServiceImpl`/`XxxQueryServiceImpl` (en
`application/internal/{commandservices,queryservices}`) son quienes realmente orquestan
repositorios y puertos. Los comandos de escritura devuelven solo el id resultante
(`TransactionId`, `UserId`, `SubscriptionId`); el controlador consulta después el detalle
completo vía el QueryService — separación clásica de lectura/escritura (CQRS), no una
llamada directa al agregado.

**Los agregados no tienen setters:** `Transaction.categorize()`/`.failExtraction()`,
`Subscription.cancel()`, `UserProfile.updateFcmToken()`/`.renameTo()` validan la
transición de estado (p. ej. no se puede categorizar una transacción dos veces, ni
cancelar una suscripción que no está ACTIVE) en vez de dejar que capas de arriba muten
campos libremente. `Transaction.categorize()` además encola el evento de dominio
`TransactionCategorized`, que `TransactionCommandServiceImpl` drena y despacha después de persistir.

**La política de notificación** (`CategorizedExpenseNotificationPolicy`) es la regla de
negocio identificada en el Event Storming ("el dueño de un gasto categorizado recibe un
push"): se nombra por la regla, no por su mecánica — no hay un `if (event == X) then Y`
expuesto como estructura; la implementación por defecto solo compone otros puertos de
dominio (`UserDirectory`, `CategoryCatalog`, `TransactionNotifier`), así que puede vivir
en `domain` igual que su contrato.

**Resources y Transform en `interfaces`:** ningún controlador recibe ni devuelve tipos de
dominio directamente. Los `Resource` son los DTOs HTTP; los `Assembler` en `transform/`
traducen Resource → Command/Query (entrada) y read-model → Resource (salida), así que un
cambio en el contrato JSON público nunca obliga a tocar el dominio, y viceversa.

## Protección de la API (IAM)

Toda la API es stateless y exige `Authorization: Bearer <token>` salvo
`/api/v1/iam/sign-up`, `/api/v1/iam/sign-in` y `/actuator/health`
(`iam/infrastructure/security/SecurityConfig`).

- **Hasheo**: `BCryptPasswordHasherAdapter` (BCrypt vía `spring-security-crypto`) — el
  dominio nunca ve ni persiste una contraseña en texto plano, solo `HashedPassword`.
- **Token**: `HmacTokenServiceAdapter` firma `userId|expiresAtEpochMillis` con
  HMAC-SHA256 (`base64url(payload).base64url(firma)`), comparación en tiempo constante
  (`MessageDigest.isEqual`) contra timing attacks. Deliberadamente no es JWT de librería
  para no traer una dependencia nueva en el MVP — vive detrás del mismo puerto
  `TokenService`, así que migrar a JJWT/Nimbus más adelante es un adaptador nuevo, no un
  cambio de dominio ni de la capa de interfaces.
- **Filtro**: `BearerTokenAuthenticationFilter` valida el header y autentica el request
  con el userId como principal. Sin token o con uno inválido, `BearerAuthenticationEntryPoint`
  devuelve `401` (no el `403` que da Spring Security por defecto sin un entry point
  configurado) con el mismo formato `ApiError` que el resto de la API.

## Modelo de datos

Migraciones Flyway en `src/main/resources/db/migration/`:

- **`V1__init_schema.sql`**: tablas `users`, `categories` y `transactions` (schema
  original, antes de dividir `users`).
- **`db/migration/dev/V2__seed_dev_user.sql`**: usuario demo
  (`11111111-1111-1111-1111-111111111111`), solo perfil `dev`.
- **`V3__split_users_into_iam_profile_subscription.sql`**: `users` → `user_profiles`
  (rename, se le quita `email` — pasa a ser propiedad exclusiva de IAM); tablas nuevas
  `credentials` y `subscriptions`. Un índice único parcial
  (`WHERE status = 'ACTIVE'`) refuerza a nivel de BD la misma regla que ya valida el
  caso de uso: una suscripción activa por usuario.
- **`db/migration/dev/V4__seed_dev_credentials.sql`**: credenciales del mismo usuario
  demo (`demo@smartcash.pe` / `demo1234`, hash BCrypt precalculado) para poder probar
  sign-in sin pasos extra. Solo perfil `dev`.
- **`V5`–`V13`**: columnas y tablas de los contextos que se sumaron después
  (`transactions.type` gasto/ingreso y `internal_transfer`, `extraction_source = MANUAL`,
  buzón de ingesta en `user_profiles`, `gmail_connections`, `pending_senders` +
  `user_trusted_senders`, `event_publication` de Spring Modulith, esquema de `groups`).
- **`V14__create_workspaces_schema.sql`**: tablas `workspaces` + `workspace_categories`;
  backfill de un módulo "General" por cada usuario existente, sembrado con una copia de
  las 8 categorías del catálogo global. Un índice único parcial (`WHERE is_default`)
  refuerza "un solo módulo General por usuario".
- **`V15__add_workspace_to_transactions.sql`**: `transactions.workspace_id` (NOT NULL,
  backfill al módulo General del dueño) + `transactions.workspace_category_id` (categoría
  de módulo custom, NULL para el General). Sin FK hacia `workspaces` (autonomía de
  contexto).

```sql
transactions (
  id UUID PK,
  user_id UUID FK -> user_profiles,
  workspace_id UUID NOT NULL,          -- módulo (sin FK: contexto workspaces)
  category_id BIGINT FK -> categories NULL,   -- categoría en el módulo "General"
  workspace_category_id UUID NULL,     -- categoría en un módulo custom (mutuamente excluyente con category_id)
  raw_text TEXT NOT NULL,      -- nunca se pierde, ni cuando falla la extracción
  amount NUMERIC(12,2),
  currency CHAR(3),
  merchant VARCHAR(150),
  status VARCHAR(20) CHECK (PENDING|PROCESSED|FAILED),
  type VARCHAR(10) CHECK (EXPENSE|INCOME),
  extraction_source VARCHAR(20) CHECK (LLM|CACHE|MANUAL),
  internal_transfer BOOLEAN NOT NULL DEFAULT FALSE,
  error_message TEXT,
  created_at TIMESTAMPTZ,
  processed_at TIMESTAMPTZ
)

workspaces (id UUID PK, owner_id UUID, name VARCHAR(60), color_hex CHAR(7),
            icon VARCHAR(40), is_default BOOLEAN, created_at, archived_at NULL)
workspace_categories (id UUID PK, workspace_id UUID FK -> workspaces, code VARCHAR(40),
                      display_name VARCHAR(60), icon VARCHAR(40), position INT,
                      archived BOOLEAN, UNIQUE (workspace_id, code))

credentials (id UUID PK, email VARCHAR UNIQUE, hashed_password VARCHAR, created_at)
user_profiles (id UUID PK, display_name VARCHAR, fcm_token TEXT, inbox_address VARCHAR,
               created_at, updated_at)
subscriptions (id UUID PK, user_id UUID, plan_code VARCHAR, status VARCHAR,
               started_at, renews_at, canceled_at)
```

No hay FKs entre `credentials`, `user_profiles`, `subscriptions` ni `workspaces`: cada
tabla pertenece a un bounded context distinto y solo comparten el UUID por convención — es
la misma regla de autonomía de contexto aplicada a nivel de esquema (`workspace_categories`
sí tiene FK a `workspaces` porque son un mismo agregado).

## Prompt de extracción

El system prompt exacto vive en
[`ExtractionPrompts.java`](src/main/java/pe/smartcash/cash/transactions/infrastructure/llm/ExtractionPrompts.java).
Exige JSON puro (sin markdown, sin texto extra), 4 claves fijas (`monto`, `moneda`,
`comercio`, `categoria`), moneda ISO 4217, categoría restringida al catálogo, y un caso
por defecto si el texto no trae una transacción reconocible. El JSON Schema real que se
envía como `response_format` (con `strict: true`) está en
[`OpenAiTransactionExtractionAdapter.java`](src/main/java/pe/smartcash/cash/transactions/infrastructure/llm/OpenAiTransactionExtractionAdapter.java).

## Manejo de errores

- **JSON malformado del LLM**: 1 reintento con prompt de corrección explícito → si
  persiste, `TransactionExtractionFailedException` → el agregado se marca `FAILED`
  (texto original preservado) → `422 Unprocessable Content`.
- **HTTP caído / timeout / 401 del LLM** (extracción): mismo resultado (`FAILED` + 422).
- **Asesor financiero sin LLM disponible**: si el proveedor primario falla (timeout de
  `ADVISOR_LLM_TIMEOUT`, 401, modelo dado de baja, respuesta vacía) se reintenta con el
  proveedor de respaldo (`FallbackAdvisorChatClient`); si ambos fallan →
  `AdvisorUnavailableException` → `503`. El fallo original del primario viaja adjunto como
  `suppressed` para no perder esa causa en Sentry.
- **Notificación push fallida**: nunca tumba el caso de uso — la transacción ya está
  persistida; solo se loguea un warning.
- **Sin token / token inválido o expirado**: `401` (`BearerAuthenticationEntryPoint`).
- **Email ya registrado** (sign-up): `409`. **Credenciales inválidas** (sign-in): `401`
  — el mensaje deliberadamente no dice si falló el email o la contraseña.
- **Usuario/perfil/suscripción no encontrados**: `404`, mapeado en un
  `@RestControllerAdvice` **local a cada contexto** (`@Order(HIGHEST_PRECEDENCE)`).
- **Suscripción activa duplicada**: `409`.
- Payload inválido (`@Valid`) o cualquier `IllegalArgumentException` (p. ej. un Value
  Object que rechaza su valor) → `400`, vía el `GlobalExceptionHandler` compartido
  (`@Order(LOWEST_PRECEDENCE)`, catch-all real). Excepciones no controladas → `500`.

> **Nota de diseño (@RestControllerAdvice):** Spring no combina por especificidad los
> `@ExceptionHandler` de distintos beans `@RestControllerAdvice` — resuelve con el
> primer bean (en orden de `@Order`) que tenga algún método aplicable. Sin `@Order`
> explícito, el catch-all genérico podía interceptar excepciones de un contexto
> específico antes que su handler dedicado y devolver `500` en vez del código correcto
> (se detectó y corrigió probando el escenario real, no por inspección de código).

> **Nota de diseño (401 vs 403):** sin un `AuthenticationEntryPoint` configurado, Spring
> Security responde `403 Forbidden` a un request sin autenticar — semánticamente
> incorrecto ("sé quién eres y no puedes" en vez de "no sé quién eres"). Se agregó
> `BearerAuthenticationEntryPoint` para forzar `401`, detectado probando el endpoint
> real, no leyendo la config.

## Cómo correr localmente

```bash
# Perfil dev: agrega el seed del usuario demo (+ sus credenciales) y arranca
# Postgres+Redis solos (spring-boot-docker-compose usa compose.yaml)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 1. Sign-in con el usuario demo sembrado
curl -X POST http://localhost:8080/api/v1/iam/sign-in \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@smartcash.pe","password":"demo1234"}'
# -> {"accessToken":"...", "expiresAt":"..."}

# 2. Usar el token para el resto de la API
curl -X POST http://localhost:8080/api/v1/transactions/webhook \
  -H "Content-Type: application/json" -H "Authorization: Bearer <accessToken>" \
  -d '{"userId":"11111111-1111-1111-1111-111111111111","rawText":"Consumo de S/24.50 en Starbucks"}'
```

Sin `LLM_API_KEY` configurada, el webhook responde `422` con la transacción marcada
`FAILED` (comportamiento esperado y probado — no hace falta clave real para levantar el
proyecto). Sin `FCM_ENABLED=true`, las notificaciones se loguean en vez de enviarse de
verdad (`NoOpTransactionNotifierAdapter`).

### Variables de entorno relevantes

| Variable | Default | Uso |
|---|---|---|
| `LLM_BASE_URL` | `https://api.openai.com/v1` | Endpoint del proveedor LLM primario (extracción de transacciones + asesor). Para Gemini vía su API OpenAI-compatible: `https://generativelanguage.googleapis.com/v1beta/openai`. |
| `LLM_API_KEY` | *(vacío)* | API key del proveedor primario. |
| `LLM_MODEL` | `gpt-4o-mini` | Modelo del proveedor primario (extracción + asesor). Ej. Gemini: `gemini-3.6-flash`. |
| `LLM_TIMEOUT` | `8s` | Timeout de la **extracción de transacciones** (`OpenAiTransactionExtractionAdapter`). Corto a propósito: corre en el worker async con reintentos (`app.llm.max-retries`), la meta es fallar rápido y reintentar. |
| `ADVISOR_LLM_TIMEOUT` | `45s` | Timeout del **asesor financiero** (`GeminiFinancialAdvisorAdapter`, vía `AdvisorLlmClientConfig`). Separado de `LLM_TIMEOUT` porque los modelos de chat con razonamiento (p. ej. `gemini-3.x-flash`) tardan 10-30s en responder sin streaming; subir `LLM_TIMEOUT` en cambio arrastraría los reintentos del worker de ingesta. |
| `GROQ_BASE_URL` | `https://api.groq.com/openai/v1` | Endpoint de Groq Cloud (groq.com -- NO confundir con Grok de xAI, que requiere saldo cargado), proveedor de respaldo del asesor y de la extracción de transacciones (ver `FallbackAdvisorChatClient`/`FallbackTransactionExtractionService`). Solo se usa si el proveedor primario (Gemini) falla. Acepta cualquier proveedor con dialecto OpenAI Chat Completions. |
| `GROQ_API_KEY` | *(vacío)* | API key del proveedor de respaldo (gratis en [console.groq.com](https://console.groq.com), sin tarjeta). |
| `GROQ_MODEL` | `llama-3.3-70b-versatile` | Modelo de respaldo a usar. |
| `GROQ_TIMEOUT` | `45s` | Timeout del proveedor de respaldo. Mismo criterio de latencia que `ADVISOR_LLM_TIMEOUT`. |
| `FCM_ENABLED` | `false` | `true` activa la integración real con Firebase. |
| `FCM_CREDENTIALS_PATH` | `/secrets/firebase-service-account.json` | Service account JSON de Firebase. |
| `IAM_TOKEN_SECRET` | placeholder de dev, **cambiar en cualquier despliegue real** | Secreto HMAC que firma los access tokens. |

## Docker

```bash
docker build -t smartcash-backend .
```

Multi-stage (`eclipse-temurin:21-jdk-alpine` para build, `-jre-alpine` para runtime),
usuario no-root, healthcheck contra `/actuator/health` (público, `permitAll` en
`SecurityConfig`) — listo para Azure Container Apps / App Service.

## Cambios hechos sobre el scaffold original (histórico)

El repo ya traía Spring Boot 4.1 + Gradle + Postgres vía Docker Compose sin código de
negocio. Al integrar el feature por primera vez se encontraron y corrigieron dos
problemas reales del scaffold (rompían el build/arranque, no relacionados con la lógica
de negocio):

1. **Conflicto de plugins Gradle**: `com.netflix.dgs.codegen` (GraphQL, sin usar) y
   `org.hibernate.orm` competían por la misma tarea `generateJava`. Se quitó el plugin DGS.
2. **Spring Modulith con dos backends de persistencia a la vez**: `spring-boot-starter-data-jdbc`
   + `spring-modulith-starter-jdbc` convivían con `-jpa`, y Modulith no podía decidir
   qué `EventPublicationRepository` usar. Se dejó solo el stack JPA.

También se ajustó el código a diferencias reales de API entre Spring Boot/Jackson
"clásico" y lo que trae Boot 4.1 (verificado compilando y corriendo la app):

- Flyway en Boot 4 requiere el starter dedicado `spring-boot-starter-flyway` — tener
  `flyway-core` en el classpath ya no alcanza (auto-config modularizado).
- Jackson pasó a Jackson 3 (`tools.jackson.databind.*`, no `com.fasterxml.jackson.databind.*`;
  las anotaciones sí siguen en `com.fasterxml.jackson.annotation`), y sus excepciones
  (`JacksonException`) ahora son unchecked.
- El builder de `RestClient` usa `org.springframework.boot.http.client.HttpClientSettings`
  (no `ClientHttpRequestFactorySettings`, que no existe en esta versión).
- `HttpStatus.UNPROCESSABLE_ENTITY` está deprecado a favor de `UNPROCESSABLE_CONTENT`.

## Validado end-to-end

Cada vuelta de este proyecto (MVP original, refactor a DDD, ajuste de capas, y el split
de `users` en `iam`/`profile`/`subscription`) se corrió de verdad (Postgres+Redis vía
Docker Compose, perfil `dev`) contra un LLM mock local. La última pasada probó
específicamente:

- Webhook sin token → `401`; con token válido → `201`/`422` según corresponda.
- Sign-in con contraseña incorrecta → `401`; sign-up con email repetido → `409`.
- Sign-up + registrar perfil + suscribirse a `PREMIUM` → `201` en cada paso.
- Segunda suscripción activa para el mismo usuario → `409`; cancelar y volver a
  consultar → `404` (ya no hay ninguna activa).
- Atajo de Redis (segunda compra en el mismo comercio no vuelve a llamar al LLM).

Dos bugs reales se encontraron y corrigieron en esta pasada, ambos solo visibles
corriendo la app (no por inspección de código):

1. **Firma de token duplicada**: `HmacTokenServiceAdapter.issue()` codificaba en base64
   la firma dos veces (`encode(sign(payload))`, cuando `sign()` ya devuelve base64url),
   mientras `validate()` comparaba contra una sola codificación — todo token válido
   fallaba su propia validación.
2. **403 en vez de 401**: sin `AuthenticationEntryPoint`, Spring Security respondía
   `403 Forbidden` a requests sin autenticar (ver nota de diseño arriba).

## Pendiente para siguientes iteraciones (fuera de este MVP)

- Endpoint de logout / revocación de tokens (hoy expiran solos a las 2h, no hay blacklist).
- Refresh tokens (hoy hay que volver a hacer sign-in cuando expira).
- Reproceso de transacciones `FAILED` (job o endpoint manual).
- Tests automatizados (unitarios de los agregados y application services, integración
  de los controladores).
- Formalizar los límites de bounded context con `spring-modulith` (`@NamedInterface` en
  cada `domain.services` y un test `ApplicationModules.of(CashApplication.class).verify()`)
  para que la regla "cada contexto solo habla con otro vía su ACL" quede garantizada en
  tiempo de compilación/test, no solo por disciplina de código.
- ~~Un flujo de "onboarding" orquestado (IAM emite el evento `AccountRegistered` → Profile
  crea el perfil automáticamente)~~ — **hecho**: `iam` emite `AccountRegisteredEvent` y
  tanto `profile` (crea el perfil + buzón de ingesta) como `workspaces` (crea el módulo
  "General") lo escuchan de forma síncrona en la misma transacción del sign-up.
