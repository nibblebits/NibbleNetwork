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

import NibbleNetwork.exceptions.NetworkException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author dansb
 */
public class NetworkServer {

    private static  NetworkServer activeServer = null;
    private ServerSocket server_sock;
    private ServerConnectionHandler connection_handler;
    private final List<ServerNetworkClient> clients;
    private Thread accepting_thread;

    public NetworkServer() {
        server_sock = null;
        connection_handler = null;
        accepting_thread = null;
        clients = new CopyOnWriteArrayList<ServerNetworkClient>();
        
        if (NetworkServer.activeServer == null) {
            // We currently have no active server so lets set it to us
            NetworkServer.activeServer = this;
        }
    }

    public static void setActiveServer(NetworkServer server) {
        NetworkServer.activeServer = server;
    }
    
    public static NetworkServer getActiveServer() {
        return NetworkServer.activeServer;
    }
    
    public void setConnectionHandler(ServerConnectionHandler handler) {
        connection_handler = handler;
    }

    public boolean hasConnectionHandler() {
        return connection_handler != null;
    }

    public synchronized List<ServerNetworkClient> getClients() {
        return this.clients;
    }

    public void listen(int port) throws IOException, NetworkException {
        listen(port, false, 1000);
    }

    public void listen(int port, boolean do_block, int client_timeout) throws IOException, NetworkException {
        if (!hasConnectionHandler()) {
            throw new NetworkException("Expecting a connection handler before listening");
        }

        if (isListening()) {
            throw new NetworkException("The network server is already listening on port " + port);
        }
        server_sock = new ServerSocket(port);
        accepting_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!server_sock.isClosed()) {
                    try {
                        Socket socket = server_sock.accept();
                        ServerNetworkClient client = connection_handler.connection(socket);
                        if (client != null) {
                            client.setSocket(socket);
                            client.getSocket().setSoTimeout(client_timeout);
                            client.setConnectionHandler(connection_handler);
                            client.setConnected(true);

                            synchronized (clients) {
                                clients.add(client);
                            }

                            if (!client.hasInitiated()) {
                                client.Init();
                                client.initiated = true;
                            }

                            client.setProcessor(client.getNetworkProcessor());
                        } else {
                            throw new Exception("Connection handler rejected connection");
                        }
                    } catch (Exception ex) {
                        connection_handler.connection_problem(ex);
                    }
                }
            }
        });
        accepting_thread.start();

        if (do_block) {
            while (accepting_thread.isAlive()) {
            }
        }
    }

    public boolean hasClient(ServerNetworkClient client) {
        return this.clients.contains(client);
    }

    public void removeClient(ServerNetworkClient client) throws Exception {
        if (!hasClient(client)) {
            throw new Exception("The client is not apart of this server");
        }
        if (client.isConnected()) {
            client.disconnect();
            // Client will recall removeClient upon calling "disconnect" so lets just return here 
            return;
        }

        this.clients.remove(client);
    }

    public void close() throws NetworkException, IOException {
        if (!isListening()) {
            throw new NetworkException("The network server is not listening on a port");
        }

        server_sock.close();
    }

    public boolean isListening() {
        return server_sock != null && !server_sock.isClosed();
    }

}
