package com.geekbrains.server;

import com.geekbrains.server.authorization.AuthenticationData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private final Server server;
    private final Socket socket;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;

    private String nickName;
    private String login;

    public String getNickName() {
        return nickName;
    }

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        authentication();
                        readMessages();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException exception) {
            throw new RuntimeException("Проблемы при создании обработчика");
        }
    }

    public void authentication() throws IOException {
        while (true) {
            String message = inputStream.readUTF();
            if (message.startsWith(ServerCommandConstants.AUTHENTICATION)) {
                String[] authInfo = message.split(" ");
                String username = authInfo[1];
                String password;
                try {
                    password = authInfo[2];
                } catch (IndexOutOfBoundsException e) {
                    password = "";
                }
                String nickName = server.getAuthService().getNickNameByLoginAndPassword(username, password);
                login = username;
                AuthenticationData authenticatedData = new AuthenticationData();
                authenticatedData.setAuthenticated(false);
                authenticatedData.setUsername(null);
                if (nickName != null) {
                    if (!server.isNickNameBusy(nickName)) {
                        authenticatedData.setUsername(nickName);
                        authenticatedData.setAuthenticated(true);
                        sendAuthenticationMessage(authenticatedData);
                        this.nickName = nickName;
                        server.broadcastMessage(ServerCommandConstants.JOIN + " " + nickName);
                        sendMessage(server.getClients());
                        server.addConnectedUser(this);
                        return;
                    } else {
                        sendAuthenticationMessage(authenticatedData);
                    }
                } else {
                    sendAuthenticationMessage(authenticatedData);
                }
            }
        }
    }

    private void sendAuthenticationMessage(AuthenticationData authenticatedData) throws IOException {
        outputStream.writeBoolean(authenticatedData.isAuthenticated());
        outputStream.writeUTF(authenticatedData.getUsername());
    }

    private void readMessages() throws IOException {
        while (true) {
            String messageInChat = inputStream.readUTF();
            System.out.println("от " + nickName + ": " + messageInChat);
            if(messageInChat.startsWith(ServerCommandConstants.PRIVATE_MESSAGE)) {
                String[] privMsg = messageInChat.split(" ");
                server.privateMessage(privMsg[1], messageInChat.substring(messageInChat.indexOf(privMsg[1]) + privMsg[1].length() + 1, messageInChat.length()));
                continue;
            } else if(messageInChat.startsWith(ServerCommandConstants.CHANGE_NICKNAME)) {
                String[] changeNick = messageInChat.split(" ");
                String old_nickName = nickName;
                this.nickName = changeNick[2];
                int userId = server.getUserIdByLogin(login);
                server.changeNickname(userId, nickName);
                server.broadcastMessage(old_nickName + " сменил никнейм на " + nickName);
                continue;
            } else if (messageInChat.equals(ServerCommandConstants.EXIT)) {
                closeConnection();
                return;
            }

            server.broadcastMessage(nickName + ": " + messageInChat);
        }
    }

    public void sendMessage(String message) {
        try {
            outputStream.writeUTF(message);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void closeConnection() {
        server.disconnectUser(this);
        server.broadcastMessage(ServerCommandConstants.EXIT + " " + nickName);
        try {
            outputStream.close();
            inputStream.close();
            socket.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
