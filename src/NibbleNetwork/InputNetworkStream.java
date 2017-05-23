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
import java.io.InputStream;
import java.net.Socket;

/**
 *
 * @author dansb
 */
public class InputNetworkStream extends NetworkStream {

    private final InputStream inputStream;

    public InputNetworkStream(Socket socket) throws IOException {
        super(socket);
        this.inputStream = socket.getInputStream();
    }

    public synchronized int read8() throws IOException {
        int b = this.inputStream.read();
        return b;
    }

    public synchronized int read16() throws IOException {
        int c1 = read8();
        int c2 = read8();
        return c1 << 8 | c2;
    }

    public synchronized int read32() throws IOException {
        int s1 = read16();
        int s2 = read16();
        return (s2 << 16 | s1);
    }

    public synchronized String readString() throws IOException {
        String str = "";
        int length = read16();
        for (int i = 0; i < length; i++) {
            str += (char) read8();
        }

        return str;
    }

    public synchronized boolean hasInput() throws IOException {
       return this.inputStream.available() > 0;
    }
}
