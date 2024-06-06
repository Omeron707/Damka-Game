#pragma once
#include "RequestHandlerFactory.h"
#include "IRequestHandler.h"
#include "LoginManager.h"
#include "User.h"

class RequestHandlerFactory;

class LoginRequestHandler : public IRequestHandler
{
	public:
		LoginRequestHandler();
		virtual ~LoginRequestHandler();

		virtual bool isRequestRelevant(RequestInfo request) override;
		virtual RequestResult handleRequest(RequestInfo request) override;
		virtual std::shared_ptr<User> getUser() override;

	private:
		RequestResult login(RequestInfo);
		RequestResult signup(RequestInfo);
		std::shared_ptr<User> m_user;
};
