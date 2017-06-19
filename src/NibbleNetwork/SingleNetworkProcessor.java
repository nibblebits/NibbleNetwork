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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dansb
 */
public abstract class SingleNetworkProcessor extends NetworkProcessor {

    private NetworkClient client;

    public SingleNetworkProcessor() throws Exception {
        this(null);
    }

    public SingleNetworkProcessor(NetworkServer server) throws Exception {
        super(server);
        client = null;
    }

    public synchronized void moveClients(NetworkProcessor new_processor) throws Exception {
        if (client != null) {
            client.setProcessor(new_processor);
        }
    }

    public synchronized void setClient(NetworkClient client) throws Exception {
        if (client == null) {
            throw new Exception("The client may not be null");
        }
        if (!shouldAllowClient(client)) {
            throw new Exception("The client was rejected by the processor");
        }
        this.client = client;
        handleNewClient(client);
    }

    public synchronized NetworkClient getClient() {
        return this.client;
    }

    @Override
    public synchronized void addClient(NetworkClient client) throws Exception {
        if (hasClient()) {
            throw new Exception("Single network processors are only allowed one client");
        }

        setClient(client);
    }

    @Override
    public synchronized void addInputProtocol(InputNetworkProtocol protocol) throws Exception {
        if (protocol instanceof FramelessInputOutputBlockingProtocol) {
            throw new Exception("This protocol is frameless meaning it is not supported by this protcol system. Frameless protocols are expected to be called by the programmer manually.");
        }
        super.addInputProtocol(protocol);
    }

    @Override
    public synchronized boolean hasClient(NetworkClient client) {
        return hasClient() && this.client == client;
    }

    public synchronized boolean hasClient() {
        return this.client != null;
    }

    @Override
    public int getTotalClients() {
        if (hasClient()) {
            return 1;
        }

        return 0;
    }

    @Override
    public synchronized List<NetworkClient> getClients() {
        List<NetworkClient> clients = new ArrayList<NetworkClient>();
        if (hasClient()) {
            clients.add(getClient());
        }

        return clients;
    }

    @Override
    public void process() throws Exception {
        if (hasClient()) {
            NetworkClient c = getClient();
            process_client(c);
        }
    }

    @Override
    public synchronized void removeClient(NetworkClient client) throws Exception {
        if (hasClient() && getClient() == client) {
            this.client = null;
            super.handleClientThatLeft(client);
        } else {
            throw new Exception("This is not the single client that was originally set");
        }
    }

}
