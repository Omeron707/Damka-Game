#include "MenuRequestHandler.h"
#include "RequestHandlerFactory.h"
#include "Constants.h"
#include <iostream>
#include <string>

MenuRequestHandler::MenuRequestHandler(std::shared_ptr<User> user):
	m_user(user)
{
}

MenuRequestHandler::~MenuRequestHandler()
{
}

std::shared_ptr<User> MenuRequestHandler::getUser()
{
	return this->m_user;
}

bool MenuRequestHandler::isRequestRelevant(RequestInfo request) 
{
	int code = request.msg[JsonKeys::CODE];
	switch (code)
	{
		case LOGOUT_CODE:
			return true;

		case ADD_FRIEND_CODE:
			return request.msg.contains(JsonKeys::USERID);
		
		case ACCEPT_FRIEND_CODE:
			return request.msg.contains(JsonKeys::USERID);

		case DENY_FRIEND_CODE:
			return request.msg.contains(JsonKeys::USERID);

		case REMOVE_FRIEND_CODE:
			return request.msg.contains(JsonKeys::USERID);

		case GET_FRIENDS_CODE:
			return true;

		case GET_PROFILE:
			return request.msg.contains(JsonKeys::USERID);

		case GET_NOTIFICATIONS_CODE:
			return true;

		case NOTIFICATION_CODE:
			return request.msg.contains(JsonKeys::NOTIFICATION_ID);
			
		case GET_LEADERBOARD_CODE:
			return true;

		case UPDATE_USER_CODE:
			return request.msg.contains(JsonKeys::USERNAME) && request.msg.contains(JsonKeys::MAIL), request.msg.contains(JsonKeys::PRIVACY);

		case FIND_GAME_CODE:
			return true;

		case INVITE_FRINED_GAME:
			return request.msg.contains(JsonKeys::USERID) && request.msg.contains(JsonKeys::GAMEID);

		case LEAVE_GAME:
			return true;

		case READY_TO_START:
			return request.msg.contains(JsonKeys::GAMEID);

		default:
			return false;
	}
}

RequestResult MenuRequestHandler::handleRequest(RequestInfo request)
{ 
	RequestResult response;
	int code = request.msg[JsonKeys::CODE];
	try
	{
		switch (code)
		{
			case LOGOUT_CODE:
				response = this->logout(request);
				response.newHandler = new LoginRequestHandler();
				response.notification = nullptr;
				break;

			case ADD_FRIEND_CODE:
				response = this->addFriend(request);
				response.newHandler = nullptr;
				break;

			case ACCEPT_FRIEND_CODE:
				response = this->acceptFriend(request);
				response.newHandler = nullptr;
				break;

			case DENY_FRIEND_CODE:
				response = this->denyFriend(request);
				response.newHandler = nullptr;
				response.notification = nullptr;
				break;

			case REMOVE_FRIEND_CODE:
				response = this->removeFriend(request);
				response.newHandler = nullptr;
				break;

			case GET_FRIENDS_CODE:
				response = this->getFriends(request);
				break;

			case GET_PROFILE:
				response = this->getUserProfile(request);
				response.newHandler = nullptr;
				response.notification = nullptr;
				break;

			case GET_NOTIFICATIONS_CODE:
				response = this->getNotifications(request);
				response.newHandler = nullptr;
				response.notification = nullptr;
				break;

			case NOTIFICATION_CODE:
				response = this->markNotificationRead(request);
				response.newHandler = nullptr;
				response.notification = nullptr;
				break;

			case GET_LEADERBOARD_CODE:
				response = this->getLeaderboard(request);
				response.newHandler = nullptr;
				response.notification = nullptr;
				break;

			case UPDATE_USER_CODE:
				response = this->updateUserData(request);
				response.newHandler = nullptr;
				response.notification = nullptr;
				break;

			case FIND_GAME_CODE:
				response = this->findMatch(request);
				response.newHandler = nullptr;
				break;

			case INVITE_FRINED_GAME:
				response = this->InviteFriendForMatch(request);
				response.newHandler = nullptr;
				break;

			case LEAVE_GAME:
				response = this->leaveMatch(request);
				response.newHandler = nullptr;
				response.notification = nullptr;
				break;

			case READY_TO_START:
				response = this->readyForMatch(request);
				break;
		}
	}
	catch (MenuException e)
	{
		RequestResult er;
		er.msg[JsonKeys::CODE] = ERROR_CODE;
		er.msg[JsonKeys::ERROR_MSG] = e.what();
		er.newHandler = nullptr;
		er.notification = nullptr;
		return er;
	}
	catch (LoginException s)
	{
		RequestResult er;
		er.msg[JsonKeys::CODE] = UPDATE_USER_CODE;
		er.msg[JsonKeys::SUCCESS_CODE] = FAILURE;
		er.msg[JsonKeys::ERROR_FIELD] = s.where();
		er.msg[JsonKeys::ERROR_MSG] = s.what();
		er.newHandler = nullptr;
		er.notification = nullptr;
		return er;
	}
	catch (...)
	{
		RequestResult er;
		er.msg[JsonKeys::CODE] = ERROR_CODE;
		er.msg[JsonKeys::ERROR_MSG] = "something went wrong";
		er.newHandler = nullptr;
		er.notification = nullptr;
		return er;
	}
	return response;
}

