package com.bharath.service;

import java.util.List;

import com.bharath.binding.ActivateUser;
import com.bharath.binding.LoginCredentials;
import com.bharath.binding.RecoverPassword;
import com.bharath.binding.UserAccount;

public interface IUserMgmtService {
public String regiserUser(UserAccount user)throws Exception;
public String activateUserAccount (ActivateUser user);
public String login (LoginCredentials credentials);
public List<UserAccount> listUsers();
public UserAccount showUserByUserId(Integer id);
public UserAccount showUserByEmailAndName(String email, String name);
public String updateUser(UserAccount user);
public String deleteUserById(Integer id);
public String changeUserStatus (Integer id, String status);
public String recoverPassword(RecoverPassword recover) throws Exception;
}
