package sample.model;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import sample.model.enums.ClientState;
import sample.server.ServerProtocols;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Client extends Folder {

    // zprávy k poslání
    private BlockingQueue<Message> messagesToSend;
    // zprávy ke zpracování
    private BlockingQueue<Message> receivedMessages;
    private ObservableList<Client> allFriends;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private ClientState state;
    // výsledek dotazování
    private ObservableList<Folder> tempQuery;
    // aktuální žádosti o přátelství
    private ObservableList<Client> requests;
    // slouží k prvotnímu načtení zpráv, čeká se pomocí metody take na to, až budou dostupné
    private BlockingQueue<Message> initialMessages = new LinkedBlockingQueue<>();
    // slouží ke změně obrázkového stavu klient (ONLINE OFFLINE)
    private ObjectProperty<Image> imageState = new SimpleObjectProperty<>();


    private String password;
    private String email;
    private ObservableList<Room> rooms;


    // Konstruktor pro práci s DB a pro LOGIN
    public Client(long id, String name, String password){
        super(id, name);
        this.password = password;
    }

    // Pro práci v kontrolerech
    public Client(long id, String name){
        super(id, name);
        allFriends = FXCollections.observableArrayList();
        this.setState(ClientState.OFFLINE);
    }


    public Client(long id, String name, ClientState state){
        super(id, name);
        allFriends = FXCollections.observableArrayList();
        this.setState(state);
    }

    //  Pro práci s DB a REGISTRACI
    public Client(long id, String name, String password, String email){
        super(id, name);
        this.password = password;
        this.email = email;
    }

    // Při spuštění aplikace
    public Client(){
        messagesToSend = new LinkedBlockingQueue<>();
        receivedMessages = new LinkedBlockingQueue<>();
        allFriends = FXCollections.observableArrayList();
        this.setState(ClientState.OFFLINE);
        tempQuery = FXCollections.observableArrayList();
        requests = FXCollections.observableArrayList();
        rooms = FXCollections.observableArrayList();


        try {
            socket = new Socket("localhost", 5000);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (UnknownHostException e){
            System.exit(1);
        } catch (IOException e){
            handleLogout();
        }

        readMessages.start();
        sendMessages.start();
    }

    // Vlákno, které se stará o INPUT stream a dle protokolu zpracovává datas
     Thread readMessages = new Thread(() -> {
         Message ms;
             while (true) {
                 try {
                     ms = (Message) in.readObject();
                     switch (ms.getProtocol()) {
                         case ServerProtocols.ADD_CLIENT:
                             addClients(ms.getId(), ms.getName(), ms.getState());
                             break;
                         case ServerProtocols.GET_ALL_CLIENTS_IN_ROOMS:
                             getAllClientsInRooms(ms.getMessages());
                             break;
                         case ServerProtocols.DESTROY:
                             destroyRoom(ms.getRoomId());
                             break;
                         case ServerProtocols.LEAVEGROUP:
                             leaveGroup(ms.getRoomId(), ms.getId());
                             break;
                         case ServerProtocols.GET_ALL_FRIENDS:
                             getFriends(ms.getMessages());
                             break;
                         case ServerProtocols.CREATE_ROOM:
                             createRoom(ms.getId(), ms.getRoomId(), ms.getName());
                             break;
                             case ServerProtocols.ADD_TO_GROUP:
                                 handleAddingToGroup(ms.getMessages(), ms.getRoomId(), ms.getId(), ms.getName(), ms.getState());
                                 break;
                         case ServerProtocols.DISCONNECTED:
                             handleDisconnection(ms.getId());
                             break;
                         case ServerProtocols.QUERY:
                             handleQuery(ms.getMessages());
                             break;
                         case ServerProtocols.ADD_FRIENDS:
                             friendsRequests(ms.getMessages());
                             break;
                             case ServerProtocols.ADD_FRIEND:
                                 Message m = ms;
                                 Platform.runLater(() -> {
                                         requests.add(new Client(m.getSenderId(), m.getName(),
                                                 m.getState()));
                                 });
                                 break;
                         case ServerProtocols.REQUEST_ACCEPTED:
                             handleRequestAccepted(ms.getId(), ms.getName(), ms.getState());
                             break;
                         case ServerProtocols.REQUEST_DECLINED:
                             handleRequestDeclined(ms.getId(), ms.getName());
                             break;
                             case ServerProtocols.REMOVE_FRIEND:
                                 removeFriend(ms.getId(), ms.getName());
                                 break;
                                 case ServerProtocols.LOADED_MESSAGES:
                                     initialMessages.add(ms);
                                     break;
                         case ServerProtocols.CHANGE_ROOM_NAME:
                             getRoomById(ms.getRoomId()).setName(ms.getName());
                             break;
                         default:
                             receivedMessages.add(ms);
                             break;
                     }
                 } catch (ClassNotFoundException | IOException e){
                     break;
                 }
             }
     });

    public BlockingQueue<Message> getInitialMessages(){
        return initialMessages;
    }

    // odstranění žádosti
    private void handleRequestDeclined(long id, String name) {
        Platform.runLater(() -> {
            requests.remove(new Client(id, name));
        });
    }

    // vytvoření nové místnosti
    private void createRoom(long clientId, long roomId, String roomName) {
        Room room = new Room(new Client(clientId, null),
                roomId, roomName);
        Platform.runLater(() -> {
            rooms.add(room);
        });
        room.addClient(this);
    }

    // pokud je ms.getMessages null, tak získej room podle id a přidej klienta,
    // v opačném případě vytvoř room a přidej ji do kolekce Room
    // a v posledním kroku té dané room nastavím onlineKlienty
    private void handleAddingToGroup(List<Message> messages, long roomId, long id, String name, ClientState state) {
        if(messages == null){
            getRoomById(roomId).addClient(new Client(id, name, state));
        } else {
            Room room = new Room(new Client(id, null),
                    roomId, name);
            Platform.runLater(() -> {
                rooms.add(room);
            });
            room.getOnlineClients().setAll(messagesParser(messages));
        }
    }

    // vratí Room podle id
    private Folder getRoomById(long roomId){
        return rooms.stream()
                .filter(folder -> folder.getId() == roomId)
                .reduce((a, b) -> {
                    throw new IllegalStateException("Multiple elements: " + a + ", " + b);
                })
                .get();
    }

    // smaže kamaráda
    private void removeFriend(long id, String name) {
        Client client = new Client(id, name);
        requests.remove(client);
        Platform.runLater(() -> {
            allFriends.remove(client);
        });
    }

    // smaže žádost z kolekce a přidá klienta s daným stavem do allFriends
    private void handleRequestAccepted(long id, String name, ClientState state) {
        Client c = new Client(id, name, state);
        Platform.runLater(() -> {
                requests.remove(c);
        });
        Platform.runLater(() -> {
            allFriends.add(c);
        });
    }

    // nastaví žádosti na ta data, co přišla
    private void friendsRequests(List<Message> messages) {
        requests.addAll(messagesParser(messages));
    }

    // slouží pro zobrazování dotazu před kolekci query
    private void handleQuery(List<Message> messages) {
        Platform.runLater(() -> {
                tempQuery.clear();
                List<Folder> folders = new ArrayList<>(messagesParser(messages));
                tempQuery.addAll(folders);
        });
    }

    // při odpojení - nastaví mu stav OFFLINE
    private void handleDisconnection(long id) {
        Client client = getClientById(id);
        client.setState(ClientState.OFFLINE);
    }

    private void getFriends(List<Message> messages) {
        allFriends.setAll(messagesParser(messages));
    }

    // jestli je daný klient dostupný již v kolekci, tak ho najdi a nastav mu stav na ONLINE
    // pokud ne, přidej ho se stavem ONLINE
    private void addClients(long id, String name, ClientState state) {
        if(allFriends.contains(new Client(id, name, state))) {
            getClientById(id).setState(ClientState.ONLINE);
        } else {
            allFriends.add(new Client(id, name, ClientState.ONLINE));
        }
    }

    // List messages prevéct na Set klientů
    private Set<Folder> updateOnlineClients(List<Message> messages, long id, String name) {
        return  messages.stream()
                .map(message -> new Client(id, name))
                .collect(Collectors.toSet());
    }

    // získej klienta podle ID
    private Client getClientById(long id){
        return allFriends.stream()
                .filter(client -> client.getId() == id)
                .reduce((a, b) -> {
                    throw new IllegalStateException("Multiple elements: " + a + ", " + b);
                })
                .get();
    }

    // Odstranění uživatele z dané skupiny (dle id)
    private void leaveGroup(long roomId, long id) {
        Folder room = (Folder) getRoomById(roomId);
        room.removeUser(id);
    }

    // vymazání skupiny
    private void destroyRoom(long roomId) {
        for(Folder room : rooms){
            if(room.getId() == roomId){
                Platform.runLater(() -> {
                        rooms.remove(room);
                        room.setName(null);
                        room.getOnlineClients().setAll(new ArrayList<>());
                });
            }
        }
    }

    private void getAllClientsInRooms(List<Message> messages) {
        messages.forEach(message -> rooms.forEach(room -> {
            if(message.getRoomId() == room.getId()){
                Client client = new Client(message.getId(), message.getSuperUserName());
                room.addClient(client);
            }
        }));
    }

    public void sendPrivateMessage(Message m){
        receivedMessages.add(m);
        messagesToSend.add(m);
    }

    // parsuje MESSAGE na KLIENTA
    private Set<Client> messagesParser(List<Message> messages){
        return messages.stream()
                .map(message -> new Client(message.getId(), message.getName(), message.getState()))
                .collect(Collectors.toSet());
    }

    // stará se o OUTPUTSTREAM
    Thread sendMessages = new Thread(() -> {
        while(true){
            try {
                Message message = messagesToSend.take();
                out.writeObject(message);
            } catch (IOException e){
                break;
            } catch (InterruptedException exc){
                System.out.println("INTERRUPTED EXC");
                break;
            }
        }
    });

    // NASTAVUJE state a zároven i aktivní obrázek.
    public void setState(ClientState state){
        Image image = new Image(state == ClientState.ONLINE ? "img/state_online.png" :
                "img/state_offline.png");
        imageState.setValue(image);
        this.state = state;
    }

    // poslání zprávy
    public void sendMessage(Message mess){
        messagesToSend.add(mess);
    }

    // získá poslední zprávu z blockingQueue
    public Message getLastMessage(){
        try {
            return receivedMessages.take();
        } catch (Exception e){
            return null;
        }
    }

    public ObservableList<Room> getRooms(){
        return rooms;
    }

    public ObservableList<Client> getRequests(){
        return requests;
    }

    // Když někdo vypne aplikaci, ztratí se spojení atd...
    public void handleLogout() {
        try {
            out.close();
            in.close();
            socket.close();
            System.exit(0);
        } catch (IOException e){
            System.exit(1);
        }
    }

    public ClientState getState(){
        return this.state;
    }

    public ObservableList<Client> getAllFriends(){
        return this.allFriends;
    }

    @Override
    public String toString() {
        return getName();
    }

    public ObservableList<Folder> getTempQuery() {
        return tempQuery;
    }

    public ObjectProperty<Image> imageObjectProperty() {
        return imageState;
    }

    public String getPassword(){
        return password;
    }
}

