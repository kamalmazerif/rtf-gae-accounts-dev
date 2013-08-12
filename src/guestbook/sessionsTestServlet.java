package guestbook;

import guestbook.login.AuthProviderAccount;
import guestbook.login.LoginManager;
import guestbook.login.LoginType;
import guestbook.login.RTFAccount;
import guestbook.login.RTFAccountException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
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

  public static final String PARAM_NAME_RTFACTION = "RTFAction";

  public static final String GOOGLE_PARAM_OAUTH_VERIFIER = "oauth_verifier";
  public static final String GOOGLE_PARAM_OAUTH_TOKEN = "oauth_token";
  public static final String GOOGLE_REDIRECT_URL_BASE = "https://www.google.com/accounts/OAuthAuthorizeToken";
  public static final String GOOGLE_REDIRECT_REQUEST_TOKEN_SECRET = "requestTokenSecret";
  public static final String GOOGLE_REDIRECT_REQUEST_TOKEN = "requestToken";

  public static final String ACTION_DESTROY_ACCOUNT = "destroyAccount";
  public static final String ACTION_LOGOUT = "logout";

  public static final String ACTION_GOOGLE_AUTH = "UserReqGoogleAuth";
  public static final String ACTION_FACEBOOK_AUTH_SCRIBE = "UserReqFacebookAuthScribe";
  public static final String ACTION_FACEBOOK_AUTH = "UserReqFacebookAuth";
  public static final String ACTION_TWITTER_AUTH = "UserReqTwitterAuth";

  public static final String ACTION_CALLBACK_GOOGLE_AUTH = "CallbackGoogleUserAuth";
  public static final String ACTION_CALLBACK_FACEBOOK_AUTH = "CallbackFacebookUserAuth";
  public static final String ACTION_CALLBACK_TWITTER_AUTH = "CallbackTwitterUserAuth";

  public static final String TWITTER_PROTECTED_URL_USERINFO = "https://api.twitter.com/1.1/account/verify_credentials.json";
  public static final String FACEBOOK_PROTECTED_URL_USERINFO = "https://graph.facebook.com/me";
  public static final String GOOGLE_PROTECTED_URL_USERINFO = "https://www.googleapis.com/oauth2/v2/userinfo";

  private static final Logger log = Logger.getLogger(new Object() {
  }.getClass().getEnclosingClass().getName());

  // Facebook redirects user back to us after auth

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    HttpSession session = req.getSession();
    PrintWriter outWriter = resp.getWriter();

    Document doc = Document.createShell("");
    writeStartOverLink(doc);
    doc.body().appendText("Handling GET - This is the document body").appendElement("br");

    writeDebugInfo(req, doc);

    RTFAccount currentLogin = LoginManager.getCurrentLogin(session);
    if (currentLogin == null) {
      writeGoogleLoginButton(doc);
      writeFacebookLoginButton(doc);
      writeTwitterLoginButton(doc);
    } else {

      // Drawing should come after action handling...
      if (!currentLogin.isLoggedInAPType(LoginType.GOOGLE)) {
        writeGoogleLoginButton(doc);
      }

      if (!currentLogin.isLoggedInAPType(LoginType.FACEBOOK)) {
        writeFacebookLoginButton(doc);
      }

      if (!currentLogin.isLoggedInAPType(LoginType.TWITTER)) {
        writeTwitterLoginButton(doc);
      }
    }

    String paramRTFAction = req.getParameter(PARAM_NAME_RTFACTION);
    if (ACTION_GOOGLE_AUTH.equals(paramRTFAction)) {
      actionUserGoogleAuthStart(req, resp, doc);
    }

    if (ACTION_FACEBOOK_AUTH.equals(paramRTFAction)) {
      actionUserFacebookCustomAuthStart(req, doc);
    }

    if (ACTION_FACEBOOK_AUTH_SCRIBE.equals(paramRTFAction)) {
      actionUserFacebookScribeAuthStart(req, resp);
    }

    if (ACTION_TWITTER_AUTH.equals(paramRTFAction)) {
      actionUserTwitterAuthStart(resp);
    }

    if (ACTION_CALLBACK_GOOGLE_AUTH.equals(paramRTFAction)) {
      actionCallbackGoogleAuthScribe(req, resp, doc);
    }

    if (ACTION_CALLBACK_FACEBOOK_AUTH.equals(paramRTFAction)) {
      actionCallbackFacebookAuthScribe(req, resp, doc);
    }

    if (ACTION_CALLBACK_TWITTER_AUTH.equals(paramRTFAction)) {
      actionCallbackTwitterAuth(req, resp, doc);
    }

    if (ACTION_LOGOUT.equals(paramRTFAction)) {
      LoginManager.logOutUser(session);
      resp.sendRedirect(RTFServletConfig.PATH_HOME);
    }

    if (ACTION_DESTROY_ACCOUNT.equals(paramRTFAction)) {
      LoginManager.destroyRTFAccount(session);
      LoginManager.logOutUser(session);
      resp.sendRedirect(RTFServletConfig.PATH_HOME);
    }

    // writeForm(doc);

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

    String redirectUrl = libraryActionGoogleAuthStart(req, resp);
    resp.sendRedirect(redirectUrl);
  }

  private String libraryActionGoogleAuthStart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    OAuthService service = new ServiceBuilder().provider(GoogleApi.class).apiKey(RTFServletConfig.GOOGLE_API_KEY)
        .apiSecret(RTFServletConfig.GOOGLE_API_SECRET).callback(RTFServletConfig.GOOGLE_USER_AUTH_CALLBACK_URL)
        .scope(RTFServletConfig.GOOGLE_OAUTH_REQ_SCOPE).build();

    // Obtain the Request Token
    Token requestToken = service.getRequestToken();
    req.getSession().setAttribute(GOOGLE_REDIRECT_REQUEST_TOKEN, requestToken.getToken());
    req.getSession().setAttribute(GOOGLE_REDIRECT_REQUEST_TOKEN_SECRET, requestToken.getSecret());

    String redirectUrl = GOOGLE_REDIRECT_URL_BASE + "?" + GOOGLE_PARAM_OAUTH_TOKEN + "=" + requestToken.getToken();
    return redirectUrl;
  }

  private void actionUserFacebookScribeAuthStart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Attempt to implement Facebook with scribe
    OAuthService service = new ServiceBuilder().provider(FacebookApi.class).apiKey(RTFServletConfig.FACEBOOK_ID)
        .apiSecret(RTFServletConfig.FACEBOOK_SECRET).callback(RTFServletConfig.FACEBOOK_USER_AUTH_SCRIBE_CALLBACK_URL)
        .build();
    // Scanner in = new Scanner(System.in);

    // Obtain the Authorization URL
    System.out.println("Fetching the Authorization URL...");
    String authorizationUrl = service.getAuthorizationUrl(null); // Dont need request token here
    log.info("Facebook User Auth URL: " + authorizationUrl);

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

  private void writeAccountConflictMessage(Document doc, RTFAccountException e) {
    long originalRTFAcctId = e.getOriginalOwner().getAppEngineKeyLong();
    long contestorRTFAcctId = e.getNewClaimant().getAppEngineKeyLong();
    String apName = e.getContestedAPAccount().getProperty(AuthProviderAccount.AUTH_PROVIDER_NAME);
    String apDetails = e.getContestedAPAccount().getDescription();

    doc.body().appendElement("h2").appendText("Account Ownership Conflict").appendElement("br");
    doc.body()
        .appendText(
            "The " + apName + " account you are trying to add to your RateThisFest account ID " + contestorRTFAcctId
                + " is already being used by another RateThisFest user ID " + originalRTFAcctId + ".")
        .appendElement("br").appendElement("br");
    doc.body().appendElement("a").attr("href", RTFServletConfig.PATH_HOME).appendText("Back To Home Page")
        .appendElement("br");

  }

  private void actionCallbackGoogleAuthScribe(HttpServletRequest req, HttpServletResponse resp, Document doc)
      throws JsonProcessingException, IOException {
    String paramToken = req.getParameter(GOOGLE_PARAM_OAUTH_TOKEN);
    String paramVerifier = req.getParameter(GOOGLE_PARAM_OAUTH_VERIFIER);

    Verifier verifier = new Verifier(paramVerifier);

    OAuthRequest request = new OAuthRequest(Verb.GET, GOOGLE_PROTECTED_URL_USERINFO);

    OAuthService service = new ServiceBuilder().provider(GoogleApi.class).apiKey(RTFServletConfig.GOOGLE_API_KEY)
        .apiSecret(RTFServletConfig.GOOGLE_API_SECRET).callback(RTFServletConfig.GOOGLE_USER_AUTH_CALLBACK_URL)
        .scope(RTFServletConfig.GOOGLE_OAUTH_REQ_SCOPE).build();

    String reqToken = (String) req.getSession().getAttribute(GOOGLE_REDIRECT_REQUEST_TOKEN);
    String reqTokenSecret = (String) req.getSession().getAttribute(GOOGLE_REDIRECT_REQUEST_TOKEN_SECRET);
    Token requestToken = new Token(reqToken, reqTokenSecret);
    Token accessToken = service.getAccessToken(requestToken, verifier);
    service.signRequest(accessToken, request);
    // request.addHeader("GData-Version", "3.0"); //TODO is this needed? Not sure. Seems like it's not.
    Response response = request.send();
    log.info(response.getCode() + "\r\n" + response.getBody());

    LoginType loginType = LoginType.GOOGLE;
    AuthProviderAccount newProviderAcct = new AuthProviderAccount(response.getBody(), loginType);
    try {
      LoginManager.authProviderLoginAccomplished(req.getSession(), loginType, newProviderAcct);
      // Can log something here with this, descriptive info should go here once we stop dumping APAccount to log
      String idStr = newProviderAcct.getProperty(AuthProviderAccount.AUTH_PROVIDER_ID);
      String name = newProviderAcct.getProperty(AuthProviderAccount.LOGIN_PERSON_NAME);
      String email = newProviderAcct.getProperty(AuthProviderAccount.LOGIN_EMAIL);
      String picUrl = newProviderAcct.getProperty(AuthProviderAccount.LOGIN_PICTURE_URL);
      log.info("Login Success with:" + loginType.getName());
      resp.sendRedirect(RTFServletConfig.PATH_HOME);
    } catch (RTFAccountException e) {
      e.printStackTrace();
      writeAccountConflictMessage(doc, e);
    }

  }

  private void actionCallbackFacebookAuthScribe(HttpServletRequest req, HttpServletResponse resp, Document doc)
      throws IOException, JsonProcessingException {

    // Facebook user auth has returned
    OAuthService service = new ServiceBuilder().provider(FacebookApi.class).apiKey(RTFServletConfig.FACEBOOK_ID)
        .apiSecret(RTFServletConfig.FACEBOOK_SECRET).callback(RTFServletConfig.FACEBOOK_USER_AUTH_SCRIBE_CALLBACK_URL)
        .build();
    // OAuthService service = (OAuthService)req.getSession().getAttribute("scribeservice");
    // String authorizationUrl = service.getAuthorizationUrl(null);

    Verifier verifier = new Verifier(req.getParameter("code"));
    // Trade the Request Token and Verfier for the Access Token
    System.out.println("Trading the Request Token for an Access Token...");
    Token nullToken = null;
    Token accessToken = service.getAccessToken(nullToken, verifier);

    // Now let's go and ask for a protected resource!
    OAuthRequest request = new OAuthRequest(Verb.GET, FACEBOOK_PROTECTED_URL_USERINFO);
    service.signRequest(accessToken, request);
    Response response = request.send();
    int responseCode = response.getCode();
    log.info("Response Body: " + response.getBody());

    LoginType loginType = LoginType.FACEBOOK;
    AuthProviderAccount newProviderAcct = new AuthProviderAccount(response.getBody(), loginType);
    try {
      LoginManager.authProviderLoginAccomplished(req.getSession(), loginType, newProviderAcct);
      // Can log something here with this, descriptive info should go here once we stop dumping APAccount to log
      String id = newProviderAcct.getProperty(AuthProviderAccount.AUTH_PROVIDER_ID);
      String name = newProviderAcct.getProperty(AuthProviderAccount.LOGIN_PERSON_NAME);
      String email = newProviderAcct.getProperty(AuthProviderAccount.LOGIN_EMAIL);

      // doc.body().appendText("Got Facebook ID: " + id + " name: " + name + " email: " + email);
      resp.sendRedirect(RTFServletConfig.PATH_HOME);
    } catch (RTFAccountException e) {
      e.printStackTrace();
      writeAccountConflictMessage(doc, e);
    }

  }

  private void actionCallbackTwitterAuth(HttpServletRequest req, HttpServletResponse resp, Document doc)
      throws IOException, JsonProcessingException {
    // User approved/cancelled twitter authorization of RateThisFest
    Token token = new Token(req.getParameter(GOOGLE_PARAM_OAUTH_TOKEN), req.getParameter(GOOGLE_PARAM_OAUTH_VERIFIER));
    Verifier verifier = new Verifier(req.getParameter(GOOGLE_PARAM_OAUTH_VERIFIER));

    OAuthService service = new ServiceBuilder().provider(TwitterApi.class).apiKey(RTFServletConfig.TWITTER_KEY)
        .apiSecret(RTFServletConfig.TWITTER_SECRET).callback(RTFServletConfig.TWITTER_REDIRECT_URL).build();
    Token accessToken = service.getAccessToken(token, verifier);

    OAuthRequest request = new OAuthRequest(Verb.GET, TWITTER_PROTECTED_URL_USERINFO);
    service.signRequest(accessToken, request); // the access token from step 4
    Response response = request.send();
    log.info(response.getBody());

    LoginType loginType = LoginType.TWITTER;
    AuthProviderAccount newProviderAcct = new AuthProviderAccount(response.getBody(), loginType);
    try {
      LoginManager.authProviderLoginAccomplished(req.getSession(), loginType, newProviderAcct);
      // Can log something here with this, descriptive info should go here once we stop dumping APAccount to log
      String id = newProviderAcct.getProperty(AuthProviderAccount.AUTH_PROVIDER_ID);
      String name = newProviderAcct.getProperty(AuthProviderAccount.LOGIN_PERSON_NAME);
      String twitterName = newProviderAcct.getProperty(AuthProviderAccount.LOGIN_SCREEN_NAME);
      resp.sendRedirect(RTFServletConfig.PATH_HOME);
    } catch (RTFAccountException e) {
      e.printStackTrace();
      writeAccountConflictMessage(doc, e);
    }

  }

  private void writeStartOverLink(Document doc) {
    doc.body().appendElement("a").attr("href", RTFServletConfig.PATH_HOME).appendText("Home Page").appendElement("br");
    doc.body().appendElement("a")
        .attr("href", RTFServletConfig.PATH_HOME + "?" + PARAM_NAME_RTFACTION + "=" + ACTION_LOGOUT)
        .appendText("Wipe Login Data").appendElement("br");
    doc.body().appendElement("br").appendElement("a")
        .attr("href", RTFServletConfig.PATH_HOME + "?" + PARAM_NAME_RTFACTION + "=" + ACTION_DESTROY_ACCOUNT)
        .attr("align", "right").appendText("DESTROY MY RateThisFest ACCOUNT").appendElement("br");
  }

  private void writeForm(Document doc) {
    // <form action="/sign" method="post">
    Element formElement = doc.body().appendElement("form").attr("action", RTFServletConfig.PATH_HOME)
        .attr("method", "post");

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
        .attr("src", RTFServletConfig.IMAGE_URL_SIGNIN_GOOGLE).attr("border", "0").appendElement("br");
  }

  private void writeFacebookLoginButton(Document doc) {
    String urlToUseWhenYouClickOnTheFacebookButton = RTFServletConfig.FACEBOOK_USER_AUTH_SCRIBE_START_URL;

    doc.body().appendElement("A").attr("href", urlToUseWhenYouClickOnTheFacebookButton).appendElement("img")
        .attr("src", RTFServletConfig.IMAGE_URL_SIGNIN_FACEBOOK).attr("border", "0").appendElement("br");

  }

  private void writeTwitterLoginButton(Document doc) {
    String urlToUse = RTFServletConfig.PATH_HOME + "?" + PARAM_NAME_RTFACTION + "=" + ACTION_TWITTER_AUTH;
    doc.body().appendElement("A").attr("href", urlToUse).appendElement("img")
        .attr("src", RTFServletConfig.IMAGE_URL_SIGNIN_TWITTER).attr("border", "0").appendElement("br");
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

    RTFAccount currentLogin = LoginManager.getCurrentLogin(session);
    if (currentLogin != null) {
      long rtfAccountId = currentLogin.getAppEngineKeyLong();
      String personName = currentLogin.getProperty(RTFAccount.PROPERTY_PERSON_NAME);
      doc.body().appendText("Logged in as RateThisFest Account#: " + rtfAccountId + " Name: " + personName)
          .appendElement("br");

      Collection<AuthProviderAccount> providerAccounts = currentLogin.getAPAccounts();
      for (AuthProviderAccount apAccount : providerAccounts) {
        doc.body()
            .appendText(
                apAccount.getProperty(AuthProviderAccount.AUTH_PROVIDER_NAME) + ": " + apAccount.getDescription())
            .appendElement("br");
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
