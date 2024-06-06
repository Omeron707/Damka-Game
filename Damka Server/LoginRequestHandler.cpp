#include "LoginRequestHandler.h"

#include "Constants.h"


LoginRequestHandler::LoginRequestHandler():
	m_user(nullptr)
{
}

LoginRequestHandler::~LoginRequestHandler()
{
}

std::shared_ptr<User> LoginRequestHandler::getUser()
{
	return this->m_user;
}

/*
check if the fields necessary for each operation exist in the json
*/
bool LoginRequestHandler::isRequestRelevant(RequestInfo request)
{
	if (request.msg[JsonKeys::CODE] == LOGIN_CODE)
	{
		return request.msg.contains(JsonKeys::MAIL) && request.msg.contains(JsonKeys::PASSWORD);
	}
	else if (request.msg[JsonKeys::CODE] == SIGN_UP_CODE)
	{
		return request.msg.contains(JsonKeys::USERNAME) && request.msg.contains(JsonKeys::MAIL) && request.msg.contains(JsonKeys::PASSWORD) && request.msg.contains(JsonKeys::BIRTHDATE);
	}
	return false;
}

/*
handle the request from the client
*/
RequestResult LoginRequestHandler::handleRequest(RequestInfo request)
{
	RequestResult response;
	std::string username;
	try
	{
		if (request.msg[JsonKeys::CODE] == LOGIN_CODE)
		{
			response = this->login(request);
		}
		else
		{
			response = this->signup(request);
		}
		response.newHandler = new MenuRequestHandler(this->m_user);
	}
	catch (LoginException s)
	{
		RequestResult er;
		er.msg[JsonKeys::CODE] = ERROR_CODE;
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
	response.notification = nullptr;
	return response;
}

RequestResult LoginRequestHandler::login(RequestInfo request)
{
	LoginManager& manager = RequestHandlerFactory::getInstance()->getLoginManager();

	json userData = manager.login(request.msg[JsonKeys::MAIL], request.msg[JsonKeys::PASSWORD]);

	m_user = manager.findUser(userData[JsonKeys::USERID]);

	RequestResult res;
	res.msg[JsonKeys::CODE] = SUCCESS;
	res.msg[JsonKeys::USERID] = userData[JsonKeys::USERID];
	res.msg[JsonKeys::USERNAME] = userData[JsonKeys::USERNAME];
	res.msg[JsonKeys::RATING] = userData[JsonKeys::RATING];
	res.msg[JsonKeys::MAIL] = userData[JsonKeys::MAIL];
	res.msg[JsonKeys::PRIVACY] = userData[JsonKeys::PRIVACY];
	
	return res;
}

RequestResult LoginRequestHandler::signup(RequestInfo request)
{	
	LoginManager& manager = RequestHandlerFactory::getInstance()->getLoginManager();
	
	m_user = manager.signup(request.msg[JsonKeys::USERNAME], request.msg[JsonKeys::PASSWORD], request.msg[JsonKeys::MAIL], request.msg[JsonKeys::BIRTHDATE]);

	RequestResult res;
	res.msg[JsonKeys::CODE] = SUCCESS;
	res.msg[JsonKeys::USERID] = m_user->getUserID();
	res.msg[JsonKeys::USERNAME] = m_user->getUsername();
	res.msg[JsonKeys::RATING] = m_user->getRating();
	res.msg[JsonKeys::PRIVACY] = false;
	res.msg[JsonKeys::MAIL] = request.msg[JsonKeys::MAIL];
	
	return res;
}