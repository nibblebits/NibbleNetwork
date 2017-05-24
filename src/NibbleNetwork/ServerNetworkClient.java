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
public abstract class ServerNetworkClient extends NetworkClient {

    private final NetworkServer server;
    
    public ServerNetworkClient(NetworkProcessor processor, Socket socket, NetworkServer server) throws IOException, Exception {
        super(processor, socket);
        this.server = server;
    }
    
    public NetworkServer getServer() {
        return this.server;
    }

    @Override
    public void disconnect() throws Exception {
        super.disconnect();
        getServer().removeClient(this);
    }
}
