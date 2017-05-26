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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author dansb
 */
public abstract class SharedNetworkProcessor extends NetworkProcessor {

    private final List<NetworkClient> network_clients;

    public SharedNetworkProcessor() throws Exception {
        this.network_clients = new CopyOnWriteArrayList<NetworkClient>();
    }

    @Override
    public void moveClients(NetworkProcessor new_processor) throws Exception {
        synchronized (this.network_clients) {
            for (NetworkClient networkClient : this.network_clients) {
                networkClient.setProcessor(new_processor);
            }
        }
    }

    @Override
    public void addClient(NetworkClient client) throws Exception {
        if (!shouldAllowClient(client)) {
            throw new Exception("The client was rejected by the processor: ");
        }
        synchronized (this.network_clients) {
            this.network_clients.add(client);
        }

        synchronized (client) {
            handleNewClient(client);
        }
    }

    @Override
    public boolean hasClient(NetworkClient client) {
        synchronized (this.network_clients) {
            for (NetworkClient c : this.network_clients) {
                if (c == client) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public synchronized List<NetworkClient> getClients() {
        return this.network_clients;
    }

    @Override
    public int getTotalClients() {
        synchronized (this.network_clients) {
            return this.network_clients.size();
        }
    }

    @Override
    public void removeClient(NetworkClient client) throws Exception {
        synchronized (this.network_clients) {
            this.network_clients.remove(client);
            handleClientThatLeft(client);
        }
    }

    @Override
    public void process() throws Exception {
        synchronized (this.network_clients) {
            for (NetworkClient client : this.network_clients) {
                process_client(client);
            }
        }
    }

}
