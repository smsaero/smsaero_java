package ru.smsaero;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLException;
import java.time.Instant;
import java.util.*;

import org.json.simple.*;
import org.json.simple.parser.*;

/**
 * Java client for the SMS Aero API.
 * Provides methods for sending SMS, managing contacts, groups, HLR requests, and more.
 *
 * @see <a href="https://smsaero.ru/integration/documentation/api/">SmsAero API Documentation</a>
 */
public class SmsAero {
    /** Connection timeout (ms). */
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    /** Read timeout (ms). */
    private static final int READ_TIMEOUT_MS = 30_000;

    private static final String USER_AGENT = "SAJavaClient/3.2.0";

    private static final JSONParser JSON_PARSER = new JSONParser();

    private final String authHeader;

    private static final List<String> GATE_URLS = Arrays.asList(
        "https://gate.smsaero.ru/v2/",
        "https://gate.smsaero.org/v2/",
        "https://gate.smsaero.net/v2/"
    );
    private final ThreadLocal<String> page = ThreadLocal.withInitial(() -> null);
    private final ThreadLocal<Map<String, String>> postParam = ThreadLocal.withInitial(() -> null);
    private volatile boolean testMode = false;

    /**
     * Creates an SmsAero API client.
     *
     * @param emailAddr Email for SmsAero account authentication
     * @param apiKey    API key from the SmsAero cabinet
     * @throws IllegalArgumentException if email or apiKey is null or blank
     */
    public SmsAero(String emailAddr, String apiKey) {
        requireNonBlankAll("email", emailAddr, "apiKey", apiKey);
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(
            (emailAddr + ":" + apiKey).getBytes(StandardCharsets.UTF_8));
    }

