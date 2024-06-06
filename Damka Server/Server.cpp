#include <iostream>
#include <string>
#include "Server.h"
#include "Constants.h"

Server::Server()
{
	this->m_database = new SqliteDataBase();
	if (!this->m_database->open())
	{
		throw std::string("Can't open the database");
	}
	
	RequestHandlerFactory::getInstance()->setParams(this->m_database);
}

Server::~Server()
{
	this->m_database->close();
	delete this->m_database;
}

/**
 * set the websocket handlers:
 * on open handler => onOpen()
 * on close handler => onClose()
 * on message handler => handleRequest()
 */
void Server::startHandleRequest()
{
	m_server.set_open_handler(std::bind(&Server::onOpen, this, std::placeholders::_1));
	m_server.set_close_handler(std::bind(&Server::onClose, this, std::placeholders::_1));
	m_server.set_message_handler(std::bind(&Server::handleRequest, this, std::placeholders::_1, std::placeholders::_2));

	std::cout << "Start listning on " << SERVER_PORT << std::endl;
	m_server.listen(SERVER_PORT);
	m_server.start_accept();

	m_server.run();
}

/**
 * start the lisening for connections thread.
 * 
 * create a terminal to run commands.
 * for example: 
 * "EXIT" - close the server.
 * "users" - display all connected users.
 */
void Server::run()
{
	std::string input = "";
	m_server.set_access_channels(websocketpp::log::alevel::all);
	m_server.clear_access_channels(websocketpp::log::alevel::frame_payload);
	m_server.init_asio();

	std::thread acceptingThread(&Server::startHandleRequest, this);

	Sleep(1000);
	// command line for the server
	while (input != "EXIT")
	{
		std::cout << ">> ";
		std::getline(std::cin, input);

		if (input == "EXIT")
		{
			std::cout << "Goodbye!" << std::endl;
			this->m_server.stop_listening();
			this->m_server.stop();
		}
		else if (input == "users")
		{
			RequestHandlerFactory::getInstance()->getLoginManager().printUsers();
		}
		else if (input == "conn") 
		{
			std::cout << "Connections: " << this->m_clients.size() << std::endl;;
		}
		else if (input == "clear" || input == "clr")
		{
			system("CLS");
		}
		else
		{
			std::cout << "'" << input << "' is not recognized as an internal or external command." << std::endl;
		}
	}

	acceptingThread.join();
}

void Server::onOpen(websocketpp::connection_hdl hdl) 
{
	m_clients.insert(std::make_pair(hdl, new LoginRequestHandler()));
}

/**
 * on close websocket connection handler.
 *
 * the function make sure to dissconnect the client properly 
 * in a case of closed connection
 * 
 * logout
 * exit game - of inside one
 * remove from waitng queue - if inside
 */
void Server::onClose(websocketpp::connection_hdl hdl)
{
	RequestHandlerFactory::getInstance()->getLoginManager().logout(m_clients[hdl]->getUser()->getUserID());
	RequestHandlerFactory::getInstance()->getGameManager().removeFromQueue(m_clients[hdl]->getUser());
	if (GameRequestHandler* d_ptr = dynamic_cast<GameRequestHandler*>(m_clients[hdl]))
	{
		RequestInfo rr;
		rr.receivalTime = getCurrentTimeAsString();
		json j;
		j[JsonKeys::CODE] = 20;
		rr.msg = j;
		RequestResult r = d_ptr->handleRequest(rr);
		if (r.notification != nullptr)
		{
			sendNotification(r.notification);
		}
	}
	if (m_clients[hdl] != nullptr)
	{
		delete m_clients[hdl];
	}
	m_clients.erase(hdl);
}

void Server::sendMessage(websocketpp::connection_hdl hdl, RequestResult response)
{
	// convert json to str
	std::string data = response.msg.dump();
	std::cout << "Sending:" << data << std::endl;
	m_server.send(hdl, data, websocketpp::frame::opcode::text);
}

void Server::sendMessage(websocketpp::connection_hdl hdl, const std::string& message)
{

	std::cout << "Sending:" << message << std::endl;
	m_server.send(hdl, message, websocketpp::frame::opcode::text);

}

/**
 * parse string formated as json to json object
 * also saves the time of the reciving the message
 * 
 * @return RequestInfo - struct of json + time(string)
 */
