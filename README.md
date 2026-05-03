# OTP Service

Backend-сервис для защиты пользовательских операций одноразовыми кодами (OTP).
Реализован на Java 17 + JDBC + PostgreSQL 17.
HTTP API построено на пакете `com.sun.net.httpserver` (без Spring).
Для удобства проверки работы, а также с учетом того, что из задания явно и очевидно не следует, то
каким образом будет проверятся функционал приложения, мной был предусмотрен docker-compose-файл, в соответствии
с конфигурацией которого, поднимаются докер-контейнеры с преднастроенными сервисами необходимыми для тестирования
(БД, СМС, email). Исключение представляет Тегерам-бот, токен и id которого необходимо вставить в telegram.properies
для тестирования самостоятельно, так как токен бота относится уже к совсем личной и конфиденциальной информации

**Важное примечание**
Подтверждение тестирования отправки кодов посредством Телеграм-бота,SMS,email, а также выгрузки в файл 
приложил отдельно в виде скриншотов в папке docs/screenshots.
Тестирование функционала приложения осуществлялось на MacBook Pro M4 (MacOS)
**Для удобства тестирования имеется файлик requests.http в котором можно комфортно протестировать весь функционал**
**Указанный файлик проще всего тестировать в IntelijIDEA или VSCode**

## Возможности

- Регистрация и аутентификация пользователей с выдачей JWT-токена.
- Две роли: `ADMIN` и `USER`. Администратор в системе может быть только один.
- Конфигурирование длины кода и времени его жизни (только администратором).
- Генерация OTP-кода для произвольной операции.
- Доставка кода по: Email, SMS (через эмулятор SMPP), Telegram, файл.
- Валидация введённого кода.
- Автоматическая пометка просроченных кодов фоновой задачей.

## Используемый инструментарий
- Java 17, Gradle
- PostgreSQL 17 (через JDBC)
- `com.sun.net.httpserver` — HTTP API
- Jakarta Mail (Angus Mail) — Email
- jSMPP — SMS через SMPP эмулятор
- `java.net.http.HttpClient` — Telegram
- SLF4J + Logback — логирование
- JJWT — выпуск/парсинг JWT
- jBCrypt — хеширование паролей

## Структура проекта

```
otp-service/
├── build.gradle                  — сборка + Gradle-задачи для Docker
├── docker-compose.yml            — PostgreSQL, Mailpit, SMPP-эмулятор
├── smpp-sim/Dockerfile           — образ SMPP-эмулятора (на jSMPP)
├── requests.http                 — готовые HTTP-запросы для проверки
├── src/main/
│   ├── java/ru/otpservice/       — исходный код (см. ниже)
│   └── resources/                — application.properties, schema.sql, logback.xml и проч.
└── docs/screenshots/             — скриншоты с демонстрацией работы
```

```
src/main/java/ru/otpservice
├── Main.java                     — точка входа
├── config/AppConfig.java         — загрузка application.properties
├── model/                        — POJO (User, OtpCode, OtpConfig, Role, OtpStatus)
├── dao/                          — JDBC-слой
├── service/                      — бизнес-логика
│   └── notification/             — каналы доставки кодов
├── scheduler/                    — фоновая задача истечения OTP
└── api/                          — HTTP-обработчики
```

---

## Что нужно установить для комфортного запуска приложения

Чтобы запустить и проверить проект, на компьютере должны быть:

1. **Java 17** — `https://adoptium.net/temurin/releases/?version=17`. Проверка:
   ```
   java -version
   ```
   Должна быть строка `17.x.x` или совместимая.

2. **Docker Desktop** — `https://www.docker.com/products/docker-desktop/`. После установки запусти Docker Desktop из Applications и убедись, что в системном трее значок Docker'а зелёный/белый. Проверка в терминале:
   ```
   docker --version
   docker compose version
   ```

3. **Git** — для клонирования репозитория.

**Больше ничего ставить не нужно** — ни PostgreSQL, ни SMPP-эмулятор, ни SMTP-сервер: всё поднимается автоматически в Docker.

---

## Шаг 1. Клонирование

```bash
git clone https://github.com/<автор_репозитория>/otp-service.git
cd otp-service
```

---

## Шаг 2. Конфигурация Telegram-бота

