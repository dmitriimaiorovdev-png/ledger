# Ledger — UML Class Diagram

Упорядоченное текстовое описание. Обозначения UML и исходные сигнатуры сохранены.

Обозначения: `+` public, `-` private, `#` protected.

## Model (Entity)

### User

```text
User
-id: Long
-email: String
-passwordHash: String
-firstName: String
-lastName: String
-role: Role = USER
-status: UserStatus = ACTIVE
-failedLoginAttempts: int = 0
-createdAt: Instant
-updatedAt: Instant
+getId()/setId(Long)
+getEmail()/setEmail(String)
+getPasswordHash()/setPasswordHash(String)
+getFirstName()/setFirstName(String)
+getLastName()/setLastName(String)
+getRole()/setRole(Role)
+getStatus()/setStatus(Status)
+getFailedLoginAttempts()/setFailedLoginAttempts(int)
+getCreatedAt(): Instant
+getUpdatedAt(): Instant
-onCreate(): void «@PrePersist»
-onUpdate(): void «@PreUpdate»
```

### Wallet

```text
Wallet
-id: Long
-user: User «LAZY, NOT NULL»
-currency: String «length=3, immutable»
-status: WalletStatus = ACTIVE
-version: long «@Version»
-createdAt: Instant
-balance: BigDecimal = 0 «NUMERIC(28,12)»
+getId()/setId(Long)
+getUser()/setUser(User)
+getCurrency()/setCurrency(String)
+getStatus()/setStatus(Status)
+getVersion(): long
+getBalance()/setBalance(BigDecimal)
+getCreatedAt(): Instant
-onCreate(): void «@PrePersist»
```

### Transfer

```text
Transfer
-id: Long
-idempotencyKey: String «UNIQUE»
-user: User «NOT NULL»
-fromWallet: Wallet «LAZY»
-toWallet: Wallet «LAZY»
-currency: String
-amount: BigDecimal
-convertedAmount: BigDecimal
-fxRate: BigDecimal
-fee: BigDecimal = 0
-status: TransferStatus = PENDING
-errorMessage: String
-version: long «@Version»
-createdAt: Instant
-completedAt: Instant
+все геттеры/сеттеры полей
+getIdempotencyKey()/setIdempotencyKey(String)
+setCompletedAt(Instant)
-onCreate(): void «@PrePersist»
```

### PaymentTransaction

```text
PaymentTransaction
-id: Long
-idempotencyKey: String «UNIQUE»
-user: User «NOT NULL»
-wallet: Wallet «NOT NULL»
-type: PaymentType
-amount: BigDecimal
-currency: String
-status: PaymentStatus = PENDING
-externalPaymentId: String
-failureReason: String
-attempts: int = 0
-version: long «@Version»
-createdAt: Instant
-updatedAt: Instant
+все геттеры/сеттеры полей
-onCreate(): void «@PrePersist»
-onUpdate(): void «@PreUpdate»
```

### LedgerEntry

```text
LedgerEntry
-id: Long
-wallet: Wallet «NOT NULL»
-transfer: Transfer «nullable FK»
-payment: PaymentTransaction «nullable FK»
-entryType: EntryType
-amount: BigDecimal
-balanceAfter: BigDecimal
-currency: String
-description: String
-createdAt: Instant
+все геттеры/сеттеры полей
-operationKey: String «UNIQUE(wallet_id, operation_key)»
-onCreate(): void «@PrePersist»
```

### ExchangeRate

```text
ExchangeRate
-id: Long
-baseCurrency: String «immutable»
-quoteCurrency: String «immutable»
-updatedAt: Instant
+все геттеры/сеттеры полей
-rate: BigDecimal «NUMERIC(28,12)»
-onPersist(): void «@PrePersist @PreUpdate»
```

### OutboxEvent

```text
OutboxEvent
-id: Long
-aggregateType: String
-aggregateId: String
-eventType: String
-status: OutboxStatus = PENDING
-attempts: int = 0
-availableAt: Instant
-createdAt: Instant
-publishedAt: Instant
-version: long «@Version»
+все геттеры/сеттеры полей
-payload: String «JSONB, @JdbcTypeCode(JSON)»
-onCreate(): void «@PrePersist»
```

## Enum

### Role

```text
Role
USER
ADMIN
SYSTEM
```

### UserStatus

```text
UserStatus
ACTIVE
LOCKED
```

