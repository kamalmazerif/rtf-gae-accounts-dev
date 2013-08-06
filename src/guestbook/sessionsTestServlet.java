package guestbook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FacebookApi;
import org.scribe.builder.api.GoogleApi;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class sessionsTestServlet extends HttpServlet {

  private static final Logger log = Logger.getLogger(FacebookReturnServlet.class.getName());

  // Facebook redirects user back to us after auth


  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
  
    HttpSession session = req.getSession();
    PrintWriter outWriter = resp.getWriter();
  
    Document doc = Document.createShell("");
    writeStartOverLink(doc);
    doc.body().appendText("Handling GET - This is the document body").appendElement("br");
  
    writeDebugInfo(req, doc);
  
    HashMap<String, String> loginCommonNameDescriptions = LoginManager.getLoginCommonNameDescriptions(req.getSession());
    Set<String> loginNames = loginCommonNameDescriptions.keySet();
    
    
    // Drawing should come after action handling...
    if (!loginNames.contains(LoginType.GOOGLE.getName())) {
      writeGoogleLoginButton(doc);
    }
    
    if (!loginNames.contains(LoginType.FACEBOOK.getName())) {
      writeFacebookLoginButton(doc);
    } 
    
    if (!loginNames.contains(LoginType.TWITTER.getName())) {
      writeTwitterLoginButton(doc);
    }
  
    
    
    String paramRTFAction = req.getParameter("RTFAction");
    if ("UserReqGoogleAuth".equals(paramRTFAction)) {
      actionUserGoogleAuthStart(req, resp, doc);
    }
  
    if ("UserReqFacebookAuth".equals(paramRTFAction)) {
      actionUserFacebookCustomAuthStart(req, doc);
    }
  
    if ("UserReqFacebookAuthScribe".equals(paramRTFAction)) {
      actionUserFacebookScribeAuthStart(req, resp);
    }
  
    if ("UserReqTwitterAuth".equals(paramRTFAction)) {
      actionUserTwitterAuthStart(resp);
    }
  
    if ("CallbackGoogleUserAuth".equals(paramRTFAction)) {
      actionCallbackGoogleAuthScribe(req, resp, doc);
    }
  
    if ("CallbackFacebookUserAuth".equals(paramRTFAction)) {
      actionCallbackFacebookAuthScribe(req, resp, doc);
    }
  
    if ("CallbackTwitterUserAuth".equals(paramRTFAction)) {
      actionCallbackTwitterAuth(req, resp, doc);
    }
    
    if ("logout".equals(paramRTFAction)) {
      LoginManager.logOutUser(session);
      resp.sendRedirect(RTFServletConfig.PATH_HOME);
    }
  
    writeForm(doc);
  
    doc.body().appendText("Done");
    outWriter.write(doc.toString());
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    HttpSession session = req.getSession();
    PrintWriter outWriter = resp.getWriter();

    Document doc = Document.createShell("");
    writeStartOverLink(doc);
    doc.body().appendText("Handling POST - This is the document body").appendElement("br");
    writeDebugInfo(req, doc);

    // Find request parameters
    Map<String, String[]> parameters = req.getParameterMap();
    for (String parameter : parameters.keySet()) {
      // Here we ASSUME that no parameters are included more than once...
      if (parameters.get(parameter).length > 1) {
        // Should log duplicate parameter
        doc.body().appendText("Warning: Duplicate parameter!!!").appendElement("br");
      }
      // session.setAttribute(parameter, parameters.get(parameter)[0]);

    }

    doc.body().appendText("Done");
    outWriter.write(doc.toString());
    resp.sendRedirect(RTFServletConfig.PATH_HOME);
  }

  private void actionUserGoogleAuthStart(HttpServletRequest req, HttpServletResponse resp, Document doc)
      throws IOException {

    OAuthService service = new ServiceBuilder().provider(GoogleApi.class).apiKey(RTFServletConfig.GOOGLE_API_KEY)
        .apiSecret(RTFServletConfig.GOOGLE_API_SECRET).callback(RTFServletConfig.GOOGLE_USER_AUTH_CALLBACK_URL)
        .scope(RTFServletConfig.GOOGLE_OAUTH_REQ_SCOPE).build();

    // Obtain the Request Token
    Token requestToken = service.getRequestToken();
    req.getSession().setAttribute("requestToken", requestToken.getToken());
    req.getSession().setAttribute("requestTokenSecret", requestToken.getSecret());
    
    resp.sendRedirect("https://www.google.com/accounts/OAuthAuthorizeToken?oauth_token=" + requestToken.getToken());

  }

  private void actionUserFacebookScribeAuthStart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Attempt to implement Facebook with scribe
    OAuthService service = new ServiceBuilder().provider(FacebookApi.class).apiKey(RTFServletConfig.FACEBOOK_ID)
        .apiSecret(RTFServletConfig.FACEBOOK_SECRET).callback(RTFServletConfig.FACEBOOK_USER_AUTH_SCRIBE_CALLBACK_URL).build();
    // Scanner in = new Scanner(System.in);

    // Obtain the Authorization URL
    System.out.println("Fetching the Authorization URL...");
    String authorizationUrl = service.getAuthorizationUrl(null); // Dont need request token here
    log.info("Facebook User Auth URL: " + authorizationUrl);
    req.getSession().setAttribute("sessionType", "mysession");
    resp.sendRedirect(authorizationUrl);
  }

  // Done with no OAuth library code, following Facebook's developer instructions 8/1/2013
  private void actionUserFacebookCustomAuthStart(HttpServletRequest req, Document doc) {
    if ("code".equals(req.getParameter("response_type"))) {
      String code = req.getParameter("code");
      // String token = getFacebookAccessToken(code);
      // doc.body().appendText("Got token: " + token).appendElement("br");
      // getFacebookUserData(doc, token);
    }
  }

  private void actionUserTwitterAuthStart(HttpServletResponse resp) throws IOException {
    OAuthService service = new ServiceBuilder().provider(TwitterApi.class).apiKey(RTFServletConfig.TWITTER_KEY)
        .apiSecret(RTFServletConfig.TWITTER_SECRET).callback(RTFServletConfig.TWITTER_REDIRECT_URL).build();

    Token requestToken = service.getRequestToken();
    String authUrl = service.getAuthorizationUrl(requestToken);
    // doc.appendText("Twitter Auth URL: " + authUrl);
    log.info("Twitter Auth URL: " + authUrl);
    resp.sendRedirect(authUrl);
  }

  private void actionCallbackGoogleAuthScribe(HttpServletRequest req, HttpServletResponse resp, Document doc) throws JsonProcessingException, IOException {
    String paramToken = req.getParameter("oauth_token");
    String paramVerifier = req.getParameter("oauth_verifier");

    Verifier verifier = new Verifier(paramVerifier);

    OAuthRequest request = new OAuthRequest(Verb.GET, "https://www.googleapis.com/oauth2/v2/userinfo");
    
    OAuthService service = new ServiceBuilder().provider(GoogleApi.class).apiKey(RTFServletConfig.GOOGLE_API_KEY)
        .apiSecret(RTFServletConfig.GOOGLE_API_SECRET).callback(RTFServletConfig.GOOGLE_USER_AUTH_CALLBACK_URL)
        .scope(RTFServletConfig.GOOGLE_OAUTH_REQ_SCOPE).build();
    
    String reqToken = (String) req.getSession().getAttribute("requestToken");
    String reqTokenSecret = (String) req.getSession().getAttribute("requestTokenSecret");
    Token requestToken = new Token(reqToken, reqTokenSecret);
    Token accessToken = service.getAccessToken(requestToken, verifier);
    service.signRequest(accessToken, request);
    request.addHeader("GData-Version", "3.0");
    Response response = request.send();

    log.info(response.getCode() +"\r\n"+ response.getBody());
    String responseBody = response.getBody();

    ObjectMapper m = new ObjectMapper();
    JsonNode rootNode = m.readTree(response.getBody());
    String id = rootNode.path("sub").textValue();
    String name = rootNode.path("name").textValue();
    String email = rootNode.path("email").textValue();
    String picUrl = rootNode.path("picture").textValue();
    
    doc.body().appendElement("img").attr("src",picUrl);
    doc.body().appendText("Got Google ID: " + id + " name: " + name + " email: " + email).appendElement("br");
    
    HashMap<String, String> googleLoginHash = new HashMap<String, String>();
    googleLoginHash.put(RTFConstants.LOGIN_ID, id);
    googleLoginHash.put(RTFConstants.LOGIN_EMAIL, email);
    googleLoginHash.put(RTFConstants.LOGIN_PERSON_NAME, name);
    LoginManager.addLoginType(req.getSession(), LoginType.GOOGLE, googleLoginHash);
    resp.sendRedirect(RTFServletConfig.PATH_HOME);
  }

  private void actionCallbackFacebookAuthScribe(HttpServletRequest req, HttpServletResponse resp, Document doc) throws IOException,
      JsonProcessingException {

    // Facebook user auth has returned
    OAuthService service = new ServiceBuilder().provider(FacebookApi.class).apiKey(RTFServletConfig.FACEBOOK_ID)
        .apiSecret(RTFServletConfig.FACEBOOK_SECRET).callback(RTFServletConfig.FACEBOOK_USER_AUTH_SCRIBE_CALLBACK_URL).build();
    // OAuthService service = (OAuthService)req.getSession().getAttribute("scribeservice");
    // String authorizationUrl = service.getAuthorizationUrl(null);

    Verifier verifier = new Verifier(req.getParameter("code"));
    // Trade the Request Token and Verfier for the Access Token
    System.out.println("Trading the Request Token for an Access Token...");
    Token nullToken = null;
    Token accessToken = service.getAccessToken(nullToken, verifier);

    // Now let's go and ask for a protected resource!
    OAuthRequest request = new OAuthRequest(Verb.GET, "https://graph.facebook.com/me");
    service.signRequest(accessToken, request);
    Response response = request.send();
    int responseCode = response.getCode();
    String responseBody = response.getBody();

    ObjectMapper m = new ObjectMapper();
    JsonNode rootNode = m.readTree(response.getBody());
    String id = rootNode.path("id").textValue();
    String name = rootNode.path("name").textValue();
    String email = rootNode.path("email").textValue();

    doc.body().appendText("Got Facebook ID: " + id + " name: " + name + " email: " + email);
    log.info("Response Body: " + responseBody);
    
    HashMap<String, String> facebookLoginHash = new HashMap<String, String>();
    facebookLoginHash.put(RTFConstants.LOGIN_ID, id);
    facebookLoginHash.put(RTFConstants.LOGIN_EMAIL, email);
    facebookLoginHash.put(RTFConstants.LOGIN_PERSON_NAME, name);
    LoginManager.addLoginType(req.getSession(), LoginType.FACEBOOK, facebookLoginHash);
    resp.sendRedirect(RTFServletConfig.PATH_HOME);
  }

  private void actionCallbackTwitterAuth(HttpServletRequest req, HttpServletResponse resp, Document doc) throws IOException,
      JsonProcessingException {
    // User approved/cancelled twitter authorization of RateThisFest
    Token token = new Token(req.getParameter("oauth_token"), req.getParameter("oauth_verifier"));
    Verifier verifier = new Verifier(req.getParameter("oauth_verifier"));

    OAuthService service = new ServiceBuilder().provider(TwitterApi.class).apiKey(RTFServletConfig.TWITTER_KEY)
        .apiSecret(RTFServletConfig.TWITTER_SECRET).callback(RTFServletConfig.TWITTER_REDIRECT_URL).build();
    Token accessToken = service.getAccessToken(token, verifier);

    OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.twitter.com/1.1/account/verify_credentials.json");
    service.signRequest(accessToken, request); // the access token from step 4
    Response response = request.send();
    String responseBody = response.getBody();

    ObjectMapper m = new ObjectMapper();
    JsonNode rootNode = m.readTree(response.getBody());
    String id = rootNode.path("id_str").textValue();
    String name = rootNode.path("name").textValue();
    String twitterName = rootNode.path("screen_name").textValue();

    doc.body().appendText("Got twitter id: " + id + " name: " + name + " Twitter Handle: " + twitterName)
        .appendElement("br");

    log.info(responseBody);
    
    HashMap<String, String> twitterLoginHash = new HashMap<String, String>();
    twitterLoginHash.put(RTFConstants.LOGIN_ID, id);
    twitterLoginHash.put(RTFConstants.LOGIN_SCREEN_NAME, twitterName);
    twitterLoginHash.put(RTFConstants.LOGIN_PERSON_NAME, name);
    LoginManager.addLoginType(req.getSession(), LoginType.TWITTER, twitterLoginHash);
    resp.sendRedirect(RTFServletConfig.PATH_HOME);
  }

  private void writeStartOverLink(Document doc) {
    doc.body().appendElement("a").attr("href", RTFServletConfig.PATH_HOME).appendText("Home Page")
    .appendElement("br");    
    doc.body().appendElement("a").attr("href", RTFServletConfig.PATH_HOME +"?RTFAction=logout").appendText("Wipe Login Data")
    .appendElement("br");
  }

  private void writeForm(Document doc) {
    // <form action="/sign" method="post">
    Element formElement = doc.body().appendElement("form").attr("action", RTFServletConfig.PATH_HOME).attr("method", "post");

    // <div><textarea name="content" rows="3" cols="60"></textarea></div>
    formElement.appendElement("div").appendElement("textarea").attr("name", "content").attr("rows", "3")
        .attr("cols", "60");

    // <input type="hidden" name="guestbookName"
    // value="${fn:escapeXml(guestbookName)}"/>
    formElement.appendElement("input").attr("type", "hidden").attr("name", "hiddenAttributeName")
        .attr("value", "hiddenAttributeValue");

    // <div><input type="submit" value="Post Greeting" /></div>
    formElement.appendElement("div").appendElement("input").attr("type", "submit").attr("value", "Set New Attribute");

    // </form>
  }

  private void writeGoogleLoginButton(Document doc) {
    String urlToUse = RTFServletConfig.GOOGLE_USER_AUTH_START_URL;
    doc.body().appendElement("A").attr("href", urlToUse).appendElement("img")
        .attr("src", "https://developers.google.com/accounts/images/sign-in-with-google.png").attr("border", "0")
        .appendElement("br");
  }

  private void writeFacebookLoginButton(Document doc) {
    String urlToUseWhenYouClickOnTheFacebookButton = RTFServletConfig.FACEBOOK_USER_AUTH_SCRIBE_START_URL;
  
    doc.body().appendElement("A").attr("href", urlToUseWhenYouClickOnTheFacebookButton).appendElement("img")
        .attr("src", "http://dragon.ak.fbcdn.net/hphotos-ak-ash3/851558_153968161448238_508278025_n.png")
        .attr("border", "0").appendElement("br");
  }

  private void writeTwitterLoginButton(Document doc) {
    String urlToUse = RTFServletConfig.PATH_HOME+ "?RTFAction=UserReqTwitterAuth";
    doc.body()
        .appendElement("A")
        .attr("href", urlToUse)
        .appendElement("img")
        .attr("src",
            "https://encrypted-tbn3.gstatic.com/images?q=tbn:ANd9GcSdYJnRQqyWHMkm9VgP_aHf0gc4wbREFiv0z72T_xVltn4vNWb9NA")
        .attr("border", "0").appendElement("br");
  }

  private void writeDebugInfo(HttpServletRequest req, Document doc) throws IOException {
    // Output session attributes
    HttpSession session = req.getSession();
    // session.setAttribute("sessionType", "The wrong type!!!");
    doc.body().appendText("Session ID:[" + req.getSession().getId() + "] Attributes:").appendElement("br");

    Enumeration attributeNameEnumeration = session.getAttributeNames();
    if (!attributeNameEnumeration.hasMoreElements()) {
      doc.body().appendText("(none)").appendElement("br");
    }

    String attribName;
    Object attribValue;
    while (attributeNameEnumeration.hasMoreElements()) {
      attribName = (String) attributeNameEnumeration.nextElement();
      attribValue = session.getAttribute(attribName);
      doc.body().appendText(attribName + " = " + attribValue).appendElement("br");
    }

    // Output Query String
    String queryString = req.getQueryString();
    doc.body().appendElement("br").appendText("Query String:").appendElement("br");
    if (queryString == null) {
      doc.body().appendText("").appendElement("br");
    } else {
      doc.body().appendText(queryString).appendElement("br");
    }

    // Output Request Parameters
    Map<String, String[]> parameters = req.getParameterMap();
    doc.body().appendElement("br").appendText("Request Parameters(" + parameters.size() + ")").appendElement("br");

    for (String parameter : parameters.keySet()) {
      // Here we ASSUME that no parameters are included more than once...
      if (parameters.get(parameter).length > 1) {
        doc.body().appendText("Warning: Duplicate parameter!!!").appendElement("br");
      }
      doc.body().appendText(parameter + " = " + parameters.get(parameter)[0]).appendElement("br");
    }
    
    HashMap<String, String> loginCommonNameDescriptions = LoginManager.getLoginCommonNameDescriptions(session);
    if (!loginCommonNameDescriptions.isEmpty()) {
      doc.body().appendText("Active Login Types:").appendElement("br");
      
      
      
      Set<String> keySet = loginCommonNameDescriptions.keySet();
      ArrayList<String> loginTypeList = new ArrayList<String>(keySet);
      Collections.sort(loginTypeList);
      for (String loginTypeName : loginTypeList) {
        String loginTypeDescription = loginCommonNameDescriptions.get(loginTypeName);
        doc.body().appendText(loginTypeName +": "+loginTypeDescription).appendElement("br");
      }
    }

    doc.body().appendText("Remainder of request is as follows:").appendElement("br");
    InputStreamReader streamReader = new InputStreamReader(req.getInputStream());
    BufferedReader bufReader = new BufferedReader(streamReader);

    String lineRead = null;
    while ((lineRead = bufReader.readLine()) != null) {
      doc.body().appendText(lineRead).appendElement("br");
    }
    doc.body().appendElement("hr");
  }
  

  
}
