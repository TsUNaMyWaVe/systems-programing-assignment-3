CFLAGS:=-c -Wall  -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lpthread

all: Client
	g++ -o bin/BGSclient bin/connectionHandler.o bin/client.o $(LDFLAGS) 

Client: bin/connectionHandler.o bin/client.o
	
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp

bin/client.o: src/client.cpp
	g++ $(CFLAGS) -o bin/client.o src/client.cpp
	
.PHONY: clean
clean:
	rm -f bin/*
