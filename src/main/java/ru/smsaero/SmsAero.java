package ru.smsaero;

import java.io.*;
import java.net.*;
import java.util.*;

import org.json.simple.*;
import org.json.simple.parser.*;


public class SmsAero {
    private static String _email;
    private static String _apiKey;

    private static final List<String> _gateUrls = Arrays.asList(
            "https://gate.smsaero.ru/v2/",
            "https://gate.smsaero.org/v2/",
            "https://gate.smsaero.net/v2/"
    );
    private String _baseDomain = null;
    private String _page = null;
    private Map<String, String> _postParam = null;

    public SmsAero(String emailAddr, String apiKey) {
        _email = emailAddr;
        _apiKey = apiKey;
    }

    public void SetPage(String page) {
        _page = page;
    }

    public void AddPostParam(String key, String value) {
        if (_postParam == null) {
            _postParam = new HashMap<String, String>();
        }
        _postParam.put(key, value);
    }

    private String _getUrl(String method) {
        String apiUrl = _baseDomain + method;

		if (_page != null) {
		    apiUrl = apiUrl + "?page=" + _page;
		    _page = null;
		}
		return apiUrl;
    }

    private String _getAuth() {
		String auth = _email + ":" + _apiKey;
        return "Basic " + new String(Base64.getEncoder().encode(auth.getBytes()));
    }

    private String _getData(Map<String, String> form) {
        JSONObject json = new JSONObject();
        if (form != null) {
            for (Map.Entry<String, String> entry : form.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
        }
        if (_postParam != null) {
            for (Map.Entry<String, String> entry : _postParam.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            _postParam = null;
        }
        return json.toString();
    }

    private JSONObject _doRequest(String method, Map<String, String> form) throws IOException, ParseException {
        int urlsLen = _gateUrls.size();
        for (int i = 0; i < urlsLen; i++) {
            try {
                _baseDomain = _gateUrls.get(i);
                return _doSendRequest(method, form);
            } catch (
                BindException
                | ConnectException
                | HttpRetryException
                | NoRouteToHostException
                | PortUnreachableException
                | ProtocolException
                | SocketTimeoutException
                | UnknownHostException
                | UnknownServiceException
                e
            ) {
                if(urlsLen == (i + 1)) {
                    throw new IOException(e.getMessage());
                }
                continue;
            }
        }
        return null;
    }

    private JSONObject _doSendRequest(String method, Map<String, String> form) throws IOException, ParseException {
        URL urlObj = new URL(_getUrl(method));
        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
        con.setRequestProperty ("Authorization", _getAuth());
        con.setRequestProperty("Content-Type", "application/json");
        con.addRequestProperty("User-Agent", "SAJavaClient/2.0");
        con.setRequestMethod("POST");
        con.setDoOutput(true);

        OutputStream os = con.getOutputStream();
        byte[] input = _getData(form).getBytes("utf-8");
        os.write(input, 0, input.length);
        os.flush();
        os.close();

        int responseCode = con.getResponseCode();
        BufferedReader reader;
        if (responseCode == HttpURLConnection.HTTP_OK) {
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
        }

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(reader);
        JSONObject jsonObj = (JSONObject) obj;
        if (jsonObj.get("success").equals(false)) {
            if (jsonObj.get("message") != null) {
                throw new IOException((String) jsonObj.get("message"));
            }
            throw new IOException((String) jsonObj.get("reason"));
        }
        return jsonObj;
	}

    public JSONObject IsAuthorized() throws IOException, ParseException {
        return _doRequest("auth", null);
    }

    public JSONObject Tariffs() throws IOException, ParseException {
        return _doRequest("tariffs", null);
    }

    public JSONObject SignList() throws IOException, ParseException {
        return _doRequest("sign/list", null);
    }

    public JSONObject Balance() throws IOException, ParseException {
        return _doRequest("balance", null);
    }

    public JSONObject SendSms(String number, String text, String sign) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("number", number);
        data.put("text", text);
        data.put("sign", sign);
        return _doRequest("sms/send", data);
    }

    public JSONObject SendSms(String number, String text) throws IOException, ParseException {
        return SendSms(number, text, "SMS Aero");
    }

    public JSONObject SmsStatus(int smsId) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("id", Integer.toString(smsId));
        return _doRequest("sms/status", data);
    }

    public JSONObject SmsList() throws IOException, ParseException {
        /*
            // Example
            src.main.java.com.smsaero.SmsAeroClient client = new src.main.java.com.smsaero.SmsAeroClient(email, apiKey);
            client.SetPage(2);
            System.out.println(client.SmsList());
        */
        return _doRequest("sms/list", null);
    }

    public JSONObject NumberOperator(String number) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("number", number);
        return _doRequest("number/operator", data);
    }

    public JSONObject GroupAdd(String name) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("name", name);
        return _doRequest("group/add", data);
    }

    public JSONObject GroupList() throws IOException, ParseException {
        return _doRequest("group/list", null);
    }

    public JSONObject GroupDelete(int groupId) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("id", Integer.toString(groupId));
        return _doRequest("group/delete", data);
    }

    public JSONObject Cards() throws IOException, ParseException {
        return _doRequest("cards", null);
    }

    public JSONObject AddBalance(int sum, int cardId) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("sum", Integer.toString(sum));
        data.put("card_id", Integer.toString(cardId));
        return _doRequest("balance/add", data);
    }

    public JSONObject BlackListAdd(String number) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("number", number);
        return _doRequest("blacklist/add", data);
    }

    public JSONObject BlackListList() throws IOException, ParseException {
        return _doRequest("blacklist/list", null);
    }

    public JSONObject BlackListDelete(int blacklistId) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("id", Integer.toString(blacklistId));
        return _doRequest("blacklist/delete", data);
    }

    public JSONObject HlrCheck(String number) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("number", number);
        return _doRequest("hlr/check", data);
    }

    public JSONObject HlrStatus(int hlrId) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("id", Integer.toString(hlrId));
        return _doRequest("hlr/status", data);
    }

    public JSONObject ContactAdd(String number) throws IOException, ParseException {
        /*
            // Example
            src.main.java.com.smsaero.SmsAeroClient client = new src.main.java.com.smsaero.SmsAeroClient(email, apiKey);
            client.AddPostParam("fname", "First name");
            client.AddPostParam("lname", "Last name");
            System.out.println(client.ContactAdd("79038800350"));
        */
        Map<String, String> data = new HashMap<String, String>();
        data.put("number", number);
        return _doRequest("contact/add", data);
    }

    public JSONObject ContactDelete(int contactId) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("id", Integer.toString(contactId));
        return _doRequest("contact/delete", data);
    }

    public JSONObject ContactList() throws IOException, ParseException {
        return _doRequest("contact/list", null);
    }

    public JSONObject ViberSend(String sign, String channel, String text, String number) throws IOException, ParseException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("number", number);
        data.put("sign", sign);
        data.put("channel", channel);
        data.put("text", text);
        return _doRequest("viber/send", data);
    }

    public JSONObject ViberSignList() throws IOException, ParseException {
        return _doRequest("viber/sign/list", null);
    }

    public JSONObject ViberList() throws IOException, ParseException {
        return _doRequest("viber/list", null);
    }
}
