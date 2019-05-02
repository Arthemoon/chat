package sample.model;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

// třída, ze které dědí ROOM a CLIENT
// společné vlastnosti: id, name, onlineClients
public class Folder {

    private ObservableList<Client> onlineClients;
    private long id;
    // slouží k bindingu s komponentou Label
    private SimpleStringProperty name = new SimpleStringProperty();

    public Folder(long id, String name){
        onlineClients = FXCollections.observableArrayList();
        this.id = id;
        this.name = new SimpleStringProperty(name);
        com.sun.javafx.application.PlatformImpl.startup(()->{}); // řeší TOOLKIT INITIZIALIZATION EXC
    }

    public Folder(){
        onlineClients = FXCollections.observableArrayList();
    }

    // odebere klienta z kolekce, není třeba synchronizace, protože Platform.runLater zajistí,
    //    // že to proběhne v jednom konkrétním vlákně
    public void removeUser(long id) {
        Platform.runLater(() -> {
            onlineClients.removeIf(client -> client.getId() == id);
        });
    }

    // přidá klienta do kolekce, není třeba synchronizace, protože Platform.runLater zajistí,
    // že to proběhne v jednom konkrétním vlákně
    public void addClient(Client client){
        if(!onlineClients.contains(client)){
            Platform.runLater(() -> {
                onlineClients.add(client);
            });
        }
    }

    public ObservableList<Client> getOnlineClients() {
        return this.onlineClients;
    }

    // ID se nepoužívá při práci s vlákny, není třeba synch
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name.get();
    }

    // nastavuje jméno - není třeba synchronizovat - proběhne to v jednom vlákne
    public void setName(String name) {
        Platform.runLater(() -> {
            this.name.set(name);
        });
    }

    public void setUserName(String  name){
        this.name.set(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Folder)) return false;
        Folder folder = (Folder) o;
        return id == folder.id;
    }

    public SimpleStringProperty getSimpleName(){
        return name;
    }

    // řeší concurrent exception
    public List<Client> getOnlineClientsSafe(){
        return new CopyOnWriteArrayList<>(onlineClients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
