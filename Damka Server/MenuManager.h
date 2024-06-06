#pragma once
#include "SqliteDataBase.h"
#include "JsonHelper.h"

class MenuManager
{
public:
	MenuManager(SqliteDataBase* db);
	~MenuManager();

	void addFriend(std::string userID, std::string otherUserID);
	void acceptFriend(std::string userID, std::string otherUserID);
	void denyFriend(std::string userID, std::string otherUserID);
	void removeFriend(std::string userID, std::string otherUserID);
	json getFriendsList(std::string userID);
	json getFriendsRequestsList(std::string userID);
	json getUserProfile(std::string loggedUserID, std::string userID);
	json getOfflineNotifications(std::string userID);
	void removeNotifications(int id, std::string userID);
	json getLeaderboardList();

private:
	SqliteDataBase* db;
};

class MenuException : public std::exception
{
public:
	MenuException(const std::string& message) : m_message(message) {}

	const char* what() const noexcept override
	{
		return m_message.c_str();
	}

private:
	std::string m_message;
};