### WalletStatus

```text
WalletStatus
ACTIVE
FROZEN
CLOSED
```

### EntryType

```text
EntryType
DEBIT
CREDIT
```

### TransferStatus

```text
TransferStatus
PENDING
COMPLETED
ROLLED_BACK
FAILED
```

### PaymentType

```text
PaymentType
TOP_UP
WITHDRAW
```

### PaymentStatus

```text
PaymentStatus
PENDING
COMPLETED
FAILED
ROLLED_BACK
```

### OutboxStatus

```text
OutboxStatus
PENDING
PUBLISHED
FAILED
```

## Repository

### UserRepository

```text
UserRepository
+findByEmailIgnoreCase(String): Optional<User>
+existsByEmailIgnoreCase(String): boolean
```

### WalletRepository

```text
WalletRepository
+findByUserIdOrderByIdAsc(Long): List<Wallet>
+findByIdAndUserId(Long, Long): Optional<Wallet>
+existsByUserIdAndCurrency(Long, String): boolean
+findByIdForUpdate(Long): Optional<Wallet> «PESSIMISTIC_WRITE»
+findSystemWallet(String, String): Optional<Wallet> «PESSIMISTIC_WRITE»
```

### LedgerEntryRepository

```text
LedgerEntryRepository
+findByWalletIdOrderByCreatedAtDescIdDesc(Long, Pageable): Page<LedgerEntry>
+findFirstByWalletIdOrderByIdDesc(Long): Optional<LedgerEntry>
+existsByWalletIdAndOperationKey(Long, String): boolean
```

### TransferRepository

```text
TransferRepository
+findByIdAndUserId(Long, Long): Optional<Transfer>
+findByUserIdOrderByCreatedAtDesc(Long): List<Transfer>
+findByIdempotencyKey(String): Optional<Transfer>
+findByIdForUpdate(Long): Optional<Transfer> «PESSIMISTIC_WRITE»
```

### PaymentTransactionRepository

```text
PaymentTransactionRepository
+findByIdAndUserId(Long, Long): Optional<PaymentTransaction>
+findByIdempotencyKey(String): Optional<PaymentTransaction>
+findByUserIdOrderByCreatedAtDesc(Long): List<PaymentTransaction>
+findByIdForUpdate(Long): Optional<PaymentTransaction> «PESSIMISTIC_WRITE»
```

### ExchangeRateRepository

```text
ExchangeRateRepository
+findByBaseCurrencyAndQuoteCurrency(String, String): Optional<ExchangeRate>
+findByBaseCurrencyOrderByQuoteCurrency(String): List<ExchangeRate>
```

### OutboxRepository

```text
OutboxRepository
+lockNextBatch(Instant, int): List<OutboxEvent> «FOR UPDATE SKIP LOCKED»
+countByStatus(OutboxEvent.Status): long
+findFirstByEventTypeOrderByIdDesc(String): Optional<OutboxEvent>
```

## DTO

### AuthResponse

```text
«record»
AuthResponse
+accessToken: String
+tokenType: String
+expiresInSeconds: long
+refreshToken: String
+user: UserDto
+of(String, long, String, UserDto): AuthResponse
```

### RegisterRequest

```text
«record»
RegisterRequest
+firstName: String
+lastName: String
+email: String «@Email @Size(max=255)»
+password: String «@Size(min=8,max=72)»
```

### LoginRequest

```text
«record»
LoginRequest
+email: String «@Email»
+password: String
```

### RefreshRequest

```text
«record»
RefreshRequest
+refreshToken: String «@NotBlank»
```

### UserDto

```text
«record»
UserDto
+id: Long
+email: String
+firstName: String
+lastName: String
+role: String
+status: String
+createdAt: Instant
+from(User): UserDto
```

### WalletDto

```text
«record»
WalletDto
+id: Long
+currency: String
+status: String
+balance: BigDecimal
+createdAt: Instant
+of(Wallet, BigDecimal): WalletDto
```

### CreateWalletRequest

```text
«record»
CreateWalletRequest
+currency: String «@Pattern([A-Z]{3})»
```

### TransferRequest

```text
«record»
TransferRequest
+fromWalletId: Long
+toWalletId: Long
+amount: BigDecimal «@DecimalMin(0.01) @Digits(10,2)»
```

### TransferDto

