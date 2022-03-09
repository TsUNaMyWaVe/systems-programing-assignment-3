package bgu.spl.net.srv;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.bidi.BidiMessagingProtocol;

public class TcpServer<T> implements Server<T> {
	
	private final int port;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;

    private ConnectionsImpl<T> connections = new ConnectionsImpl<T>();
    
    public TcpServer(
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
    }

    @Override
    public void serve() {
    	System.out.println("Server started");
        try (ServerSocket serverSock = new ServerSocket(port)) {

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {
            	
                Socket clientSock = serverSock.accept();
                
                BidiMessagingProtocol<T> protocol = protocolFactory.get();
                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                        clientSock,
                        encdecFactory.get(),
                        protocol);
                int connectionId = connections.connect(handler);
                protocol.start(connectionId, connections);

                execute(handler);
            }
        } catch (IOException ex) {
        }
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected void execute(BlockingConnectionHandler<T>  handler) {
    	new Thread(handler).start();
    }

}
