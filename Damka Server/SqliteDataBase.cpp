#include <iostream>
#include "SqliteDataBase.h"
#include "Security.h"
#include "Constants.h"



SqliteDataBase::SqliteDataBase():
    db(nullptr)
{
}

SqliteDataBase::~SqliteDataBase()
{
}

bool SqliteDataBase::open()
{
	// try to open the DB
    std::string dbFileName = DATABASE_PATH;
    int file_exist = _access(dbFileName.c_str(), 0);
    int res = sqlite3_open(dbFileName.c_str(), &this->db);
    if (res != SQLITE_OK) 
    {
        this->db = nullptr;
        return false;
    }
  
    // if DB doesn't exist, create it..
    if (file_exist != 0) { 
        std::cout << ">> Create new Database." << std::endl;
        //create USERS table 
        if (!runQuery("CREATE TABLE USERS (\
                        USERID TEXT PRIMARY KEY NOT NULL, \
                        USERNAME TEXT NOT NULL, \
                        MAIL TEXT UNIQUE NOT NULL, \
                        PASSWORD TEXT NOT NULL, \
                        SALT TEXT NOT NULL, \
                        BIRTHDATE TEXT, \
                        CREATED_AT TEXT NOT NULL);"))
        {
            return false;
        }

        //create USERS-stats relation table 
        if (!runQuery("CREATE TABLE STATS ( \
                        USERID TEXT PRIMARY KEY, \
                        USERNAME TEXT NOT NULL, \
                        ONLINE INTEGER DEFAULT 0, \
                        PRIVATE INTEGER DEFAULT 0, \
                        FRIEND_COUNT INTEGER DEFAULT 0, \
                        RATING REAL DEFAULT 500.0, \
                        MAX_RATING REAL DEFAULT 500.0, \
                        GAMES_PLAYED INTEGER DEFAULT 0, \
                        WINS INTEGER DEFAULT 0, \
                        LOSSES INTEGER DEFAULT 0, \
                        DRAWS INTEGER DEFAULT 0, \
                        FOREIGN KEY(USERID) REFERENCES USERS(USERID));"))
        {
            return false;
        }

        //create rating over time table 
        if (!runQuery("CREATE TABLE ELO_RATINGS ( \
                        ID INTEGER PRIMARY KEY AUTOINCREMENT,\
                        USERID TEXT NOT NULL, \
                        TIME TEXT NOT NULL, \
                        RATING REAL NOT NULL);"))
        {
            return false;
        }

        //create USERS-Friends relation table 
        if (!runQuery("CREATE TABLE RELATIONSHIPS ( \
                        USERID1 TEXT, \
                        USERID2 TEXT, \
                        STATUS CHAR(1), \
                        CREATED TEXT, \
                        PRIMARY KEY(USERID1, USERID2), \
                        FOREIGN KEY(USERID1) REFERENCES USERS(USERID), \
                        FOREIGN KEY(USERID2) REFERENCES USERS(USERID), \
                        CONSTRAINT FriendsAreDifferent_CK CHECK (USERID1 <> USERID2));"))
        { 
            return false;
        }

        // create notification table
        if (!runQuery("CREATE TABLE NOTIFICATIONS ( \
                        ID INTEGER PRIMARY KEY AUTOINCREMENT,\
                        SOURCE TEXT NOT NULL, \
                        DEST TEXT NOT NULL, \
                        TIME TEXT NOT NULL, \
                        TYPE INTEGER NOT NULL, \
                        CONTENT TEXT NOT NULL); "))
        {
            return false;
        }

        // more tables can be added here
    }

    return true;
}

bool SqliteDataBase::close()
{
    std::string query = "UPDATE STATS SET ONLINE = " + std::to_string(OFFLINE) + ";";
    runQuery(query.c_str());

    std::lock_guard<std::mutex> lock(this->databaseMutex);
	sqlite3_close(this->db);
	this->db = nullptr;
	return true;
}

