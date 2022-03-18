package com.geekbrains.client;

import com.geekbrains.CommonConstants;
import com.geekbrains.server.ServerCommandConstants;
import com.geekbrains.server.authorization.AuthenticationData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Network {
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private String userLogin;

    private final ChatController controller;

    public Network(ChatController chatController) {
        this.controller = chatController;
    }

    private void startReadServerMessages() throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        String messageFromServer = inputStream.readUTF();
                        System.out.println(messageFromServer);
                        if (messageFromServer.startsWith(ServerCommandConstants.JOIN)) {
                            String[] client = messageFromServer.split(" ");
                            controller.displayClient(client[1]);
                            controller.displayMessage("Пользователь " + client[1] + " зашёл в чат");
                        } else if (messageFromServer.startsWith(ServerCommandConstants.EXIT)) {
                            String[] client = messageFromServer.split(" ");
                            controller.removeClient(client[1]);
                            controller.displayMessage(client[1] + " покинул чат");
                        } else if (messageFromServer.startsWith(ServerCommandConstants.CLIENTS)) {
                            String[] client = messageFromServer.split(" ");
                            for (int i = 1; i < client.length; i++) {
                                controller.displayClient(client[i]);
                            }
                        } else {
                            controller.displayMessage(messageFromServer);
                        }
                    }
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }).start();
    }

    private void initializeNetwork() throws IOException {
        socket = new Socket(CommonConstants.SERVER_ADDRESS, CommonConstants.SERVER_PORT);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
    }


    public void sendMessage(String message) {
        try {
            outputStream.writeUTF(message);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public AuthenticationData sendAuth(String login, String password) {
        AuthenticationData authenticationData = new AuthenticationData();
        authenticationData.setAuthenticated(false);
        authenticationData.setUsername(null);
        try {
            if (socket == null || socket.isClosed()) {
                initializeNetwork();
            }
            outputStream.writeUTF(ServerCommandConstants.AUTHENTICATION + " " + login + " " + password);


            authenticationData.setAuthenticated(inputStream.readBoolean());
            authenticationData.setUsername(inputStream.readUTF());
            if (authenticationData.isAuthenticated()) {
                startReadServerMessages();
            }

            setUserLogin(login);

            return authenticationData;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return authenticationData;
    }

    public void closeConnection() {
        if (socket != null) {
            try {
                outputStream.writeUTF(ServerCommandConstants.EXIT);
                outputStream.close();
                inputStream.close();
                socket.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

}
