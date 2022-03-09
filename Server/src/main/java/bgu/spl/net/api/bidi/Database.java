package bgu.spl.net.api.bidi;

import java.util.HashMap;
import java.util.Map.Entry;

public class Database {
	private HashMap<String, User> users;
	
	public Database() {
		users = new HashMap<>();
	}
	
	public boolean isRegistered(String username) {
		return users.containsKey(username);
	}
	
	public void addUser(User user) {
		users.put(user.getUsername(), user);
	}
	
	public User getUser(String username) {
		if (!isRegistered(username))
			return null;
		return users.get(username);
	}
	
	public int getNumOfUsers() {
		return users.size();
	}
	
	public String usersList() {
		String output = "";
		for(Entry<String, User> entry : users.entrySet()) {
			output = output + entry.getKey() + " ";
		}
		return output;
	}

}
