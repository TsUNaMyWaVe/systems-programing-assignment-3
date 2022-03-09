package bgu.spl.net.api.bidi;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import bgu.spl.net.srv.ConnectionsImpl;

public class BidiMessagingProtocolImpl implements BidiMessagingProtocol<String> {
	private Database database;
	private int connectionId;
	private ConnectionsImpl<String> connections;
	private boolean shouldTerminate;
	private User user;
	
	public BidiMessagingProtocolImpl(Database database) {
		this.database = database;
	}
	
	public void start(int connectionId, Connections<String> connections) {
		this.connectionId = connectionId;
		this.connections = (ConnectionsImpl<String>) connections;
		shouldTerminate = false;
		user = null;
	}
	
	public void process(String message) {
		String[] split = message.split(" ");
		if (split[0].compareTo("1") == 0)
			Register(split);
		else if (split[0].compareTo("2") == 0)
			Login(split);
		else if (split[0].compareTo("3") == 0)
			Logout(split);
		else if (split[0].compareTo("4") == 0)
			Follow(split);
		else if (split[0].compareTo("5") == 0)
			Post(split);
		else if (split[0].compareTo("6") == 0)
			Pm(split);
		else if (split[0].compareTo("7") == 0)
			Userlist(split);
		else if (split[0].compareTo("8") == 0)
			Stat(split);
	}
	
	public boolean shouldTerminate() {
		return shouldTerminate;
	}
	
	public void Register(String[] split) {
		String result;
		if(database.isRegistered(split[1])) { //Already registered
			result = "11" + " " + "1";
			connections.send(connectionId, result);
			return;
		}
		User newUser = new User(split[1], split[2]);
		database.addUser(newUser);
		result = "10" + " " + "1";
		connections.send(connectionId, result);
		return;
	}
	
	public void Login(String[] split) {
		String result;
		if(!database.isRegistered(split[1])) { //Not registered
			result = "11" + " " + "2";
			connections.send(connectionId, result);
			return;
		}	
		if(database.getUser(split[1]).isLoggedIn()) { //Already logged in
			result = "11" + " " + "2";
			connections.send(connectionId, result);
			return;
		}	
		if(database.getUser(split[1]).getPassword().compareTo(split[2]) != 0) { //Wrong password
			result = "11" + " " + "2";
			connections.send(connectionId, result);
			return;
		}	
		user = database.getUser(split[1]); //Getting the user
		user.Login(connectionId); //Logging in
		result = "10" + " " + "2";
		connections.send(connectionId, result); //Sending ACK
		checkAndSendMissedMsg(); //Sending notifications of missed posts
		return;
	}
	
	public void Logout(String[] split) {
		String result;
		if(user == null) { //No one is logged in
			result = "11" + " " + "3";
			connections.send(connectionId, result);
			return;
		}
		user.Logout();
		result = "10" + " " + "3";
		connections.send(connectionId, result); //Sending ACK
		connections.disconnect(connectionId);
		shouldTerminate = true;
		return;
	}
	
	public void Follow(String[] split) {
		String result;
		String usersList = "";
		int count = 0;
		if(user == null) { //No user
			result = "11" + " " + "4";
			connections.send(connectionId, result);
			return;
		}
		if(!user.isLoggedIn()) { //Not logged in
			result = "11" + " " + "4";
			connections.send(connectionId, result);
			return;
		}
		if (split[1].compareTo("F") == 0) { //The command is to follow
			for (int i = 3; i < split.length; i++) {
				if(database.isRegistered(split[i]) && !user.getFollowing().containsKey(split[i])) { //The current username on the list is registered and isn't already followed by the commanding user
					user.follow(database.getUser(split[i])); //Adding the user to the following list
					database.getUser(split[i]).getFollowed(user); //Adding commanding user to followers list
					usersList = usersList + database.getUser(split[i]).getUsername() + " "; //Adding the username to success follow list
					count++;
				}
			}
			if (count != 0) {
				result = "10 4 " + count + " " + usersList;
				connections.send(connectionId, result); //Sending ACK
				return;
			}
			else {
				result = "11" + " " + "4";
				connections.send(connectionId, result); //Sending error
				return;
			}
		}	
		if (split[1].compareTo("U") == 0) { //The command is to unfollow
			for (int i = 3; i < split.length; i++) {
				if(database.isRegistered(split[i]) && user.getFollowing().containsKey(split[i])) { //The current username on the list is registered and followed by the commanding user
					user.unfollow(database.getUser(split[i])); //Removing the user from the following list
					database.getUser(split[i]).getUnfollowed(user); //Removing commanding user from followers list
					usersList = usersList + database.getUser(split[i]).getUsername() + " "; //Adding the username to success follow list
					count++;
				}
			}
			if (count != 0) {
				result = "10 4 " + count + " " + usersList;
				connections.send(connectionId, result); //Sending ACK
				return;
			}
			else {
				result = "11" + " " + "4";
				connections.send(connectionId, result); //Sending error
				return;
			}
		}
		
	}
	
