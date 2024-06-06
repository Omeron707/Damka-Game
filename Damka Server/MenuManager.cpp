#include "MenuManager.h"
#include "Constants.h"
#include <ctime>
#include <chrono>
#include <iomanip>

// Function to parse time string into std::tm
std::tm parseTimeString(const std::string& timeString) {
	std::tm tm = {};
	std::istringstream ss(timeString);
	ss >> std::get_time(&tm, "%Y-%m-%d %H:%M:%S");
	return tm;
}

// Function to check if a time is before 5 minutes from now
bool isDelayPassed(const std::string& timeString) {
	// Get current time
	std::time_t currentTime = std::time(nullptr);
	std::tm currentTm = {};
	localtime_s(&currentTm, &currentTime);

	// Parse time string
	std::tm time_tm = parseTimeString(timeString);

	// Convert to std::time_t
	time_tm.tm_isdst = currentTm.tm_isdst;
	std::time_t target = std::mktime(&time_tm);
	std::time_t current = std::mktime(&currentTm);

	// Calculate the difference in seconds
	double difference = std::difftime(current, target);

	return difference > DENY_DELAY_TIME_SECOND;
}


MenuManager::MenuManager(SqliteDataBase* db):
	db(db)
{
}

MenuManager::~MenuManager()
{
}


void MenuManager::addFriend(std::string userID, std::string otherUserID)
{
	if (userID == otherUserID)
	{
		throw MenuException("Can't add yourself ;)");
	}
	if (!this->db->doesUserIDExist(otherUserID))
	{
		throw MenuException("User Id doesn't exist");
	}
	char status = this->db->getUsersStatus(userID, otherUserID);
	switch (status)
	{
		case NONE:
			if (!this->db->addNewFriendRequest(userID, otherUserID))
			{
				throw MenuException("DB failed");
			}
			break;

		case DENY:
			if (userID == this->db->getRequestUserId(userID, otherUserID))
			{
				if (!isDelayPassed(this->db->getRelashionshipTime(userID, otherUserID)))
				{
					throw MenuException("Wait before requesting again!");
				}
			}
			if (!this->db->reAddFriend(userID, otherUserID))
			{
				throw MenuException("DB failed");
			}
			break;

		case REQUESTED:
			acceptFriend(userID, otherUserID);
			break;

		case ACCEPTED:
			throw MenuException("Already friends");
			break;
	}
}

void MenuManager::acceptFriend(std::string userID, std::string otherUserID)
{
	char status = this->db->getUsersStatus(userID, otherUserID);
	if (status != REQUESTED)
	{
		throw MenuException("There is no friend request");
	}
	std::string requesterId = this->db->getRequestUserId(userID, otherUserID);
	if (userID == requesterId)
	{
		throw MenuException("Already requested");
	}
	if (!this->db->acceptFriend(userID, otherUserID))
	{
		throw MenuException("DB failed");
	}
}

void MenuManager::denyFriend(std::string userID, std::string otherUserID)
{
	if (!this->db->doesUserIDExist(otherUserID))
	{
		throw MenuException("User Id doesn't exist");
	}
	char state = this->db->getUsersStatus(userID, otherUserID);
	if (state == REQUESTED)
	{
		if (!this->db->denyFriend(userID, otherUserID))
		{
			throw MenuException("DB failed");
		}
	}
	else
	{
		throw MenuException("There are no request");
	}
}

void MenuManager::removeFriend(std::string userID, std::string otherUserID)
{
	if (!this->db->doesUserIDExist(otherUserID))
	{
		throw MenuException("User Id doesn't exist");
	}
	if (this->db->getUsersStatus(userID, otherUserID) != ACCEPTED)
	{
		throw MenuException("Not friends");
	}
	if (!this->db->RemoveFriends(userID, otherUserID))
	{
		throw MenuException("DB failed");
	}
}

json MenuManager::getFriendsList(std::string userID)
{
	json jsonData;
	jsonData[JsonKeys::FRIEND_LIST] = json::array();

	std::vector<UserStats> friendList = this->db->getFriendList(userID);
	
	for (const auto& user : friendList) {
		json userJson;
		userJson[JsonKeys::USERNAME] = user.username;
		userJson[JsonKeys::USERID] = user.userID;
		userJson[JsonKeys::RATING] = user.rating;
		userJson[JsonKeys::ONLINE_STATE] = user.online;
		userJson[JsonKeys::PRIVACY] = user.privacy;
		jsonData[JsonKeys::FRIEND_LIST].push_back(userJson);
	}

	return jsonData;
}

