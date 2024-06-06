#include "GameManager.h"
#include <cstdlib>
#include <ctime>
#include <unordered_set>
#include <algorithm>

GameManager::GameManager(SqliteDataBase* db):
	db(db)
{
}

GameManager::~GameManager()
{
}

int GameManager::generateNewId()
{
	std::unordered_set<int> existingIds;
	// Collect all existing IDs
	for (const auto& it : this->m_games) 
	{
		existingIds.insert(it->getID());
	}
	for (const auto& it : this->m_friendsWaiting) 
	{
		existingIds.insert(it.first);
	}

	// Find the smallest unused ID
	int newId = 1;
	while (existingIds.find(newId) != existingIds.end())
	{
		newId++;
	}

	return newId;
}

std::shared_ptr<Game> GameManager::getGame(int id)
{
	for (const auto& it : this->m_games)
	{
		if (id == it->getID())
		{
			return it;
		}
	}
	return nullptr;
}

std::shared_ptr<Game> GameManager::createGame(std::shared_ptr<User> user1, std::shared_ptr<User> user2, int gameID)
{
	std::shared_ptr<Game> game = nullptr;
	std::srand(std::time(nullptr));
	if (gameID == 0)
	{
		gameID = generateNewId();
	}
	if (std::rand() % 2 == 0)
	{
		game = std::shared_ptr<Game>(new Game(gameID, user1, user2));
	}
	else
	{
		game = std::shared_ptr<Game>(new Game(gameID, user2, user1));
	}
	this->db->changeOnline(user1->getUserID(), IN_MATCH);
	this->db->changeOnline(user2->getUserID(), IN_MATCH);
	std::lock_guard<std::mutex> lock(gameMutex);
	this->m_games.push_back(game);
	return game;
}

std::shared_ptr<Game> GameManager::findMatch(std::shared_ptr<User> user)
{
	std::lock_guard<std::mutex> lock(usersMutex);
	if (this->m_watingQueue.empty())
	{
		this->m_watingQueue.push_back(user);
		return nullptr;
	}
	std::shared_ptr<User> waitingUser = this->m_watingQueue.front();
	this->m_watingQueue.pop_front();
	return this->createGame(user, waitingUser, 0);

}

int GameManager::inviteMatch(std::shared_ptr<User> user, int gameID)
{
	std::lock_guard<std::mutex> lock(usersMutex);
	if (gameID == 0)
	{
		gameID = this->generateNewId();
		this->m_friendsWaiting.insert(std::pair<int, std::shared_ptr<User>>(gameID, user));
	}
	else
	{
		const auto& it = this->m_friendsWaiting.find(gameID);
		if (it != this->m_friendsWaiting.end())
		{
			this->createGame(user, it->second, gameID);
		}
		else
		{
			gameID = -1;
		}
	}
	return gameID;
}

bool GameManager::isAvilableForMatch(const std::string userID)
{
	return this->db->getOnline(userID) == ONLINE;
}

/*
calculate the new rating based on the elo system
*/
double GameManager::calculateExpectedScore(double userRating, double opponentRating)
{
	return  1.0 / (1.0 + pow(10, (opponentRating - userRating) / 400.0));
}
double GameManager::calcNewRating(double userRating, double opponentRating, double result)
{
	double expectedScore = calculateExpectedScore(userRating, opponentRating);

	return userRating + K_VALUE * (result - expectedScore);
}

void GameManager::endGame(int id)
{
	int i = 0;
	for (auto& it : this->m_games)
	{
		if (it->getID() == id)
		{
			std::shared_ptr<User> whitePlayer = it->getWhitePlayer();
			std::shared_ptr<User> blackPlayer = it->getBlackPlayer();
			// update statistics
			json result = it->getGameResult();
			bool isWhiteWinner = result[JsonKeys::RESULT] == "1-0";
			bool isBlackWinner = result[JsonKeys::RESULT] == "0-1";

			double whiteNewRating;
			double blackNewRating;

			if (isWhiteWinner)
			{
				whiteNewRating = calcNewRating(whitePlayer->getRating(), blackPlayer->getRating(), 1);
				blackNewRating = calcNewRating(blackPlayer->getRating(), whitePlayer->getRating(), 0);
				this->db->updateUserStats(whitePlayer->getUserID(), whiteNewRating, true, false, false);
				this->db->updateUserStats(blackPlayer->getUserID(), blackNewRating, false, true, false);
			} 
			else if (isBlackWinner)
			{
				whiteNewRating = calcNewRating(whitePlayer->getRating(), blackPlayer->getRating(), 0);
				blackNewRating = calcNewRating(blackPlayer->getRating(), whitePlayer->getRating(), 1);
				this->db->updateUserStats(whitePlayer->getUserID(), whiteNewRating, false, true, false);
				this->db->updateUserStats(blackPlayer->getUserID(), blackNewRating, true, false, false);
			}
			else
			{
				whiteNewRating = calcNewRating(whitePlayer->getRating(), blackPlayer->getRating(), 0.5);
				blackNewRating = calcNewRating(blackPlayer->getRating(), whitePlayer->getRating(), 0.5);
				this->db->updateUserStats(whitePlayer->getUserID(), whiteNewRating, false, false, true);
				this->db->updateUserStats(blackPlayer->getUserID(), blackNewRating, false, false, true);
			}

			whitePlayer->setRating(whiteNewRating);
			blackPlayer->setRating(blackNewRating);

			// change user state to online
			if (this->db->getOnline(whitePlayer->getUserID()) == IN_MATCH)
			{
				this->db->changeOnline(whitePlayer->getUserID(), ONLINE);
			}
			if (this->db->getOnline(blackPlayer->getUserID()) == IN_MATCH)
			{
				this->db->changeOnline(blackPlayer->getUserID(), ONLINE);
			}
			std::lock_guard<std::mutex> lock(gameMutex);
		    this->m_games.erase(this->m_games.begin() + i);
			break;
		}
		i++;
	}
}

void GameManager::removeFromQueue(std::shared_ptr<User> user)
{
	std::lock_guard<std::mutex> lock(usersMutex);
	// remove from normal waiting queue
	for (auto it = this->m_watingQueue.begin(); it != this->m_watingQueue.end(); ++it) 
	{
		if (*it == user) 
		{
			this->m_watingQueue.erase(it);
			break;
		}
	}
	// remove from friends waiting queue
	for (auto it = this->m_friendsWaiting.begin(); it != this->m_friendsWaiting.end(); ++it)
	{
		if (it->second == user)
		{
			this->m_friendsWaiting.erase(it);
			break;
		}
	}
}