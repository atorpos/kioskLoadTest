package fes.kiosk;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;

import javax.xml.ws.BindingProvider;

import org.json.JSONObject;

public class KioskTokenHelper {
    //static final private String sEndpointUat = "https://iibdev11.smartone.com:7844/KioskAuthentication?wsdl"; // staging fail: WebServiceException: Failed to access the WSDL at: https://iibdev11.smartone.com:7844/KioskAuthentication?wsdl. It failed with: Hostname verification failed
    //static final private String sEndpointUat = "https://iibdev-lb.smartone.com:7844/KioskAuthentication?wsdl"; // staging OK  // old
//    static final private String sEndpointUat = "https://iibdev:7845/KioskAuthentication?wsdl"; 
//    static final private String sEndpointProd = "https://zib-lb:7843/KioskAuthentication?wsdl";  // production
    static final public String KIOSK_NUM_CARE = "SMP1";  // assigned value: MyAccount: "MYP1", STCare: "SMP1".
    static final private String sEndpointProd = "https://tykapi.hksmartone.com/salesop/kiosk/KioskAuthentication";  // production
    static final public String sEndpointUat = "https://tykapi-u.hksmartone.com/salesop/uat/kiosk/KioskAuthentication";
    
    public KioskTokenHelper() {
        super();
    }
    
    static public String getEndPoint (String sServerMode) {
        return (sServerMode.equals("staging"))? sEndpointUat : sEndpointProd;
    }
    
    static public JSONObject kioskTokenRequest(String sServerMode, String sKioskNum) throws Exception {
        String sEndpoint = "staging".equals(sServerMode) ? sEndpointUat.replace("?wsdl", "") : sEndpointProd.replace("?wsdl", "");
        String sAuthId = "staging".equals(sServerMode)
            ? "eyJvcmciOiI2NGY2Y2I1ZmFiYzRhYWI4MGNlZDE1NTUiLCJpZCI6Ijc4Y2RiMTAxMzczNzQ1NzA4YmI0MjA3YzE2NmZmNWZhIiwiaCI6Im11cm11cjY0In0="
            : "eyJvcmciOiI2NTcxMzJkNTc2NzRjZjY2YmE3MTc2NDIiLCJpZCI6IjA0YmI3YWUxMDI2ODQ3ODVhMzAyZjJlOWI5MzA4NmE4IiwiaCI6Im11cm11cjY0In0=";  // TODO: set production Auth_id value

        // Build SOAP envelope manually
        String soapBody =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:kio=\"http://kiosk.fes/\">" +
            "  <soapenv:Header/>" +
            "  <soapenv:Body>" +
            "    <kio:KioskTokenRequest>" +
            "      <arg0>" +
            "        <kioskNum>" + sKioskNum + "</kioskNum>" +
            "      </arg0>" +
            "    </kio:KioskTokenRequest>" +
            "  </soapenv:Body>" +
            "</soapenv:Envelope>";

        HttpURLConnection conn = null;
        JSONObject jOut = new JSONObject();
        try {
            URL url = new URL(sEndpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            conn.setRequestProperty("SOAPAction", "http://kiosk.fes/Authentication/KioskTokenRequestRequest");
            conn.setRequestProperty("Auth_id", sAuthId);   // <-- Auth_id header here
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(soapBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    java.nio.charset.StandardCharsets.UTF_8))) {

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);

                // Parse response XML manually (simple tag extraction)
                String xml = response.toString();
                jOut.put("result_code",     extractXmlValue(xml, "resultCode"));
                jOut.put("result_err_code", extractXmlValue(xml, "resultErrCode"));
                jOut.put("kiosk_num",       extractXmlValue(xml, "kioskNum"));
                jOut.put("token_id",        extractXmlValue(xml, "tokenID"));
                //
                if (responseCode < 200 || responseCode >= 300) {
                    // error response
                    JSONObject jRsp = new JSONObject();
                    jRsp.put("responseCode", responseCode);
                    jRsp.put("responseMsg", response.toString());
    
                    jOut.put("responseError", jRsp);
                }
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
        return jOut;
    }

    // Helper to pull value from a simple XML tag
    private static String extractXmlValue(String xml, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = xml.indexOf(open);
        int end   = xml.indexOf(close);
        if (start == -1 || end == -1) return "";
        return xml.substring(start + open.length(), end);
    }

}
