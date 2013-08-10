package guestbook.login;

import guestbook.FacebookReturnServlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;



import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.KeyRange;

public class RTFAccount implements Serializable {
  private static final long serialVersionUID = 1L;
  public static final String LOGIN_HTTPSESSION_ATTRIBUTE = "LOGIN_HTTPSESSION_ATTRIBUTE";
  public static final String DATASTORE_KIND = "RTFAccount";
  public static final String DATASTORE_ANCESTOR_ID = "AccountAncestor";

  private long _id;
  private HashMap<String, AuthProviderAccount> _accounts;
  private static Key _accountAncestorKey;
  
  private static final Logger log = Logger.getLogger(new Object() { }.getClass().getEnclosingClass().getName());

  public static RTFAccount fromID(String RTFAccountID) {
    if (RTFAccountID == null) {
      return null;
    }
    Key RTFAccountKey = KeyFactory.createKey(getAncestorKey(), DATASTORE_KIND, Long.valueOf(RTFAccountID));
    return new RTFAccount(RTFAccountKey);
  }

  private RTFAccount(Key useKey) {
    _id = useKey.getId();
    //TODO must populate accounts here
    _accounts = AuthProviderAccount.loadAPAccountsByParentID(_id+"");
  }

  public static Key getAncestorKey() {
    if (_accountAncestorKey == null) {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Entity rtfAccountAncestor = new Entity(RTFConstants.DATASTORE_KIND_ANCESTOR, DATASTORE_ANCESTOR_ID);
      rtfAccountAncestor.setProperty(RTFConstants.ANCESTOR_TYPE, DATASTORE_ANCESTOR_ID);
      _accountAncestorKey = datastore.put(rtfAccountAncestor);
      log.info("Ancestor key of all RTFAccount objects determined to be: "+ _accountAncestorKey);
    }
    
    return _accountAncestorKey;
  }

  public static RTFAccount createNewRTFAccount() {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key accountAncestorKey = getAncestorKey();
    log.info("RTFAccount.createRTFAccount: Ancestor Key=" + accountAncestorKey.getName() + " "
        + accountAncestorKey.toString());

    KeyRange allocatedIds = datastore.allocateIds(accountAncestorKey, DATASTORE_KIND, 1); // Ask for a new ID now
    Key allocatedId = allocatedIds.getStart();

    // All RTFAccounts will be under the same ancestor subject to strict consistency
    Entity rtfAccount = new Entity(DATASTORE_KIND, allocatedId.getId(), accountAncestorKey); // Specify ID and parent
    Key accountKey = datastore.put(rtfAccount);

    return new RTFAccount(allocatedId);
  }

  public static Key getKey(String RTFAccountID) {
    Long idToLookup = Long.valueOf(RTFAccountID);
    return KeyFactory.createKey(getAncestorKey(), DATASTORE_KIND, idToLookup);
  }

  public long getRTFAccountId() {
    return _id;
  }

  public void addAPAccount(AuthProviderAccount apAccount) {
    if (_accounts.containsValue(apAccount)) {
      throw new RuntimeException("Attempt to add an apAccount already owned by this RTFAccount");
    }

    _accounts.put(apAccount.getProperty(AuthProviderAccount.AUTH_PROVIDER_NAME), apAccount);
    apAccount.setProperty(AuthProviderAccount.RTFACCOUNT_OWNER_KEY, _id + "");
  }

  public void updateAPAccount(AuthProviderAccount newAPAccountObj) {
    String authProviderName = newAPAccountObj.getProperty(AuthProviderAccount.AUTH_PROVIDER_NAME);
    
    AuthProviderAccount oldAPAccountObj = _accounts.get(authProviderName);
    oldAPAccountObj.copyDataTo(newAPAccountObj);
    oldAPAccountObj.setProperty(AuthProviderAccount.RTFACCOUNT_OWNER_KEY, _id+"");  //Forces save to db
    
    _accounts.put(authProviderName, newAPAccountObj);
  }

  public Collection<AuthProviderAccount> getAPAccounts() {
    return _accounts.values();
  }

  public boolean isLoggedInAPType(LoginType loginType) {
    Collection<AuthProviderAccount> providerAccounts = getAPAccounts();
    
    for (AuthProviderAccount apAccount : providerAccounts) {
      if (loginType.getName().equals(apAccount.getProperty(AuthProviderAccount.AUTH_PROVIDER_NAME))) {
        return true;
      }
    }
    return false;
  }


}
