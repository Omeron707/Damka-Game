#pragma once
#include <iostream>
#include "User.h"
#include "JsonHelper.h"
#include "Constants.h"

struct Position {
	int row;
	int column;
};

class Game
{
	public:
		Game(int id, std::shared_ptr<User> whiteUser, std::shared_ptr<User> blackUser);
		~Game();

		std::shared_ptr<User> getWhitePlayer();
		std::shared_ptr<User> getBlackPlayer();
		json getStartDetails(const std::string userID);
		json getGameState();
		json getBoard();
		json getGameResult();
		std::string getOpponentId(const std::string userID);
		int getID();

		bool offerDraw(const std::string userID);
		void leaveGame(const std::string userID);
		bool playMove(std::string playerID, Position source, Position dest, int step, bool isChain);
		bool playMoves(std::string playerID, json movesArray);
		bool chekForceDraw(const std::string userID);
		bool checkWin(const std::string userID);

	private:
		void updateMove(char pieceMove, Position source, Position dest, Position middle);
		Position parseMove(json move);
		void copyBoards(char destBoard[8][8], char sourceBoard[8][8]);
		std::string getTurnID();

		int gameID;
		std::string result;
		std::string reasonForEnding;
		std::shared_ptr<User> whitePlayer;
		std::shared_ptr<User> blackPlayer;
		bool whiteDraw;
		bool blackDraw;
		char board[8][8];
		int turn;
};