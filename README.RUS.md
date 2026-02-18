# SMS Aero — Java-клиент для API

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

Библиотека для отправки SMS через API SMS Aero.

**Требования:** Java 11 или выше. Для Java 8 используйте [v3.1.0](https://github.com/smsaero/smsaero_java/releases/tag/v3.1.0).

**Документация API:** [smsaero.ru/integration/documentation/api](https://smsaero.ru/integration/documentation/api/)

> English documentation: [README.md](README.md)

## Установка

### Maven

```xml
<dependency>
  <groupId>ru.smsaero</groupId>
  <artifactId>smsaero</artifactId>
  <version>3.2.0</version>
</dependency>
```

### Gradle

```
implementation 'ru.smsaero:smsaero:3.2.0'
```

## Использование

Получите учётные данные в настройках личного кабинета: https://smsaero.ru/cabinet/settings/apikey/

```java
import org.json.simple.JSONObject;
import ru.smsaero.SmsAero;

public class Main {
    private static final String email = "ваш email";
    private static final String apiKey = "ваш api key";

    public static void main(String[] args) {
        try {
            SmsAero client = new SmsAero(email, apiKey);

            // Отправка SMS
            JSONObject sendResult = client.SendSms("70000000000", "Hello, World!");
            System.out.println(sendResult.toString());

            // Отправка кода в Telegram
            JSONObject telegramResult = client.SendTelegram("70000000000", 1234, "SMS Aero", "Ваш код 1234");
            System.out.println(telegramResult.toString());
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            System.exit(-1);
        }
    }
}
```

## Тестовый режим

Для разработки и отладки без отправки реальных SMS и списания средств используйте тестовый режим:

```java
SmsAero client = new SmsAero(email, apiKey);

// Активация тестового режима
client.EnableTestMode();

// Отправка SMS
JSONObject result = client.SendSms("70000000000", "Тест");

// Вернуться в рабочий режим
client.DisableTestMode();
```

Проверить состояние: `client.IsTestModeActive()`.

## Консольное приложение (CLI)

Для отправки SMS из командной строки используется fat JAR `smsaero-3.2.0-cli.jar`:

```bash
java -jar target/smsaero-3.2.0-cli.jar --email YOUR_EMAIL --api_key YOUR_API_KEY --phone 70000000000 --message "Привет"
```

### Docker

Сборка и запуск через Docker:

```bash
# Отправка SMS
docker run --rm smsaero/smsaero_java:latest \
  --email YOUR_EMAIL --api_key YOUR_API_KEY \
  --phone 70000000000 --message "Привет"
```

Опции:
- `--debug` — тестовый режим (без реальной отправки)
- `--sign` — подпись отправителя (по умолчанию: «SMS Aero»)

## Лицензия

```
MIT License
```