RequestResult MenuRequestHandler::logout(RequestInfo request)
{
	LoginManager& manager = RequestHandlerFactory::getInstance()->getLoginManager();

	manager.logout(this->m_user->getUserID());
	
	RequestResult response;
	response.msg[JsonKeys::CODE] = LOGOUT_CODE;
	response.msg["success"] = SUCCESS;

	return response;
}

RequestResult MenuRequestHandler::addFriend(RequestInfo request)
{
	MenuManager& manager = RequestHandlerFactory::getInstance()->getMenuManager();
	RequestResult response;
	response.msg[JsonKeys::CODE] = ADD_FRIEND_CODE;

	try
	{
		manager.addFriend(this->m_user->getUserID(), request.msg[JsonKeys::USERID]);
		response.msg[JsonKeys::SUCCESS_CODE] = SUCCESS;
		// send notification to other user 
		response.notification = new Notification();
		response.notification->srcID = this->m_user->getUserID();
		response.notification->dstID = request.msg[JsonKeys::USERID];
		response.notification->type = NEW_FRIEND;
		char buffer[100];
		sprintf_s(buffer, sizeof(buffer), NotificationMessages::NEW_FRIEND_MESSAGE, this->m_user->getUsername().c_str());
		response.notification->content = std::string(buffer);
		response.notification->saveInDB = true;
	}
	catch (MenuException e)
	{
		response.msg[JsonKeys::SUCCESS_CODE] = FAILURE;
		response.msg[JsonKeys::ERROR_MSG] = e.what();
		response.notification = nullptr;
	}
	return response;
}

RequestResult MenuRequestHandler::acceptFriend(RequestInfo request)
{
	MenuManager& manager = RequestHandlerFactory::getInstance()->getMenuManager();
	RequestResult response;
	response.msg[JsonKeys::CODE] = ACCEPT_FRIEND_CODE;
	try
	{
		manager.acceptFriend(this->m_user->getUserID(), request.msg[JsonKeys::USERID]);
		response.msg[JsonKeys::SUCCESS_CODE] = SUCCESS;
		// send notification to other user 
		response.notification = new Notification();
		response.notification->srcID = this->m_user->getUserID();
		response.notification->dstID = request.msg[JsonKeys::USERID];
		response.notification->type = ACCEPT_FRIEND;
		char buffer[100];
		sprintf_s(buffer, sizeof(buffer), NotificationMessages::ACCPET_FRIEND_MESSAGE, this->m_user->getUsername().c_str());
		response.notification->content = std::string(buffer);
		response.notification->saveInDB = true;
	}
	catch (MenuException e)
	{
		response.msg[JsonKeys::SUCCESS_CODE] = FAILURE;
		response.msg[JsonKeys::ERROR_MSG] = e.what();
		response.notification = nullptr;
	}
	return response;
}


RequestResult MenuRequestHandler::denyFriend(RequestInfo request)
{
	MenuManager& manager = RequestHandlerFactory::getInstance()->getMenuManager();
	RequestResult response;
	response.msg[JsonKeys::CODE] = DENY_FRIEND_CODE;

	try
	{
		manager.denyFriend(this->m_user->getUserID(), request.msg[JsonKeys::USERID]);
		response.msg[JsonKeys::SUCCESS_CODE] = SUCCESS;
	}
	catch (MenuException e)
	{
		response.msg[JsonKeys::SUCCESS_CODE] = FAILURE;
		response.msg[JsonKeys::ERROR_MSG] = e.what();
		response.notification = nullptr;
	}
	return response;
}