/*
run query in the DB without checking returned value
*/
bool SqliteDataBase::runQuery(const char* query)
{
    std::lock_guard<std::mutex> lock(this->databaseMutex);
    char* errMessage = nullptr;
    int res = sqlite3_exec(this->db, query, nullptr, nullptr, &errMessage);
    if (res != SQLITE_OK)
    {
        std::cout << "DB ERROR: " << errMessage << std::endl;
    }
    return res == SQLITE_OK;
}

/*
run query in the DB and return the answer
usage example: 
STRING
std::string returnedAns = "";
runQueryWithAns(query.c_str(), callbackStr, &returnedAns);

VECTOR<STIRNG>
std::vector<std::string> returnedVector;
runQueryWithAns(query.c_str(), callbackStrVector, &returnedVector);
for (auto& it : returnedVector)
{
    it =  it + ":" + std::to_string(this->getPlayerScore(it));
}
*/
bool SqliteDataBase::runQueryWithAns(const char* query, int (*callBackPtr)(void*, int, char**, char**), void* var)
{
    std::lock_guard<std::mutex> lock(this->databaseMutex);
    char** errMessage = nullptr;
    int res = sqlite3_exec(db, query, callBackPtr, var, errMessage);
    return res == SQLITE_OK;
}

//this callback func returns an str answer 
int callbackStr(void* data, int argc, char** argv, char** azColName)
{
    std::string* strPtr = static_cast<std::string*>(data);
    *strPtr = argv[0];
    return 0;
}

int callBackChar(void* data, int argc, char** argv, char** azColName)
{
    if (argc > 0 && argv[0])
    {
        char* charPtr = static_cast<char*>(data);
        *charPtr = *argv[0];
    }
    return 0;
}

int callBackInt(void* data, int argc, char** argv, char** azColName)
{
    if (argc > 0 && argv[0]) 
    {
        int* intValue = static_cast<int*>(data);
        *intValue = std::atoi(argv[0]);
    }
    return 0;
}

int callbackStrVector(void* data, int argc, char** argv, char** azColName)
{
    std::vector<std::string>* strVector = (std::vector<std::string>*)data;
    strVector->push_back(argv[0]);
    return 0;
}

int callBackUser(void* data, int argc, char** argv, char** azColName)
{
    if (argc != 5)
    {
        std::cerr << "Unexpected number of columns in user stats query result" << std::endl;
        return SQLITE_ERROR;
    }
    std::vector<UserStats>* users = reinterpret_cast<std::vector<UserStats>*>(data);
    UserStats user;
    user.username = argv[0];
    user.userID = argv[1];
    user.rating = std::stod(argv[2]);
    user.online = static_cast<OnlineStates>(std::stoi(argv[3]));
    user.privacy = std::stoi(argv[4]);
    users->push_back(user);
    return 0;
}

int callbackUserStats(void* data, int argc, char** argv, char** azColName)
{
    if (argc < 2)
    {
        std::cerr << "Unexpected number of columns in user stats query result" << std::endl;
        return SQLITE_ERROR;
    }
    UserStats* userStats = static_cast<UserStats*>(data);
    userStats->username = argv[0];
    userStats->online = static_cast<OnlineStates>(std::stoi(argv[1]));
    userStats->rating = std::stod(argv[2]);
    userStats->friendsCount = std::stoi(argv[3]);
    if (argc > 4 && !userStats->privacy)
    {
        userStats->bestRating = std::stod(argv[4]);
        userStats->gamesPlayed = std::stoi(argv[5]);
        userStats->wins = std::stoi(argv[6]);
        userStats->losses = std::stoi(argv[7]);
        userStats->draws = std::stoi(argv[8]);
    }
    return 0;
}

int callbackNotifications(void* data, int argc, char** argv, char** azColName)
{
    if (argc < 6)
    {
        std::cerr << "Unexpected number of columns in notification query result" << std::endl;
        return SQLITE_ERROR;
    }
    std::vector<Notification>* notificationsList = reinterpret_cast<std::vector<Notification>*>(data);
    Notification n;
    n.id = std::stoi(argv[0]);
    n.srcID = argv[1];
    n.dstID = argv[2];
    n.time = argv[3];
    n.type = static_cast<NotificationType>(std::stoi(argv[4]));
    n.content = argv[5];
    notificationsList->push_back(n);
    return 0;
}

