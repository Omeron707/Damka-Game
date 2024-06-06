#pragma once
#include "SqliteDataBase.h"
#include "LoginRequestHandler.h"
#include "MenuRequestHandler.h"
#include "GameRequestHandler.h"
#include "LoginManager.h"
#include "MenuManager.h"
#include "GameManager.h"
#include "User.h"

class LoginRequestHandler;
class MenuRequestHandler;

class RequestHandlerFactory
{
	public:
		RequestHandlerFactory(const RequestHandlerFactory& obj) = delete;
		static RequestHandlerFactory* getInstance();

		void setParams(SqliteDataBase* db);

		LoginManager& getLoginManager();
		MenuManager& getMenuManager();
		GameManager& getGameManager();

	private:
		RequestHandlerFactory();
		~RequestHandlerFactory();

		static RequestHandlerFactory* instance;
		SqliteDataBase* m_database;
		LoginManager* m_loginManager;
		MenuManager* m_menuManager;
		GameManager* m_GameManager;
};
