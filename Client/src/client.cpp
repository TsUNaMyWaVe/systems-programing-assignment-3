#include <stdlib.h>
#include <iostream>
#include <thread>
#include <connectionHandler.h>
#include <client.h>

class Task { //The thread that will listen to the server
private:
    CClient *pClient;
public:
    Task (CClient *client) : pClient(client) {}

    void operator()(){
    	while(1)
    	{
    		int rc;
    		rc = pClient->getServerCommand();
    		if(rc < 0)
    			break;
    	}
    }
};

void CClient::split(const std::string& str, const std::string& delim, std::vector<std::string>& parts) { //Splitting a string
  size_t start, end = 0;
  while (end < str.size()) {
    start = end;
    while (start < str.size() && (delim.find(str[start]) != std::string::npos)) {
      start++;  // skip initial whitespace
    }
    end = start;
    while (end < str.size() && (delim.find(str[end]) == std::string::npos)) {
      end++; // skip to end of word
    }
    if (end-start != 0) {  // just ignore zero-length strings.
      parts.push_back(std::string(str, start, end-start));
    }
  }
}
int CClient::getServerCommand() //Decoding the server messages
{
    const short bufsize = 1024;
    char buf2read[bufsize];

    if (!m_connectionHandler->getBytes(buf2read, 2)) { //Reading the first 2 bytes (opcode)
        return -1;
    }

    short cmd = bytesToShort(buf2read); //The opcode

    switch(cmd)
    {
		case e_ERROR:
		{
		    if (!m_connectionHandler->getBytes(&buf2read[2], 2)) { //Reading the error opcode
		        return -1;
		    }
			short opcode = bytesToShort(&buf2read[2]);
			std::cout << "ERROR " << opcode << std::endl;
			break;
		}
		case e_NOTIFICATION:
		{
		    if (!m_connectionHandler->getBytes(&buf2read[2], 1)) { //Reading the type (PM/Public)
		        return -1;
		    }
			char type = buf2read[2];
			std::string userName;
			std::string content;
			m_connectionHandler->getFrameAscii(userName, 0); //Reading the name
			userName = userName.substr(0, userName.length()-1); //Removing the last 0 byte
			m_connectionHandler->getFrameAscii(content, 0); //Reading the content
			std::string msg = userName + " " + content;
			if(type == 0)
				std::cout << "NOTIFICATION PM " << msg << std::endl;
			else
				std::cout << "NOTIFICATION Public " << msg << std::endl;
			break;
		}
		case e_ACK:
		{
		    if (!m_connectionHandler->getBytes(&buf2read[2], 2)) { //Getting the ACK opcode
		        return -1;
		    }
			short opcode = bytesToShort(&buf2read[2]);

			switch(opcode)
			{
				case e_REGISTER:
				case e_LOGIN:
				case e_POST:
				case e_PM:
					std::cout << "ACK " << opcode << std::endl;
				break;
				case e_LOGOUT:
				{
					std::cout << "ACK " << opcode << std::endl;
					m_connectionHandler->close(); //Closing the socket
					m_run = false;
					delete m_connectionHandler;
					exit(0);
				}
				break;
				case e_FOLLOW:
				{
					if (!m_connectionHandler->getBytes(&buf2read[4], 2)) { //Reading the number of users that were successfully followed/unfollowed
						return -1;
					}
					short numOfUsers = bytesToShort(&buf2read[4]);
					std::string name;
					for (int i = 0; i < numOfUsers; i++) { //Reading the usernames
						m_connectionHandler->getFrameAscii(name, 0);
						name = name.substr(0, name.length()-1);
						name += " ";
					}
					std::cout << "ACK " << opcode << " " << numOfUsers << " " << name << std::endl;
					break;
				}
				case e_STAT:
				{
					m_connectionHandler->getBytes(&buf2read[0], 2); //Reading the number of posts
					short numOfPosts = bytesToShort(&buf2read[0]);
					m_connectionHandler->getBytes(&buf2read[0], 2); //Reading the number of followers
					short numOfFollowers = bytesToShort(&buf2read[0]);
					m_connectionHandler->getBytes(&buf2read[0], 2); //Reading the number of following
					short numOfFollowing = bytesToShort(&buf2read[0]);
					std::cout << "ACK " << opcode << " " << numOfPosts << " " << numOfFollowers << " " << numOfFollowing << std::endl;
					break;
				}
				case e_USERLIST:
				{
					if(!m_connectionHandler->getBytes(&buf2read[0], 2)) { //Reading the number of users
						return -1;
					}
					short numOfUsers = bytesToShort(&buf2read[0]);
					std::string name;
					for (int i = 0; i < numOfUsers; i++) { //Reading the user names
						m_connectionHandler->getFrameAscii(name, 0);
						name = name.substr(0, name.length()-1);
						name += " ";
					}
					std::cout << "ACK " << opcode << " " << numOfUsers << " " << name << std::endl;
					break;
				}
			}
			break;
		}
    }

    return 0;
}