```text
«record»
TransferDto
+id / fromWalletId / toWalletId: Long
+currency: String
+amount / convertedAmount / fxRate / fee: BigDecimal
+status: String
+errorMessage: String
+createdAt / completedAt: Instant
+from(Transfer): TransferDto
```

### TopUpRequest

```text
«record»
TopUpRequest
+amount: BigDecimal «validated»
+cardToken: String
```

### WithdrawRequest

```text
«record»
WithdrawRequest
+amount: BigDecimal «validated»
+bankAccount: String
```

### PaymentDto

```text
«record»
PaymentDto
+id / walletId: Long
+type: String
+amount: BigDecimal
+currency: String
+status: String
+externalPaymentId: String
+failureReason: String
+createdAt / updatedAt: Instant
+from(PaymentTransaction): PaymentDto
```

### LedgerEntryDto

```text
«record»
LedgerEntryDto
+id / walletId / transferId / paymentId: Long
+entryType: String
+amount / balanceAfter: BigDecimal
+currency: String
+description: String
+operationKey: String
+createdAt: Instant
+from(LedgerEntry): LedgerEntryDto
```

### ExchangeRateDto

```text
«record»
ExchangeRateDto
+baseCurrency: String
+quoteCurrency: String
+rate: BigDecimal
+updatedAt: Instant
+from(ExchangeRate): ExchangeRateDto
```

### ExchangeRateUpdateRequest

```text
«record»
ExchangeRateUpdateRequest
+rate: BigDecimal «positive»
+baseCurrency: String «@Pattern([A-Z]{3})»
+quoteCurrency: String «@Pattern([A-Z]{3})»
```

### ErrorResponse

```text
«record»
ErrorResponse
+timestamp: Instant
+status: int
+error: String
+message: String
+fieldErrors: Map<String,String>
+of(int, String, String): ErrorResponse
+of(int, String, String, Map): ErrorResponse
```

### PageResponse

```text
«record»
PageResponse
T
+content: List<T>
+page: int
+size: int
+totalElements: long
+totalPages: int
+first: boolean
+last: boolean
+<E,T> of(Page<E>, Function<E,T>): PageResponse<T>
```

## Service

### AuthService

```text
AuthService
-userRepository: UserRepository
-passwordEncoder: PasswordEncoder
-jwtService: JwtService
-refreshTokenService: RefreshTokenService
-rateLimitService: RateLimitService
-properties: LedgerProperties
-MAX_FAILED_LOGINS: int = 5
+register(RegisterRequest): AuthResponse
+login(LoginRequest): AuthResponse
+refresh(String): AuthResponse
+logout(String): void
+me(Long): UserDto
-handleFailedLogin(User): void
-resetFailedLogins(User): void
-issueTokens(User): AuthResponse
-issueTokens(User, String): AuthResponse
```

### TransferService

```text
TransferService
-transferRepository: TransferRepository
-walletRepository: WalletRepository
-ledgerService: LedgerService
-fxService: FxService
-outboxService: OutboxService
-properties: LedgerProperties
-MONEY_SCALE: int = 8
+transfer(User, TransferRequest): TransferDto
+get(Long, Long): TransferDto
+list(Long): List<TransferDto>
+listAll(): List<TransferDto>
-computeFee(BigDecimal): BigDecimal
-requireActive(Wallet): void
-eventPayload(Transfer): Map<String,Object>
```

### PaymentSagaService

```text
PaymentSagaService
-walletRepository: WalletRepository
-gatewayClient: MockGatewayClient
-steps: PaymentSagaSteps
+topUp(User, Long, TopUpRequest, String): PaymentDto
+withdraw(User, Long, WithdrawRequest, String): PaymentDto
-runTopUp(User, Long, BigDecimal, String): PaymentDto
-runWithdraw(User, Long, BigDecimal, String): PaymentDto
+get(Long, Long): PaymentDto
+list(Long): List<PaymentDto>
+listAll(): List<PaymentDto>
```

### PaymentSagaSteps

```text
PaymentSagaSteps
-paymentRepository: PaymentTransactionRepository
-walletRepository: WalletRepository
-ledgerService: LedgerService
-outboxService: OutboxService
+initiate(Wallet, PaymentTransaction): PaymentTransaction
+settle(Long, GatewayResponse): PaymentTransaction
+compensate(Long, Throwable): PaymentTransaction
+getPayment(Long, Long): PaymentDto
+listPayments(Long): List<PaymentDto>
+listAllPayments(): List<PaymentDto>
-eventPayload(PaymentTransaction): Map<String,Object>
```

