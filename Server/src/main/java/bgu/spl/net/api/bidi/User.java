package bgu.spl.net.api.bidi;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import bgu.spl.net.srv.bidi.ConnectionHandler;

public class User {
	private String username;
	private String password;
	private HashMap<String, User> following;
	private HashMap<String, User> followedBy;
	private List<String> posts; //Posts by the users the user is following/mentions
	private List<String> pm; //PMs the user got
	private boolean isLoggedIn;
	private int numberOfPosts;
	private int lastReadPost;
	private int lastReadPm;
	private int connectionId;
	
	User(String username, String password) {
		this.username = username;
		this.password = password;
		following = new HashMap<>();
		followedBy = new HashMap<>();
		posts = new LinkedList<>();
		pm = new LinkedList<>();
		isLoggedIn = false;
		numberOfPosts = 0;
		lastReadPost = 0;
		lastReadPm = 0;
		
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public boolean isLoggedIn() {
		return isLoggedIn;
	}
	
	public HashMap<String, User> getFollowing() {
		return following;
	}
	
	public HashMap<String, User> getFollowers() {
		return followedBy;
	}
	
	public List<String> getPosts() {
		return posts;
	}
	
	public List<String> getPms() {
		return pm;
	}
	
	public int getConId() {
		return connectionId;
	}
	
	public void Login(int connectionId) {
		this.connectionId = connectionId;
		isLoggedIn = true;
	}
	
	public void Logout() {
		isLoggedIn = false;
		lastReadPost = posts.size();
		lastReadPm = pm.size();
	}
	
	public void follow(User user) {
		synchronized (following) {
			following.put(user.getUsername(), user);
		}
	}
	
	public void unfollow(User user) {
		synchronized (following) {
			following.remove(user.getUsername());
		}
	}
	
	public void getFollowed(User user) {
		synchronized (followedBy) {
			followedBy.put(user.getUsername(), user);
		}
	}
	
	public void getUnfollowed(User user) {
		synchronized (followedBy) {
			followedBy.remove(user.getUsername());
		}
	}
	
	public void addPost(String post) {
		synchronized (posts) {
			posts.add(post);
		}
	}
	
	public void addPm(String post) {
		synchronized (pm) {
			pm.add(post);
		}
	}
	
	public void post() {
		numberOfPosts++;
	}
	
	public int getNumOfPosts() {
		return numberOfPosts;
	}
	
	public int getLastReadPost() {
		return lastReadPost;
	}
	
	public int getLastReadPm() {
		return lastReadPm;
	}
	
	public void resetLastRead() {
		lastReadPost = -1;
		lastReadPm = -1;
	}

}
