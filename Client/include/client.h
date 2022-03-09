/*
 * client.h
 *
 *  Created on: Jan 4, 2019
 *      Author: user
 */

#ifndef CLIENT_H_
#define CLIENT_H_

typedef enum COMMANDS
{
	e_REGISTER = 1,
	e_LOGIN,
	e_LOGOUT,
	e_FOLLOW,
	e_POST,
	e_PM,
	e_USERLIST,
	e_STAT,
	e_NOTIFICATION,
	e_ACK,
	e_ERROR,
	e_INVALID
}COMMANDS;

class CClient
{
private:
	std::string m_host;
	short m_port;
	bool m_run;

	void split(const std::string& str, const std::string& delim, std::vector<std::string>& parts);
    ConnectionHandler* m_connectionHandler;

    short bytesToShort(char* bytesArr);
    void shortToBytes(short num, char* bytesArr);


public:
    CClient(std::string host, short port);
    int init();
	int getServerCommand();
	int getUsrCommand();
	bool &isRun(){return m_run;}

};


#endif /* CLIENT_H_ */
