#include "Game.h"

Game::Game(int id, std::shared_ptr<User> whiteUser, std::shared_ptr<User> blackUser):
    gameID(id), whitePlayer(whiteUser), blackPlayer(blackUser)
{
    this->whiteDraw = false;
    this->blackDraw = false;
    this->turn = WHITE;
    this->result = "";
    this->reasonForEnding = "";

    char initialBoard[8][8] = {
           {'0', 'b', '0', 'b', '0', 'b', '0', 'b'},
           {'b', '0', 'b', '0', 'b', '0', 'b', '0'},
           {'0', 'b', '0', 'b', '0', 'b', '0', 'b'},
           {' ', '0', ' ', '0', ' ', '0', ' ', '0'},
           {'0', ' ', '0', ' ', '0', ' ', '0', ' '},
           {'w', '0', 'w', '0', 'w', '0', 'w', '0'},
           {'0', 'w', '0', 'w', '0', 'w', '0', 'w'},
           {'w', '0', 'w', '0', 'w', '0', 'w', '0'}
    };
    // Copy initial board to board member variable
    for (int i = 0; i < 8; ++i) {
        for (int j = 0; j < 8; ++j) {
            this->board[i][j] = initialBoard[i][j];
        }
    }
}

Game::~Game()
{
}

std::shared_ptr<User> Game::getWhitePlayer()
{
    return this->whitePlayer;
}

std::shared_ptr<User> Game::getBlackPlayer()
{
    return this->blackPlayer;
}

int Game::getID()
{
    return this->gameID;
}

std::string Game::getTurnID()
{
    return this->turn == WHITE ? this->whitePlayer->getUserID() : this->blackPlayer->getUserID();
}

std::string Game::getOpponentId(const std::string userID)
{
    return userID == whitePlayer->getUserID() ? this->blackPlayer->getUserID() : this->whitePlayer->getUserID();
}

/*
create a json with data for starting the game

*/
json Game::getStartDetails(const std::string userID)
{
    json gameData;
    gameData[JsonKeys::CODE] = GAME_DETAILES;
    gameData[JsonKeys::GAMEID] = this->gameID;
    std::string playerColor = userID == whitePlayer->getUserID() ? "white" : "black";
    gameData[JsonKeys::PLAYER_COLOR] = playerColor;
    json opponentData;
    if (playerColor == "white")
    {
        opponentData[JsonKeys::USERNAME] = blackPlayer->getUsername();
        opponentData[JsonKeys::USERID] = blackPlayer->getUserID();
        opponentData[JsonKeys::RATING] = blackPlayer->getRating();
    }
    else
    {
        opponentData[JsonKeys::USERNAME] = whitePlayer->getUsername();
        opponentData[JsonKeys::USERID] = whitePlayer->getUserID();
        opponentData[JsonKeys::RATING] = whitePlayer->getRating();
    }
    gameData[JsonKeys::OPPONENT] = opponentData;
    gameData[JsonKeys::GAME_STATE] = this->getGameState();
    return gameData;
}

json Game::getGameState()
{
    json gameState;
    gameState[JsonKeys::BOARD] = this->getBoard();
    gameState[JsonKeys::TURN] = this->turn == WHITE ? "white" : "black";
    return gameState;
}

json Game::getBoard()
{
    json boardJson;

    for (int i = 0; i < 8; i++) 
    {
        for (int j = 0; j < 8; j++) 
        {
            boardJson["array"][i][j] = this->board[i][j];
        }
    }
    return boardJson;
}

json Game::getGameResult()
{
    json gameData;
    gameData[JsonKeys::CODE] = GAME_RESULT;
    gameData[JsonKeys::GAMEID] = this->gameID;
    gameData[JsonKeys::RESULT] = this->result;
    gameData[JsonKeys::END_REASON] = this->reasonForEnding;
    gameData[JsonKeys::GAME_STATE] = this->getGameState();
    return gameData;
}