json MenuManager::getFriendsRequestsList(std::string userID)
{
	json jsonData;
	jsonData[JsonKeys::REQUEST_LIST] = json::array();

	std::vector<UserStats> requestsFriendList = this->db->getRequestList(userID);

	for (const auto& user : requestsFriendList) {
		json userJson;
		userJson[JsonKeys::USERNAME] = user.username;
		userJson[JsonKeys::USERID] = user.userID;
		userJson[JsonKeys::RATING] = user.rating;
		userJson[JsonKeys::ONLINE_STATE] = user.online;
		userJson[JsonKeys::PRIVACY] = user.privacy;
		jsonData[JsonKeys::REQUEST_LIST].push_back(userJson);
	}

	return jsonData;
}

json MenuManager::getUserProfile(std::string loggedUserID, std::string userID)
{
	UserStats stats = this->db->getProfile(loggedUserID, userID);
	json userProfile;

	userProfile[JsonKeys::USERID] = stats.userID;
	userProfile[JsonKeys::USERNAME] = stats.username;
	userProfile[JsonKeys::RATING] = stats.rating;
	userProfile[JsonKeys::ONLINE_STATE] = stats.online;
	userProfile[JsonKeys::FRIENDS_AMOUNT] = stats.friendsCount;

	userProfile[JsonKeys::PRIVACY] = stats.privacy;
	if (!stats.privacy)
	{
		userProfile[JsonKeys::BEST_RATING] = stats.bestRating;
		userProfile[JsonKeys::GAME_PLAYED] = stats.gamesPlayed;
		userProfile[JsonKeys::WINS] = stats.wins;
		userProfile[JsonKeys::LOSSES] = stats.losses;
		userProfile[JsonKeys::DRAWS] = stats.draws;

		userProfile[JsonKeys::RATINGS_HISTORY] = json::array();
		std::map<std::string, double> ratingsHistory = this->db->getRatingHistory(userID);
		for (const auto& it : ratingsHistory)
		{
			json rating;
			rating[JsonKeys::TIMESTAMP] = it.first;
			rating[JsonKeys::RATING] = it.second;
			userProfile[JsonKeys::RATINGS_HISTORY].push_back(rating);
		}
	}

	return userProfile;
}

json MenuManager::getOfflineNotifications(std::string userID)
{
	json jsonData;
	jsonData[JsonKeys::NOTIFICATION] = json::array();

	std::vector<Notification> notificationsList = this->db->loadNotifications(userID);

	for (const auto& notif : notificationsList) {
		json notifJson;
		notifJson[JsonKeys::NOTIFICATION_ID] = notif.id;
		notifJson[JsonKeys::NOTIFICATION_TYPE] = static_cast<NotificationType>(notif.type);
		notifJson[JsonKeys::NOTIFICATION_SOURCE] = notif.srcID;
		notifJson[JsonKeys::NOTIFICATION_TIME] = notif.time;
		notifJson[JsonKeys::NOTIFICATION_CONTENT] = notif.content;
		jsonData[JsonKeys::NOTIFICATION].push_back(notifJson);
	}

	return jsonData;
}

void MenuManager::removeNotifications(int id, std::string userID)
{
	if (!this->db->removeNotification(id, userID))
	{
		throw MenuException("DB failed");
	}
}

json MenuManager::getLeaderboardList()
{
	json jsonData;
	jsonData[JsonKeys::LEADERBOARD] = json::array();

	std::vector<UserStats> leaderboardList = this->db->getLeaderboard(LEADERBOARD_AMOUNT);

	for (const auto& user : leaderboardList)
	{
		json userJson;
		userJson[JsonKeys::USERNAME] = user.username;
		userJson[JsonKeys::USERID] = user.userID;
		userJson[JsonKeys::RATING] = user.rating;
		userJson[JsonKeys::PRIVACY] = user.privacy;
		jsonData[JsonKeys::LEADERBOARD].push_back(userJson);
	}

	return jsonData;
}