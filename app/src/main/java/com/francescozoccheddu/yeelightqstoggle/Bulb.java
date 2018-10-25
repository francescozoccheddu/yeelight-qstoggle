package com.francescozoccheddu.yeelightqstoggle;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.BufferedOutputStream;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public final class Bulb {

    public interface ToggleCommandListener {
        void onCommandSent();

        void onSocketException(Exception exception);
    }

    private final InetAddress inet;
    private final int port;

    private static final String CMD_TOGGLE_TCP_MESSAGE = "{\"id\":0,\"method\":\"toggle\",\"params\":[]}\r\n";

    public static Bulb fromAddress(String address) {
        try {
            final URI uri = new URI("my://" + address);
            return new Bulb(InetAddress.getByName(uri.getHost()), uri.getPort());
        } catch (Exception e) {
            return null;
        }
    }

    public Bulb(InetAddress inet, int port) {
        if (inet == null) {
            throw new IllegalArgumentException("Null address");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid port");
        }
        this.inet = inet;
        this.port = port;
    }

    private static final int CMD_TOGGLE_MSG_SENT = 1;
    private static final int CMD_TOGGLE_MSG_EXCEPTION = 2;

    private static final int SOCKET_TIMEOUT = 5000;

    public void sendToggleCommand(ToggleCommandListener commandListener) {
        final Handler handler = commandListener == null ? null : new Handler(Looper.myLooper()) {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case CMD_TOGGLE_MSG_SENT:
                        commandListener.onCommandSent();
                        break;
                    case CMD_TOGGLE_MSG_EXCEPTION:
                        commandListener.onSocketException((Exception) msg.obj);
                        break;
                }
            }
        };

        final Runnable runnable = () -> {
            try (Socket socket = new Socket(inet, port)) {
                socket.setKeepAlive(true);
                socket.setSoTimeout(SOCKET_TIMEOUT);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
                bufferedOutputStream.write(CMD_TOGGLE_TCP_MESSAGE.getBytes());
                bufferedOutputStream.flush();
                if (handler != null) {
                    handler.sendEmptyMessage(CMD_TOGGLE_MSG_SENT);
                }
            } catch (Exception e) {
                if (handler != null) {
                    final Message msg = new Message();
                    msg.what = CMD_TOGGLE_MSG_EXCEPTION;
                    msg.obj = e;
                    handler.sendMessage(msg);
                }
            }
        };

        new Thread(runnable).start();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Bulb && equals((Bulb) obj);
    }

    public boolean equals(Bulb other) {
        return other != null && inet.equals(other.inet) && port == other.port;
    }

    @Override
    public int hashCode() {
        return inet.hashCode() + port;
    }

    public String getAddress() {
        return inet.getHostAddress() + ":" + port;
    }

    @Override
    public String toString() {
        return getAddress();
    }

    public URI toUri() {
        try {
            return new URI("my://" + getAddress());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static class Discoverer {

        private static final int MSG_FOUND = 0;
        private static final int MSG_EXCEPTION = 1;
        private static final int MSG_STOPME = 2;

        private static final String UDP_REQUEST_MESSAGE = "M-SEARCH * HTTP/1.1\r\n" +
                "HOST:239.255.255.250:1982\r\n" +
                "MAN:\"ssdp:discover\"\r\n" +
                "ST:wifi_bulb\r\n";
        private static final String UDP_RESPONSE_MESSAGE_HEADER = "HTTP/1.1 200 OK";
        private static final String UDP_ANNOUNCE_MESSAGE_HEADER = "NOTIFY * HTTP/1.1";
        private static final String UDP_HOST = "239.255.255.250";
        private static final int UDP_PORT = 1982;
        private final DatagramSocket socket;


        public Discoverer(final int timeout) {

            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                throw new UncheckedIOException(e);
            }

            if (timeout < 1000 || timeout > 120000) {
                throw new IllegalArgumentException("Timeout must be longer than 1 second and shorter than 2 minutes");
            }

            final Runnable runnable = () -> {
                try {
                    socket.setSoTimeout(timeout);
                    DatagramPacket dpSend = new DatagramPacket(UDP_REQUEST_MESSAGE.getBytes(), UDP_REQUEST_MESSAGE.getBytes().length, InetAddress.getByName(UDP_HOST), UDP_PORT);
                    socket.send(dpSend);
                    byte[] buffer = new byte[1024];
                    DatagramPacket dpRecv = new DatagramPacket(buffer, buffer.length);
                    while (true) {
                        socket.receive(dpRecv);
                        String message = new String(dpRecv.getData());
                        Bulb bulb = parseBulb(message);
                        if (bulb != null) {
                            final Message msg = new Message();
                            msg.what = MSG_FOUND;
                            msg.obj = bulb;
                            handler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    socket.close();
                    handler.sendEmptyMessage(MSG_EXCEPTION);
                }
            };

            thread = new Thread(runnable);
            thread.start();

            handler.sendEmptyMessageDelayed(MSG_STOPME, timeout);
        }

        private final Handler handler = new Handler(Looper.myLooper()) {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_FOUND:
                        onDiscover((Bulb) msg.obj);
                        break;
                    case MSG_EXCEPTION:
                        onInterrupted();
                        break;
                    case MSG_STOPME:
                        socket.close();
                        break;
                }
            }

        };
        private final Thread thread;

        public void onDiscover(Bulb bulb) {
        }

        public void onInterrupted() {
        }

        public final void stopSearch() {
            if (isSearching()) {
                socket.close();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                }
            }
        }

        public final boolean isSearching() {
            return thread.isAlive();
        }

        private static Bulb parseBulb(String message) {
            if (message.startsWith(UDP_RESPONSE_MESSAGE_HEADER) || message.startsWith(UDP_ANNOUNCE_MESSAGE_HEADER)) {
                final String locationFieldHeader = "Location: yeelight://";
                final int fieldIndex = message.indexOf(locationFieldHeader);
                if (fieldIndex != -1) {
                    String temp = message.substring(fieldIndex + locationFieldHeader.length());
                    final int fieldEndIndex = temp.indexOf("\r\n");
                    if (fieldEndIndex != -1) {
                        final String location = temp.substring(0, fieldEndIndex);
                        try {
                            final URI uri = new URI("my://" + location);
                            return new Bulb(InetAddress.getByName(uri.getHost()), uri.getPort());
                        } catch (URISyntaxException | UnknownHostException e) {
                        }
                    }
                }
            }
            return null;
        }
    }

}
