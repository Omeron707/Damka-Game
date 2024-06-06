#include "RequestHandlerFactory.h"

RequestHandlerFactory* RequestHandlerFactory::instance = nullptr;

// singelton pattern
RequestHandlerFactory* RequestHandlerFactory::getInstance()
{
	if (instance == nullptr)
	{
		instance = new RequestHandlerFactory();
	}
	return instance;
}

RequestHandlerFactory::RequestHandlerFactory()
{
}

void RequestHandlerFactory::setParams(SqliteDataBase* db)
{
	this->m_database = db;
	this->m_loginManager = new LoginManager(db);
	this->m_menuManager = new MenuManager(db);
	this->m_GameManager = new GameManager(db);
}

RequestHandlerFactory::~RequestHandlerFactory()
{
	delete this->m_loginManager;
	delete this->m_menuManager;
}

LoginManager& RequestHandlerFactory::getLoginManager()
{
	return *this->m_loginManager;
}

MenuManager& RequestHandlerFactory::getMenuManager()
{
	return *this->m_menuManager;
}

GameManager& RequestHandlerFactory::getGameManager()
{
	return *this->m_GameManager;
}