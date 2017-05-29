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
import java.io.OutputStream;
import java.net.Socket;

/**
 *
 * @author dansb
 */
public class OutputNetworkStream extends NetworkStream {

    private final OutputStream outputStream;

    public OutputNetworkStream(Socket socket) throws IOException {
        super(socket);
        this.outputStream = socket.getOutputStream();

    }

    public synchronized void createFrame(int protocol_id) throws IOException, Exception {
        if (protocol_id > 255 || protocol_id < 0) {
            throw new Exception("Protocol id's must be within the 0-255 range");
        }
        lock();
        write8(protocol_id);
    }

    public synchronized void finishFrame() throws IOException {
        // Flush the network
        this.outputStream.flush();
        unlock();
    }

    public synchronized void write8(int i) throws IOException {
        this.outputStream.write(i);
    }

    public synchronized void write16(int i) throws IOException {
        int s1 = i >> 8;
        int s2 = i & 0xff;
        this.outputStream.write(s1);
        this.outputStream.write(s2);
    }

    public synchronized void write32(int i) throws IOException {
        int s1 = i & 0xffff;
        int s2 = i >> 16;
        write16(s1);
        write16(s2);
    }

    public synchronized void writeString(String s) throws Exception {
        if (s.length() > 65535) {
            throw new Exception("The string cannot be bigger than a word 16 bits in size");
        }
        write16(s.length());
        for (int i = 0; i < s.length(); i++) {
            write8(s.charAt(i));
        }
    }

}