### LedgerService

```text
LedgerService
-ledgerEntryRepository: LedgerEntryRepository
+currentBalance(Wallet): BigDecimal
+post(Wallet, EntryType, BigDecimal, String, String): BigDecimal
```

### FxService

```text
FxService
-exchangeRateRepository: ExchangeRateRepository
-pivot: String
-pivotRates: Map<String,BigDecimal> «volatile»
-ONE: BigDecimal = 1
+refresh(): void
+rate(String, String): BigDecimal
+convert(BigDecimal, String, String): BigDecimal
+supportedCurrencies(): List<String>
-normalize(String): void
```

### WalletService

```text
WalletService
-walletRepository: WalletRepository
-userRepository: UserRepository
-ledgerService: LedgerService
-fxService: FxService
+create(Long, CreateWalletRequest): WalletDto
+list(Long): List<WalletDto>
+get(Long, Long): WalletDto
+requireOwned(Long, Long): Wallet
+findOwned(Long, Long): Wallet
```

### IdempotencyService

```text
IdempotencyService
-redisTemplate: RedisTemplate<String,Object>
-objectMapper: ObjectMapper
-ttl: Duration
-useRedis: boolean
-KEY_PREFIX: String = "idem:"
-PENDING: String = "pending"
-PENDING_WAIT_MS: int = 3000
-PENDING_POLL_MS: int = 100
+execute(Long, String, Supplier<ResponseEntity<?>>): ResponseEntity<?>
-read(String): Optional<CachedResponse>
-waitForResult(String): Optional<CachedResponse>
-reconstruct(CachedResponse): ResponseEntity<?>
-bodyOf(ResponseEntity<?>): String
-store(String, CachedResponse): void
-serialize(CachedResponse): String
-deserialize(String): CachedResponse
-deserializeBody(String): Object
-key(Long, String): String
```

### OutboxService

```text
OutboxService
-outboxRepository: OutboxRepository
-objectMapper: ObjectMapper
+emit(String, String, String, Map<String,Object>): void
-writeJson(Map<String,Object>): String
```

### ExchangeRateService

```text
ExchangeRateService
-exchangeRateRepository: ExchangeRateRepository
-fxService: FxService
+list(String): List<ExchangeRateDto>
+upsert(ExchangeRateUpdateRequest): ExchangeRateDto
+delete(String, String): void
```

### RateLimitService

```text
RateLimitService
-redisTemplate: RedisTemplate<String,Object>
-useRedis: boolean
+check(String, int, Duration): void
```

## Controller

### AuthController

```text
AuthController
-authService: AuthService
+register(RegisterRequest): ResponseEntity<AuthResponse>
+login(LoginRequest): ResponseEntity<AuthResponse>
+refresh(RefreshRequest): ResponseEntity<AuthResponse>
+logout(RefreshRequest): ResponseEntity<Void>
+me(User): ResponseEntity<UserDto>
```

### WalletController

```text
WalletController
-walletService: WalletService
-ledgerEntryRepository: LedgerEntryRepository
+create(User, CreateWalletRequest): ResponseEntity<WalletDto>
+list(User): ResponseEntity<List<WalletDto>>
+get(User, Long): ResponseEntity<WalletDto>
+ledger(User, Long, int, int): ResponseEntity<PageResponse<LedgerEntryDto>>
```

### TransferController

```text
TransferController
-transferService: TransferService
-idempotencyService: IdempotencyService
-rateLimitService: RateLimitService
-properties: LedgerProperties
+transfer(User, TransferRequest, String): ResponseEntity<?>
+list(User): ResponseEntity<List<TransferDto>>
+get(User, Long): ResponseEntity<TransferDto>
```

### PaymentController

```text
PaymentController
-sagaService: PaymentSagaService
-idempotencyService: IdempotencyService
-rateLimitService: RateLimitService
-properties: LedgerProperties
+topUp(User, Long, TopUpRequest, String): ResponseEntity<?>
+withdraw(User, Long, WithdrawRequest, String): ResponseEntity<?>
+list(User): ResponseEntity<List<PaymentDto>>
+get(User, Long): ResponseEntity<PaymentDto>
```

### ExchangeRateController

