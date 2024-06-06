#pragma once
#include <iostream>
#include <thread>
#include <set>
#include <map>
#include <websocketpp/config/asio_no_tls.hpp>
#include <websocketpp/server.hpp>
#include <websocketpp/common/connection_hdl.hpp>
#include "jsonHelper.h"
#include "IRequestHandler.h"
#include "RequestHandlerFactory.h"
#include "Constants.h"
#include "SqliteDataBase.h"

typedef websocketpp::server<websocketpp::config::asio> server;

class Server
{
	public:
		Server();
		~Server();
		void run();

	private:
		void startHandleRequest();
		void onOpen(websocketpp::connection_hdl hdl);
		void onClose(websocketpp::connection_hdl hdl);

		void sendMessage(websocketpp::connection_hdl hdl, const std::string& message);
		void sendMessage(websocketpp::connection_hdl hdl, RequestResult response);
		RequestInfo parseMsg(const std::string& message);
		void handleRequest(websocketpp::connection_hdl hdl, server::message_ptr msg);

		std::string parseNotification(Notification* notification);
		void sendNotification(Notification* notification);
		std::string getCurrentTimeAsString();

		SqliteDataBase* m_database;
		server m_server;
		std::map<websocketpp::connection_hdl, IRequestHandler*, std::owner_less<websocketpp::connection_hdl>> m_clients;
};
