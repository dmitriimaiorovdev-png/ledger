# Интеграционные тесты Repository

Java 17, Spring Boot 4.1.2, JUnit Jupiter 6.0.3. Используются существующие зависимости и версии Spring Boot; pom.xml не изменён при подготовке тестов.

## Подключение

Перед разрешённым запуском задайте переменные окружения:

- LEDGER_TEST_DB_URL — по умолчанию jdbc:postgresql://localhost:5432/ledger_test.
- LEDGER_TEST_DB_USERNAME — обязательное имя пользователя тестовой базы.
- LEDGER_TEST_DB_PASSWORD — обязательная переменная с паролем, без сохранения в файлах проекта.

URL допускает только PostgreSQL, один host, необязательный port и точный путь /ledger_test. Параметры запроса, userinfo, fragment, percent encoding имени базы и альтернативные формы JDBC URL запрещены. Для нестандартного адреса измените LEDGER_TEST_DB_URL, сохранив имя базы.

Обычные DB_URL, DB_USERNAME, DB_PASSWORD не используются тестовым DataSource.

## Защита и изоляция

RepositoryIntegrationTest подключает только RepositoryTestConfiguration и LedgerTestDatabaseGuard. Конфигурация не включает основное приложение и его auto-configuration. Существующий LedgerApplicationTests также наследует защищённую конфигурацию.

Initializer проверяет эффективный spring.datasource.url до создания любых beans. Неверный URL останавливает контекст до открытия соединения. Отдельный spring.flyway.url и альтернативные URL JPA/Hikari также проверяются и при конфликте отклоняются.

Настройки фиксируются в отдельном объекте после проверки. Единственный DataSource создаётся явно; Flyway и JPA используют именно его. При каждом открытии соединения URL проверяется повторно и Connection.getCatalog() должен вернуть ledger_test до установки схемы.

Внутри ledger_test создаётся новая схема repository_it_<UUID> на каждый новый Spring context. Flyway применяет существующую V1 только в эту схему. Hibernate выполняет validate. Flyway clean запрещён; baselineOnMigrate выключен. Созданная схема и Flyway history остаются после запуска, автоматического удаления схем нет.

Обычные тесты откатываются транзакционно. Проверки блокировок создают собственные зафиксированные fixtures и в finally удаляют только их записи по ID. Последовательный запуск исключает взаимное влияние тестов. Последовательности PostgreSQL при rollback не откатываются.

## Покрытие

- UserRepository: поиск и exists без учёта регистра, отсутствие результата.
- ExchangeRateRepository: поиск пары, фильтрация baseCurrency, сортировка quoteCurrency.
- WalletRepository: владелец, валюта, порядок ID, SYSTEM wallet.
- TransferRepository и PaymentTransactionRepository: владелец, idempotencyKey, сортировка времени, UNIQUE idempotency_key.
- LedgerEntryRepository: фильтрация wallet, страницы, createdAt DESC / id DESC, последний ID, exists, UNIQUE(wallet_id, operation_key); одинаковый key в разных кошельках допустим.
- OutboxRepository: PENDING, availableAt <= now, граница времени, порядок availableAt / id, limit, статусы, последний eventType, отказ без транзакции.
- RepositoryLockingIntegrationTest: все три findByIdForUpdate, findSystemWallet, SKIP LOCKED и освобождение блокировок.
- LedgerTestDatabaseGuardTest: отклонение production URL и обходных форм без подключений к БД.

Ограничения уникальности email, пары exchange rate и пары user/currency отсутствуют в V1 и не предполагаются тестами.

Блокировки проверяются независимыми транзакциями REQUIRES_NEW с отдельными соединениями. Пока внешняя транзакция удерживает блокировку, внутренняя выполняет NOWAIT и должна получить PostgreSQL SQLSTATE 55P03. Проверяется также EntityManager.getLockMode() == PESSIMISTIC_WRITE. Для SKIP LOCKED вторая транзакция получает следующую строку, первая остаётся заблокированной; после rollback обе строки снова доступны. Таймауты ограничены, sleep не используется.

## Запуск только после отдельного разрешения

В окружении с Java 17 и заданными LEDGER_TEST_DB_*:

```powershell
.\mvnw.cmd test
```

Этот запуск применит V1 в новой тестовой схеме ledger_test. Не запускайте тесты до согласования. Для проверки компиляции без тестов и миграций: .\mvnw.cmd -DskipTests test-compile.