int CClient::getUsrCommand() //Encoding user commands
{
    const short bufsize = 1024;
    char buf[bufsize];
    char buf2send[bufsize];

    std::vector<std::string> parts;
    std::cin.getline(buf, bufsize);
	std::string line(buf);
	int len=line.length();

	split(line, " ", parts);

	if(parts[0] == "REGISTER" || parts[0] == "LOGIN" || parts[0] == "PM")
	{
		len = 0;
		if(parts[0] == "REGISTER") //Adding the opcode according to command
		{
			shortToBytes((short)e_REGISTER, &buf2send[len]);
		}
		else if(parts[0] == "PM")
		{
			shortToBytes((short)e_PM, &buf2send[len]);
		}
		else
		{
			shortToBytes((short)e_LOGIN, &buf2send[len]);
		}

		len+=sizeof(short);
		strcpy(&buf2send[len],parts[1].c_str()); //Adding the first data part
		len+=parts[1].length();
		buf2send[len] = 0; //Adding 0 byte
		len+=sizeof(char);
		for (unsigned int i = 2; i < parts.size(); i++) //Adding the second data part
		{
			strcpy(&buf2send[len],parts[i].c_str());
			len+=parts[i].length();
			buf2send[len] = ' ';
			len+=sizeof(char);
		}
		buf2send[len] = 0; //Adding 0 byte
		len+=sizeof(char);
	}
	else if(parts[0] == "LOGOUT" || parts[0] == "USERLIST")
	{
		len = 0;
		if (parts[0] == "LOGOUT") //Adding the opcode according to command
			shortToBytes((short)e_LOGOUT, &buf2send[len]);
		else
			shortToBytes((short)e_USERLIST, &buf2send[len]);
		len+=sizeof(short);
	}
	else if(parts[0] == "FOLLOW")
	{
		len = 0;
		shortToBytes((short)e_FOLLOW, &buf2send[len]); //Adding the opcode
		len+=sizeof(short);
		buf2send[len] = atoi(parts[1].c_str()); //Adding the type (follow/unfollow)
		len+=sizeof(char);
		short num = atoi(parts[2].c_str()); //Adding number of users
		shortToBytes(num, &buf2send[len]);
		len+=sizeof(short);
		for(short i=0; i<num; i++) //Adding usernames
		{
			strcpy(&buf2send[len],parts[3+i].c_str());
			len+=parts[3+i].length();
			buf2send[len] = 0; //Adding 0 byte
			len+=sizeof(char);
		}
	}
	else if(parts[0] == "POST" || parts[0] == "STAT")
	{
		len = 0;
		if(parts[0] == "POST") //Adding the opcode according to command
			shortToBytes((short)e_POST, &buf2send[len]);
		else
			shortToBytes((short)e_STAT, &buf2send[len]);
		len+=sizeof(short);

		for (unsigned int i = 1; i < parts.size(); i++) //Adding the second data part
		{
			strcpy(&buf2send[len],parts[i].c_str());
			len+=parts[i].length();
			buf2send[len] = ' ';
			len+=sizeof(char);
		}
		buf2send[len] = 0; //Adding 0 byte
		len+=sizeof(char);
	}
	else
	{
		std::cout << "Error - Invalid command\n" << std::endl;
		return -1;
	}

    if (!m_connectionHandler->sendBytes(buf2send, len)) {
        return -2;
    }
	return 0;
}

CClient::CClient(std::string host, short port): //Constructor
	m_host(host),
	m_port(port),
	m_run(true),
	m_connectionHandler(new ConnectionHandler(host, port))
{

}

int CClient::init()
{

    if (!m_connectionHandler->connect()) {
        std::cerr << "Cannot connect to " << m_host << ":" << m_port << std::endl;
        return -1;
    }

	return 0;
}

short CClient::bytesToShort(char* bytesArr)
{
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}

void CClient::shortToBytes(short num, char* bytesArr)
{
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}


//Main thread - reading from keyboard //
int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    
    CClient client(host, port); //Creating the client

    if(client.init() < 0)
    	return 0;

    Task task1(&client); //Creating the reading from server thread

    std::thread th1(std::ref(task1)); // Running it

	//Reading from keyboard
    while (client.isRun()) {
    int rc;

    	rc = client.getUsrCommand();

    	if(rc == -2)
    		break;

        
    }
    th1.join();

    return 0;
}