int callBackRatingHistory(void* data, int argc, char** argv, char** azColName)
{
    if (argc < 2)
    {
        std::cerr << "Unexpected number of columns in notification query result" << std::endl;
        return SQLITE_ERROR;
    }
    std::map<std::string, double>* ratingsList = reinterpret_cast<std::map<std::string, double>*>(data);
    ratingsList->insert(std::pair<std::string, double>(argv[0], std::stod(argv[1])));
    return 0;
}

/*
check if a mail already exist in the DB
*/
bool SqliteDataBase::doesMailExist(const std::string mail)
{
    std::string returndUser = "";
    std::string query = "SELECT USERNAME FROM USERS WHERE MAIL LIKE '" + mail + "';";
    runQueryWithAns(query.c_str(), callbackStr, &returndUser);
    return returndUser != "";
}

/*
check if a user id already exist in the DB
*/
bool SqliteDataBase::doesUserIDExist(const std::string userID)
{
    std::string returndUser = "";
    std::string query = "SELECT USERNAME FROM USERS WHERE USERID LIKE '" + userID + "';";
    runQueryWithAns(query.c_str(), callbackStr, &returndUser);
    return returndUser != "";
}

/*
add new user to USER table
userID - text
username - text
mail - text
password - text 
birthdate - text
*/
bool SqliteDataBase::addNewUser(const std::string userID, const std::string username, const std::string mail, const std::string password, const std::string birthdate)
{
    std::string salt = Security::generateSalt();
    std::string hashedPassword = Security::slowHash(password, salt);
    std::string query = "INSERT INTO USERS (USERID, USERNAME, MAIL, PASSWORD, SALT, BIRTHDATE, CREATED_AT) VALUES ('" + userID + "', '" + username + "', '" + mail + "', '" + hashedPassword + "', '"  + salt + "', '" + birthdate + "', datetime('now', 'localtime')); ";
    bool res1 = runQuery(query.c_str());
    query = "INSERT INTO STATS (USERID, USERNAME, RATING, MAX_RATING) VALUES ('" + userID + "', '" + username + "', " + std::to_string(STARTUP_RATING) + ", " + std::to_string(STARTUP_RATING) + "); ";
    bool res2 = runQuery(query.c_str());
    query = "INSERT INTO ELO_RATINGS (USERID, TIME, RATING) VALUES ('" + userID + "', datetime('now', 'localtime'), " + std::to_string(STARTUP_RATING) + "); ";
    bool res3 = runQuery(query.c_str());
    return res1 && res2 && res3;
}

/*
check if the passwrod that entered is correct
*/
bool SqliteDataBase::doesPasswordMatch(const std::string mail, const std::string password)
{
    std::string salt = getSalt(mail);
    std::string hashedPassword = Security::slowHash(password, salt);
    std::string returndPassword = "";
    std::string query = "SELECT PASSWORD FROM USERS WHERE MAIL LIKE '" + mail + "'; ";
    runQueryWithAns(query.c_str(), callbackStr, &returndPassword);
    return returndPassword == hashedPassword;
}

UserStats SqliteDataBase::getUserDetails(const std::string mail)
{
    std::string userID;
    std::vector<UserStats> users;
    std::string query = "SELECT USERID FROM USERS WHERE MAIL = '" + mail + "';";
    runQueryWithAns(query.c_str(), callbackStr, &userID);

    query = "SELECT USERNAME, USERID, RATING, ONLINE, PRIVATE FROM STATS WHERE USERID = '" + userID + "';";
    runQueryWithAns(query.c_str(), callBackUser, &users);
    return users[0];
}


bool SqliteDataBase::changeOnline(const std::string userID, const OnlineStates state)
{
    std::string query = "UPDATE STATS SET ONLINE = " + std::to_string(state) + " WHERE USERID = '" + userID + "';";
    bool res = runQuery(query.c_str());
    return res;
}

OnlineStates SqliteDataBase::getOnline(const std::string userID)
{
    int onlineState;
    std::string query = "SELECT ONLINE FROM STATS WHERE USERID = '" + userID + "';";
    runQueryWithAns(query.c_str(), callBackInt, &onlineState);
    return static_cast<OnlineStates>(onlineState);
}

