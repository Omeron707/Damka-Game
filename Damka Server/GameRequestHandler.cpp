#include "GameRequestHandler.h"

GameRequestHandler::GameRequestHandler(std::shared_ptr<User> user, std::shared_ptr<Game> game) :
	m_user(user), m_game(game)
{
}

GameRequestHandler::~GameRequestHandler()
{
}

std::shared_ptr<User> GameRequestHandler::getUser()
{
	return this->m_user;
}

bool GameRequestHandler::isRequestRelevant(RequestInfo request)
{
	int code = request.msg[JsonKeys::CODE];
	switch (code)
	{
		case LEAVE_GAME:
			return true;
		case OFFER_DRAW:
			return true;
		case GET_PROFILE:
			return request.msg.contains(JsonKeys::USERID);
		case MOVE_CODE:
			return request.msg.contains(JsonKeys::MOVES) && request.msg[JsonKeys::MOVES].is_array() && !request.msg[JsonKeys::MOVES].empty();
		default:
			return false;
	}
}

RequestResult GameRequestHandler::handleRequest(RequestInfo request)
{
	RequestResult response;
	int code = request.msg[JsonKeys::CODE];
	try
	{
		switch (code)
		{
			case LEAVE_GAME:
				response = this->leaveMatch(request);
				break;
			case OFFER_DRAW:
				response = this->offerDraw(request);
				break;
			case GET_PROFILE:
				response = this->getUserProfile(request);
				break;
			case MOVE_CODE:
				response = this->playMove(request);
				break;
		}
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

RequestResult GameRequestHandler::getUserProfile(RequestInfo request)
{
	MenuManager& manager = RequestHandlerFactory::getInstance()->getMenuManager();

	json stats = manager.getUserProfile(this->m_user->getUserID(), request.msg[JsonKeys::USERID]);

	RequestResult response;
	response.msg[JsonKeys::CODE] = GET_PROFILE;
	response.msg[JsonKeys::STATS] = stats;

	response.newHandler = nullptr;
	response.notification = nullptr;
	return response;
}

RequestResult GameRequestHandler::offerDraw(RequestInfo request)
{
	RequestResult response;
	if (this->m_game->offerDraw(this->m_user->getUserID()))
	{
		response = this->getEndResult();
		GameManager& manager = RequestHandlerFactory::getInstance()->getGameManager();
		manager.endGame(this->m_game->getID());
	}
	else
	{
		json offer;
		offer[JsonKeys::CODE] = OFFER_DRAW;
		offer[JsonKeys::USERID] = this->m_user->getUserID();
		response.msg = offer;

		response.notification = new Notification();
		response.notification->srcID = this->m_user->getUserID();
		response.notification->dstID = this->m_game->getOpponentId(this->m_user->getUserID());
		response.notification->type = GAME_NOTIFICATION;
		response.notification->content = offer.dump();
		response.notification->saveInDB = false;
	}
	response.newHandler = nullptr;
	return response;
}

RequestResult GameRequestHandler::leaveMatch(RequestInfo request)
{
	RequestResult response;
	GameManager& manager = RequestHandlerFactory::getInstance()->getGameManager();
	manager.removeFromQueue(this->m_user);
	if (manager.getGame(this->m_game->getID()) != nullptr)
	{
		this->m_game->leaveGame(this->m_user->getUserID());
		manager.endGame(this->m_game->getID());
		response = this->getEndResult();
	}
	else
	{
		response.msg[JsonKeys::CODE] = LEAVE_GAME;
		response.notification = nullptr;
	}
	response.newHandler = new MenuRequestHandler(this->m_user);
	return response;
}

RequestResult GameRequestHandler::getEndResult()
{
	RequestResult response;
	json endingData = this->m_game->getGameResult();
	response.msg = endingData;

	response.notification = new Notification();
	response.notification->srcID = this->m_user->getUserID();
	response.notification->dstID = this->m_game->getOpponentId(this->m_user->getUserID());
	response.notification->type = GAME_NOTIFICATION;
	response.notification->content = endingData.dump();
	response.notification->saveInDB = false;
	return response;
}

Position GameRequestHandler::parseMove(json move)
{
	Position p;
	p.row = move[JsonKeys::ROW];
	p.column = move[JsonKeys::COL];
	return p;	
}

RequestResult GameRequestHandler::playMove(RequestInfo request)
{
	RequestResult response;
	json moveArray = request.msg[JsonKeys::MOVES];
	bool result = this->m_game->playMoves(this->m_user->getUserID(), moveArray);
	
	// win - no more opponent pieces
	if (result && this->m_game->checkWin(this->m_user->getUserID()))
	{
		response = this->getEndResult();
		GameManager& manager = RequestHandlerFactory::getInstance()->getGameManager();
		manager.endGame(this->m_game->getID());
		response.newHandler = nullptr;
		return response;
	}

	// draw - opponents doesn't have any move
	if (result && this->m_game->chekForceDraw(this->m_user->getUserID()))
	{
		response = this->getEndResult();
		GameManager& manager = RequestHandlerFactory::getInstance()->getGameManager();
		manager.endGame(this->m_game->getID());
		response.newHandler = nullptr;
		return response;
	}

	response.msg[JsonKeys::CODE] = MOVE_CODE;
	response.msg[JsonKeys::SUCCESS_CODE] = result;
	json gameState = this->m_game->getGameState();
	response.msg[JsonKeys::GAME_STATE] = gameState;
	if (result)
	{
		// send updated board detailes
		response.notification = new Notification();
		response.notification->srcID = this->m_user->getUserID();
		response.notification->dstID = this->m_game->getOpponentId(this->m_user->getUserID());
		response.notification->type = GAME_NOTIFICATION;
		json notifState;
		notifState[JsonKeys::CODE] = UPDATE_GAME_STATE;
		notifState[JsonKeys::GAME_STATE] = gameState;
		response.notification->content = notifState.dump();
		response.notification->saveInDB = false;
	}
	else
	{
		response.notification = nullptr;
	}
	response.newHandler = nullptr;
	return response;
}