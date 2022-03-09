package bgu.spl.net.impl.BGSServer;

import java.util.function.Supplier;

import bgu.spl.net.api.bidi.BidiEncoderDecoder;
import bgu.spl.net.api.bidi.BidiMessagingProtocol;
import bgu.spl.net.api.bidi.BidiMessagingProtocolImpl;
import bgu.spl.net.api.bidi.Database;
import bgu.spl.net.srv.TcpServer;

public class TPCMain {

	public static void main(String[] args) {
		Database db = new Database();
		
		Supplier<BidiMessagingProtocol<String>> protocolfactory =()-> new BidiMessagingProtocolImpl(db);
		Supplier<BidiEncoderDecoder> encdecfactory =()-> new BidiEncoderDecoder();
		TcpServer server = new TcpServer(Integer.parseInt(args[0]), protocolfactory, encdecfactory);
		server.serve();

	}

}