std::string SqliteDataBase::getSalt(const std::string mail)
{
    std::string salt;
    std::string query = "SELECT SALT FROM USERS WHERE MAIL LIKE '" + mail + "'; ";
    runQueryWithAns(query.c_str(), callbackStr, &salt);
    return salt;
}

/*
count the amount of friend a user have
and save it in table STATS in field FRIEND_AMOUNT
*/
bool SqliteDataBase::refreshFriendsAmount(const std::string userID)
{
    int friendsCnt;
    std::string query = "SELECT COUNT(*) AS FRIEND_AMOUNT FROM RELATIONSHIPS WHERE STATUS = '" + std::string(1, static_cast<char>(ACCEPTED)) + "' AND (USERID1 = '" + userID + "' OR USERID2 = '" + userID + "')";
    runQueryWithAns(query.c_str(), callBackInt, &friendsCnt);
    query = "UPDATE STATS SET FRIEND_COUNT = " + std::to_string(friendsCnt) + " WHERE USERID = '" + userID + "';";
    bool res = runQuery(query.c_str());
    return res;
}

bool SqliteDataBase::addNewFriendRequest(const std::string userID, const std::string otherUserID)
{
    std::string query = "INSERT INTO RELATIONSHIPS (USERID1, USERID2, STATUS, CREATED) VALUES ('" + userID + "', '" + otherUserID + "', '" + std::string(1, static_cast<char>(REQUESTED)) + "', datetime('now', 'localtime'));";
    bool res = runQuery(query.c_str());
    return res;
}

std::string SqliteDataBase::getRequestUserId(const std::string userID, const std::string otherUserID)
{
    std::string requestID = "";
    std::string query = "SELECT USERID1 FROM RELATIONSHIPS WHERE(USERID1 = '" + userID + "' AND USERID2 = '" + otherUserID + "') OR(USERID1 = '" + otherUserID + "' AND USERID2 = '" + userID + "')";
    runQueryWithAns(query.c_str(), callbackStr, &requestID);
    return requestID;
}

bool SqliteDataBase::reAddFriend(const std::string userID, const std::string otherUserID)
{
    bool res1 = RemoveFriends(userID, otherUserID);
    bool res2 = addNewFriendRequest(userID, otherUserID);
    return res1 && res2;
}

bool SqliteDataBase::acceptFriend(const std::string userID, const std::string otherUserID)
{
    std::string query = "UPDATE RELATIONSHIPS SET STATUS = '" + std::string(1, static_cast<char>(ACCEPTED)) + "', CREATED = datetime('now', 'localtime') WHERE USERID1 = '" + otherUserID + "' AND USERID2 = '" + userID + "';";
    bool res = runQuery(query.c_str());
    if (res)
    {
        refreshFriendsAmount(userID);
        refreshFriendsAmount(otherUserID);
    }
    return res;
}

bool SqliteDataBase::denyFriend(const std::string userID, const std::string otherUserID)
{
    std::string query = "UPDATE RELATIONSHIPS SET STATUS = '" + std::string(1, static_cast<char>(DENY)) + "', CREATED = datetime('now', 'localtime') WHERE USERID1 = '" + otherUserID + "' AND USERID2 = '" + userID + "';";
    bool res = runQuery(query.c_str());
    return res;
}

/*
remove pair of friends to the DB
*/
bool SqliteDataBase::RemoveFriends(const std::string userID, const std::string friendID)
{       
    std::string query = "DELETE FROM RELATIONSHIPS WHERE (USERID1 = '" + userID + "' AND USERID2 = '" + friendID + "') OR (USERID1 = '" + friendID + "' AND USERID2 = '" + userID +"');";
    bool res = runQuery(query.c_str());
    if (res)
    {
        refreshFriendsAmount(userID);
        refreshFriendsAmount(friendID);
    }
    return res;
}