Повторюсь, это единственное место, где нужно вписать свои значения вручную. 
В репозитории в `src/main/resources/telegram.properties` стоят **плейсхолдеры** (`YOUR_BOT_TOKEN`, `YOUR_CHAT_ID`) — 
это сделано умышленно: токен бота даёт полный контроль над ним, его нельзя публиковать в публичном репозитории. 
Поэтому я предлагаю проверяющему создать **свой собственный** тестовый бот.

**Не думаю, что это понадобиться, но всё-же**
**Инструкция по созданию и настройке Телеграм-бота**

1. Открой Telegram, найди `@BotFather`, напиши `/newbot`, придумай имя и username бота (любые). BotFather пришлёт сообщение с **токеном** вида `1234567890:AAxxxxxxx` — скопируй его.

2. Найди только что созданного бота в Telegram по username (он будет в сообщении от BotFather), открой чат, нажми синюю кнопку **Start** внизу, напиши ему любое сообщение (например, `привет`).

3. Открой в браузере (вместо `<TOKEN>` подставь свой токен):
   ```
   https://api.telegram.org/bot<TOKEN>/getUpdates
   ```
   В JSON-ответе найди блок `"chat":{"id":...}` — число после `"id":` это и есть ваш `chat_id` (для приватного чата с ботом — положительное число).

4. Откройте `src/main/resources/telegram.properties` и подставте свои значения вместо плейсхолдеров:
   ```
   telegram.bot.token=1234567890:AAxxxxxxx
   telegram.default.chat_id=825627223
   telegram.api.url=https://api.telegram.org/bot
   ```

5. Сохраните файл.

После проверки бота можно удалить через `@BotFather` → `/mybots` → выбрать бота → **Delete Bot**.

Остальные `*.properties` (БД, Email, SMS) уже настроены на эмуляторы, ничего менять не надо.

---

## Шаг 3. Запуск приложения

В корне проекта **одна и та же** команда работает во всех сценариях:

```bash
./gradlew run
```

(на Windows — `gradlew.bat run`).

Gradle сам разберётся, что делать в зависимости от того, что у тебя запущено:

| Что есть на машине | Что делает Gradle | Что делать тебе |
|---|---|---|
| Запущен Docker Desktop, локального Postgres нет | Поднимает все три контейнера (Postgres, Mailpit, SMPP) и стартует приложение | Ничего — всё автоматически |
| Запущен Docker Desktop, локальный Postgres на 5432 | Поднимает только Mailpit и SMPP в Docker, БД использует твою локальную | Заранее создать юзера и БД (см. ниже) |
| Docker НЕ запущен или не установлен | Печатает предупреждение и пропускает шаг с контейнерами. Приложение пытается подключиться к локально поднятой инфраструктуре | Заранее поднять PostgreSQL, Mailpit и SMPP вручную (см. **Шаг 3-альтернатива**) |

В любом из этих случаев — **одна и та же команда `./gradlew run`**, без флагов и переключателей.

Если используете **локальный** PostgreSQL — заранее создайте юзера и БД (разовая операция):
```bash
psql -d postgres -c "CREATE USER otp_user WITH PASSWORD '1725';"
psql -d postgres -c "CREATE DATABASE otp_service OWNER otp_user;"
```

### Что Вы увидите в логе при успешном старте

```
INFO  ru.otpservice.dao.DatabaseManager - Schema initialized
INFO  r.o.s.OtpExpirationScheduler      - Starting OTP expiration scheduler with period 30s
INFO  ru.otpservice.Main                - OTP service started on port 8080
```

**Первый запуск через Docker займёт ~1–2 минуты** — качаются образы и собирается SMPP-эмулятор. Все последующие запуски — секунды.

Чтобы остановить приложение — **Ctrl+C** (не Ctrl+Z!) в терминале с `./gradlew run`.

Чтобы остановить и контейнеры:
```bash
./gradlew stopInfra
```

### Кросс-платформенность

Команда `./gradlew run` работает одинаково на macOS, Linux и Windows (на Windows нужно использовать `gradlew.bat run`). 
Единственное требование — установлены Java 17, Docker Desktop (или Docker Engine + Compose v2 на Linux) и Git.

---

## Шаг 3-альтернативный запуск приложения, в случаях, когда Вы не готовы использовать и запускать Docker