RequestResult MenuRequestHandler::removeFriend(RequestInfo request)
{
	RequestResult response;
	response.msg[JsonKeys::CODE] = REMOVE_FRIEND_CODE;
	MenuManager& manager = RequestHandlerFactory::getInstance()->getMenuManager();
	try
	{
		manager.removeFriend(this->m_user->getUserID(), request.msg[JsonKeys::USERID]);
		response.msg[JsonKeys::SUCCESS_CODE] = SUCCESS;
		// send notification to other user 
		response.notification = new Notification();
		response.notification->srcID = this->m_user->getUserID();
		response.notification->dstID = request.msg[JsonKeys::USERID];
		response.notification->type = DELETE_FRIEND;
		response.notification->content = "";
		response.notification->saveInDB = false;
	}
	catch (MenuException e)
	{
		response.msg[JsonKeys::SUCCESS_CODE] = FAILURE;
		response.msg[JsonKeys::ERROR_MSG] = e.what();
		response.notification = nullptr;
	}
	return response;
}

RequestResult MenuRequestHandler::getFriends(RequestInfo request)
{
	MenuManager& manager = RequestHandlerFactory::getInstance()->getMenuManager();

	json friendsList = manager.getFriendsList(this->m_user->getUserID());
	json friendsRequestsList = manager.getFriendsRequestsList(this->m_user->getUserID());

	RequestResult response;
	response.msg[JsonKeys::CODE] = GET_FRIENDS_CODE;
	response.msg[JsonKeys::FRIEND_LIST] = friendsList[JsonKeys::FRIEND_LIST];
	response.msg[JsonKeys::REQUEST_LIST] = friendsRequestsList[JsonKeys::REQUEST_LIST];

	response.newHandler = nullptr;
	response.notification = nullptr;
	return response;
}

RequestResult MenuRequestHandler::getUserProfile(RequestInfo request)
{
	MenuManager& manager = RequestHandlerFactory::getInstance()->getMenuManager();

	json stats = manager.getUserProfile(this->m_user->getUserID(), request.msg[JsonKeys::USERID]);

	RequestResult response;
	response.msg[JsonKeys::CODE] = GET_PROFILE;
	response.msg[JsonKeys::STATS] = stats;

	return response;
}

RequestResult MenuRequestHandler::getNotifications(RequestInfo request)
{
	MenuManager& manager = RequestHandlerFactory::getInstance()->getMenuManager();

	json notificationsList = manager.getOfflineNotifications(this->m_user->getUserID());

	RequestResult response;
	response.msg[JsonKeys::CODE] = GET_NOTIFICATIONS_CODE;
	response.msg[JsonKeys::NOTIFICATION] = notificationsList[JsonKeys::NOTIFICATION];

	return response;
}

/*
remove notification from saved notifications
and send back the updated notifications list
*/
RequestResult MenuRequestHandler::markNotificationRead(RequestInfo request)
{
	MenuManager& manager = RequestHandlerFactory::getInstance()->getMenuManager();
	manager.removeNotifications(request.msg[JsonKeys::NOTIFICATION_ID], this->m_user->getUserID());

	return getNotifications(request);
}

RequestResult MenuRequestHandler::getLeaderboard(RequestInfo request)
{
	MenuManager& manager = RequestHandlerFactory::getInstance()->getMenuManager();
	json leaderboardList = manager.getLeaderboardList();

	RequestResult response;
	response.msg[JsonKeys::CODE] = GET_LEADERBOARD_CODE;
	response.msg[JsonKeys::LEADERBOARD] = leaderboardList[JsonKeys::LEADERBOARD];

	return response;
}

