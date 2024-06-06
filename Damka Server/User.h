#pragma once
#include <iostream>
#include <string.h>

class User
{
public:
	User(std::string username, std::string id, double rating);
	~User();

	std::string getUsername() const;
	std::string getUserID() const;
	double getRating() const;
	void setUsername(const std::string newUsername);
	void setRating(const double rating);
	bool operator== (const User& user);
	

private:
	std::string username;
	std::string userID;
	double rating;
};