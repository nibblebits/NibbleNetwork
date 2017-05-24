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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dansb
 */
public abstract class NetworkProcessor implements Runnable, IProcessable {

    private final ArrayList<InputNetworkProtocol> input_protocols;
    private boolean is_running;

    public NetworkProcessor() {
        input_protocols = new ArrayList<InputNetworkProtocol>();

        // Add the ping protocol to the network processor to handle incoming pings.
        input_protocols.add(new InputPingProtocol());

        is_running = false;
    }

    public synchronized void startThread() {
        if (is_running) {
            throw new RuntimeException("There is already a thread running");
        }
        new Thread(this).start();
        is_running = true;
    }

    public synchronized void stopThread() {
        is_running = false;
    }

    public boolean isRunning() {
        return is_running;
    }

    /**
     * Handles the new client and starts the processor thread if it is not
     * already running
     *
     * @param client
     * @throws Exception
     */
    protected void handleNewClient(NetworkClient client) throws Exception {
        // Welcome the new client to the processor
        welcome(client);
        
        if (client.getNetworkProcessor() != this) {
            client.setProcessor(this);
        }
        // Start the processor thread if required
        if (!isRunning()) {
            startThread();
        }
    }

    protected synchronized void handleClientThatLeft(NetworkClient client) throws Exception {
        clientRemoved(client);
        if (isRunning() && !hasClients()) {
            stopThread();
        }
    }

    protected void processProtocolsForClient(NetworkClient client) throws Exception {
        InputNetworkStream input_stream = client.getInputStream();
        // Lets process the client with all its protocols if we have data
        synchronized (input_stream) {
            if (input_stream.hasInput()) {
                int protocol_id = input_stream.read8();
                InputNetworkProtocol protocol = getInputProtocolById(protocol_id);
                protocol.process_input(client, input_stream);
            }
        }
    }

    public void process_client(NetworkClient c) throws Exception {
        synchronized (c) {
            if (c.isConnected()) {
                try {
                    processProtocolsForClient(c);
                    if (c.isConnected()) {
                        c.process();
                    }
                } catch (Exception ex) {
                    c.getConnectionHandler().connection_problem(ex);
                    c.disconnect();
                }
            } else {
                // Remove the client as they are not connected
                removeClient(c);
            }
        }
    }

    public synchronized void addInputProtocol(InputNetworkProtocol protocol) throws Exception {
        if (hasInputProtocol(protocol)) {
            throw new Exception("The protocol is already apart of this network processor");
        }
        if (protocol.getId() == 0 && !(protocol instanceof InputPingProtocol)) {
            throw new Exception("The protocol ID of zero is reserved for pinging. " + protocol.getClass().getName());
        }
        input_protocols.add(protocol);
    }

    public synchronized ArrayList<InputNetworkProtocol> getInputProtocols() {
        return this.input_protocols;
    }

    public synchronized InputNetworkProtocol getInputProtocolById(int protocol_id) throws Exception {
        for (InputNetworkProtocol protocol : getInputProtocols()) {
            if (protocol.getId() == protocol_id) {
                return protocol;
            }
        }

        throw new Exception("No protocol could be found with the id: " + protocol_id);
    }

    public synchronized boolean hasInputProtocol(InputNetworkProtocol protocol) {
        for (InputNetworkProtocol p : getInputProtocols()) {
            if (p == protocol) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void run() {
        while (isRunning()) {
            try {
                synchronized (this) {
                    process();
                }
                Thread.sleep(10);
            } catch (Exception ex) {
                Logger.getLogger(NetworkProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public synchronized boolean hasClients() {
        return getTotalClients() != 0;
    }

    public abstract void welcome(NetworkClient client) throws Exception;
    
    public abstract void moveClients(NetworkProcessor new_processor) throws Exception;

    public abstract void addClient(NetworkClient client) throws Exception;

    public abstract boolean hasClient(NetworkClient client);
    
    public abstract List<NetworkClient> getClients();
    
    public abstract int getTotalClients();

    public abstract void removeClient(NetworkClient client) throws Exception;
    
    public abstract void clientRemoved(NetworkClient client) throws Exception;
}