/*
get the status between to users
return NONE if there are no relation
*/
char SqliteDataBase::getUsersStatus(const std::string userID, const std::string otherUserID)
{
    char status = NULL;
    std::string query = "SELECT STATUS FROM RELATIONSHIPS WHERE(USERID1 = '" + userID + "' AND USERID2 = '" + otherUserID + "') OR (USERID1 = '" + otherUserID + "' AND USERID2 = '" + userID + "')";
    runQueryWithAns(query.c_str(), callBackChar, &status);
    if (status == NULL)
    {
        status = NONE;
    }
    return status;
}

std::string SqliteDataBase::getRelashionshipTime(const std::string userID, const std::string otherUserID)
{
    std::string time;
    std::string query = "SELECT CREATED FROM RELATIONSHIPS WHERE(USERID1 = '" + userID + "' AND USERID2 = '" + otherUserID + "') OR (USERID1 = '" + otherUserID + "' AND USERID2 = '" + userID + "')";
    runQueryWithAns(query.c_str(), callbackStr, &time);
    return time;
}


std::vector<UserStats> SqliteDataBase::getFriendList(const std::string userID)
{
    std::vector<std::string> friendsID;
    std::vector<UserStats> friends;
    std::string query = "SELECT USERID2 FROM RELATIONSHIPS WHERE STATUS = 'A' AND USERID1 = '" + userID + "' UNION SELECT USERID1 FROM RELATIONSHIPS WHERE STATUS = 'A' AND USERID2 = '" + userID + "';";
    runQueryWithAns(query.c_str(), callbackStrVector, &friendsID);
    for (const auto& id : friendsID)
    {
        query = "SELECT USERNAME, USERID, RATING, ONLINE, PRIVATE FROM STATS WHERE USERID = '" + id + "';";
        runQueryWithAns(query.c_str(), callBackUser, &friends);
    }

    return friends;
}

std::vector<UserStats> SqliteDataBase::getRequestList(const std::string userID)
{
    std::vector<std::string> usersID;
    std::vector<UserStats> users;
    std::string query = "SELECT USERID1 FROM RELATIONSHIPS WHERE STATUS = 'R' AND USERID2 = '" + userID + "';";
    runQueryWithAns(query.c_str(), callbackStrVector, &usersID);
    for (const auto& id : usersID)
    {
        query = "SELECT USERNAME, USERID, RATING, ONLINE, PRIVATE FROM STATS WHERE USERID = '" + id + "';";
        runQueryWithAns(query.c_str(), callBackUser, &users);
    }

    return users;
}

bool SqliteDataBase::doesUserPrivate(const std::string userID)
{
    int check;
    std::string query = "SELECT PRIVATE FROM STATS WHERE USERID = '" + userID + "';";
    runQueryWithAns(query.c_str(), callBackInt, &check);
    return check == 1;
}

UserStats SqliteDataBase::getProfile(std::string loggetUserID, std::string userID)
{
    UserStats userProfile;
    std::string query;
    int privacy;
    userProfile.userID = userID;

    // make sure the user and it friends can bypass privacy
    if (loggetUserID == userID || getUsersStatus(loggetUserID, userID) == ACCEPTED) 
    {
        privacy = 0;
    }
    else
    {
        privacy = doesUserPrivate(userID);
    }

    if (privacy)
    {
        userProfile.privacy = 1;
        query = "SELECT USERNAME, ONLINE, RATING ,FRIEND_COUNT FROM STATS WHERE USERID = '" + userID + "';";
    }
    else
    {
        userProfile.privacy = 0;
        query = "SELECT USERNAME, ONLINE, RATING, FRIEND_COUNT, MAX_RATING, GAMES_PLAYED, WINS, LOSSES, DRAWS FROM STATS WHERE USERID = '" + userID + "';";
    }
    
    runQueryWithAns(query.c_str(), callbackUserStats, &userProfile);
    return userProfile;
}

/*
save notification to database to send to the user when he login
*/
int SqliteDataBase::saveNotification(Notification notification)
{
    std::string query = "INSERT INTO NOTIFICATIONS (SOURCE, DEST, TIME, TYPE, CONTENT) VALUES ('" + notification.srcID + "', '" + notification.dstID + "', '" + notification.time + "', '" + std::to_string(notification.type) + "', '" + notification.content + "');";
    bool res = runQuery(query.c_str());
    if (res)
    {
        int lastID = sqlite3_last_insert_rowid(this->db);
        return lastID;
    }
    return -1;
}

