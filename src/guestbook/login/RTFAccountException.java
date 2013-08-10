package guestbook.login;

public class RTFAccountException extends Exception {

  private RTFAccount _originalOwner;
  private RTFAccount _newClaimant;
  private AuthProviderAccount _contestedAPAccount;

  public RTFAccountException(RTFAccount currentSessionLogin, RTFAccount ownerOfAddedAPAccount,
      AuthProviderAccount newAPAccountLogin) {
    _originalOwner = ownerOfAddedAPAccount;
    _newClaimant = currentSessionLogin;
    _contestedAPAccount = newAPAccountLogin;
  }
  
  public RTFAccount getOriginalOwner() {
    return _originalOwner;
  }
  
  public RTFAccount getNewClaimant() {
    return _newClaimant;
  }
  
  public AuthProviderAccount getContestedAPAccount() {
    return _contestedAPAccount;
  }
  
}