RequestInfo Server::parseMsg(const std::string& receivedData)
{
	RequestInfo request;
	json jsonData;

	std::cout << "Received message from client: " << receivedData << std::endl;

	request.receivalTime = getCurrentTimeAsString();

	// convert response to json
	jsonData = json::parse(receivedData);
	request.msg = jsonData;
	return request;
}

/**
 * on message hadler
 * 
 * recive the messages from the clients 
 * and send it to the client current handler
 * return the result to the client
 * 
 * switch cliecnt between handler as needed
 * send notification to other clients when needed
 */
void Server::handleRequest(websocketpp::connection_hdl hdl, server::message_ptr msg)
{
	RequestResult response;
	try
	{
		std::vector<char> responceVector;
		

		IRequestHandler* currentHandler = m_clients[hdl];

		const std::string& message = msg->get_payload();
		RequestInfo request = parseMsg(message);

		if (!currentHandler->isRequestRelevant(request))
		{
			response.msg[JsonKeys::CODE] = ERROR_CODE;
			response.msg[JsonKeys::ERROR_MSG] = "Request not relevant here.";
		}
		else
		{
			response = currentHandler->handleRequest(request);

			if (response.newHandler != nullptr) // switch to new handler if not nullptr
			{
				delete currentHandler;
				m_clients[hdl] = response.newHandler;
			}
			if (response.notification != nullptr) // send notification if not nullptr
			{
				sendNotification(response.notification);
			}
		}
		sendMessage(hdl, response);
	}
	catch (const json::parse_error e)
	{
		response.msg[JsonKeys::CODE] = ERROR_CODE;
		response.msg[JsonKeys::ERROR_MSG] = "Request not in Json format.";
		sendMessage(hdl, response);
	}
	catch (const std::exception& e)
	{
		std::cout << "ERROR HANDLEING REQUEST " << e.what() << std::endl;
	}
}

/**
 * parse notification struct 
 * to string in json format
 *
 * @param notification the notification object to parse
 * @return the notification formated as json as string
 */
std::string Server::parseNotification(Notification* notification)
{
	json data;
	data[JsonKeys::CODE] = NOTIFICATION_CODE;
	data[JsonKeys::NOTIFICATION_ID] = notification->id;
	data[JsonKeys::NOTIFICATION_TYPE] = static_cast<NotificationType>(notification->type);
	data[JsonKeys::NOTIFICATION_SOURCE] = notification->srcID;
	data[JsonKeys::NOTIFICATION_TIME] = notification->time;
	data[JsonKeys::NOTIFICATION_CONTENT] = notification->content;
	return data.dump();
}

/*
Send notification to users, 
if the user is currently in menu send him the message,
if the user is offline or unable to recive new messages 
- save them in database so he could load them later
*/
/**
 * Send notification to users
 * 
 * if the user is currently in menu or game send him the message
 * if the user is offline or unable to recive new messages 
 * 
 * save notifications in the database
 * 
 * @param notification the notification object send
 */
void Server::sendNotification(Notification* notification)
{
	int id = 0;
	notification->time = getCurrentTimeAsString();
	// save notification in database
	if (notification->saveInDB)
	{
		id = this->m_database->saveNotification(*notification);
	}
	if (id != -1)
	{
		notification->id = id;
	}
	for (auto& it : this->m_clients)
	{
		// check if user is online
		if (notification->dstID == it.second->getUser()->getUserID())
		{
			// check if current handler is of type: MenuRequestHandler
			if (MenuRequestHandler* d_ptr = dynamic_cast<MenuRequestHandler*>(it.second))
			{
				sendMessage(it.first, parseNotification(notification));
				break;
			}
			else if (GameRequestHandler* d_ptr = dynamic_cast<GameRequestHandler*>(it.second))
			{
				sendMessage(it.first, notification->content);
				break;
			}
		}
	}
	delete notification;
}


/**
 * Save the current local time as string.
 *
 * @return the time formated as %d/%m/%y %H:%M:%S
 */
std::string Server::getCurrentTimeAsString()
{
	std::time_t currentTime = std::time(nullptr);
	std::tm localTime;

	// Use localtime_s to convert time to local time representation
	if (localtime_s(&localTime, &currentTime) != 0)
	{
		std::cerr << "Failed to convert time to local representation." << std::endl;
		return "NULL";
	}

	// Convert the time components to strings
	char timeString[18];  // HH:MM:SS + null terminator
	std::strftime(timeString, sizeof(timeString), "%d/%m/%y %H:%M:%S", &localTime);

	return timeString;
}