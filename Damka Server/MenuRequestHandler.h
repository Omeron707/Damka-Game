#pragma once
#include "IRequestHandler.h"

class RequestHandlerFactory;

class MenuRequestHandler : public IRequestHandler
{
	public:
	MenuRequestHandler(std::shared_ptr<User> user);
	virtual ~MenuRequestHandler();

	virtual bool isRequestRelevant(RequestInfo request) override;
	virtual RequestResult handleRequest(RequestInfo request) override;
	virtual std::shared_ptr<User> getUser() override;

	private:
		RequestResult logout(RequestInfo request);
		RequestResult addFriend(RequestInfo request);
		RequestResult acceptFriend(RequestInfo request);
		RequestResult denyFriend(RequestInfo request);
		RequestResult removeFriend(RequestInfo request);
		RequestResult getFriends(RequestInfo request);
		RequestResult getUserProfile(RequestInfo request);
		RequestResult getNotifications(RequestInfo request);
		RequestResult markNotificationRead(RequestInfo request);
		RequestResult getLeaderboard(RequestInfo request);
		RequestResult updateUserData(RequestInfo request);
		RequestResult findMatch(RequestInfo request);
		RequestResult InviteFriendForMatch(RequestInfo request);
		RequestResult leaveMatch(RequestInfo request);
		RequestResult readyForMatch(RequestInfo request);

		std::shared_ptr<User> m_user;
};