/*
load notifications from the database to send to the user
*/
std::vector<Notification> SqliteDataBase::loadNotifications(std::string userID)
{
    std::vector<Notification> notificationList;
    std::string query = "SELECT * FROM NOTIFICATIONS WHERE DEST = '" + userID + "';";
    runQueryWithAns(query.c_str(), callbackNotifications, &notificationList);
    return notificationList;
}

/*
delete notification from database
*/
bool SqliteDataBase::removeNotification(int id, std::string userID)
{
    std::string query = "DELETE FROM NOTIFICATIONS WHERE ID = " + std::to_string(id) + " AND DEST = '" + userID + "';";
    bool res = runQuery(query.c_str());
    return res;
}

bool SqliteDataBase::changeUserData(std::string userID, std::string username, std::string mail, int privacy)
{   
    bool res1 = true, res2 = true, res3 = true, res4 = true;
    std::string query = "";
    if (!username.empty())
    {
        query = "UPDATE USERS SET USERNAME = '" + username + "' WHERE USERID = '" + userID + "';";
        res1 = runQuery(query.c_str());
        query = "UPDATE STATS SET USERNAME = '" + username + "' WHERE USERID = '" + userID + "';";
        res2 = runQuery(query.c_str());
    }

    if (!mail.empty())
    {
       query = "UPDATE USERS SET MAIL = '" + mail + "' WHERE USERID = '" + userID + "';";
       res3 = runQuery(query.c_str());
    }

    if (privacy != -1)
    {
        query = "UPDATE STATS SET PRIVATE = '" + std::to_string(privacy) + "' WHERE USERID = '" + userID + "';";
        res4 = runQuery(query.c_str());
    }

    return res1 && res2 && res3 && res4;
}

std::vector<UserStats> SqliteDataBase::getLeaderboard(int topAmount)
{
    std::vector<std::string> usersID;
    std::vector<UserStats> topUsers;
    std::string query = "SELECT USERID FROM STATS ORDER BY RATING DESC LIMIT " + std::to_string(topAmount) + ";";
    runQueryWithAns(query.c_str(), callbackStrVector, &usersID);
    for (const auto& id : usersID)
    {
        query = "SELECT USERNAME, USERID, RATING, ONLINE, PRIVATE FROM STATS WHERE USERID = '" + id + "';";
        runQueryWithAns(query.c_str(), callBackUser, &topUsers);
    }

    return topUsers;
}

bool SqliteDataBase::updateUserStats(std::string userID, double newRating, bool win, bool lose, bool draw)
{
    UserStats stats = this->getProfile(userID, userID);
    std::string query = "UPDATE STATS SET RATING = " + std::to_string(newRating) + ", GAMES_PLAYED = " + std::to_string(stats.gamesPlayed + 1);
    if (newRating > stats.bestRating)
    {
        query += ", MAX_RATING = " + std::to_string(newRating);
    }
    if (win)
    {
        query += ", WINS = " + std::to_string(stats.wins + 1);
    }
    else if (lose)
    {
        query += ", LOSSES = " + std::to_string(stats.losses + 1);
    }
    else if (draw)
    {
        query += ", DRAWS = " + std::to_string(stats.draws + 1);
    }

    query += " WHERE USERID = '" + userID + "';";
    return runQuery(query.c_str()) && addNewRating(userID, newRating);
}

bool SqliteDataBase::addNewRating(std::string userID, double rating)
{
    std::string query = "INSERT INTO ELO_RATINGS (USERID, TIME, RATING) VALUES ('" + userID + "', datetime('now', 'localtime'), " + std::to_string(rating) + "); ";
    return runQuery(query.c_str());
}

std::map<std::string, double> SqliteDataBase::getRatingHistory(std::string userID)
{
    std::map<std::string, double> ratingsHistory;
    std::string query = "SELECT TIME, RATING FROM ELO_RATINGS WHERE USERID = '" + userID + "';";
    bool res = runQueryWithAns(query.c_str(), callBackRatingHistory, &ratingsHistory);
    return ratingsHistory;
}