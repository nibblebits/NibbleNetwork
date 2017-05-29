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

import java.io.IOException;
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
            this.input_stream = new InputNetworkStream(socket);
            this.output_stream = new OutputNetworkStream(socket);
        }
        this.network_processor = processor;
        this.initiated = false;
        this.lastRecievedPing = System.currentTimeMillis();
        this.lastSentPing = 0;
        this.ping_protocol = new OutputPingProtocol(this);

    }

    public void connect(String host, int port) throws Exception {
        connect(host, port, 3000);
    }

    public void connect(String host, int port, int timeout) throws Exception {
        if (!hasConnectionHandler()) {
            throw new Exception("You cannot connect without a connection handler");
        }
        try {
            Socket sock = new Socket(host, port);
            sock.setSoTimeout(timeout);
            setSocket(sock);
            setConnected(true);
            Init();
            setProcessor(this.network_processor);
            this.initiated = true;
        } catch (IOException ex) {
            getConnectionHandler().connection_problem(ex);
            throw new Exception("Failed to connect to " + host + " on port " + port + ": " + ex.getMessage());
        }

    }

    protected void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean hasInitiated() {
        return this.initiated;
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

    public synchronized void setSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.input_stream = new InputNetworkStream(socket);
        this.output_stream = new OutputNetworkStream(socket);
    }

    public void setProcessor(NetworkProcessor processor) throws Exception {
        if (processor == null) {
            throw new Exception("A client must have a processor");
        }

        // A new processor could mean invalid opcodes are now in the input stream so it is important to wipe it and ignore all bytes currently in the stream
        this.input_stream.wipe();
        
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