bool Game::checkWin(const std::string userID)
{
    if (userID == whitePlayer->getUserID())
    {
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                if (this->board[i][j] == BLACK_SOLDIER || this->board[i][j] == BLACK_QUEEN)
                {
                    return false;
                }
            }
        }
        this->result = "1-0";
        this->reasonForEnding = "White Won!";
    }
    else
    {
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                if (this->board[i][j] == WHITE_SOLDIER || this->board[i][j] == WHITE_QUEEN)
                {
                    return false;
                }
            }
        }
        this->result = "0-1";
        this->reasonForEnding = "Black Won!";
    }
    return true;
}

bool Game::offerDraw(const std::string userID)
{
    if (userID == whitePlayer->getUserID())
    {
        this->whiteDraw = !this->whiteDraw;
    }
    else
    {
        this->blackDraw = !this->blackDraw;
    }

    if (this->whiteDraw && this->blackDraw)
    {
        this->result = "1/2-1/2";
        this->reasonForEnding = "Accepted Draw";
        return true;
    }
    return false;
}

void Game::leaveGame(const std::string userID)
{
    if (userID == this->whitePlayer->getUserID())
    {
        this->result = "0-1";
    }
    else
    {
        this->result = "1-0";
    } 
    this->reasonForEnding = "Resignation";
}

void Game::updateMove(char pieceMove, Position source, Position dest, Position middle)
{
    board[source.row][source.column] = ' ';
    board[dest.row][dest.column] = pieceMove;

    if (middle.row != -1 && middle.column != -1) // capture
    {
        board[middle.row][middle.column] = ' ';
    }
    // check premotions
    if (pieceMove == WHITE_SOLDIER && dest.row == 0)
    {
        board[dest.row][dest.column] = WHITE_QUEEN;
    } 
    else if (pieceMove == BLACK_SOLDIER && dest.row == 7)
    {
        board[dest.row][dest.column] = BLACK_QUEEN;
    }
}

/*
play the one move
input: userID of the playing player, source Position, dest Position, step and if chain
output: true/false if the move is valid + update the board accurding to the move
*/
bool Game::playMove(std::string playerID, Position source, Position dest, int step, bool isChain)
{
    char piece = this->board[source.row][source.column];
    char destination = this->board[dest.row][dest.column];

    // check turn
    if (playerID != this->getTurnID()) 
    {
        return false;
    }
    
    // Piece is in the player turn color
    if (this->turn)
    {
        if (piece != WHITE_SOLDIER && piece != WHITE_QUEEN)
        {
            return false;
        }
    }
    else
    {
        if (piece != BLACK_SOLDIER && piece != BLACK_QUEEN) 
        {
            return false;
        }
    }

    // Destination must be empty
    if (destination != EMPTY_SQUARE) 
    {
        return false; 
    }


    // Check if the move is diagonal (row difference equals column difference)
    int rowDiff = abs(dest.row - source.row);
    int colDiff = abs(dest.column - source.column);
    if (rowDiff != colDiff) 
    {
        return false;
    }

    // Check the direction of the move of soldiers
    if (step == 0)
    {
        if (piece == WHITE_SOLDIER && dest.row > source.row) // White pieces can only move up
        {
            return false;
        }
        if (piece == BLACK_SOLDIER && dest.row < source.row) // Black pieces can only move down
        {
            return false;
        }
    }
    if (!isChain) // chain moves have to be captures
    {
        // Simple move
        if (rowDiff == 1)
        {
            updateMove(piece, source, dest, { -1, -1 });
            return true;
        }
    }
   
    // Capture move
    if (rowDiff == 2) 
    {
        int midRow = (source.row + dest.row) / 2;
        int midCol = (source.column + dest.column) / 2;
        char middlePiece = this->board[midRow][midCol];

        // Check if there is an opponent piece to capture
        if (((piece == WHITE_SOLDIER || piece == WHITE_QUEEN) && (middlePiece == BLACK_SOLDIER || middlePiece == BLACK_QUEEN)) || 
            ((piece == BLACK_SOLDIER || piece == BLACK_QUEEN) && (middlePiece == WHITE_SOLDIER || middlePiece == WHITE_QUEEN)))
        {
            updateMove(piece, source, dest, { midRow, midCol });
            return true;
        }
    }

    // Check queen move
    if (piece == WHITE_QUEEN || piece == BLACK_QUEEN)
    {
        int stepHorizontal = source.column < dest.column ? 1 : -1;
        int stepVertical = source.row < dest.row ? 1 : -1;
        Position curr;
        curr.column = source.column + stepHorizontal;
        curr.row = source.row + stepVertical;
        char currPiece;
        Position enemy = { -1, -1 };

        for (int i = 1; i <= rowDiff; i++)
        {
            currPiece = this->board[curr.row][curr.column];
            if (currPiece != EMPTY_SQUARE)
            {
                // queen can only move one square after the capture
                if (i == rowDiff - 1 &&
                    ((piece == WHITE_QUEEN && (currPiece == BLACK_SOLDIER || currPiece == BLACK_QUEEN)) ||
                    (piece == BLACK_QUEEN && (currPiece == WHITE_SOLDIER || currPiece == WHITE_QUEEN))))
                {
                    enemy = curr;
                }
                else
                {
                    return false;
                }
            }
            curr.column += stepHorizontal;
            curr.row += stepVertical;
        }
        if (isChain) // chain moves have to be captures
        {
            if (enemy.row == -1)
            {
                return false;
            }
        }
        updateMove(piece, source, dest, enemy);
        return true;
    }
    return false;
}