RequestResult MenuRequestHandler::updateUserData(RequestInfo request)
{
	LoginManager& manager = RequestHandlerFactory::getInstance()->getLoginManager();

	std::string username = request.msg[JsonKeys::USERNAME];
	std::string mail = request.msg[JsonKeys::MAIL];
	int privacy = request.msg[JsonKeys::PRIVACY];

	manager.updateUserData(this->m_user->getUserID(), username, mail, privacy);

	RequestResult response;
	response.msg[JsonKeys::CODE] = UPDATE_USER_CODE;
	response.msg[JsonKeys::SUCCESS_CODE] = SUCCESS;

	if (!username.empty())
	{
		response.msg[JsonKeys::USERNAME] = username;
		this->m_user->setUsername(username);
	}
	else
	{
		response.msg[JsonKeys::USERNAME] = "";
	}
	if (!mail.empty())
	{
		response.msg[JsonKeys::MAIL] = mail;
	}
	else
	{
		response.msg[JsonKeys::MAIL] = "";
	}
	if (privacy != -1)
	{
		response.msg[JsonKeys::PRIVACY] = privacy;
	}
	else
	{
		response.msg[JsonKeys::PRIVACY] = -1;
	}

	return response;
}

RequestResult MenuRequestHandler::findMatch(RequestInfo request)
{
	RequestResult response;
	GameManager& manager = RequestHandlerFactory::getInstance()->getGameManager();

	std::shared_ptr<Game> game = manager.findMatch(this->m_user);
	if (game != nullptr)
	{
		response.msg = game->getStartDetails(this->m_user->getUserID());
		std::string opponentID = response.msg[JsonKeys::OPPONENT][JsonKeys::USERID];
		response.notification = new Notification();
		response.notification->srcID = this->m_user->getUserID();
		response.notification->dstID = opponentID;
		response.notification->type = GAME_NOTIFICATION;
		response.notification->content = game->getStartDetails(opponentID).dump();
		response.notification->saveInDB = false;
	}
	else
	{
		response.msg[JsonKeys::CODE] = WAIT_FOR_GAME;
		response.notification = nullptr;
	}
	return response;
}


RequestResult MenuRequestHandler::InviteFriendForMatch(RequestInfo request)
{
	RequestResult response;
	GameManager& manager = RequestHandlerFactory::getInstance()->getGameManager();

	int id = request.msg[JsonKeys::GAMEID];
	std::string friendsID = request.msg[JsonKeys::USERID];

	if (!manager.isAvilableForMatch(friendsID))
	{
		response.msg[JsonKeys::CODE] = FAILED_INVITE;
		response.newHandler = nullptr;
		response.notification = nullptr;
		return response;
	}

	int gameID = manager.inviteMatch(this->m_user, id);
	
	if (id != 0)
	{
		// send game data
		std::shared_ptr<Game> game = manager.getGame(request.msg[JsonKeys::GAMEID]);
		response.msg = game->getStartDetails(this->m_user->getUserID());
		std::string opponentID = response.msg[JsonKeys::OPPONENT][JsonKeys::USERID];
		response.notification = new Notification();
		response.notification->srcID = this->m_user->getUserID();
		response.notification->dstID = opponentID;
		response.notification->type = GAME_NOTIFICATION;
		response.notification->content = game->getStartDetails(opponentID).dump();
		response.notification->saveInDB = false;
	}
	else
	{
		// send invintation and wait
		response.msg[JsonKeys::CODE] = WAIT_FOR_GAME;
		response.notification = new Notification();
		response.notification->srcID = this->m_user->getUserID();
		response.notification->dstID = friendsID;
		response.notification->type = GAME_INVITE;
		response.notification->content = std::to_string(gameID);
		response.notification->saveInDB = false;
	}
	return response;
}

RequestResult MenuRequestHandler::leaveMatch(RequestInfo request)
{
	RequestResult response;
	GameManager& manager = RequestHandlerFactory::getInstance()->getGameManager();
	manager.removeFromQueue(this->m_user);
	response.msg[JsonKeys::CODE] = LEAVE_GAME;
	return response;
}

RequestResult MenuRequestHandler::readyForMatch(RequestInfo request)
{
	RequestResult response;
	GameManager& manager = RequestHandlerFactory::getInstance()->getGameManager();

	std::shared_ptr<Game> game = manager.getGame(request.msg[JsonKeys::GAMEID]);

	response.msg[JsonKeys::CODE] = START_GAME;
	response.newHandler = new GameRequestHandler(this->m_user, game);
	response.notification = nullptr;
	return response;
}