# SmsAero Client library for Java

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

Library for sending SMS messages using the SmsAero API. Written in Java.

## Installation

### Maven

```xml
<dependency>
  <groupId>ru.smsaero</groupId>
  <artifactId>smsaero</artifactId>
  <version>3.1.0</version>
</dependency>
```

### Gradle

```
implementation 'ru.smsaero:smsaero:3.1.0'
```

### Usage example:

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

            // Отправка SMS сообщения
            JSONObject sendResult = client.SendSms("70000000000", "Hello, World!");
            System.out.println(sendResult.toString());
            
            // Отправка Telegram кода
            JSONObject telegramResult = client.SendTelegram("70000000000", 1234, "SMS Aero", "Ваш код 1234");
            System.out.println(telegramResult.toString());
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            System.exit(-1);
        }
    }
}
```

### License

```
MIT License
```
