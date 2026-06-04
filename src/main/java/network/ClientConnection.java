package network;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

class ClientConnection {
    private static final Logger LOGGER = Logger.getLogger(ClientConnection.class.getName());

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Đảm bảo chỉ có một instance của ClientConnection
    synchronized boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    // Kết nối đến server
    synchronized boolean connect(String ip, int port) {
        if (isConnected()) {
            LOGGER.info("Connection is already available.");
            return true;
        }

        try {
            socket = new Socket(ip, port);
            socket.setSoTimeout(0);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            LOGGER.info("Connected to server.");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Cannot connect to server: " + e.getMessage(), e);
            close();
            return false;
        }
    }

    // Gửi dữ liệu đến server
    synchronized boolean sendData(Object data) {
        try {
            if (out != null && isConnected()) {
                out.writeObject(data);
                out.reset();
                out.flush();
                return true;
            }

            LOGGER.warning("Cannot send data: not connected to server.");
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Cannot send data", e);
            return false;
        }
    }

    // Nhận dữ liệu từ server
    synchronized ObjectInputStream getInputStream() {
        return in;
    }

    // Đóng kết nối
    synchronized void close() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while closing connection", e);
        } finally {
            out = null;
            in = null;
            socket = null;
            LOGGER.info("Disconnected from server.");
        }
    }
}
