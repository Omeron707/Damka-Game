#include <iostream>
#include "Server.h"


int main() 
{
	try
	{
		Server s;
		s.run();
	}
	catch (LoginException e) 
	{
		std::cout << "Failed to start" << std::endl;
	}

	std::cout << "Done exiting." << std::endl;
	return 0;
}