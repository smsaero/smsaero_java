# SMS Aero Client library for Java

[![Maven Central](https://img.shields.io/maven-central/v/ru.smsaero/smsaero.svg)](https://central.sonatype.com/artifact/ru.smsaero/smsaero)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

Library for sending SMS messages using the SMS Aero API. Written in Java.

**Requirements:** Java 11 or higher. For Java 8, use [v3.1.0](https://github.com/smsaero/smsaero_java/releases/tag/v3.1.0).

**API documentation:** [smsaero.ru/integration/documentation/api](https://smsaero.ru/integration/documentation/api/)

> Русская документация: [README.RUS.md](README.RUS.md)

## Installation

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

## Usage

Get credentials from account settings page: https://smsaero.ru/cabinet/settings/apikey/

```java
import org.json.simple.JSONObject;
import ru.smsaero.SmsAero;

public class Main {
    private static final String email = "your email";
    private static final String apiKey = "your api key";

    public static void main(String[] args) {
        try {
            SmsAero client = new SmsAero(email, apiKey);

            // Send SMS
            JSONObject sendResult = client.SendSms("70000000000", "Hello, World!");
            System.out.println(sendResult.toString());

            // Send Telegram code
            JSONObject telegramResult = client.SendTelegram("70000000000", 1234, "SMS Aero", "Your code 1234");
            System.out.println(telegramResult.toString());
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            System.exit(-1);
        }
    }
}
```

## Test mode

For development and debugging without sending real SMS or charging your account, use test mode:

```java
SmsAero client = new SmsAero(email, apiKey);

// Enable Test mode
client.EnableTestMode();

// No real SMS are sent
JSONObject result = client.SendSms("70000000000", "Test");

// switch back to normal mode
client.DisableTestMode();
```

Check status: `client.IsTestModeActive()`.

## Command-line interface (CLI)

To send SMS from the command line, use the fat JAR `smsaero-3.2.0-cli.jar`:

```bash
java -jar target/smsaero-3.2.0-cli.jar --email YOUR_EMAIL --api_key YOUR_API_KEY --phone 70000000000 --message "Hello"
```

### Docker

Run on Docker:

```bash
docker pull 'smsaero/smsaero_java:latest'
docker run --rm smsaero/smsaero_java:latest --email YOUR_EMAIL --api_key YOUR_API_KEY --phone 70000000000 --message "Hello"
```

Options:
- `--debug` — test mode (no real SMS sent)
- `--sign` — sender signature (default: "SMS Aero")

## License

```
MIT License
```
