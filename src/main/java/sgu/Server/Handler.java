package sgu.Server;

import sgu.Common.Command;
import sgu.Common.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class Handler implements Runnable {
    Connection connection;
    String userName;
    Handler partner;
    boolean pendingInvite = false;
    Set<String> rejected = new HashSet<>();

    public Handler(Socket s, String id) throws IOException {
        this.connection = new Connection(s, id);
    }

    public void run() {
        try {
            connection.handle(new Connection.MessageHandler() {
                @Override
                public void onRequest(Message message) throws IOException {
                    Command cmd = message.cmd();
                    switch (cmd) {
                        case REGISTER -> register(message.data());
                        case ACCEPT -> accept(message.data());
                        case REJECT -> reject(message.data());
                        case LEAVE -> leave();
                        case MESSAGE -> message(message.data());
                        case CLOSE -> close();
                        default -> {
                            connection.send(Command.ERROR, "Invalid Command");
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void register(String data) throws IOException {
        System.out.println("Register user: " + data);
        this.userName = data;
        if (Server.clients.containsKey(data)) {
            this.connection.send(Command.USERNAME_EXISTED);
        } else {
            Server.clients.put(this.userName, this);
            this.connection.send(Command.REGISTER_SUCCESS, this.userName);
            findPartner();
        }
    }

    public void findPartner() throws IOException {
        for (String userName : Server.waitingList) {
            Handler handler = Server.clients.get(userName);
            boolean notSelf = !handler.userName.equals(this.userName);
            boolean notPaired = handler.partner == null;
            boolean notRejected = !this.rejected.contains(handler.userName);
            if (notSelf && notPaired && notRejected) {
                this.invite(handler);
                break;
            }
        }
        if (!pendingInvite && partner == null) {
            addToWaiting();
        }
    }

    public void addToWaiting() throws IOException {
        connection.send(Command.WAITING);
        Server.waitingList.add(userName);
    }

    public void invite(Handler target) throws IOException {
        System.out.println(this.userName + " invite " + target.userName);
        this.connection.send(Command.INVITE, target.userName);
        this.pendingInvite = true;
    }

    public void reject(String userName) throws IOException {
        this.rejected.add(userName);
        this.pendingInvite = false;
        findPartner();
    }

    public void accept(String userName) throws IOException {
        Handler target = Server.clients.get(userName);
        this.pendingInvite = false;

        if (target == null || target.partner != null) {
            this.connection.send(Command.PAIR_FAIL);
            findPartner();
        } else {
            this.partner = target;
            this.partner.partner = this;

            this.connection.send(Command.PAIRED, userName);
            this.partner.connection.send(Command.PAIRED, this.userName);

            Server.waitingList.remove(this.userName);
            Server.waitingList.remove(userName);
        }
    }

    public void message(String data) throws IOException {
        if(this.partner != null)
            this.partner.connection.send(Command.MESSAGE, data);
    }

    public void unPair() throws IOException {
        if(this.partner != null){
            this.rejected.add(this.partner.userName);
            this.partner = null;
            this.connection.send(Command.UNPAIRED);
            findPartner();
        }
    }

    public void leave() throws IOException {
        if(this.partner != null)
            this.partner.unPair();
        this.unPair();
    }

    public void close() throws IOException {
        leave();
        this.connection.close();
        Server.waitingList.remove(this.userName);
        Server.clients.remove(this.userName);
    }
}