Если по каким-то причинам Docker'а нет (или не хочется его ставить), всю инфраструктуру можно поднять вручную — 
в виде нативных приложений. Это дольше, но возможно. Ниже инструкции для каждого компонента.


### А. PostgreSQL 17 (нативно)

1. Установить Postgres:
   - **macOS**: `brew install postgresql@17` и `brew services start postgresql@17`
   - **Linux (Ubuntu/Debian)**: `sudo apt install postgresql-17`
   - **Windows**: установщик с `https://www.postgresql.org/download/windows/`

2. Создать юзера и БД:
   ```bash
   psql -d postgres -c "CREATE USER otp_user WITH PASSWORD '1725';"
   psql -d postgres -c "CREATE DATABASE otp_service OWNER otp_user;"
   ```

3. Проверить, что всё работает:
   ```bash
   psql -h localhost -U otp_user -d otp_service -c "SELECT 1;"
   ```
   Введёшь пароль `1725`, должно вернуть `1`.

### Б. Mailpit (нативно, без Docker)

Mailpit — это один бинарник, без зависимостей.

1. Скачать с `https://mailpit.axllent.org/docs/install/`:
   - **macOS**: `brew install mailpit`
   - **Linux**: скачать `tar.gz` со страницы релизов GitHub, распаковать.
   - **Windows**: скачать `.zip`, распаковать.

2. Запустить (в отдельном терминале, не закрывать):
   ```bash
   mailpit
   ```
   Будут слова `[smtp] starting on [::]:1025` и `[http] starting on [::]:8025`.

3. Веб-интерфейс с пришедшими письмами — `http://localhost:8025`.

`email.properties` уже настроен на `localhost:1025` без авторизации, ничего менять не надо.

### В. SMPP-эмулятор (нативно, без Docker)

Запускается тем же jSMPP, который мы используем как клиент, только в режиме сервера.

1. Создать в любом удобном месте папку (например, `~/smpp-local`) и скачать туда четыре jar-а:
   ```bash
   mkdir -p ~/smpp-local && cd ~/smpp-local
   curl -fsSL -o jsmpp.jar          https://repo1.maven.org/maven2/org/jsmpp/jsmpp/3.0.1/jsmpp-3.0.1.jar
   curl -fsSL -o jsmpp-examples.jar https://repo1.maven.org/maven2/org/jsmpp/jsmpp-examples/3.0.1/jsmpp-examples-3.0.1.jar
   curl -fsSL -o slf4j-api.jar      https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar
   curl -fsSL -o slf4j-simple.jar   https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar
   ```

2. Запустить (в отдельном терминале, не закрывать):
   ```bash
   java -cp "jsmpp.jar:jsmpp-examples.jar:slf4j-api.jar:slf4j-simple.jar" \
        org.jsmpp.examples.SMPPServerSimulator
   ```
   На Windows разделитель в classpath — `;` вместо `:`.

3. По умолчанию слушает порт **8056**, а не 2775. В нашем `sms.properties` стоит 2775 — поправь его на 8056:
   ```
   smpp.host=localhost
   smpp.port=8056
   smpp.system_id=j
   smpp.password=jpwd
   smpp.system_type=OTP
   smpp.source_addr=OTPService
   ```

### Г. Запуск приложения без Docker

После того как Postgres, Mailpit и SMPP-эмулятор запущены вручную, **ничего в коде менять не нужно** — просто запустите:

```bash
./gradlew run
```

Gradle сначала попытается поднять Docker-контейнеры. Не получится (Docker не запущен) - 
напечатает в консоль предупреждение и **продолжит** запуск приложения. 
Приложение подключится к локальным Postgres / Mailpit / SMPP, которые Вы подняли вручную.

Сообщение в консоли будет такое:

```
>>> Docker недоступен или есть конфликт портов.
>>> Считаю, что PostgreSQL, Mailpit и SMPP подняты вручную (см. README, Шаг 3-альтернатива).
>>> Если это не так — приложение упадёт при подключении к БД/SMTP/SMPP.
```

Это нормально и ожидаемо, когда работаете без Docker.

---

## Шаг 4. Проверка работы

API доступно на `http://localhost:8080`. Тестировать можно тремя способами.

### Способ А — через `requests.http` (рекомендуемый способ)