```text
ExchangeRateController
-exchangeRateService: ExchangeRateService
+list(String): ResponseEntity<List<ExchangeRateDto>>
```

### AdminController

```text
AdminController
-exchangeRateService: ExchangeRateService
-transferService: TransferService
-paymentSagaService: PaymentSagaService
+upsertRate(ExchangeRateUpdateRequest): ResponseEntity<ExchangeRateDto>
+deleteRate(String, String): ResponseEntity<Void>
+allTransfers(): ResponseEntity<List<TransferDto>>
+allPayments(): ResponseEntity<List<PaymentDto>>
```

## Security

### JwtService

```text
JwtService
-key: SecretKey
-accessTokenTtl: Duration
+generateAccessToken(User): String
+parse(String): Claims
+getAccessTokenTtlSeconds(): long
```

### RefreshTokenService

```text
RefreshTokenService
-redisTemplate: RedisTemplate<String,Object>
-objectMapper: ObjectMapper
-ttl: Duration
-useRedis: boolean
-secureRandom: SecureRandom
-memory: ConcurrentHashMap<String,TokenEntry>
-memoryFamilies: ConcurrentHashMap<String,Set<String>>
-TOKEN_KEY_PREFIX: String = "rt:"
-FAMILY_KEY_PREFIX: String = "rtf:"
+issue(Long): String
-issueInFamily(String, Long): String
+rotate(String): String
+revoke(String): void
+validate(String): RefreshTokenRecord
-revokeOne(String, String): void
-revokeFamily(String): void
-toJson(TokenEntry): String
-fromJson(String): TokenEntry
-randomId(int): String
```

### JwtAuthenticationFilter

```text
JwtAuthenticationFilter
-jwtService: JwtService
-userRepository: UserRepository
#doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain): void
```

### SecurityConfig

```text
SecurityConfig
+securityFilterChain(HttpSecurity): SecurityFilterChain
+corsConfigurationSource(): CorsConfigurationSource
+passwordEncoder(): PasswordEncoder
+authenticationManager(AuthenticationConfiguration): AuthenticationManager
```

### RestAuthenticationEntryPoint

```text
RestAuthenticationEntryPoint
-objectMapper: ObjectMapper
+commence(HttpServletRequest, HttpServletResponse, AuthenticationException): void
```

### RefreshTokenRecord

```text
«record»
RefreshTokenRecord
+familyId: String
+tokenId: String
+userId: Long
+expiresAtEpochSeconds: long
+asString(): String
+split(String): String[]
```

### RefreshTokenReuseException

```text
RefreshTokenReuseException
+RefreshTokenReuseException(String familyId)
```

## Gateway

### MockGatewayClient

```text
MockGatewayClient
-restClient: RestClient
-gatewayTimer: Timer
+charge(String, String, BigDecimal): GatewayResponse
+payout(String, String, BigDecimal): GatewayResponse
-call(String, GatewayChargeRequest): GatewayResponse
-recordDuration(long): void
```

### RestClientFactory

```text
«utility»
RestClientFactory
+withTimeouts(Duration, Duration): JdkClientHttpRequestFactory
```

### GatewayClientException

```text
«record»
GatewayClientException
```

### GatewayResponse

```text
«record»
GatewayResponse
+id: String
+status: String
```

### GatewayChargeRequest

```text
«record»
GatewayChargeRequest
+amount: BigDecimal
+currency: String
+reference: String
```

## Messaging

### OutboxPoller

```text
OutboxPoller
-outboxRepository: OutboxRepository
-publisher: KafkaEventPublisher
-properties: LedgerProperties
-meterRegistry: MeterRegistry
+publishPending(): void
-backoff(int): Duration
```

### KafkaEventPublisher

```text
KafkaEventPublisher
-kafkaTemplate: KafkaTemplate<String,Object>
-objectMapper: ObjectMapper
-publishedCounter: Counter
-publishErrors: Counter
-SEND_TIMEOUT: Duration = 5s
+publish(OutboxEvent): void
-parsePayload(String): Object
```

### NotificationConsumer

```text
NotificationConsumer
-kafkaTemplate: KafkaTemplate<String,Object>
-consumed: Counter
-failed: Counter
+onEvent(OutboxMessage, Acknowledgment): void
+onRetry(OutboxMessage, Acknowledgment): void
+onDlt(OutboxMessage, Acknowledgment): void
-handle(OutboxMessage): void
-forward(OutboxMessage, String): void
```