    private static void requireNonBlank(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(paramName + " cannot be null or blank");
        }
    }

    /** Validates multiple parameters as (paramName, value) pairs. Accepts null - throws IAE. */
    private static void requireNonBlankAll(String... paramNameValuePairs) {
        if (paramNameValuePairs.length % 2 != 0) {
            throw new IllegalStateException("paramNameValuePairs must have even length");
        }
        for (int i = 0; i < paramNameValuePairs.length; i += 2) {
            requireNonBlank(paramNameValuePairs[i + 1], paramNameValuePairs[i]);
        }
    }

    /**
     * Sets the page number for paginated results.
     * Pass null to clear pagination.
     *
     * @param page Page number (e.g. "2") or null to clear
     * @throws IllegalArgumentException if page is not null but blank
     */
    public void SetPage(String page) {
        if (page != null && page.isBlank()) {
            throw new IllegalArgumentException("page cannot be blank");
        }
        this.page.set(page);
    }

    /**
     * Adds an extra parameter to the next request.
     * For ContactAdd you can pass fname, lname, etc.
     *
     * @param key   Parameter name
     * @param value Parameter value
     */
    public void AddPostParam(String key, String value) {
        requireNonBlank(key, "key");
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        Map<String, String> params = postParam.get();
        if (params == null) {
            params = new HashMap<>();
            postParam.set(params);
        }
        params.put(key, value);
    }

    /**
     * Enables test mode. In this mode SendSms, SmsStatus and SmsList use test endpoints
     * (sms/testsend, sms/teststatus, sms/testlist), so no real SMS are sent and no charges apply.
     */
    public void EnableTestMode() {
        testMode = true;
    }

    /** Disables test mode. */
    public void DisableTestMode() {
        testMode = false;
    }

    /**
     * Checks if test mode is active.
     *
     * @return true if test mode is enabled
     */
    public boolean IsTestModeActive() {
        return testMode;
    }

    private String getUrl(String baseDomain, String method) {
        String apiUrl = baseDomain + method;
        String pageVal = page.get();
        if (pageVal != null) {
            apiUrl = apiUrl + "?page=" + pageVal;
        }
        return apiUrl;
    }

    private static boolean isTransientNetworkError(IOException e) {
        return e instanceof BindException
                || e instanceof ConnectException
                || e instanceof HttpRetryException
                || e instanceof NoRouteToHostException
                || e instanceof PortUnreachableException
                || e instanceof ProtocolException
                || e instanceof SocketTimeoutException
                || e instanceof UnknownHostException
                || e instanceof UnknownServiceException;
    }

    @SuppressWarnings("unchecked")
    private String getData(Map<String, ?> form) {
        JSONObject json = new JSONObject();
        if (form != null) {
            for (Map.Entry<String, ?> entry : form.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
        }
        Map<String, String> params = postParam.get();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
        }
        return json.toString();
    }

    private JSONObject doRequest(String method, Map<String, ?> form) throws IOException, ParseException {
        try {
            if (GATE_URLS.isEmpty()) {
                throw new IOException("No gate URLs configured");
            }
            IOException lastError = null;
            for (String baseDomain : GATE_URLS) {
                try {
                    return trySend(baseDomain, true, method, form);
                } catch (SSLException e) {
                    try {
                        return trySend(baseDomain, false, method, form);
                    } catch (IOException e2) {
                        lastError = e2;
                    }
                } catch (IOException e) {
                    if (isTransientNetworkError(e)) {
                        lastError = new IOException(e.getMessage(), e);
                    } else {
                        throw e;
                    }
                }
            }
            throw lastError != null ? lastError : new IOException("All gate URLs failed");
        } finally {
            page.remove();
            postParam.remove();
        }
    }

    private JSONObject trySend(String baseDomain, boolean useHttps, String method, Map<String, ?> form)
            throws IOException, ParseException {
        String url = useHttps ? baseDomain : baseDomain.replace("https://", "http://");
        return doSendRequest(method, form, url);
    }

    private JSONObject doSendRequest(String method, Map<String, ?> form, String baseDomain) throws IOException, ParseException {
        URL urlObj = new URL(getUrl(baseDomain, method));
        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
        try {
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            con.setRequestProperty("Authorization", authHeader);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestMethod("POST");
            con.setDoOutput(true);

            byte[] input = getData(form).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = con.getOutputStream()) {
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = con.getResponseCode();
            InputStream in = responseCode == HttpURLConnection.HTTP_OK
                ? con.getInputStream()
                : con.getErrorStream();
            if (in == null) {
                in = new ByteArrayInputStream(new byte[0]);
            }
            try (InputStream stream = in;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

                Object obj = JSON_PARSER.parse(reader);
                JSONObject jsonObj = (JSONObject) obj;
                if (Boolean.FALSE.equals(jsonObj.get("success"))) {
                    Object msg = jsonObj.get("message");
                    Object reason = jsonObj.get("reason");
                    String errText = msg != null ? String.valueOf(msg)
                            : (reason != null ? String.valueOf(reason) : "Unknown error");
                    throw new IOException(errText);
                }
                return jsonObj;
            }
        } finally {
            con.disconnect();
        }
    }

    /**
     * Checks authorization by email and API key.
     *
     * @return API response with authorization info
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject IsAuthorized() throws IOException, ParseException {
        return doRequest("auth", null);
    }

    /**
     * Retrieves the list of tariffs.
     *
     * @return API response with tariffs
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject Tariffs() throws IOException, ParseException {
        return doRequest("tariffs", null);
    }

    /**
     * Retrieves the list of sender signatures.
     *
     * @return API response with signatures list
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject SignList() throws IOException, ParseException {
        return doRequest("sign/list", null);
    }

    /**
     * Retrieves the account balance.
     *
     * @return API response with balance. Example: {@code {"data": {"balance": 337.03}}}
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject Balance() throws IOException, ParseException {
        return doRequest("balance", null);
    }

    /**
     * Sends SMS with the specified sender signature.
     * In test mode ({@link #EnableTestMode()}) uses sms/testsend endpoint - no real SMS are sent.
     *
     * @param number Recipient number (format 70000000000)
     * @param text   Message text
     * @param sign   Sender signature
     * @return API response with sent SMS data. Example: {@code {"data": {"id": 12345, "from": "SMS Aero",
     * "number": "79031234567", "text": "Hello", "status": 0, "extendStatus": "queue", "cost": 5.49}}}
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject SendSms(String number, String text, String sign) throws IOException, ParseException {
        return SendSms(number, text, sign, null, null, null);
    }

    /**
     * Sends SMS with optional scheduled send and callback parameters.
     * Aligns with Python API: date_to_send, callback_url, callback_format.
     *
     * @param number         Recipient number (format 70000000000)
     * @param text           Message text
     * @param sign           Sender signature
     * @param dateToSend     Scheduled send time (null = send immediately)
     * @param callbackUrl    URL for delivery status webhook (null = disabled)
     * @param callbackFormat Callback format, e.g. "json" (null = default)
     * @return API response with sent SMS data
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject SendSms(String number, String text, String sign,
            Instant dateToSend, String callbackUrl, String callbackFormat) throws IOException, ParseException {
        requireNonBlankAll("number", number, "text", text, "sign", sign);
        Map<String, Object> data = new HashMap<>();
        data.put("number", number);
        data.put("text", text);
        data.put("sign", sign);
        if (dateToSend != null) {
            data.put("dateSend", dateToSend.getEpochSecond());
        }
        if (callbackUrl != null) {
            data.put("callbackUrl", callbackUrl);
        }
        if (callbackFormat != null) {
            data.put("callbackFormat", callbackFormat);
        }
        String smsMethod = testMode ? "sms/testsend" : "sms/send";
        return doRequest(smsMethod, data);
    }

    /**
     * Sends SMS with the default signature "SMS Aero".
     *
     * @param number Recipient number (format 70000000000)
     * @param text   Message text
     * @return API response with sent SMS data. Example: {@code {"data": {"id": 12345, "from": "SMS Aero",
     * "number": "79031234567", "text": "Hello", "status": 0, "extendStatus": "queue", "cost": 5.49}}}
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject SendSms(String number, String text) throws IOException, ParseException {
        return SendSms(number, text, "SMS Aero");
    }

    /**
     * Retrieves SMS status by ID.
     * In test mode ({@link #EnableTestMode()}) uses sms/teststatus.
     *
     * @param smsId SMS identifier
     * @return API response with status. Example: {@code {"data": {"id": 12345, "number": "79031234567", "status": 1,
     * "extendStatus": "delivery", "dateCreate": 1719115820, "dateAnswer": 1719115825}}}
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject SmsStatus(int smsId) throws IOException, ParseException {
        String statusMethod = testMode ? "sms/teststatus" : "sms/status";
        return doRequest(statusMethod, Map.of("id", Integer.toString(smsId)));
    }

    /**
     * Retrieves the list of sent SMS.
     * In test mode ({@link #EnableTestMode()}) uses sms/testlist.
     * Use {@link #SetPage(String)} before calling for paginated results.
     *
     * @return API response with SMS list
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject SmsList() throws IOException, ParseException {
        /*
            // Example
            SmsAero client = new SmsAero(email, apiKey);
            client.SetPage("2");
            System.out.println(client.SmsList());
        */
        String listMethod = testMode ? "sms/testlist" : "sms/list";
        return doRequest(listMethod, null);
    }

    /**
     * Determines the operator by phone number.
     *
     * @param number Phone number (format 70000000000)
     * @return API response with operator info
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject NumberOperator(String number) throws IOException, ParseException {
        requireNonBlank(number, "number");
        return doRequest("number/operator", Map.of("number", number));
    }

    /**
     * Creates a new contact group.
     *
     * @param name Group name
     * @return API response with created group data
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject GroupAdd(String name) throws IOException, ParseException {
        requireNonBlank(name, "name");
        return doRequest("group/add", Map.of("name", name));
    }

    /**
     * Retrieves the list of contact groups.
     *
     * @return API response with groups list
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject GroupList() throws IOException, ParseException {
        return doRequest("group/list", null);
    }

    /**
     * Deletes a contact group.
     *
     * @param groupId Group identifier
     * @return API response
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject GroupDelete(int groupId) throws IOException, ParseException {
        return doRequest("group/delete", Map.of("id", Integer.toString(groupId)));
    }

    /**
     * Deletes all contact groups.
     *
     * @return API response
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject GroupDeleteAll() throws IOException, ParseException {
        return doRequest("group/delete-all", null);
    }

    /**
     * Retrieves the list of linked bank cards.
     *
     * @return API response with cards list
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject Cards() throws IOException, ParseException {
        return doRequest("cards", null);
    }

    /**
     * Adds balance from a linked card.
     *
     * @param sum    Amount to add (in rubles)
     * @param cardId Card identifier from {@link #Cards()}
     * @return API response
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject AddBalance(int sum, int cardId) throws IOException, ParseException {
        return doRequest("balance/add",
            Map.of("sum", Integer.toString(sum), "card_id", Integer.toString(cardId)));
    }

    /**
     * Adds a number to the blacklist.
     *
     * @param number Phone number (format 70000000000)
     * @return API response
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject BlackListAdd(String number) throws IOException, ParseException {
        requireNonBlank(number, "number");
        return doRequest("blacklist/add", Map.of("number", number));
    }

    /**
     * Retrieves the list of numbers in the blacklist.
     *
     * @return API response with blacklist entries
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject BlackListList() throws IOException, ParseException {
        return doRequest("blacklist/list", null);
    }

    /**
     * Removes a number from the blacklist.
     *
     * @param blacklistId Blacklist entry identifier
     * @return API response
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject BlackListDelete(int blacklistId) throws IOException, ParseException {
        return doRequest("blacklist/delete", Map.of("id", Integer.toString(blacklistId)));
    }

    /**
     * HLR request: checks the number status in the network.
     *
     * @param number Phone number (format 70000000000)
     * @return API response with HLR request identifier
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject HlrCheck(String number) throws IOException, ParseException {
        requireNonBlank(number, "number");
        return doRequest("hlr/check", Map.of("number", number));
    }

    /**
     * Retrieves the status of an HLR request.
     *
     * @param hlrId HLR request identifier from {@link #HlrCheck(String)}
     * @return API response with status
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject HlrStatus(int hlrId) throws IOException, ParseException {
        return doRequest("hlr/status", Map.of("id", Integer.toString(hlrId)));
    }

    /**
     * Adds a contact. Extra fields (fname, lname, etc.) can be set via {@link #AddPostParam(String, String)}.
     *
     * <p>Example:
     * <pre>{@code
     * SmsAero client = new SmsAero(email, apiKey);
     * client.AddPostParam("fname", "First name");
     * client.AddPostParam("lname", "Last name");
     * System.out.println(client.ContactAdd("79038800350"));
     * }</pre>
     *
     * @param number Phone number (format 70000000000)
     * @return API response with contact data
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject ContactAdd(String number) throws IOException, ParseException {
        requireNonBlank(number, "number");
        return doRequest("contact/add", Map.of("number", number));
    }

    /**
     * Deletes a contact.
     *
     * @param contactId Contact identifier
     * @return API response
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject ContactDelete(int contactId) throws IOException, ParseException {
        return doRequest("contact/delete", Map.of("id", Integer.toString(contactId)));
    }

    /**
     * Deletes all contacts.
     *
     * @return API response
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject ContactDeleteAll() throws IOException, ParseException {
        return doRequest("contact/delete-all", null);
    }

    /**
     * Retrieves the list of contacts.
     *
     * @return API response with contacts list
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject ContactList() throws IOException, ParseException {
        return doRequest("contact/list", null);
    }

    /**
     * Sends a Viber message.
     *
     * @param sign    Sender signature (from {@link #ViberSignList()})
     * @param channel Channel ID
     * @param text    Message text
     * @param number  Recipient number (format 70000000000)
     * @return API response
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject ViberSend(String sign, String channel, String text, String number) throws IOException, ParseException {
        requireNonBlankAll("sign", sign, "channel", channel, "text", text, "number", number);
        return doRequest("viber/send", Map.of("number", number, "sign", sign, "channel", channel, "text", text));
    }

    /**
     * Retrieves the list of Viber signatures.
     *
     * @return API response with signatures list
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject ViberSignList() throws IOException, ParseException {
        return doRequest("viber/sign/list", null);
    }

    /**
     * Retrieves the list of sent Viber messages.
     *
     * @return API response with messages list
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject ViberList() throws IOException, ParseException {
        return doRequest("viber/list", null);
    }

    /**
     * Retrieves Viber delivery statistics by phone numbers.
     * Use {@link #SetPage(String)} before calling for paginated results.
     *
     * @param sendingId Viber sending identifier (from {@link #ViberSend} response)
     * @return API response with statistics. Example: {@code {"data": {"0": {"number": "79031234567",
     * "status": 0, "extendStatus": "send", "dateSend": 1511153341}, ...}}}
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject ViberStatistics(int sendingId) throws IOException, ParseException {
        return doRequest("viber/statistic", Map.of("sendingId", Integer.toString(sendingId)));
    }

    /**
     * Sends a confirmation code via Telegram.
     *
     * @param number Phone number (format 70000000000)
     * @param code   Confirmation code (4-8 digits)
     * @return API response. Example: {@code {"data": {"id": 1, "number": "79990000000", "telegramCode": "1234",
     * "status": 0, "extendStatus": "queue", "cost": "1.00"}}}
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject SendTelegram(String number, int code) throws IOException, ParseException {
        requireNonBlank(number, "number");
        return doRequest("telegram/send", Map.of("number", number, "code", Integer.toString(code)));
    }

    /**
     * Sends a Telegram message with the specified signature and text.
     * If the code cannot be delivered via Telegram, falls back to SMS with the given sign and text.
     *
     * @param number Phone number (format 70000000000)
     * @param code   Confirmation code
     * @param sign   Sender signature
     * @param text   Message text
     * @return API response. Example: {@code {"data": {"id": 1, "number": "79990000000", "telegramCode": "1234",
     * "status": 0, "extendStatus": "queue", "cost": "1.00"}}}
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject SendTelegram(String number, int code, String sign, String text) throws IOException, ParseException {
        requireNonBlankAll("number", number, "sign", sign, "text", text);
        return doRequest("telegram/send",
            Map.of("number", number, "code", Integer.toString(code), "sign", sign, "text", text));
    }

    /**
     * Retrieves the status of a Telegram message.
     *
     * @param telegramId Telegram message identifier (from {@link #SendTelegram(String, int)} or
     *                   {@link #SendTelegram(String, int, String, String)})
     * @return API response with status. Example: {@code {"data": {"id": 1, "number": "79990000000",
     * "status": 1, "extendStatus": "delivery", "cost": "1.00"}}}
     * @throws IOException    on network error or API response with success=false
     * @throws ParseException on JSON parse error
     */
    public JSONObject TelegramStatus(int telegramId) throws IOException, ParseException {
        return doRequest("telegram/status", Map.of("id", Integer.toString(telegramId)));
    }
}
