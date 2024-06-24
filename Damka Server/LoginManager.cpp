#include <iostream>
#include <cstdlib>
#include <ctime>
#include "LoginManager.h"

#include "Constants.h"

LoginManager::LoginManager(SqliteDataBase* db):
	db(db)
{
}

LoginManager::~LoginManager()
{
}


/*
functions to check user parameters,
check patterns, avalibility and strognes.
doesn't return anything,
throw LoginExeption if something is wrong
*/
void LoginManager::checkNewUsername(const std::string username)
{
	if (!(username.size() >= 4 && username.size() <= 12))
	{
		throw LoginException("username length must be between 4 - 12", JsonKeys::USERNAME);
	}

	if (!regex_match(username, std::regex("^[a-zA-Z0-9_-]+$")))
	{
		throw LoginException("username should have only letters and number seperated with '_', '-'", JsonKeys::USERNAME);
	}
}

void LoginManager::checkNewPassword(const std::string password)
{
	if (!regex_match(password, std::regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*^#?&])[A-Za-z\\d@$!%*^#?&]{8,}$")))
	{
		throw LoginException("The password should be 8 characters long, contain at least one uppercase letter, lowercase letter, number and special character", JsonKeys::PASSWORD);
	}
}

void LoginManager::checkNewMail(const std::string mail)
{
	if (!regex_match(mail, std::regex("^[\\w\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")))
	{
		throw LoginException("The mail pattern is invalid", JsonKeys::MAIL);
	}

	if (db->doesMailExist(mail))
	{
		throw LoginException("Mail already in use", JsonKeys::MAIL);
	}
}

void LoginManager::checkNewBirthdate(const std::string birthdate)
{
	if (!regex_match(birthdate, std::regex("^(0[1-9]|[1-2][0-9]|3[0-1])[./](0[1-9]|1[0-2])[./](\\d{4})$")))
	{
		throw LoginException("Invalid birth date", JsonKeys::BIRTHDATE);
	}
}


/*
Generate a 8 lenght string that 
consists of capital letters and numbers
*/
std::string LoginManager::generateNewUserID() 
{
	const std::string charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	const int length = 8;
	std::string userID;

	std::srand(static_cast<unsigned int>(std::time(nullptr)));

	for (int i = 0; i < length; ++i)
	{
		int index = std::rand() % charset.length();
		userID += charset[index];
	}

	return userID;
}

/*
add new user to the DB
use regex to check parameters
username: length 2 - 12
password: at least 8len, A, a, 1, !
mail: <something>@<something>.<something>
birthday: DD/MM/YYYY
*/
std::shared_ptr<User> LoginManager::signup(std::string username, std::string password, std::string mail, std::string birthdate)
{
	std::string userID = "";

	checkNewUsername(username);
	checkNewPassword(password);
	checkNewMail(mail);
	checkNewBirthdate(birthdate);

	// create userID
	do
	{
		userID = generateNewUserID();
	} while (db->doesUserIDExist(userID));

	if (!db->addNewUser(userID, username, mail, password, birthdate))
	{
		throw LoginException("Db failure", "DB");
	}
	std::shared_ptr<User> user = std::make_shared<User>(username, userID, STARTUP_RATING);

	if (!db->changeOnline(userID, ONLINE))
	{
		throw LoginException("Db failure", "DB");
	}

	std::lock_guard<std::mutex> lock(userVectorMutex);
	
	this->logged_users.push_back(user);
	return user;
}


json LoginManager::login(std::string mail, std::string password)
{
	if (!db->doesMailExist(mail))
	{
		throw LoginException("Mail doesn't exist :(", JsonKeys::MAIL);
	}

	if (!db->doesPasswordMatch(mail, password))
	{
		throw LoginException("Password invalid :(", JsonKeys::PASSWORD);
	}

	UserStats userLogin = db->getUserDetails(mail);

	std::lock_guard<std::mutex> lock(userVectorMutex);
	for (auto& user : this->logged_users)
	{
		if (user->getUserID() == userLogin.userID)
		{
			throw LoginException("User is already logged in", JsonKeys::USERID);
		}
	}

	if (!db->changeOnline(userLogin.userID, ONLINE))
	{
		throw LoginException("Db failure", "DB");
	}
	std::shared_ptr<User> newUser = std::make_shared<User>(userLogin.username, userLogin.userID, userLogin.rating);
	this->logged_users.push_back(newUser);

	json loginDetails;
	loginDetails[JsonKeys::USERID] = userLogin.userID;
	loginDetails[JsonKeys::USERNAME] = userLogin.username;
	loginDetails[JsonKeys::RATING] = userLogin.rating;
	loginDetails[JsonKeys::PRIVACY] = userLogin.privacy;
	loginDetails[JsonKeys::MAIL] = mail;

	return loginDetails;
}


void LoginManager::logout(std::string userID)
{
	std::lock_guard<std::mutex> lock(userVectorMutex);
	auto it = std::find_if(this->logged_users.begin(), this->logged_users.end(), [userID](const std::shared_ptr<User>& ptr) {
		return ptr->getUserID() == userID;
		});
	if (it != this->logged_users.end()) {
		this->db->changeOnline(userID, OFFLINE);
		this->logged_users.erase(it);
	}
}

void LoginManager::printUsers() const
{
	if (this->logged_users.empty())
	{
		std::cout << "no users connected :(" << std::endl;
	}
	else
	{
		for (auto& user : this->logged_users)
		{
			std::cout << user->getUsername() << ", ";
		}
		std::cout << std::endl;
	}
}

std::shared_ptr<User> LoginManager::findUser(std::string userID)
{
	std::lock_guard<std::mutex> lock(userVectorMutex);
	for (auto& it : this->logged_users)
	{
		if (it->getUserID() == userID)
		{
			return it;
		}
	}
	throw std::runtime_error("user not found");
}

void LoginManager::updateUserData(std::string userID, std::string username, std::string mail, int privacy)
{
	if (!username.empty())
	{
		checkNewUsername(username);
	}
	if (!mail.empty())
	{
		checkNewMail(mail);
	}

	if (!(privacy == -1 || privacy == 0 || privacy == 1))
	{
		throw LoginException("Privacy code doesn't exisy", "Privacy");
	}

	if (!db->changeUserData(userID, username, mail, privacy))
	{
		throw LoginException("Db failure", "DB");
	}
}