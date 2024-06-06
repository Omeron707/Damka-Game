#pragma once
#include <iostream>
#include <string.h>
#include <io.h>
#include <sstream>
#include <vector>
#include <map>
#include <windows.h>
#include <mutex>
#include <cstdlib>
#include <ctime>
#include <list>
#include "sqlite3.h"
#include "Constants.h"


struct UserStats
{
	std::string username;
	std::string userID;
	OnlineStates online;
	bool privacy;
	double rating;
	int friendsCount;
	double bestRating;
	int gamesPlayed;
	int wins;
	int losses;
	int draws;
};


struct Notification
{
	int id;
	bool saveInDB;
	NotificationType type;
	std::string srcID;
	std::string dstID;
	std::string time;
	std::string content;
};

class SqliteDataBase
{
public:
	SqliteDataBase();
	virtual ~SqliteDataBase();
	bool open();
	bool close();

	// login related
	bool doesMailExist(const std::string mail);
	bool doesUserIDExist(const std::string id);
	bool addNewUser(const std::string userID, const std::string username, const std::string mail, const std::string password, const std::string birthdate);
	bool doesPasswordMatch(const std::string mail, const std::string password);
	UserStats getUserDetails(const std::string mail);
	bool changeOnline(const std::string userID, const OnlineStates state);
	OnlineStates getOnline(const std::string userID);
	std::string getSalt(const std::string mail);

	// friends related
	bool refreshFriendsAmount(const std::string userID);
	bool addNewFriendRequest(const std::string userID, const std::string otherUserID);
	std::string getRequestUserId(const std::string userID, const std::string otherUserID);
	bool reAddFriend(const std::string userID, const std::string otherUserID);
	bool acceptFriend(const std::string userID, const std::string otherUserID);
	bool denyFriend(const std::string userID, const std::string otherUserID);
	bool doesUserPrivate(const std::string userID);
	std::string getRelashionshipTime(const std::string userID, const std::string otherUserID);
	bool RemoveFriends(const std::string userID, const std::string friendID);
	char getUsersStatus(const std::string userID, const std::string otherUserID);
	std::vector<UserStats> getFriendList(const std::string userID);
	std::vector<UserStats> getRequestList(const std::string userID);

	// notifications related
	int saveNotification(Notification notification);
	std::vector<Notification> loadNotifications(std::string userID);
	bool removeNotification(int id, std::string userID);

	// profile + statistics related
	UserStats getProfile(std::string loggetUserID, std::string userID);
	bool changeUserData(std::string userID, std::string username, std::string mail, int privacy);
	std::vector<UserStats> getLeaderboard(int topAmount);

	bool updateUserStats(std::string userID, double newRating, bool win, bool lose, bool draw);
	bool addNewRating(std::string userID, double newRating);
	std::map<std::string, double> getRatingHistory(std::string userID);

private:
	bool runQuery(const char* x);
	bool runQueryWithAns(const char* x, int (*ptr)(void*, int, char**, char**), void* var);

	sqlite3* db;
	std::mutex databaseMutex;
};
