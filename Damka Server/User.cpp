#include "User.h"

User::User(std::string username, std::string id, double rating) :
	username(username), userID(id), rating(rating)
{
}

User::~User()
{
}

std::string User::getUsername() const
{
	return this->username;
}

std::string User::getUserID() const
{
	return this->userID;
}

double User::getRating() const
{
	return this->rating;
}

void User::setUsername(const std::string newUsername)
{
	this->username = newUsername;
}

void User::setRating(const double rating)
{
	this->rating = rating;
}

bool User::operator== (const User& user)
{
	return this->getUserID() == user.getUserID();
}