В корне проекта лежит файл `requests.http` с готовыми запросами в правильной последовательности. Откройте его в:
- **IntelliJ IDEA** — слева от каждой строки `###` появится зелёная стрелочка. Нажимайте по очереди сверху вниз.
- **VS Code** + расширение **REST Client** (от Huachao Mao) — над каждым запросом появится «Send Request».
- **Bruno** (open-source, `brew install bruno`) — откройте файл и запускайте по очереди.

Токены автоматически сохраняются в переменные `{{user_token}}` и `{{admin_token}}` — копировать их вручную не нужно.

### Способ Б — через Postman

Скачать: `https://www.postman.com/downloads/`. Создать коллекцию и набить запросы по образцу из `requests.http`.

### Способ В — через `curl` в терминале

Полный набор команд для проверки всех сценариев. Команды нужно выполнять по порядку. Используется `jq` для парсинга JSON — на macOS ставится через `brew install jq`.

```bash
BASE=http://localhost:8080

# Регистрация админа (201) и блок второго админа (409)
curl -i -X POST $BASE/api/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"admin","role":"ADMIN"}'

curl -i -X POST $BASE/api/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"username":"admin2","password":"admin2","role":"ADMIN"}'

# Регистрация обычного пользователя (201) и логин с JWT
curl -i -X POST $BASE/api/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"username":"user1","password":"qwerty","role":"USER"}'

USER_TOKEN=$(curl -s -X POST $BASE/api/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"username":"user1","password":"qwerty"}' | jq -r .token)
echo "USER_TOKEN=$USER_TOKEN"

ADMIN_TOKEN=$(curl -s -X POST $BASE/api/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"admin"}' | jq -r .token)
echo "ADMIN_TOKEN=$ADMIN_TOKEN"

# Разграничение по ролям (401 → 403 → 200)
curl -i $BASE/api/admin/users
curl -i -H "Authorization: Bearer $USER_TOKEN"  $BASE/api/admin/users
curl -i -H "Authorization: Bearer $ADMIN_TOKEN" $BASE/api/admin/users

# Выгрузка кода в файл
curl -X POST $BASE/api/otp/generate \
     -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' \
     -d '{"operationId":"op-file","channel":"file"}'
cat otp-codes.log

# Эмуляция отправуки кода по email (письмо смотреть в Mailpit на http://localhost:8025 или в логах)
curl -X POST $BASE/api/otp/generate \
     -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' \
     -d '{"operationId":"op-email","channel":"email","destination":"alice@example.com"}'

# Отправка кода по sms (смотреть в `docker logs smpp-sim` или в окне нативного эмулятора)
curl -X POST $BASE/api/otp/generate \
     -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' \
     -d '{"operationId":"op-sms","channel":"sms","destination":"79991234567"}'

# Канал telegram (сообщение придёт в Telegram-бот), скриншот моего теста приложен
curl -X POST $BASE/api/otp/generate \
     -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' \
     -d '{"operationId":"op-tg","channel":"telegram","destination":"Alex"}'

# Валидация
# ВАЖНО!!!! Необходимо подставь свой код из otp-codes.log в CODE
CODE=482719
# Верный код (200, valid:true)
curl -i -X POST $BASE/api/otp/validate \
     -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' \
     -d "{\"code\":\"$CODE\"}"
# Заведомо неверный код (400, valid:false)
curl -i -X POST $BASE/api/otp/validate \
     -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' \
     -d '{"code":"000000"}'
# Повторно тот же (уже USED, 400, valid:false)
curl -i -X POST $BASE/api/otp/validate \
     -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' \
     -d "{\"code\":\"$CODE\"}"

# Шедулер истечения (TTL = 60 сек, ждём ~90 сек, смотрим лог + БД)
curl -X PUT $BASE/api/admin/config \
     -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
     -d '{"codeLength":6,"ttlSeconds":60}'

curl -X POST $BASE/api/otp/generate \
     -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' \
     -d '{"operationId":"op-expire","channel":"file"}'

# Подождать 90 секунд (60 TTL + 30 интервал шедулера). В логе приложения появится:
# INFO  r.o.s.OtpExpirationScheduler - Marked 1 OTP code(s) as EXPIRED
# Проверить статус в БД:
psql -h localhost -U otp_user -d otp_service -c \
     "SELECT id, code, status, expires_at FROM otp_codes ORDER BY id DESC LIMIT 3;"

# Вернуть нормальный TTL
curl -X PUT $BASE/api/admin/config \
     -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
     -d '{"codeLength":6,"ttlSeconds":300}'

# Операции администратора
curl -i -H "Authorization: Bearer $ADMIN_TOKEN" $BASE/api/admin/users
curl -i -X DELETE -H "Authorization: Bearer $ADMIN_TOKEN" $BASE/api/admin/users/2
```

