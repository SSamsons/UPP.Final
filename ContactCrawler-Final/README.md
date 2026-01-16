# Contact Crawler Final

Многопоточное веб-приложение на Spring Boot для сбора контактной информации организаций с веб-сайтов и бизнес-директорий с расширенным мониторингом производительности.

## Технологии

- **Spring Boot 2.7.0** - основной фреймворк
- **Spring Data JPA** - работа с базой данных
- **H2 Database** - встроенная база данных
- **WebFlux (WebClient)** - реактивные HTTP запросы
- **RestTemplate** - синхронные HTTP запросы
- **OpenFeign** - декларативный HTTP клиент
- **JSoup** - парсинг HTML
- **Кастомный ExecutorService** - управление потоками краулера
- **ForkJoinPool** - параллельная обработка данных
- **@Scheduled** - планировщик задач
- **ScheduledExecutorService** - дополнительный планировщик
- **ParallelStream API** - параллельная обработка коллекций
- **Потокобезопасные структуры данных** - ConcurrentHashMap, BlockingQueue, AtomicInteger
- **Micrometer & Prometheus** - сбор метрик производительности
- **OpenTelemetry & Jaeger** - распределенный трейсинг
- **JMH** - бенчмарки производительности

## Требования

- Java 11 или выше
- Maven 3.6 или выше
- Docker (опционально, для мониторинга)

## Быстрый старт

### 1. Запуск приложения

Откройте терминал в корневой папке проекта и выполните:

```bash
mvn spring-boot:run
```

Или соберите JAR и запустите:

```bash
mvn clean package
java -jar target/contact-crawler-final-1.0.0.jar
```

Приложение будет доступно по адресу: `http://localhost:8080`

### 2. Запуск краулинга

Выполните POST запрос на эндпоинт `/api/crawler/start` с массивом стартовых URL:

**Пример запроса (PowerShell):**

```powershell
$urls = @("https://2gis.ru", "https://yandex.ru/maps")
$body = $urls | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/crawler/start" -Body $body -ContentType "application/json"
```

**Пример запроса (curl):**

```bash
curl -X POST http://localhost:8080/api/crawler/start \
  -H "Content-Type: application/json" \
  -d '["https://2gis.ru", "https://yandex.ru/maps"]'
```

**Ответ:**

```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "message": "Crawling started successfully"
}
```

### 3. Проверка статуса задания

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/crawler/status/550e8400-e29b-41d4-a716-446655440000"
```

### 4. Получение результатов

Основной эндпоинт для получения результатов: `/api/data/answer`

**Пример запроса:**

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/data/answer?page=0&size=10&search=it&sortBy=name&ascending=true"
```

**Параметры:**
- `page` - номер страницы (по умолчанию: 0)
- `size` - размер страницы (по умолчанию: 10)
- `search` - поисковый запрос (опционально)
- `sortBy` - поле для сортировки: name, website, crawledAt (по умолчанию: name)
- `ascending` - направление сортировки: true/false (по умолчанию: true)

## Система мониторинга и метрики

### Интеграция с Micrometer и Prometheus

Система автоматически собирает показатели производительности через Micrometer и предоставляет их в формате, совместимом с Prometheus.

**Эндпоинты для получения метрик:**
- Экспорт метрик Prometheus: `http://localhost:8080/actuator/prometheus`
- Общий список метрик: `http://localhost:8080/actuator/metrics`
- Проверка состояния: `http://localhost:8080/actuator/health`

**Типы собираемых метрик:**
- `crawler.parsing.duration` - длительность процесса парсинга
- `crawler.parsing.success` - счетчик успешных операций парсинга
- `crawler.parsing.errors` - счетчик ошибок при парсинге
- `crawler.database.records.inserted` - количество добавленных записей в БД
- `crawler.pages.crawled` - общее количество обработанных страниц
- `crawler.urls.visited` - количество посещенных URL
- `crawler.database.save.duration` - время выполнения операций сохранения
- `crawler.html.fetch.duration` - время загрузки HTML-контента

