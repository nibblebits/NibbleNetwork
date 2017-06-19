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

import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author dansb
 */
public abstract class NetworkStream {

    private final NetworkClient client;
    private final Socket socket;
    private final ReentrantLock lock;

    public NetworkStream(NetworkClient client, Socket socket) {
        this.client = client;
        this.socket = socket;
        this.lock = new ReentrantLock();
    }

    public NetworkClient getNetworkClient() {
        return this.client;
    }
    
    public void lock() {
        this.lock.lock();
    }

    public void unlock() {
        this.lock.unlock();
    }
    
    public Socket getSocket() {
        return this.socket;
    }

}

