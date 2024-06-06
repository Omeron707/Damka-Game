#pragma once
#include <iostream>
#include <ctime>
#include <vector>
#include "JsonHelper.h"
#include "User.h"
#include "SqliteDataBase.h"

class IRequestHandler;

typedef struct RequestInfo
{
	json msg;
	std::string receivalTime;
}RequestInfo;

typedef struct RequestResult
{
	json msg;
	Notification* notification;
	IRequestHandler* newHandler;
}RequestResult;

class IRequestHandler
{
	public:
		IRequestHandler();
		virtual ~IRequestHandler();

		virtual bool isRequestRelevant(RequestInfo request) = 0;
		virtual RequestResult handleRequest(RequestInfo request) = 0;
		virtual std::shared_ptr<User> getUser() = 0;
};