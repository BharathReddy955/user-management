package com.bharath.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import com.bharath.binding.ActivateUser;
import com.bharath.binding.LoginCredentials;
import com.bharath.binding.RecoverPassword;
import com.bharath.binding.UserAccount;
import com.bharath.entity.UserMaster;
import com.bharath.repository.IUserMasterRepository;
import com.bharath.utils.EmailUtils;

@Service
public class UserMgmtServiceImpl implements IUserMgmtService {

	@Autowired
	private IUserMasterRepository userMasterRepo;

	@Autowired
	private EmailUtils emailUtils;
	@Autowired
	private Environment env;

	@Override
	public String regiserUser(UserAccount user) throws Exception {
//convert UserAccount obj data to UserMaster obj(Entity obj) data
		UserMaster master = new UserMaster();
		BeanUtils.copyProperties(user, master);
//set random string of 6 chars as password
		String tempPwd = generateRandomPassword(6);
		master.setPassword(tempPwd);
		master.setActive_sw("InActive");
//save object
		UserMaster savedMaster = userMasterRepo.save(master);
//perform  send the mail operation
		String subject = "User Registration Success";
		String body = readEmailMessageBody(env.getProperty("mailbody.registeruser.location"), subject, tempPwd);
		emailUtils.sendEmailMessage(user.getEmail(), subject, body);
//return message
		return savedMaster != null ? "User is registered with Id value::" + savedMaster.getUserld()
				: " Problem is user registration";
	}

	@Override
	public String activateUserAccount(ActivateUser user) {
//use findBy method
		UserMaster entity = userMasterRepo.findByEmailAndPassword(user.getEmail(), user.getTempPassword());
		if (entity == null) {
			return "User is not found for activation";
		} else {
//set the password
			entity.setPassword(user.getConfirmPassword());
// change the user account status to active
			entity.setActive_sw("Active");
//update the obj
			UserMaster updatedEntity = userMasterRepo.save(entity);
			return "User is activated with new Password";
		}
	}

	@Override
	public String login(LoginCredentials credentials) {
//convert LoginCredentials obj to USerMaster obj (Entity obj)
		UserMaster master = new UserMaster();
		BeanUtils.copyProperties(credentials, master);
//prepare Example obj
		Example<UserMaster> example = Example.of(master);
		List<UserMaster> listEntities = userMasterRepo.findAll(example);
		if (listEntities.size() == 0) {
			return " Invalid Credentails";
		} else {
//get entity obj
			UserMaster entity = listEntities.get(0);
			if (entity.getActive_sw().equalsIgnoreCase("Active")) {
				return " Valid credentials and Login successful";
			} else {
				return " User Account is not active";
			}
		}
	}

	@Override
	public List<UserAccount> listUsers() {
// Load all entities and convert to UserAccount obj
		List<UserAccount> listUsers = userMasterRepo.findAll().stream().map(entity -> {
			UserAccount user = new UserAccount();
			BeanUtils.copyProperties(entity, user);
			return user;
		}).toList();
		return listUsers;

	}

	@Override
	public UserAccount showUserByUserId(Integer id) {
// Load the user by user id
		Optional<UserMaster> opt = userMasterRepo.findById(id);
		UserAccount account = null;
		if (opt.isPresent()) {
			account = new UserAccount();
			BeanUtils.copyProperties(opt.get(), account);
		}
		return account;
	}

	@Override
	public UserAccount showUserByEmailAndName(String email, String name) {
//use the custom findBy(-) method
		UserMaster master = userMasterRepo.findByNameAndEmail(name, email);
		UserAccount account = null;
		if (master != null) {
			account = new UserAccount();
			BeanUtils.copyProperties(master, account);
		}
		return account;
	}

	@Override
	public String updateUser(UserAccount user) {
//use the custom findBy(-) method
		Optional<UserMaster> opt = userMasterRepo.findById(user.getUserld());
		if (opt.isPresent()) {
//get Entity objet
			UserMaster master = opt.get();
			BeanUtils.copyProperties(user, master);
			userMasterRepo.save(master);
			return "User Details are updated";
		} else {
			return "User not found for updation";
		}
	}// method

	@Override
	public String deleteUserById(Integer id) {
// Load the obj
		Optional<UserMaster> opt = userMasterRepo.findById(id);
		if (opt.isPresent()) {
			userMasterRepo.deleteById(id);
			return "User is deleted";
		}
		return "user is not found for deletion";
	}

	@Override
	public String changeUserStatus(Integer id, String status) {
// Load the obj
		Optional<UserMaster> opt = userMasterRepo.findById(id);
		if (opt.isPresent()) {
//get Entity obj
			UserMaster master = opt.get();
//change the status
			master.setActive_sw(status);
//update the obj
			userMasterRepo.save(master);
			return "User status changed";
		}
		return "user not found for changing the status";
	}

	@Override
	public String recoverPassword(RecoverPassword recover) throws Exception {
//get UserMaster [Entity obj) by name, email
		UserMaster master = userMasterRepo.findByNameAndEmail(recover.getName(), recover.getEmail());
		if (master != null) {
			String pwd = master.getPassword();
//sent the recovered to email account
			String subject = "mail for password recovery";
			String mailBody = readEmailMessageBody(env.getProperty("mailBody.recoverpwd.location"), recover.getName(),
					pwd);
			emailUtils.sendEmailMessage(recover.getEmail(), subject, mailBody);
			return pwd;
		}
		return "User and email is not found";
	}

//helper methods for same class
	private String generateRandomPassword(int length) {
//a list of characters to choose from in form of a string
		String AlphaNumericStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz0123456789";
//creating a StringBuffer size of AlphaNumericStr
		StringBuilder randomWord = new StringBuilder(length);
		int i;
		for (i = 0; i < length; i++) {
//generating a random number using math.random() (gives psuedo random number 0.0 to 1.0)
			int ch = (int) (AlphaNumericStr.length() * Math.random());
//adding Random character one by one at the end of randonword
			randomWord.append(AlphaNumericStr.charAt(ch));
		}
		return randomWord.toString();
	}

	private String readEmailMessageBody(String fileName, String fullName, String pwd) throws Exception {
		String mailBody = null;
		String url = "";
		try (FileReader reader = new FileReader(fileName); BufferedReader br = new BufferedReader(reader)) {
//read file content to StringBuffer object line by line
			StringBuffer buffer = new StringBuffer();
			String line = null;
			do {
				line = br.readLine();
				buffer.append(line);
			} while (line != null);
			mailBody = buffer.toString();
			mailBody = mailBody.replace("{FULL-NAME}", fullName);
			mailBody = mailBody.replace("{PWD}", pwd);
			mailBody = mailBody.replace("{URL}", url);
		} // try
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return mailBody;
	}

}
