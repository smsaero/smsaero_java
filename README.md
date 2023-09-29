# SmsAero Java Client


### Usage:

    import org.json.simple.*;
    
    
    public class Main {
        
        // Get credentials from account settings page: https://smsaero.ru/cabinet/settings/apikey/
        private static final String email = "your email";
        private static final String apiKey = "your api key";
        private static final String sign = "SmsAero";
    
        public static void main(String[] args) {
            SmsAeroClient client = new SmsAeroClient(email, apiKey);
    
            try {
                JSONObject balanceResult = client.Balance();
                JSONObject data = (JSONObject) balanceResult.get("data");
                if (data.get("balance").equals(0.00)) {
                    System.out.println("Insufficient balance.");
                    System.exit(-1);
                }
    
                JSONObject sendResult = client.Send("70000000000", "Hello, World!", sign);
                if (sendResult == null) {
                    System.out.println("Can not send sms.");
                    System.exit(-1);
                }
                if (sendResult.get("success").equals(false)) {
                    System.out.println(sendResult.get("reason"));
                    System.exit(-1);
                }
                data = (JSONObject) sendResult.get("data");
                System.out.println("Successfully sent.");
                System.out.println(String.format("Msg ID: %d", data.get("id")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


### License

    MIT License
