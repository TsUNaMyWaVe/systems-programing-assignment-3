package bgu.spl.net.srv;

import java.util.HashMap;
import java.util.Map.Entry;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.srv.bidi.ConnectionHandler;

public class ConnectionsImpl<T> implements Connections<T> {
	HashMap<Integer, ConnectionHandler<T>> connectionsMap;
	Integer index = 0;
	
	public ConnectionsImpl() {
		connectionsMap = new HashMap<Integer, ConnectionHandler<T>>();
	}
	
	public boolean send(int connectionId, T msg) {
		ConnectionHandler<T> connection = connectionsMap.get(connectionId);
		if(connection == null)
			return false;
		connection.send(msg);
		return true;
	}

	public void broadcast(T msg) {
		for(Entry<Integer, ConnectionHandler<T>> entry : connectionsMap.entrySet()) {
			entry.getValue().send(msg);
		}
	}

	public void disconnect(int connectionId) {
		connectionsMap.remove(connectionId);
	}

	public int connect(ConnectionHandler<T> newConnection) {
		synchronized(index)
		{
			index++;
			connectionsMap.put(index, newConnection);
			return index;
		}
	}
	
	public ConnectionHandler<T> getConnection(int connectionId) {
		return connectionsMap.get(connectionId);
	}

}