	public void Post(String[] split) {
		String result;
		if(!user.isLoggedIn()) { //Not logged in
			result = "11" + " " + "5";
			connections.send(connectionId, result);
			return;
		}
		String msg = "9 1 " + user.getUsername() + " "; //Constructing a message
		for (int i = 1; i < split.length; i++) {
			msg = msg + split[i] + " ";
		}
		user.post();
		int count = msg.length() - msg.replaceAll("@","").length(); //Finding and notifying tagged users
		notifTagged(count, split, msg);
		HashMap<String, User> followedBy = user.getFollowers(); //Getting the user followers
		for(Entry<String, User> entry : followedBy.entrySet()) { //Adding the post to the followers
			entry.getValue().addPost(msg);
			if (entry.getValue().isLoggedIn()) //Sending notifs if they are logged in
				connections.send(entry.getValue().getConId(), msg);
		}
		result = "10" + " " + "5";
		connections.send(connectionId, result); //Sending ACK
		
	}
	
	public void notifTagged(int count, String[] split, String msg) {
		while (count != 0) { //Some users are tagged
			for (int i = 1; i < split.length; i++) {
				if (split[i].indexOf('@') == 0) {
					count--;
					String username = split[i].substring(1); //The tagged username
					if(database.isRegistered(username)) { //Checking if registered
						database.getUser(username).addPost(msg); //Adding to received posts
						if (database.getUser(username).isLoggedIn()) //Checking if logged in
							connections.send(database.getUser(username).getConId(), msg); //Sending notification
					}
				}
			}
		}
	}
	
	public void Pm(String[] split) {
		String result;
		if(!user.isLoggedIn()) { //Not logged in
			result = "11" + " " + "6";
			connections.send(connectionId, result);
			return;
		}
		if(!database.isRegistered(split[1])) { //Target user is not registered
			result = "11" + " " + "6";
			connections.send(connectionId, result);
			return;
		}
		String msg = "9 0 " + user.getUsername() + " ";
		for (int i = 2; i < split.length; i++) { //Constructing the PM
			msg = msg + split[i] + " ";
		}
		User targetuser = database.getUser(split[1]);
		int targetId = targetuser.getConId();
		targetuser.addPm(msg); //Adding to the user PMs
		result = "10" + " " + "6";
		connections.send(connectionId, result); //Sending ACK
		if (targetuser.isLoggedIn()) //Sending a notif if logged in
			connections.send(targetId, msg);
	}
	
	public void Userlist(String[] split) {
		String result;
		if(!user.isLoggedIn()) { //Not logged in
			result = "11" + " " + "7";
			connections.send(connectionId, result);
			return;
		}
		result = "10 7 " + database.getNumOfUsers() + " " + database.usersList();
		connections.send(connectionId, result); //Sending ACK of the requested data
	}
	
	public void Stat(String[] split) {
		String result;
		if(!user.isLoggedIn()) { //Not logged in
			result = "11" + " " + "8";
			connections.send(connectionId, result);
			return;
		}
		if(!database.isRegistered(split[1])) { //No such user
			result = "11" + " " + "8";
			connections.send(connectionId, result);
			return;
		}
		User stats = database.getUser(split[1]);
		result = "10" + " " + "8" + " " + stats.getNumOfPosts() + " " + stats.getFollowers().size() + " " + stats.getFollowing().size(); //Sending ACK of the requested data
		connections.send(connectionId, result);
		return;
		
	}
	
	public void checkAndSendMissedMsg() {
		int lastReadPost = user.getLastReadPost();
		int lastReadPm = user.getLastReadPm();
		if (lastReadPost != -1) { //Sending unread posts
			List<String> posts = user.getPosts();
			for (int i = lastReadPost; i < posts.size(); i++)
				connections.send(connectionId, posts.get(i));
		}
		if (lastReadPm != -1) { //Sending unread PMs
			List<String> posts = user.getPms();
			for (int i = lastReadPm; i < posts.size(); i++)
				connections.send(connectionId, posts.get(i));
		}
		user.resetLastRead();
	}

}
