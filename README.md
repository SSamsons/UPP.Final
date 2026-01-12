# Система сбора контактных данных компаний

Веб-приложение на базе Spring Boot с поддержкой многопоточности для автоматизированного извлечения контактной информации организаций (номера телефонов, электронная почта, физические адреса) из веб-ресурсов и бизнес-каталогов.

Технологический стек: пользовательский ExecutorService, ForkJoinPool для параллельных вычислений, аннотация @Scheduled и ScheduledExecutorService для задач по расписанию, WebFlux с WebClient, RestTemplate, OpenFeign клиент, встроенная БД H2 с JPA, thread-safe коллекции и parallelStream для параллельной обработки.

## Требования к окружению

Для работы приложения необходимо:
- JDK версии 11 или выше
- Система сборки Maven версии 3.8 или новее
- PowerShell для Windows (в примерах используется PowerShell)

## Инструкция по запуску

Выполните команды в PowerShell из корневой директории проекта.

**Способ 1 (запуск через Maven):**
```powershell
mvn spring-boot:run
```

**Способ 2 (сборка исполняемого файла):**
```powershell
mvn -U -DskipTests clean package
java -jar target/company-crawler-1.0.0.jar
```

После успешного запуска сервис будет доступен по адресу `http://localhost:8080`.

Для остановки сервера нажмите комбинацию клавиш Ctrl+C в окне терминала.

## Тестирование API

Для проверки работы API откройте новое окно PowerShell (основной процесс должен продолжать работать).

**1. Инициализация процесса сканирования**
- Метод: `POST /api/crawler/start`
- Формат данных: массив URL в формате JSON
```powershell
$urls = @("https://2gis.ru","https://yandex.ru/maps","https://example.org")
$payload = $urls | ConvertTo-Json -Compress
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/crawler/start" -Body $payload -ContentType "application/json"
```
Ожидаемый ответ:
```json
{"taskId":"<uuid>","message":"Crawling started"}
```

**2. Мониторинг состояния задачи**
- Метод: `GET /api/crawler/status/{taskId}`
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/crawler/status/<taskId>"
```
Примечание: замените `<taskId>` на реальный идентификатор без угловых скобок. После перезапуска приложения идентификаторы задач становятся недействительными.

**3. Получение собранных данных**
- Метод: `GET /api/data/answer?page=0&size=10&search=it`
  - Параметры `page` и `size` управляют постраничной навигацией
  - Параметр `search` позволяет фильтровать по названию, домену, адресу или контактным данным (необязательный)
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/data/answer?page=0&size=10&search=it"
```

**Дополнительные возможности API:**
- Получение списка с сортировкой:
  - `GET /api/data/companies?search=&sortBy=name&ascending=true`
- Поиск по номеру телефона:
  - `GET /api/data/companies/phone/81234567890`
- Поиск по электронной почте:
  - `GET /api/data/companies/email/info@example.org`

**Служебные методы:**
- `GET /api/crawler/stats` - статистика работы системы
- `GET /api/crawler/active-tasks` - список активных задач

## Работа с базой данных H2

Для прямого доступа к базе данных:
- Веб-интерфейс: `http://localhost:8080/h2-console`
- Строка подключения: `jdbc:h2:file:./data/crawlerdb`
- Имя пользователя: `sa`
- Пароль: `password`

Основная таблица: `companies`

## Автоматическое выполнение задач

В системе реализованы два механизма планирования:
- `@Scheduled` - автоматический запуск каждый день в 2:00 ночи
- `ScheduledExecutorService` - периодический запуск каждые 30 минут

Для немедленного запуска используйте ручной вызов POST-метода (см. раздел выше).

## Параметры конфигурации

Файл настроек находится в `src/main/resources/application.properties`:
- Настройки сервера: `server.port=8080`, `server.servlet.context-path=/`
- База данных H2 и JPA предварительно настроены
- Файл журнала: `./logs/crawler.log`
- Настройки Feign клиента: `feign.htmlFetch.baseUrl=https://r.jina.ai/http`

## Работа с PowerShell и кодировкой

**Решение проблем с отображением кириллицы:**
Если русские символы отображаются некорректно, выполните:
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

**Полезные советы:**
- Для переноса команд на новую строку используйте обратную кавычку `` ` `` (не символ ^, который используется в cmd.exe)
- Для просмотра полного JSON ответа:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/data/companies?search=&sortBy=name&ascending=true" | ConvertTo-Json -Depth 6
```

## Решение типичных проблем

- **Ошибка 400 при POST /start** - убедитесь, что тело запроса является валидным JSON-массивом. Проверьте содержимое переменной `$payload` через вывод в консоль.
- **Ошибка 404 для корневого пути /** - это ожидаемое поведение, так как корневой контроллер не реализован. Используйте пути, начинающиеся с `/api/...`
- **Проблемы с выполнением `spring-boot:run`** - попробуйте явно указать версию плагина:
```powershell
mvn org.springframework.boot:spring-boot-maven-plugin:2.7.0:run
```
или используйте способ 2 для сборки jar-файла.
- **Отсутствие данных в результатах** - проверьте доступность целевых сайтов. Возможны ограничения со стороны сетевых фильтров или прокси-серверов.

## Основной функционал

Реализованные возможности:
- Автоматический обход веб-страниц с извлечением контактной информации
- Thread-safe реализация с использованием пользовательского ExecutorService
- Параллельная обработка данных с применением ForkJoinPool и parallelStream
- Два независимых планировщика: @Scheduled и ScheduledExecutorService
- Сохранение данных в H2 с использованием Spring Data JPA
- Множественные HTTP-клиенты: WebFlux, RestTemplate, OpenFeign с механизмом fallback
- REST API для управления задачами и получения результатов

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
- `save_company` - этап сохранения информации о компании
- `extract_links` - этап поиска ссылок на странице

**Просмотр трейсов:**
1. Откройте веб-интерфейс Jaeger: `http://localhost:16686`
2. Выберите сервис `company-crawler` из списка
3. Нажмите кнопку "Find Traces"
4. Изучите временные диаграммы выполнения операций

### Интеграция с VisualVM и JFR

**Настройка VisualVM:**
1. Установите VisualVM с официального сайта: https://visualvm.github.io/
2. Запустите приложение с включенным мониторингом (см. инструкции выше)
3. В VisualVM создайте новое JMX-подключение к `localhost:9999`

**Анализ записей JFR:**
- Файлы записей сохраняются в директории `./logs/recording.jfr`
- Откройте их в VisualVM или JDK Mission Control для анализа

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

### Дополнительная документация

Расширенные материалы находятся в директории `docs/`:
- `PERFORMANCE_MONITORING.md` - подробное руководство по настройке мониторинга
- `JVM_SETUP.md` - рекомендации по настройке параметров JVM
- `PERFORMANCE_REPORT.md` - шаблон для составления отчетов о производительности

## Примененные оптимизации

Реализованные улучшения производительности:

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
