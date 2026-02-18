package ru.smsaero;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * CLI for sending SMS via SMS Aero API.
 *
 * <p>Accepts arguments and environment variables (SMSAERO_EMAIL, SMSAERO_API_KEY, etc.).
 *
 * <p>Example:
 *
 * <pre>
 * java -jar smsaero-cli.jar --email YOUR_EMAIL --api_key YOUR_API_KEY --phone 70000000000 --message "Hello"
 * </pre>
 *
 * <p>Docker:
 *
 * <pre>
 * docker run --rm -e SMSAERO_EMAIL=... -e SMSAERO_API_KEY=... smsaero/smsaero_java:latest --phone 70000000000 --message "Hello"
 * </pre>
 */
public final class CommandLine {

    private static final String DEFAULT_SIGN = "SMS Aero";

    private CommandLine() {}

    private static final String ENV_EMAIL = "SMSAERO_EMAIL";
    private static final String ENV_API_KEY = "SMSAERO_API_KEY";
    private static final String ENV_PHONE = "SMSAERO_PHONE";
    private static final String ENV_MESSAGE = "SMSAERO_MESSAGE";
    private static final String ENV_SIGN = "SMSAERO_SIGN";

    /**
     * Parses command-line arguments.
     *
     * @param args argument array (e.g. --email, value, --api_key, value, ...)
     * @return map of key -> value
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> parsed = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    parsed.put(key, args[i + 1]);
                    i++;
                } else {
                    parsed.put(key, "true");
                }
            }
        }
        return parsed;
    }

    /**
     * Prints usage help.
     */
    private static void printUsage() {
        System.err.println(
                "Usage: smsaero-cli --email EMAIL --api_key API_KEY --phone PHONE --message MESSAGE [--sign SIGN] [--debug]");
        System.err.println("");
        System.err.println("Required (args or env):");
        System.err.println("  --email     Email registered in SmsAero (env: SMSAERO_EMAIL)");
        System.err.println("  --api_key   API key from SmsAero dashboard (env: SMSAERO_API_KEY)");
        System.err.println("  --phone     Recipient number, intl format without + (env: SMSAERO_PHONE)");
        System.err.println("  --message   Message text, 2-640 chars (env: SMSAERO_MESSAGE)");
        System.err.println("");
        System.err.println("Optional:");
        System.err.println("  --sign      Sender signature (default: " + DEFAULT_SIGN + ", env: SMSAERO_SIGN)");
        System.err.println("  --debug     Enable debug mode (test send)");
        System.err.println("  --help      Show this help");
        System.err.println("");
        System.err.println("Example:");
        System.err.println(
                "  smsaero-cli --email user@example.com --api_key KEY --phone 70000000000 --message \"Hello\"");
    }

    /**
     * Resolves a value from args or environment (args take precedence).
     */
    private static String resolve(Map<String, String> opts, String argKey, String envKey) {
        String arg = opts.get(argKey);
        if (arg != null && !arg.isEmpty()) {
            return arg;
        }
        String env = System.getenv(envKey);
        return (env != null && !env.isEmpty()) ? env : null;
    }

    /**
     * Entry point of the CLI.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Map<String, String> opts = parseArgs(args);

        if (opts.containsKey("help") || args.length == 0) {
            printUsage();
            System.exit(opts.containsKey("help") ? 0 : 1);
        }

        String email = resolve(opts, "email", ENV_EMAIL);
        String apiKey = resolve(opts, "api_key", ENV_API_KEY);
        String phone = resolve(opts, "phone", ENV_PHONE);
        String message = resolve(opts, "message", ENV_MESSAGE);
        String sign = resolve(opts, "sign", ENV_SIGN);
        if (sign == null) {
            sign = DEFAULT_SIGN;
        }
        boolean debug = opts.containsKey("debug");

        if (email == null || apiKey == null || phone == null || message == null) {
            System.err.println("Error: missing required parameters (provide via args or env).");
            printUsage();
            System.exit(1);
        }

        try {
            SmsAero client = new SmsAero(email, apiKey);
            if (debug) {
                client.EnableTestMode();
            }

            JSONObject result = client.SendSms(phone, message, sign);
            if (result != null) {
                System.out.println(result.toString());
                System.exit(0);
            } else {
                System.err.println("Error: failed to send SMS.");
                System.exit(1);
            }
        } catch (IOException | ParseException e) {
            System.err.println("SMS send error: " + e.getMessage());
            if (debug) {
                e.printStackTrace(System.err);
            }
            System.exit(1);
        }
    }
}
