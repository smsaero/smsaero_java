package ru.smsaero;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for SmsAero.
 */
class SmsAeroTest {

    @Test
    void constructorRejectsNullEmail() {
        assertThrows(IllegalArgumentException.class, () ->
            new SmsAero(null, "apiKey"));
    }

    @Test
    void constructorRejectsBlankEmail() {
        assertThrows(IllegalArgumentException.class, () ->
            new SmsAero("   ", "apiKey"));
    }

    @Test
    void constructorRejectsNullApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
            new SmsAero("email@example.com", null));
    }

    @Test
    void constructorAcceptsValidCredentials() {
        SmsAero client = new SmsAero("email@example.com", "apiKey123");
        assertNotNull(client);
        assertFalse(client.IsTestModeActive());
    }

    @Test
    void testModeToggle() {
        SmsAero client = new SmsAero("e@e.com", "key");
        assertFalse(client.IsTestModeActive());
        client.EnableTestMode();
        assertTrue(client.IsTestModeActive());
        client.DisableTestMode();
        assertFalse(client.IsTestModeActive());
    }

    // --- SendSms validation ---

    @Test
    void sendSmsRejectsNullNumber() {
        SmsAero client = new SmsAero("e@e.com", "key");
        client.EnableTestMode();
        assertThrows(IllegalArgumentException.class, () ->
            client.SendSms(null, "text", "Sign"));
    }

    @Test
    void sendSmsRejectsBlankNumber() {
        SmsAero client = new SmsAero("e@e.com", "key");
        client.EnableTestMode();
        assertThrows(IllegalArgumentException.class, () ->
            client.SendSms("   ", "text", "Sign"));
    }

    @Test
    void sendSmsRejectsNullText() {
        SmsAero client = new SmsAero("e@e.com", "key");
        client.EnableTestMode();
        assertThrows(IllegalArgumentException.class, () ->
            client.SendSms("70000000000", null, "Sign"));
    }

    @Test
    void sendSmsRejectsNullSign() {
        SmsAero client = new SmsAero("e@e.com", "key");
        client.EnableTestMode();
        assertThrows(IllegalArgumentException.class, () ->
            client.SendSms("70000000000", "text", null));
    }

    @Test
    void sendSmsRejectsBlankText() {
        SmsAero client = new SmsAero("e@e.com", "key");
        client.EnableTestMode();
        assertThrows(IllegalArgumentException.class, () ->
            client.SendSms("70000000000", "", "Sign"));
    }

    // --- SendTelegram validation ---

    @Test
    void sendTelegramTwoArgsRejectsNullNumber() {
        SmsAero client = new SmsAero("e@e.com", "key");
        assertThrows(IllegalArgumentException.class, () ->
            client.SendTelegram(null, 1234));
    }

    @Test
    void sendTelegramTwoArgsRejectsBlankNumber() {
        SmsAero client = new SmsAero("e@e.com", "key");
        assertThrows(IllegalArgumentException.class, () ->
            client.SendTelegram("", 1234));
    }

    @Test
    void sendTelegramFourArgsRejectsNullNumber() {
        SmsAero client = new SmsAero("e@e.com", "key");
        assertThrows(IllegalArgumentException.class, () ->
            client.SendTelegram(null, 1234, "Sign", "text"));
    }

    @Test
    void sendTelegramFourArgsRejectsNullSign() {
        SmsAero client = new SmsAero("e@e.com", "key");
        assertThrows(IllegalArgumentException.class, () ->
            client.SendTelegram("70000000000", 1234, null, "text"));
    }

    @Test
    void sendTelegramFourArgsRejectsNullText() {
        SmsAero client = new SmsAero("e@e.com", "key");
        assertThrows(IllegalArgumentException.class, () ->
            client.SendTelegram("70000000000", 1234, "Sign", null));
    }
}
