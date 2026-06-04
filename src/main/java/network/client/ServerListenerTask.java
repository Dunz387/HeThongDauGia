package network.client;

import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

class ServerListenerTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ServerListenerTask.class.getName());

    private final ObjectInputStream in;
    private final ServerMessageDispatcher dispatcher;
    private final Runnable onDisconnect;

    ServerListenerTask(ObjectInputStream in, ServerMessageDispatcher dispatcher, Runnable onDisconnect) {
        this.in = in;
        this.dispatcher = dispatcher;
        this.onDisconnect = onDisconnect;
    }

    @Override
    public void run() {
        try {
            Object serverData;
            while ((serverData = in.readObject()) != null) {
                dispatcher.dispatch(serverData);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lost connection to server", e);
            onDisconnect.run();
        }
    }
}
