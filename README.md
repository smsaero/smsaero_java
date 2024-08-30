# SmsAero Client library for Java

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ru.smsaero/smsaero/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ru.smsaero/smsaero)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

Library for sending SMS messages using the SmsAero API. Written in Java.

## Installation

### Maven

```xml
<dependency>
  <groupId>ru.smsaero</groupId>
  <artifactId>smsaero</artifactId>
  <version>3.0.0</version>
</dependency>
```

### Gradle

```
implementation 'ru.smsaero:smsaero:3.0.0'
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
            JSONObject sendResult = client.SendSms("70000000000", "Hello, World!");
            System.out.println(sendResult.toString());
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
