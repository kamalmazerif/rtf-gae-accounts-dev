package guestbook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FacebookReturnServlet extends HttpServlet {

  private static final Logger log = Logger.getLogger(FacebookReturnServlet.class.getName());

  public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

    String queryString = req.getQueryString();
    log.info("Query String:" + queryString);

    Map<String, String[]> parameters = req.getParameterMap();
    log.info("Request Parameters(" + parameters.size() + "):");
    for (String parameter : parameters.keySet()) {
      String[] values = parameters.get(parameter);

      StringBuffer valueAggregate = new StringBuffer();
      int valueCount = 0;
      for (String value : values) {
        if (valueCount > 0) {
          valueAggregate.append(", ");
        }
        valueAggregate.append(value);
        valueCount++;
      }

      log.info(parameter + "[" + values.length + "] = " + valueAggregate.toString());
    }

    String code = req.getParameter("code");
    if (code == null || code.equals("")) {
      // an error occurred, handle this
    }

    String token = null;
    try {
      String g = "https://graph.facebook.com/oauth/access_token?client_id=1404819023065237&redirect_uri="
          + URLEncoder.encode("http://te-s-t.appspot.com/facebookreturn") // must be the same as before EVEN THOUGH NOT
                                                                          // USED
          + "&client_secret=636bd9d4a3c57abbeead5e9ac85c710a&code=" + code;
      log.info("Requesting: " + g);
      URL u = new URL(g);
      URLConnection c = u.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
      String inputLine;
      StringBuffer b = new StringBuffer();
      log.info("Result:");
      while ((inputLine = in.readLine()) != null) {
        log.info(inputLine);
        // log.info("Value of b1: " + b.toString());
        b.append(inputLine + "\n");
        // log.info("Value of b2: " + b.toString());
      }
      in.close();
      // log.info("Value of b3: " + b.toString());
      // This part is wrong, token must be extracted from the response
      // token = b.toString();
      // if (token.startsWith("{")) {
      // String exString = "error on requesting token: " + token + " with code: " + code;
      // throw new Exception(exString);
      // }
      log.info("Trying to split: " + b.toString());
      String[] resultStrings = b.toString().split("&"); // Must escape ampersand in regex
      for (String resultString : resultStrings) {

        if (resultString.indexOf("access_token") != -1) {
          String[] accessTokenKeyValue = resultString.split("=");
          log.info("Access token found:" + accessTokenKeyValue[0] + " " + accessTokenKeyValue[1]); // kill this too
          if (accessTokenKeyValue.length > 1) {
            token = accessTokenKeyValue[1];
          }
        }
      }
    } catch (Exception e) {
      // an error occurred, handle this
    }

    String graph = null;
    try {
      String g = "https://graph.facebook.com/me?access_token=" + token;
      log.info("Requesting: " + g);
      URL u = new URL(g);
      URLConnection c = u.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
      String inputLine;
      StringBuffer b = new StringBuffer();
      log.info("Result:");
      while ((inputLine = in.readLine()) != null)
        log.info(inputLine);
      b.append(inputLine + "\n");
      in.close();
      graph = b.toString();
    } catch (Exception e) {
      // an error occurred, handle this
    }

    String facebookId;
    String firstName;
    String middleNames;
    String lastName;
    String email;

  }
}