### Запуск с расширенным мониторингом

**Для Windows:**
```powershell
.\scripts\start-with-monitoring.bat
```

**Для Linux/Mac:**
```bash
chmod +x scripts/start-with-monitoring.sh
./scripts/start-with-monitoring.sh
```

Скрипт активирует следующие возможности:
- Детальное логирование работы сборщика мусора (GC)
- JMX для подключения VisualVM
- Java Flight Recorder (JFR) для профилирования
- Автоматическое создание heap dump при ошибках OutOfMemoryError

### Настройка Prometheus и Grafana

1. **Запуск контейнеров мониторинга:**
```bash
docker-compose up -d
```

Это развернет следующие сервисы:
- **Prometheus** на порту 9090: `http://localhost:9090`
- **Grafana** на порту 3000: `http://localhost:3000` (логин: admin, пароль: admin)
- **Jaeger** на порту 16686: `http://localhost:16686`

2. **Конфигурация Prometheus:**
   - Файл `prometheus.yml` содержит готовые настройки
   - Prometheus автоматически собирает метрики с эндпоинта `/actuator/prometheus`

3. **Настройка дашборда в Grafana:**
   - Авторизуйтесь в Grafana
   - Добавьте источник данных Prometheus с адресом `http://prometheus:9090`
   - Создайте визуализации для метрик, перечисленных выше

### Инструменты трейсинга: OpenTelemetry и Jaeger

Приложение интегрировано с OpenTelemetry для распределенного трейсинга запросов и отправляет данные в Jaeger.

**Отслеживаемые операции:**
- `fetch_html` - этап получения HTML-контента
- `parse_contacts` - этап извлечения контактных данных
- `save_organization` - этап сохранения информации об организации
- `extract_links` - этап поиска ссылок на странице

**Просмотр трейсов:**
1. Откройте веб-интерфейс Jaeger: `http://localhost:16686`
2. Выберите сервис `contact-crawler-final` из списка
3. Нажмите кнопку "Find Traces"
4. Изучите временные диаграммы выполнения операций

### Бенчмарки производительности (JMH)

**Выполнение тестов производительности:**
```bash
mvn clean package
java -jar target/benchmarks.jar
```

Бенчмарки позволяют сравнить эффективность различных подходов к парсингу:
- Классический цикл for
- Stream API
- Параллельные стримы (parallelStream)
- Оптимизированная версия Stream API
- LinkedHashSet для уникальности

## Консоль H2 Database

1. Откройте браузер и перейдите по адресу: `http://localhost:8080/h2-console`
2. Введите следующие данные:
   - **JDBC URL:** `jdbc:h2:file:./data/contactdb`
   - **User Name:** `sa`
   - **Password:** (оставьте пустым)
3. Нажмите "Connect"
4. Выполните SQL запрос для просмотра данных:

```sql
SELECT * FROM organizations;
SELECT * FROM organization_phones;
SELECT * FROM organization_emails;
SELECT * FROM organization_addresses;
```

## Планировщики

### @Scheduled

Автоматический запуск краулинга каждый день в 02:00. Настраивается в `SchedulerService` через аннотацию `@Scheduled(cron = "0 0 2 * * ?")`.

### ScheduledExecutorService

Периодический запуск каждые 30 минут. Можно активировать через вызов метода `startPeriodicCrawl()` в `SchedulerService`.

## Конфигурация

Основные настройки в `application.properties`:

```properties
# Порт сервера
server.port=8080

# База данных H2
spring.datasource.url=jdbc:h2:file:./data/contactdb

# Параметры краулера
crawler.max.depth=3          # Максимальная глубина обхода
crawler.max.pages=200        # Максимальное количество страниц
crawler.thread.pool.size=10  # Размер пула потоков
crawler.timeout.ms=10000     # Таймаут запросов
```

## Структура проекта

