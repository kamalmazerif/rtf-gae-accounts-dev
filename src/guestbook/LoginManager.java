package guestbook;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class LoginManager {

  public static void addLoginType(HttpSession session, LoginType loginType, HashMap<String, String> loginSettings) {
    HashMap<String, HashMap<String, String>> loginHashSet = getLoginHashSet(session);
    loginHashSet.put(loginType.getName(), loginSettings);
    session.setAttribute(RTFConstants.LOGIN_HASHMAP_OBJ, loginHashSet); // Must save to GAE after object modification
  }

  private static HashMap<String, HashMap<String, String>> getLoginHashSet(HttpSession session) {
    HashMap<String, HashMap<String, String>> loginHash = (HashMap<String, HashMap<String, String>>) session
        .getAttribute(RTFConstants.LOGIN_HASHMAP_OBJ);
    if (loginHash == null) {
      loginHash = new HashMap<String, HashMap<String, String>>();
      session.setAttribute(RTFConstants.LOGIN_HASHMAP_OBJ, loginHash);
    }
    return loginHash;
  }

  public static boolean isLoggedIn(HttpSession session) {
    HashMap<String, HashMap<String, String>> loginHashSet = getLoginHashSet(session);
    return !loginHashSet.isEmpty();
  }

  public static HashMap<String, String> getLoginCommonNameDescriptions(HttpSession session) {
    HashMap<String, String> returnMap = new HashMap<String, String>();

    HashMap<String, HashMap<String, String>> loginHashSet = getLoginHashSet(session);
    Set<String> loginTypeNames = loginHashSet.keySet();
    for (String loginTypeName : loginTypeNames) {
      StringBuilder sBuilder = new StringBuilder();
      HashMap<String, String> loginProperties = loginHashSet.get(loginTypeName);
      if (loginTypeName.equals(LoginType.GOOGLE.getName())) {
        sBuilder.append(loginProperties.get(RTFConstants.LOGIN_PERSON_NAME)).append(" (")
            .append(loginProperties.get(RTFConstants.LOGIN_EMAIL)).append(")");
      } else if (loginTypeName.equals(LoginType.FACEBOOK.getName())) {
        sBuilder.append(loginProperties.get(RTFConstants.LOGIN_PERSON_NAME)).append(" (")
            .append(loginProperties.get(RTFConstants.LOGIN_EMAIL)).append(")");
      } else if (loginTypeName.equals(LoginType.TWITTER.getName())) {
        sBuilder.append(loginProperties.get(RTFConstants.LOGIN_PERSON_NAME)).append(" (@")
            .append(loginProperties.get(RTFConstants.LOGIN_SCREEN_NAME)).append(")");
      }

      returnMap.put(loginTypeName, sBuilder.toString());
    }

    return returnMap;
  }

  // Removes every session attribute starting with the login prefix
  public static void logOutUser(HttpSession session) {
    Enumeration attributeNames = session.getAttributeNames();

    String attributeName;

    ArrayList<String> attributesToRemove = new ArrayList<String>();
    // Cannot remove attributes while iterating over attributes, ConcurrentModificationException will throw
    while (attributeNames.hasMoreElements()) {
      attributeName = (String) attributeNames.nextElement();
      if (attributeName.startsWith(RTFConstants.LOGIN_SETTING_PREFIX)) {
        attributesToRemove.add(attributeName);

      }
    }

    //Done iterating, now remove some attributes
    for (String attributeNameToRemove : attributesToRemove) {
      session.removeAttribute(attributeNameToRemove);
    }
  }

}
