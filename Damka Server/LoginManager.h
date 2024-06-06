#pragma once
#include <iostream>
#include <string.h>
#include <vector>
#include <regex>
#include "User.h"
#include "SqliteDataBase.h"
#include "JsonHelper.h"

class LoginManager
{
	public:
		LoginManager(SqliteDataBase* db);
		~LoginManager();

		std::string generateNewUserID();
		std::shared_ptr<User> signup(std::string username, std::string password, std::string mail, std::string birthdate);
		json login(std::string mail, std::string password);
		void logout(std::string userID);
		std::shared_ptr<User> findUser(std::string UserID);
		void printUsers() const;
		void updateUserData(std::string userID, std::string username, std::string mail, int privacy);

	private:
		// throw LoginExeption if wrong
		void checkNewUsername(const std::string username);
		void checkNewPassword(const std::string password);
		void checkNewMail(const std::string mail);
		void checkNewBirthdate(const std::string birthdate);

		std::vector<std::shared_ptr<User>> logged_users;
		std::mutex userVectorMutex;
		SqliteDataBase* db;
};

class LoginException : public std::exception 
{
public:
	LoginException(const std::string& message, const std::string& field) : m_message(message), m_errorAtField(field) {}

	const char* what() const noexcept override 
	{
		return m_message.c_str();
	}

	const char* where()
	{
		return m_errorAtField.c_str();
	}

private:
	std::string m_message;
	std::string m_errorAtField;
};