package sample.controller.services;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;
import sample.controller.AccessData;
import sample.model.Client;
import sample.model.Message;

// updatuje chaty zprávami, pokud neexistuje pro danou room, založí novou
public class MessageService extends Service<Void> {

    private Client client;
    private AccessData accessData;

    public MessageService(Client client, AccessData accessData){
        this.client = client;
        this.accessData = accessData;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (true) {
                    Message message = client.getLastMessage();
                    String id = accessData.convertChatID(message.getId(), message.getSenderId());
                    if(message.getId() == message.getSenderId() && !accessData.getAllChats().containsKey(id)){
                        accessData.addChat(message.getId(), message.getSenderId());
                    }
                    accessData.updateData(id, message);
                }
            }
        };
    }
}
