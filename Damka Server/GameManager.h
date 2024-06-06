#pragma once
#include <iostream>
#include <queue>
#include <deque>
#include "User.h"
#include "SqliteDataBase.h"
#include "Game.h"

class GameManager
{
	public:
		GameManager(SqliteDataBase* db);
		~GameManager();
		std::shared_ptr<Game> findMatch(std::shared_ptr<User> user);
		std::shared_ptr<Game> getGame(int id);
		int inviteMatch(std::shared_ptr<User> user, int gameID);
		bool isAvilableForMatch(const std::string userID);
		std::shared_ptr<Game> createGame(std::shared_ptr<User> user1, std::shared_ptr<User> user2, int gameID);
		void endGame(int id);
		void removeFromQueue(std::shared_ptr<User> user);
		double calculateExpectedScore(double userRating, double opponentRating);
		double calcNewRating(double userRating, double opponentRating, double result);
	private:
		int generateNewId();
		std::vector<std::shared_ptr<Game>> m_games;
		std::deque<std::shared_ptr<User>> m_watingQueue;
		std::mutex gameMutex;
		std::mutex usersMutex;
		std::map<int, std::shared_ptr<User>> m_friendsWaiting;
		SqliteDataBase* db;
};