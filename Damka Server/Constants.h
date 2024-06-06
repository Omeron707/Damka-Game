// file contains constants values to use through out the code.
#pragma once
#include <iostream> 
#include <string>


#define DATABASE_PATH "DamkaDatabase.sqlite"
#define SERVER_PORT 9999

#define LEADERBOARD_AMOUNT 10
#define DENY_DELAY_TIME_SECOND 300

#define K_VALUE 32
#define STARTUP_RATING 500.0

namespace JsonKeys
{
	const std::string CODE = "code";
	const std::string SUCCESS_CODE = "success";
	const std::string ERROR_MSG = "error";
	// login - signup
	const std::string ERROR_FIELD = "error_field";
	const std::string USERID = "userID";
	const std::string USERNAME = "username";
	const std::string MAIL = "mail";
	const std::string PASSWORD = "password";
	const std::string BIRTHDATE = "birthdate";

	const std::string ONLINE_STATE = "online";

	// friends
	const std::string FRIEND_LIST = "friends_list";
	const std::string REQUEST_LIST = "request_friends_list";
	const std::string FRIENDS_AMOUNT = "friends_amount";

	const std::string NOTIFICATION = "notifications";
	const std::string NOTIFICATION_ID = "notification_id";
	const std::string NOTIFICATION_TYPE = "type";
	const std::string NOTIFICATION_SOURCE = "source";
	const std::string NOTIFICATION_TIME = "time";
	const std::string NOTIFICATION_CONTENT = "content";
	
	//statistics
	const std::string LEADERBOARD = "leaderboard";
	const std::string STATS = "profile";
	const std::string RATING = "rating";
	const std::string PRIVACY = "private";
	const std::string BEST_RATING = "best_rating";
	const std::string GAME_PLAYED = "games_played";
	const std::string WINS = "wins";
	const std::string LOSSES = "losses";
	const std::string DRAWS = "draws";
	const std::string RATINGS_HISTORY = "ratings_history";
	const std::string TIMESTAMP = "timestamp";

	//game
	const std::string GAME_STATE = "game_state";
	const std::string GAMEID = "gameID";
	const std::string PLAYER_COLOR = "color";
	const std::string OPPONENT = "opponent";
	const std::string BOARD = "board_state";
	const std::string TURN = "turn";
	const std::string RESULT = "result";
	const std::string END_REASON = "reason";
	const std::string MOVES = "moves";
	const std::string SRC_POS = "source_position";
	const std::string DST_POS = "destination_position";
	const std::string ROW = "row";
	const std::string COL = "column";
}

enum MessageCodes
{
	ERROR_CODE = 1,
	LOGIN_CODE,
	SIGN_UP_CODE,
	LOGOUT_CODE,
	ADD_FRIEND_CODE,
	ACCEPT_FRIEND_CODE,
	DENY_FRIEND_CODE,
	REMOVE_FRIEND_CODE,
	GET_FRIENDS_CODE,
	GET_PROFILE,
	NOTIFICATION_CODE,
	GET_NOTIFICATIONS_CODE,
	GET_LEADERBOARD_CODE,
	UPDATE_USER_CODE,

	FIND_GAME_CODE,
	INVITE_FRINED_GAME,
	WAIT_FOR_GAME,
	GAME_DETAILES,
	READY_TO_START,
	START_GAME,
	LEAVE_GAME,
	GAME_RESULT,
	OFFER_DRAW,
	MOVE_CODE,
	UPDATE_GAME_STATE,
	FAILED_INVITE
};

enum OnlineStates
{
	OFFLINE,
	ONLINE,
	IN_MATCH
};

enum Relationships
{
	NONE = 'N',
	REQUESTED = 'R',
	ACCEPTED = 'A',
	DENY = 'D',
};

enum ResponseCodes
{
	SUCCESS = 0,
	FAILURE = 1
};

enum NotificationType
{
	NEW_FRIEND = 1,
	ACCEPT_FRIEND,
	DELETE_FRIEND,
	GAME_INVITE,
	GAME_NOTIFICATION
};

enum Pieces
{
	WHITE_SOLDIER = 'w',
	BLACK_SOLDIER = 'b',
	WHITE_QUEEN = 'q',
	BLACK_QUEEN = 'k',
	EMPTY_SQUARE = ' ',
	LIGHT_SQUARE = '0'
};

enum Turn
{
	BLACK = 0,
	WHITE = 1
};

namespace NotificationMessages
{
	const char NEW_FRIEND_MESSAGE[] = "You have new friend request from %s!";
	const char ACCPET_FRIEND_MESSAGE[] = "%s accepted your friend request!";
}