```
contact-crawler-final/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── contactcrawler/
│   │   │           ├── ContactCrawlerApplication.java
│   │   │           ├── benchmark/
│   │   │           │   └── ParsingBenchmark.java
│   │   │           ├── config/
│   │   │           │   ├── ThreadPoolConfiguration.java
│   │   │           │   ├── MetricsConfig.java
│   │   │           │   └── OpenTelemetryConfig.java
│   │   │           ├── controller/
│   │   │           │   ├── CrawlerController.java
│   │   │           │   └── DataController.java
│   │   │           ├── model/
│   │   │           │   ├── Organization.java
│   │   │           │   └── CrawlJob.java
│   │   │           ├── repository/
│   │   │           │   └── OrganizationRepository.java
│   │   │           ├── service/
│   │   │           │   ├── CrawlerService.java
│   │   │           │   ├── DataProcessingService.java
│   │   │           │   └── SchedulerService.java
│   │   │           ├── client/
│   │   │           │   ├── WebClientService.java
│   │   │           │   ├── RestTemplateService.java
│   │   │           │   ├── FeignHtmlClient.java
│   │   │           │   └── FeignHtmlClientFallback.java
│   │   │           └── util/
│   │   │               ├── ContactParser.java
│   │   │               ├── LinkExtractor.java
│   │   │               └── TracingUtil.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── scripts/
│   └── start-with-monitoring.bat
├── docker-compose.yml
├── prometheus.yml
├── pom.xml
└── README.md
```

## Особенности реализации

1. **Трехуровневая система HTTP клиентов:** WebClient → RestTemplate → FeignClient (с fallback)
2. **Рекурсивный краулинг** с ограничением глубины и количества страниц
3. **Извлечение контактов:** телефоны, email, адреса из HTML контента
4. **Параллельная обработка** данных через parallelStream и ForkJoinPool
5. **Потокобезопасность** на всех уровнях приложения
6. **Автоматическое планирование** задач через @Scheduled и ScheduledExecutorService
7. **Мониторинг производительности** через Micrometer и Prometheus
8. **Распределенный трейсинг** через OpenTelemetry и Jaeger
9. **Оптимизация БД** с использованием индексов и JOIN FETCH для устранения N+1 запросов

## Примененные оптимизации

1. **Оптимизация запросов к БД:**
   - Применение `JOIN FETCH` для устранения проблемы N+1 запросов
   - Предзагрузка связанных коллекций вместе с основными сущностями

2. **Индексация базы данных:**
   - Создание индексов на полях `website`, `name`, `crawledAt`
   - Индексы для коллекций `phones` и `emails` для ускорения поиска

3. **Оптимизация использования памяти:**
   - Замена `ArrayList.contains()` на `LinkedHashSet` для более эффективной проверки уникальности
   - Предкомпиляция регулярных выражений для ускорения работы

4. **Управление параллелизмом:**
   - Использование потокобезопасных коллекций (ConcurrentHashMap, Collections.synchronizedSet)
   - Применение атомарных операций для минимизации блокировок
   - Использование AtomicInteger для счетчиков в многопоточной среде

## Логи

Логи приложения сохраняются в файл: `./logs/contact-crawler.log`

## Примечания

- При первом запуске создается база данных H2 в папке `./data/`
- Для тестирования можно использовать любые публичные веб-сайты
- Некоторые сайты могут блокировать запросы без правильного User-Agent (настроен в конфигурации)
- Рекомендуется использовать реальные бизнес-директории (2GIS, Яндекс.Справочник) для получения результатов

## Решение проблем

**Проблема:** Приложение не запускается
- Проверьте версию Java: `java -version` (должна быть 11+)
- Проверьте версию Maven: `mvn -version`

**Проблема:** Нет данных после краулинга
- Убедитесь, что стартовые URL доступны
- Проверьте логи в `./logs/contact-crawler.log`
- Некоторые сайты могут блокировать автоматические запросы

**Проблема:** Ошибки подключения к H2
- Убедитесь, что папка `./data/` существует и доступна для записи
- Проверьте, что приложение не запущено в нескольких экземплярах одновременно