### KafkaTopics

```text
«utility»
KafkaTopics
+TRANSFERS: String
+PAYMENTS: String
+WALLETS: String
+NOTIFICATIONS: String
+RETRY_SUFFIX: String = ".retry"
+DLT_SUFFIX: String = ".dlt"
+retryOf(String): String
+dltOf(String): String
+topicFor(String): String
```

### OutboxMessage

```text
«record»
OutboxMessage
+eventId: String
+aggregateType: String
+aggregateId: String
+eventType: String
+payload: Object
+occurredAt: Instant
```

## Exception

### ApiException

```text
ApiException
#status: HttpStatus
+ApiException(HttpStatus, String) «protected»
+getStatus(): HttpStatus
```

### BadRequestException

```text
BadRequestException
+BadRequestException(String) «400»
```

### ConflictException

```text
ConflictException
+ConflictException(String) «409»
```

### ResourceNotFoundException

```text
ResourceNotFoundException
+ResourceNotFoundException(String) «404»
```

### InsufficientFundsException

```text
InsufficientFundsException
+InsufficientFundsException(String) «422»
```

### RateLimitExceededException

```text
RateLimitExceededException
+RateLimitExceededException(String) «429»
```

### GatewayUnavailableException

```text
GatewayUnavailableException
+GatewayUnavailableException(String) «502»
```

### GlobalExceptionHandler

```text
GlobalExceptionHandler
-log: Logger
+handleApi(ApiException): ResponseEntity<ErrorResponse>
+handleValidation(MethodArgumentNotValidException): ResponseEntity<ErrorResponse>
+handleMissingHeader(MissingRequestHeaderException): ResponseEntity<ErrorResponse>
+handleBadCredentials(BadCredentialsException): ResponseEntity<ErrorResponse>
+handleAuthentication(AuthenticationException): ResponseEntity<ErrorResponse>
+handleAccessDenied(AccessDeniedException): ResponseEntity<ErrorResponse>
+handleOptimisticLock(OptimisticLockingFailureException): ResponseEntity<ErrorResponse>
+handleDataIntegrity(DataIntegrityViolationException): ResponseEntity<ErrorResponse>
+handleGeneric(Exception): ResponseEntity<ErrorResponse>
```

## Config

### LedgerApplication

```text
LedgerApplication
+main(String[]): void
```

### AppConfig

```text
AppConfig
+redisTemplate(RedisConnectionFactory): RedisTemplate<String,Object>
```

### OpenApiConfig

```text
OpenApiConfig
+ledgerOpenApi(): OpenAPI
```

### DataSeeder

```text
DataSeeder
-userRepository / walletRepository
-fxService / passwordEncoder / properties
+run(String...): void
-seedAdmin(): void
-seedFeeWallets(): void
```

### LedgerProperties

```text
«record»
LedgerProperties
+security(): Security
+idempotency(): Idempotency
+transfer(): Transfer
+rateLimit(): RateLimit
+outbox(): Outbox
+gateway(): Gateway
+fx(): Fx
+app(): App
```

### LedgerProperties.Security

```text
LedgerProperties.Security
+jwtSecret: String
+accessTokenTtl: Duration
+refreshTokenTtl: Duration
+refreshTokenStore: String
```

### LedgerProperties.Transfer

```text
LedgerProperties.Transfer
+feePercent: BigDecimal
+minAmount: BigDecimal
+maxAmount: BigDecimal
```

### LedgerProperties.Outbox

```text
LedgerProperties.Outbox
+pollIntervalMs: long
+batchSize: int
+maxAttempts: int
```

### LedgerProperties.Gateway

```text
LedgerProperties.Gateway
+baseUrl: String
+connectTimeoutMs: int
+readTimeoutMs: int
+settleAsync: boolean
```

## Дополнительные элементы и подписи исходной диаграммы

```text
PasswordEncoder
RedisTemplate
JdkClientHttpRequestFactory
не extends,
самостоятельный
отправляет
получает
создаёт
потребляет
маршрутизация
владеет
1
совершает
1
инициирует
1
содержит
1
from/to
transfer_id
1
payment_id
1
обрабатывает
возвращает
пишет
```

Примечание: последние подписи связей сохранены как в источнике; без графической диаграммы нельзя однозначно сопоставить их с конкретными классами.
