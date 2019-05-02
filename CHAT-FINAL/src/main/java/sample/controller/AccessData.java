package sample.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sample.model.Client;
import sample.model.Folder;
import sample.model.Message;

import java.util.*;

// přenosná třída mezi controllery
public class AccessData {

    // jaký je aktinví klient v aplikaci
    private Client activeClient;
    // jaký je aktivní chat v aplikaci
    private ObservableList<Message> activeChat;
    // jaká je aktivní room v aplikaci
    private Folder selectedRoom;

    // všechny chaty v aplikaci
    private Map<String, ObservableList<Message>> chats = new HashMap<>();
    // udržuje informace o tom, z jakých chatů si již uživatel vyžádal historii.
    private Set<String> requested = new HashSet<>();

    public Client getActiveClient() {
        return activeClient;
    }

    public void setActiveClient(Client activeClient) {
        this.activeClient = activeClient;
    }

    // vrátí chat na základě ID
    public ObservableList<Message> getChat(long id1, long id2) {
        String value = convertChatID(id1, id2);
        if (chats.containsKey(value)) {
            return chats.get(value);
        }
        return null;
    }

    // přidá chat na základě ID, pokud je již vytvořen, tak ho nastaví jako aktivní
    public void addChat(long id1, long id2) {

        String value = convertChatID(id1, id2);

        if (!chats.containsKey(value)) {
            ObservableList<Message> chat = FXCollections.observableArrayList();
            chats.put(value, chat);
            setActiveChat(chat);
        } else {
            setActiveChat(chats.get(value));
        }
    }

    public ObservableList<Message> getActiveChat(){
        return activeChat;
    }

    public void setActiveChat(ObservableList<Message> chat) {
        this.activeChat = chat;
    }

    // vypočítává ID pro chaty
    public String convertChatID(long id1, long id2) {
        String value = "";
            if (id1 > id2) {
                value = id2 + "" + id1;
                return  value;
            } else if(id1 < id2){
                value = id1 + "" + id2;
                return  value;
            } else if(id1 == id2) {
                value = id1 + "" + id1;
                return value;
            }
        return null;
    }

    // updatuje určité chaty (podle id)
    public void updateData(String id, Message message){
        if(chats.containsKey(id)){
            Platform.runLater(() -> chats.get(id).add(message));
        }
    }


    public Folder getSelectedRoom() {
        return selectedRoom;
    }

    public void setSelectedRoom(Folder selectedRoom) {
        this.selectedRoom = selectedRoom;
    }

    public void addMessageRequest(String key){
        requested.add(key);
    }

    public Set<String> getRequested(){
        return requested;
    }

    // updatuje data - přidává kolekci
    public void updateData(String id, List<Message> messages){
        if(chats.containsKey(id)){
            Platform.runLater(() -> chats.get(id).addAll(messages));
        }
    }

    public Map<String, ObservableList<Message>> getAllChats(){
        return this.chats;
    }
}

