package fes.kiosk;

import java.net.URL;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;


import java.net.MalformedURLException;

import org.json.JSONObject;

public class KioskEnquireNumHelper {
    //static final private String sEndpointUat = "https://iibdev-lb.smartone.com:7844/KioskNumberEnquiry?wsdl"; // staging  // old
//    static final private String sEndpointUat = "https://iibdev:7845/KioskNumberEnquiry?wsdl"; // staging
//    static final private String sEndpointProd = "https://zib-lb:7843/KioskNumberEnquiry?wsdl";  // production
    
    static final private String sEndpointUat = "https://tykapi-u.hksmartone.com/salesop/uat/kiosk/KioskNumberEnquiry";
    static final private String sEndpointProd = "https://tykapi.hksmartone.com/salesop/kiosk/KioskNumberEnquiry";  // production
    
    
    public KioskEnquireNumHelper() {
        super();
    }
    
    static public JSONObject kioskEnquireNumberRequest(String sServerMode, String sKioskNum, String sToken, String sCustNum) throws Exception {
        String sEndpoint = "staging".equals(sServerMode) ? sEndpointUat.replace("?wsdl", "") : sEndpointProd.replace("?wsdl", "");
        String sAuthId = "staging".equals(sServerMode)
            ? "eyJvcmciOiI2NGY2Y2I1ZmFiYzRhYWI4MGNlZDE1NTUiLCJpZCI6Ijc4Y2RiMTAxMzczNzQ1NzA4YmI0MjA3YzE2NmZmNWZhIiwiaCI6Im11cm11cjY0In0="
            : "eyJvcmciOiI2NTcxMzJkNTc2NzRjZjY2YmE3MTc2NDIiLCJpZCI6IjA0YmI3YWUxMDI2ODQ3ODVhMzAyZjJlOWI5MzA4NmE4IiwiaCI6Im11cm11cjY0In0=";
        if(sKioskNum == null) {
            sKioskNum = "SMP1";
        }
        String soapBody =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:kio=\"http://kiosk.fes/\">" +
                    "  <soapenv:Header/>" +
                    "  <soapenv:Body>" +
                    "    <kio:KioskEnquireNumberRequest>" +
                    "      <arg0>" +
                    "        <kioskNum>" + sKioskNum + "</kioskNum>" +
                    "        <tokenID>" + sToken + "</tokenID>" +
                    "        <inputNum>" + sCustNum + "</inputNum>" +
                    "      </arg0>" +
                    "    </kio:KioskEnquireNumberRequest>" +
                    "  </soapenv:Body>" +
                    "</soapenv:Envelope>";
        
                HttpURLConnection conn = null;
                JSONObject jOut = new JSONObject();
                try {
                    URL url = new URL(sEndpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
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
                        jOut.put("num_type", extractXmlValue(xml, "numType"));
                        jOut.put("input_by_subr", extractXmlValue(xml, "inputBySubr"));
                        jOut.put("cust_num", extractXmlValue(xml, "custNum"));
                        jOut.put("subr_num", extractXmlValue(xml, "subrNum"));
                        jOut.put("cust_name", extractXmlValue(xml, "custName"));
                        jOut.put("os_subr_amt", extractXmlValue(xml, "osSubrAmt"));   // type double 
                        jOut.put("os_cust_amt", extractXmlValue(xml, "osCustAmt"));
                        
                    }
                } finally {
                    if (conn != null) conn.disconnect();
                }
        
        return jOut;
    }
    
    private static String extractXmlValue(String xml, String tag) {
            String open = "<" + tag + ">";
            String close = "</" + tag + ">";
            int start = xml.indexOf(open);
            int end   = xml.indexOf(close);
            if (start == -1 || end == -1) return "";
            return xml.substring(start + open.length(), end);
        }
}