После прогона можно зачистить тестовое состояние:
```bash
psql -h localhost -U otp_user -d otp_service -c \
     "TRUNCATE TABLE otp_codes, users RESTART IDENTITY CASCADE;"
```

## API

### Аутентификация (без токена)

| Метод | Путь                  | Тело запроса                                                    |
|-------|-----------------------|-----------------------------------------------------------------|
| POST  | `/api/auth/register`  | `{"username":"...","password":"...","role":"USER"}` или `ADMIN` |
| POST  | `/api/auth/login`     | `{"username":"...","password":"..."}` → возвращает `token`      |

### Пользовательский API (`Authorization: Bearer <token>`)

| Метод | Путь                  | Тело запроса                                                                       |
|-------|-----------------------|------------------------------------------------------------------------------------|
| POST  | `/api/otp/generate`   | `{"operationId":"...","channel":"email|sms|telegram|file","destination":"..."}`     |
| POST  | `/api/otp/validate`   | `{"code":"123456"}`                                                                |

`destination` — для `email` это адрес почты, для `sms` — номер телефона, для `telegram` — имя получателя в тексте сообщения (chat_id берётся из `telegram.properties`). Для `file` поле игнорируется.

### Админский API (роль `ADMIN`)

| Метод     | Путь                          | Описание                              |
|-----------|-------------------------------|---------------------------------------|
| GET       | `/api/admin/config`           | Получить текущую конфигурацию OTP     |
| PUT/PATCH | `/api/admin/config`           | `{"codeLength":6,"ttlSeconds":300}`   |
| GET       | `/api/admin/users`            | Список пользователей (без админа)     |
| DELETE    | `/api/admin/users/{id}`       | Удалить пользователя и его OTP-коды   |

## Полезные команды

```bash
# Поднять только контейнеры (без приложения)
./gradlew startInfra

# Остановить контейнеры
./gradlew stopInfra

# Полная очистка (удалить данные PostgreSQL и образы)
docker compose down -v
docker rmi axllent/mailpit my-smpp-sim postgres:17-alpine

# Посмотреть состояние БД (если хочется)
docker exec -it otp-postgres psql -U otp_user -d otp_service
# внутри psql: \dt — список таблиц, SELECT * FROM users;

# Посмотреть, что прилетело в SMPP-эмулятор
docker logs smpp-sim

# Mailpit (письма) — в браузере
open http://localhost:8025
```

---

## Логирование

- Консоль и файл `logs/otp-service.log` (см. `src/main/resources/logback.xml`).
- При выгрузке в файл коды дописываются в `otp-codes.log` в корне проекта.

---

## Частые проблемы и их решения

**`Cannot connect to the Docker daemon`** — Docker Desktop не запущен. Запустить его и подождать, пока значок не станет статичным.

**`Address already in use` на порту 8080** — другое приложение использует 8080. Найти и убить:
```bash
lsof -i :8080
kill -9 <PID>
```
Или поменять порт в `application.properties` (`server.port=...`) и в curl-командах.

**`Address already in use` на портах 5432, 1025, 8025, 2775** — у Вас локально стоят PostgreSQL/Mailpit/что-то ещё, занимающее эти порты. Останови их (`brew services stop postgresql@17` и т.п.) или поменяйте маппинг портов в `docker-compose.yml`.

**Контейнеры не запускаются с ошибкой `Conflict, container name already in use`** — у Вас есть контейнеры с такими именами от прошлых запусков (не через compose). Удалить:
```bash
docker rm -f otp-postgres mailpit smpp-sim
```

**Telegram возвращает `chat not found`** — Вы не написали боту первым. Откройте бота в Telegram, нажмите Start, отправьте любое сообщение, и заново запросите `getUpdates` для chat_id.