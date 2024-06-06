#pragma once
#include "IRequestHandler.h"
#include "MenuRequestHandler.h"
#include "RequestHandlerFactory.h"
#include "GameManager.h"
#include "Game.h"
#include <memory>

class GameRequestHandler : public IRequestHandler
{
public:
	GameRequestHandler(std::shared_ptr<User> user, std::shared_ptr<Game> game);
	virtual ~GameRequestHandler();

	virtual bool isRequestRelevant(RequestInfo request) override;
	virtual RequestResult handleRequest(RequestInfo request) override;
	virtual std::shared_ptr<User> getUser() override;


private:
	RequestResult leaveMatch(RequestInfo request);
	RequestResult offerDraw(RequestInfo request);
	RequestResult playMove(RequestInfo request);
	RequestResult getUserProfile(RequestInfo request);
	RequestResult getEndResult();
	Position parseMove(json move);

	std::shared_ptr<Game> m_game;
	std::shared_ptr<User> m_user;
};