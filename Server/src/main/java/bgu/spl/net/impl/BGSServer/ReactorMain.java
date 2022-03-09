package bgu.spl.net.impl.BGSServer;

import java.util.function.Supplier;

import bgu.spl.net.api.bidi.BidiEncoderDecoder;
import bgu.spl.net.api.bidi.BidiMessagingProtocol;
import bgu.spl.net.api.bidi.BidiMessagingProtocolImpl;
import bgu.spl.net.api.bidi.Database;
import bgu.spl.net.srv.Reactor;

public class ReactorMain {
	public static void main(String[] args) {
		Database db = new Database();
		
		Supplier<BidiMessagingProtocol<String>> protocolfactory =()-> new BidiMessagingProtocolImpl(db);
		Supplier<BidiEncoderDecoder> encdecfactory =()-> new BidiEncoderDecoder();
		Reactor server = new Reactor(Integer.parseInt(args[0]), Integer.parseInt(args[1]), protocolfactory, encdecfactory);
		server.serve();

	}

}
