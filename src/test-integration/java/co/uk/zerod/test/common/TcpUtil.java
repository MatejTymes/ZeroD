package co.uk.zerod.test.common;

import java.io.IOException;
import java.net.ServerSocket;

import static java.net.InetAddress.getLocalHost;

public class TcpUtil {

    public static int getFreeServerPort() {
        try (ServerSocket serverSocket = new ServerSocket(0, 0, getLocalHost())) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
