/*
  NibbleNetwork - A multithreaded Java networking library
    Copyright (C) 2016  Daniel McCarthy

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package NibbleNetwork;

import NibbleNetwork.exceptions.DeniedOperationException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 *
 * @author dansb
 */
public abstract class NetworkClient extends NetworkObject implements IProcessable {

    private NetworkProcessor network_processor;
    private Socket socket;
    private InputNetworkStream input_stream;
    private OutputNetworkStream output_stream;
    private ConnectionHandler connection_handler;
    private OutputPingProtocol ping_protocol;
    protected boolean initiated;
    protected boolean ready;
    private boolean connected;
    private long lastRecievedPing;
    private long lastSentPing;

    public NetworkClient(NetworkProcessor processor) throws Exception {
        this(processor, null);
    }

    public NetworkClient(NetworkProcessor processor, Socket socket) throws IOException, Exception {
        if (processor == null) {
            throw new Exception("A client must have a processor");
        }

        this.socket = socket;
        if (socket != null) {
            this.input_stream = new InputNetworkStream(this, socket);
            this.output_stream = new OutputNetworkStream(this, socket);
        }
        this.network_processor = processor;
        this.initiated = false;
        this.ready = false;
        this.lastRecievedPing = System.currentTimeMillis();
        this.lastSentPing = 0;
        this.ping_protocol = new OutputPingProtocol(this);

    }

    public void connect(String host, int port) throws Exception {
        connect(host, port, 1000);
    }

    public void connect(String host, int port, int timeout) throws Exception {
        if (!hasConnectionHandler()) {
            throw new Exception("You cannot connect without a connection handler");
        }

        if (!(getConnectionHandler() instanceof ClientConnectionHandler)) {
            throw new Exception("A client connection handler is expected when connecting");
        }

        ClientConnectionHandler client_connection_handler = (ClientConnectionHandler) getConnectionHandler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket sock = new Socket();
                    sock.setSoTimeout(timeout);
                    sock.connect(new InetSocketAddress(host, port));
                    setSocket(sock);
                    setConnected(true);
                    Init();
                    setProcessor(network_processor);
                    client_connection_handler.connection(NetworkClient.this);
                    initiated = true;
                } catch (Exception ex) {
                    client_connection_handler.connection_problem(ex);
                }
            }
        }).start();
    }

    protected void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean hasInitiated() {
        return this.initiated;
    }

    public boolean isReady() {
        return this.ready;
    }

    public void setConnectionHandler(ConnectionHandler connection_handler) {
        this.connection_handler = connection_handler;
    }

    public ConnectionHandler getConnectionHandler() {
        return this.connection_handler;
    }

    public boolean hasConnectionHandler() {
        return this.connection_handler != null;
    }

    private void EnsureIOSafe() throws DeniedOperationException {
        // Coming soon.
         if (!isReady() && (Thread.currentThread() != getNetworkProcessor().getThread() && getNetworkProcessor().getThread() != null)) {
//            throw new DeniedOperationException("The network client is not ready. "
  //+ "When the network client is not ready I/O operations can only be preformed on the processor thread that is running this client");
        }
    }
    
    public void EnsureSafe() throws DeniedOperationException {
       EnsureIOSafe();
    }
    
    public synchronized void setSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.input_stream = new InputNetworkStream(this, socket);
        this.output_stream = new OutputNetworkStream(this, socket);
    }

    public void setProcessor(NetworkProcessor processor) throws Exception {
        if (processor == null) {
            throw new Exception("A client must have a processor");
        }
        ready = false;
        synchronized (this.network_processor) {
            // Lets remove the client from the old processor
            if (this.network_processor.hasClient(this)) {
                this.network_processor.removeClient(this);
            }
            this.network_processor = processor;
        }

        synchronized (processor) {
            if (!processor.hasClient(this)) {
                processor.addClient(this);
            }
        }
        ready = true;
    }

    public boolean hasSocket() {
        return this.socket != null;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public InputNetworkStream getInputStream() {
        return this.input_stream;
    }

    public OutputNetworkStream getOutputStream() {
        return this.output_stream;
    }

    public NetworkProcessor getNetworkProcessor() {
        return this.network_processor;
    }

    @Override
    public void process() throws Exception {
        if (!isConnected()) {
            throw new Exception("The client is not connected");
        }
        long curTime = System.currentTimeMillis();
        if (curTime - getLastRecievedPing() > 3000) {
            // No ping for three seconds lets disconnect the client.
            disconnect();
            return;
        }

        if (curTime - getLastSentPing() > 500) {
            // Its been 500ms since the last ping lets send a ping.
            ping_protocol.ping();
            this.lastSentPing = System.currentTimeMillis();
        }
    }

    public void disconnect() throws Exception {
        priorDisconnection();
        getConnectionHandler().disconnection(this);
        this.connected = false;
        this.socket.close();
    }

    protected void setLastSentPing(long new_ping) {
        this.lastSentPing = new_ping;
    }

    public long getLastSentPing() {
        return this.lastSentPing;
    }

    protected void setLastRecievedPing(long new_ping) {
        this.lastRecievedPing = new_ping;
    }

    public long getLastRecievedPing() {
        return this.lastRecievedPing;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public abstract void Init() throws Exception;

    public abstract void priorDisconnection();
}
