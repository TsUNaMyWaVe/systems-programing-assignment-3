package bgu.spl.net.api.bidi;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import bgu.spl.net.api.MessageEncoderDecoder;

public class BidiEncoderDecoder implements MessageEncoderDecoder<String>{
	
	private byte[] bytes = new byte[1 << 10]; //start with 1k
	private int len = 0;
	private String currentMsg;
	private short currentOpc;
	private int counter = 0;
	private int currCounter = 0;
	private boolean flag = true;
	private boolean flag2 = true;
	
	public String decodeNextByte(byte nextByte) {
		if (len == 2 & currentOpc == 0) { //Getting the opcode and adding it
			currentOpc = bytesToShort(Arrays.copyOfRange(bytes, 0, 2));
			addOpcode(currentOpc);
			currCounter = 0;
		}
		if (currentOpc == 4) { //Case: follow
			if (nextByte == 0 & flag) { //The command is to follow
				currentMsg = currentMsg + "F";
				flag = false;
				return null;
			}	
			else if (nextByte == 1 & flag) { //The command is to unfollow
				currentMsg = currentMsg + "U";
				flag = false;
				return null;
			}	
			if (len == 2 & flag2) { //Getting the number of users to follow/unfollow
				counter = bytesToShort(bytes);
				currentMsg = currentMsg + " " + counter + " ";
				currCounter = 0;
				len = 0;
				flag2 = false;
			}	
		}
		
		if (nextByte == '\0') { //End of a data part
			currCounter++;
		}	
		if (currCounter == counter) { //End of message
			addToMsg(bytes);
			counter = 0;
			currCounter = 0;
			currentOpc = 0;
			flag = true;
			flag2 = true;
			return currentMsg;
		}
        pushByte(nextByte);
        
        if (len == 2 & (nextByte == 3 | nextByte == 7) & currentOpc == 0) { //Cases: Logout and Userlist
			currentOpc = bytesToShort(Arrays.copyOfRange(bytes, 0, 2));
			addOpcode(currentOpc);
			currCounter = 0;
			return currentMsg;
		}
        return null; //not a line yet
	}
	
	private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }
	
	public byte[] encode(String message) {
		String[] split = message.split(" ");
		short opcode = Short.decode(split[0]);
		byte[] opc = shortToBytes(opcode);
		byte[] msg = null;
		if (opcode == 9)
			msg = encodeNotif(split);
		else if (opcode == 10)
			msg = encodeAck(split);
		else if (opcode == 11)
			msg = encodeError(split);		
        byte[] result = mergeArrays(opc, msg);
		return result;
	}
	
	public byte[] encodeError(String[] split) {
		short opcode = Short.decode(split[1]);
		return shortToBytes(opcode);
	}
	
	public byte[] encodeAck(String[] split) {
		short opcode = Short.decode(split[1]);
		if (opcode == 1 | opcode == 2 | opcode == 3 | opcode == 5 | opcode == 6) //Cases: Register, Login, Logout, Post, Pm
			return shortToBytes(opcode);
		else if (opcode == 4 | opcode == 7) { //Cases: Follow, Userlist
			byte[] result = new byte[0];
			short data1 = Short.decode(split[1]);
			short data2 = Short.decode(split[2]);
			byte[] arr1 = shortToBytes(data1);
			byte[] arr2 = shortToBytes(data2);
			result = mergeArrays(arr1, arr2);
			String msg = "";
			for (int i = 3; i < split.length; i++)
				msg = msg + split[i] + "\0";
			byte[] data3 = msg.getBytes();
			result = mergeArrays(result, data3);
			return result;
		}
		else { //opcode == 8, case: Stat
			byte[] result = shortToBytes(opcode);
			for (int i = 2; i < split.length; i++) {
				short data = Short.decode(split[i]);
				byte[] arr = shortToBytes(data);
				result = mergeArrays(result, arr);
			}	
			return result;
		}
	}
	
	public byte[] encodeNotif(String[] split) {
		byte type;
		if (split[1].compareTo("0") == 0) //Type is PM
			type = 0;
		else //Type is public
			type = 1;
		byte[] result = new byte[1];
		result[0] = type;
		String username = split[2] + "\0";
		byte[] usernameData = username.getBytes(); //The username of the posting user
		result = mergeArrays(result, usernameData);
		String content = "";
		for (int i = 3; i < split.length; i++) { //Constructing the message
			content = content + split[i] + " ";
		}
		content = content + "\0"; //Adding 0 byte
		byte[] msg = content.getBytes();
		result = mergeArrays(result, msg);
		return result;
	}
	
	public void addOpcode(short opcode) { //Adding the opcode to the message and change the counter (of 0 byte) accordingly
		Short opc = opcode;
		currentMsg = opc.toString() + ' ';
		len = 0;
		if (opcode == 1 | opcode == 2 | opcode == 6 | opcode == 9)
			counter = 2;
		if (opcode == 5 | opcode == 8)
			counter = 1;
	}
	
	public void addToMsg(byte[] byteArr) { //Adding current bytes array to the message
		String command = new String(bytes, 0, len, StandardCharsets.UTF_8);
		currentMsg = currentMsg + command + ' ';
		currentMsg = currentMsg.replace("\0", " ");
		counter++;
		len = 0;
	}
	
	public short bytesToShort(byte[] byteArr){
	    short result = (short)((byteArr[0] & 0xff) << 8);
	    result += (short)(byteArr[1] & 0xff);
	    return result;
	}
	
	public byte[] shortToBytes(short num){
	    byte[] bytesArr = new byte[2];
	    bytesArr[0] = (byte)((num >> 8) & 0xFF);
	    bytesArr[1] = (byte)(num & 0xFF);
	    return bytesArr;
	}
	
	public byte[] mergeArrays(byte[] arr1, byte[] arr2) { //Merging to arrays to one
		int aLen = arr1.length;
        int bLen = arr2.length;
        byte[] result = new byte[aLen + bLen];
        System.arraycopy(arr1, 0, result, 0, aLen);
        System.arraycopy(arr2, 0, result, aLen, bLen);
        return result;
	}

}
