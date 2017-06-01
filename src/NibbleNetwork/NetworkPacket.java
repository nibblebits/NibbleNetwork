/*
    Craft compiler v0.1.0 - The standard compiler for the Craft programming language.
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
public abstract class NetworkPacket {

    private List<NetworkClient> clients;

    public NetworkPacket() {
        this.clients = new ArrayList<NetworkClient>();
    }

    public NetworkPacket(NetworkClient client) {
        this.clients = new ArrayList<NetworkClient>();
        this.clients.add(client);
    }

    public NetworkPacket(List<NetworkClient> clients) {
        this.clients = clients;
    }

    public void send() throws Exception {
        for (NetworkClient client : clients) {
            send_to_client(client);
        }
    }
    public abstract void send_to_client(NetworkClient client) throws Exception;
}