// json position => Position
Position Game::parseMove(json move)
{
    Position p;
    p.row = move[JsonKeys::ROW];
    p.column = move[JsonKeys::COL];
    return p;
}

/*
play the moves
input: userID of the playing player, json Array of moves to play
output: true/false if the move is valid + update the board accurding to the move
*/
bool Game::playMoves(std::string playerID, json movesArray)
{
    Position src = { -1, -1 };
    Position dst = { -1, -1 };
    char saveBoard[8][8] = { 0 }; // save current board
    copyBoards(saveBoard, this->board);

    bool isChain = movesArray.size() > 1;
    // check if the move sequance is valid
    int i = 0;
    for (const auto& move : movesArray)
    {
        src = parseMove(move[JsonKeys::SRC_POS]);
        if (dst.row != -1) // check if continue from last destenation
        {
            if (src.row != dst.row || src.column != dst.column)
            {
                copyBoards(this->board, saveBoard); // retrive board 
                return false;
            }
        }
        dst = parseMove(move[JsonKeys::DST_POS]);

        if (!playMove(playerID, src, dst, i, isChain))
        {
            copyBoards(this->board, saveBoard); // retrive board
            return false;
        }
        i++;
    }
    // switch turns
    this->turn = this->turn == WHITE ? BLACK : WHITE; 
    return true;
}

/*
copy the board to another board
*/
void Game::copyBoards(char destBoard[8][8], char sourceBoard[8][8])
{
    for (int i = 0; i < 8; ++i)
    {
        for (int j = 0; j < 8; ++j)
        {
            destBoard[i][j] = sourceBoard[i][j];
        }
    }
}


/*
check for all of the opponent pieces if they have any moves
*/
bool Game::chekForceDraw(const std::string userID)
{
    // save current board
    char saveBoard[8][8] = { 0 }; 
    copyBoards(saveBoard, this->board);
    for (int i = 0; i < 8; i++)
    {
        for (int j = 0; j < 8; j++)
        {
            if (userID == whitePlayer->getUserID())
            {
                if (this->board[i][j] == BLACK_SOLDIER || this->board[i][j] == BLACK_QUEEN)
                {
                    for (int k = 0; k < 8; k++)
                    {
                        for (int l= 0; l < 8; l++)
                        {
                            if (playMove(getOpponentId(userID), { i, j }, { k, l }, 0, false))
                            {
                                copyBoards(this->board, saveBoard);
                                return false;
                            }
                            copyBoards(this->board, saveBoard);
                        }
                    }
                }
            }
            else
            {
                if (this->board[i][j] == WHITE_SOLDIER || this->board[i][j] == WHITE_QUEEN)
                {
                    for (int k = 0; k < 8; k++)
                    {
                        for (int l = 0; l < 8; l++)
                        {
                            if (playMove(getOpponentId(userID), { i, j }, { k, l }, 0, false))
                            {
                                copyBoards(this->board, saveBoard);
                                return false;
                            }
                            copyBoards(this->board, saveBoard);
                        }
                    }
                }
            }
        }
    } 
    this->result = "1/2-1/2";
    this->reasonForEnding = "Force Draw";
    return true;
}