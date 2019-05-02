package sample.server;

import org.mindrot.jbcrypt.BCrypt;
import sample.model.*;
import sample.model.enums.ClientState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// stará se o chod serveru, ukládání informací o uživatelech, skupinách, přířazuje ID skupině přes AtomicInteger,
// ukládá zprávy do databáze přes vyhrazené vlákno
public class Server {
    private ServerSocket serverSocket;
    private ConcurrentMap<Long, ObjectOutputStream> clients;
    private DatabaseManager databaseManager;
    private List<Room> rooms;
    private AtomicInteger atomicInteger;
    private BlockingQueue<Message> messages;

    public Server() {
        clients = new ConcurrentHashMap<>();
        rooms = new CopyOnWriteArrayList<>();
        atomicInteger = new AtomicInteger();
        databaseManager = new DatabaseManager();
        messages = new LinkedBlockingQueue<>();
        handleMessages.start();
    }

    // stará se o privátní zprávy, které přijdou na server a ukládá je do DB
    private Thread handleMessages = new Thread(() -> {
        while(true){
            try {
                Message message = messages.take();
                databaseManager.saveMessage(message.getUniqueID(), message.getId(), message.getSenderId(),
                        message.getMessage());
            } catch (InterruptedException e){
                e.printStackTrace();
                break;
            }
        }
    });


    // JEDNO vlákno na jednoho klienta, obstárává toho daného klienta po dobu, kdy je online
    private class ClientHandler implements Runnable {
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Socket socket;


        ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                close();
                e.printStackTrace();
            }
        }

        // metoda vykonananá při spuštění vlákna - prakticky obsluhuje klienta po celou dobu
        @Override
        public void run() {
            boolean logged = false;
            while (!logged) {
                try {
                    Message msg = (Message) in.readObject();
                    int protocol = msg.getProtocol();
                    switch (protocol) {
                        case ServerProtocols.LOGIN:
                            if (login(msg.getName(), msg.getPassword())) {
                                logged = true;
                            }
                            break;
                        case ServerProtocols.REGISTER:
                            register(msg.getName(), msg.getPassword(), msg.getEmail());
                            break;
                        default:
                            System.out.println("Unknown protocl");
                            break;
                    }
                } catch (IOException e) {
                    close();
                    break;
                } catch (ClassNotFoundException exc) {
                    break;
                }
            }
        }

        // hlavní obsluha klienta volaná po úspěšném LOGINU
        private void serve() {
            while (true) {
                Message m = null;
                try {
                    m = (Message) in.readObject();
                    int protocol = 0;
                    if (m != null) {
                        protocol = m.getProtocol();
                    }
                    switch (protocol) {
                        case ServerProtocols.PRIVATE:
                            sendMessage(m);
                            messages.add(m);
                            break;
                        case ServerProtocols.GROUP:
                            sendBroadcastMessage(m, getRoomById(m.getRoomId()));
                            break;
                        case ServerProtocols.CREATE_ROOM:
                            Client client = databaseManager.getClient(m.getId());
                            createRoom(client);
                            break;
                        case ServerProtocols.ADD_TO_GROUP:
                            addClientToGroup(m.getId(), m.getRoomId());
                            break;
                        case ServerProtocols.LEAVEGROUP:
                            leaveGroup(m);
                            break;
                        case ServerProtocols.DESTROY:
                            destroyGroup(m.getRoomId(), 0);
                            break;
                        case ServerProtocols.GET_ALL_FRIENDS:
                            getAllFriends(m.getId(), "accepted");
                            break;
                        case ServerProtocols.QUERY:
                            query(m.getName());
                            break;
                            case ServerProtocols.ADD_FRIEND:
                                addFriend(m.getSenderId(), m.getId());
                                break;
                        case ServerProtocols.REQUEST_ACCEPTED:
                                handleRequest(m.getProtocol(), m.getId(), m.getSenderId());
                            break;
                        case ServerProtocols.REQUEST_DECLINED:
                            handleRequest(m.getProtocol(), m.getId(), m.getSenderId());
                            break;
                        case ServerProtocols.REMOVE_FRIEND:
                            removeFriendRequest(m.getSenderId(), m.getId());
                                break;
                        case ServerProtocols.LOAD_MESSAGE:
                            getMessages(m.getSenderId(), m.getId());
                            break;
                        case ServerProtocols.CHANGE_ROOM_NAME:
                            changeRoomName(m);
                            break;
                        default:
                            System.out.println("UNSUPPORTED PROTOCOL " + protocol);
                            break;
                    }
                } catch (IOException | ClassNotFoundException e) {
                    close();
                    break;
                }
            }
        }

        // nastavuje nové jméno na danou room a posíláme informace o změně jména
        // na všechny klienty v té skupině
        private void changeRoomName(Message m) {
            Room room = getRoomById(m.getRoomId());
            room.setName(m.getName());
            room.getOnlineClients().forEach(folder -> {
                sendResponseToDifferentClient(folder.getId(), m);
            });
        }

        // odstranuje žádost o přítelství, potřebuji jména klientů, proto je získávám z DB
        // a pokud je klient s ID online, pošlu mu informaci o tom, že má odstranit toho
        // daného klienta a to samé pošlu na druhou stranu
        private void removeFriendRequest(long senderId, long id) {
            if(databaseManager.removeRequest(id, senderId) > 0){
                Message m;
                Client IDclient = databaseManager.getClient(id);
                Client senderClient = databaseManager.getClient(senderId);
                if(clients.containsKey(id)){
                    sendResponse(ServerProtocols.REMOVE_FRIEND, id, senderId, senderClient.getName());
                }
                m = new Message(ServerProtocols.REMOVE_FRIEND, IDclient.getId(), IDclient.getName(),
                        clients.containsKey(id) ? ClientState.ONLINE : ClientState.OFFLINE);
                sendNormalMessage(m);
            }
        }

        // posílá odpověd o klientovi (senderId) na klienta s ID
        private void sendResponse(int protocol, long id, long senderId, String name){
            Message m = new Message(protocol);
            m.setId(senderId);
            m.setName(name);
            m.setState(clients.containsKey(senderId) ? ClientState.ONLINE : ClientState.OFFLINE);
            sendResponseToDifferentClient(id, m);
        }

        // stará se o vyřízení žádosti o přátelství. Pokud je požadavek přijat, uloží se informace do databáze,
        // získají se informace o klientovi s ID a senderId a pošle se odpověd klientovi IDclient a pošle se informace
        // o klientovi senderClient za předpokladu, že IDclient je ONLINE. Vždy se odešle zpráva danému klientovi
        // (tedy přímo přes outputStream v té dané třídě) informace o tom daném klientovi a jeho stav.
        // Pokud dojde k zamítnutí žádosti, vymaže se z databáze informace a pošle se o tom informace na daného klienta
        //, kde se uvede ID klienta, který se chtěl stát přítelem a protokol
        private void handleRequest(int protocol, long id, long senderId) {
            Message m;
            if(protocol == ServerProtocols.REQUEST_ACCEPTED){
                databaseManager.updateRequest(id, senderId);
                Client IDclient = databaseManager.getClient(id);
                Client senderClient = databaseManager.getClient(senderId);
                    if(clients.containsKey(id)){
                        sendResponse(ServerProtocols.REQUEST_ACCEPTED, id, senderId, senderClient.getName());
                    }
                    m = new Message(ServerProtocols.REQUEST_ACCEPTED, IDclient.getId(), IDclient.getName(),
                            clients.containsKey(id) ? ClientState.ONLINE : ClientState.OFFLINE);
                    sendNormalMessage(m);
                }
                else {
                databaseManager.removeRequest(id, senderId);
                m = new Message(ServerProtocols.REQUEST_DECLINED);
                m.setId(id);
                sendNormalMessage(m);
            }
        }

        // přidá request do databáze a pošle informaci o tom, že někdo chce se stát
        // kamarád
        private void addFriend(long sender, long receiver){
                if(databaseManager.addRequest(sender, receiver) > 0){
                    Message m = new Message(ServerProtocols.ADD_FRIEND);
                    Client client = databaseManager.getClient(sender);
                    m.setSenderId(sender);
                    m.setName(client.getName());
                    if(clients.containsKey(receiver)){
                        try {
                            clients.get(receiver).writeObject(m);
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        // posílá výsledek dotazu
        private void query(String name) {
            sendNormalMessage(new Message(ServerProtocols.QUERY, parseClientToMessage(databaseManager.query(name))));
        }

        // parsuje klienty na zprávy
        private List<Message> parseClientToMessage(List<? extends Folder> list){
            return  list.stream()
                    .map(client -> {
                        Message m = new Message();
                        m.setId(client.getId());
                        m.setName(client.getName());
                        return m;
                    })
                    .collect(Collectors.toList());
        }

        // získá všechny přátele, bud accepted - což jsou všichni "schválení" přátelé,
        // pokud je type requested, tak to jsou žádosti o přátelství
        private void getAllFriends(long id, String type) {
            List<Client> listOfFriends = databaseManager.getAllFriends(id, type);
            int protocol = type.equals("accepted")?ServerProtocols.GET_ALL_FRIENDS : ServerProtocols.ADD_FRIENDS;
            sendNormalMessage(new Message(protocol, parseClientToMessage(listOfFriends)));
        }

        // nastavuje STAVY klientů a posílá
        private void getAllClients(long id){
            List<Message> messages;
            messages = setStates(id).stream()
                    .map(client -> new Message(0, client.getId(), client.getName(), client.getState()))
                    .collect(Collectors.toList());
            sendNormalMessage(new Message(ServerProtocols.GET_ALL_FRIENDS, messages));
        }

        // získá room podle id
        private Room getRoomById(long roomId){
            return rooms.stream()
                    .filter(folder -> folder.getId() == roomId)
                    .reduce((a, b) -> {
                        throw new IllegalStateException("Multiple elements: " + a + ", " + b);
                    })
                    .get();
        }

        // pokud se někdo odlogne a vytvořil nějakou skupinu, tak pošli informaci o tom
        // na klienty
        private void removeGroupAfterLeave(long roomId, long id){
            if (clients.containsKey(id)) {
                Message msg = new Message(ServerProtocols.DESTROY);
                msg.setRoomId(roomId);
                sendResponseToDifferentClient(id, msg); // ADDED
            }
        }

        // broadcast zprava
        private void sendBroadcastMessage(Message m, Room room){
            room.getOnlineClientsSafe().forEach(folder -> {
                if (clients.containsKey(folder.getId())) {
                    sendResponseToDifferentClient(folder.getId(), m);
                }
            });
        }

        // odstranění uživatele z dané room a poslání informace klientum v té skupině
        private void leaveGroup(Message m) {
            Room room = getRoomById(m.getRoomId());
            room.removeUser(m.getId());
            sendBroadcastMessage(m, room);
            removeGroupAfterLeave(m.getRoomId(), m.getId());
        }


        // odstraní danou room a pošle informaci všem klientům v té skupině, že
        // skupina je zrušena
        private void destroyGroup(long roomId, long cancelledClient){
                Room cancelledRoom = getRoomById(roomId);
                rooms.removeIf(room -> room.getId() == roomId);
                Message m = new Message(ServerProtocols.DESTROY);
                m.setRoomId(roomId);
                cancelledRoom.getOnlineClients().stream()
                        .filter(folder -> clients.containsKey(folder.getId()) && folder.getId() != cancelledClient)
                        .forEach(folder -> sendResponseToDifferentClient(folder.getId(), m));
        }

        // posílám všem onlineKlientům informace o nově připojeném klientovi
        private void sendLoginInformation(List<? extends Folder> onlClients, Client baseClient, int prot
        , long roomId){
            onlClients.stream()
                    .filter(folder -> folder.getId() != baseClient.getId())
                    .forEach(folder -> {
                        sendResponseToDifferentClient(folder.getId(), new Message(prot, baseClient.getId(),
                                baseClient.getName(), ClientState.ONLINE, roomId));
                    });
        }

        // ONLINEKLIENTI
        private List<Client> getAllOnlineClientsById(long id){
           return databaseManager.getAllFriends(id, "accepted").stream()
                    .filter(client -> clients.containsKey(client.getId()))
                    .collect(Collectors.toList());
        }

        // všem online klientům posílám zprávu o odpojení daného uživatele
        private void handleDisconnect(long id){
            getAllOnlineClientsById(id).forEach(client -> {
                sendResponseToDifferentClient(client.getId(), new Message(ServerProtocols.DISCONNECTED, id));
            });
        }

        // posílám zprávu
        private void sendNormalMessage(Message m){
            try {
                out.writeObject(m);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        // přidávám klienta do room, poté 10milisekund čekám, je to z důvodu toho, že běží v addClient
        // runnable (Platform.runLater), takže bez zpomalení dojde k tomu ,že není viditelná změna v dalším
        // kroku. Poté vytvořím message, pošlu na daného klienta a pošlu danému klientovi všechny klienty
        // v dané room
        private void addClientToGroup(long id, long roomId) {
            Client client = databaseManager.getClient(id);
            Room room = getRoomById(roomId);

            // přidávám klienta nového do dané skupiny
            room.addClient(client);

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Message ms = new Message(ServerProtocols.ADD_TO_GROUP, parseClientToMessage(room.getOnlineClientsSafe()));
            ms.setId(room.getSuperUser().getId());
            ms.setRoomId(room.getId());
            ms.setName(room.getName());

            sendResponseToDifferentClient(id, ms);

            sendLoginInformation(room.getOnlineClients(), client, ServerProtocols.ADD_TO_GROUP, room.getId());
        }


        // pošlu zprávu danému klientovi
        private void sendResponseToDifferentClient(long id, Message m){
            try {
                clients.get(id).writeObject(m);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        // metoda nastaví počíteční stavy, pokud je klient v clients, tak se mu nastaví, že je online
        // jinak je offline
        private List<Client> setStates(long id){
            List<Client> friends = databaseManager.getAllFriends(id, "accepted");
            friends.forEach(client -> {
                client.setState(clients.containsKey(client.getId()) ? ClientState.ONLINE : ClientState.OFFLINE);
            });

            return friends;
        }

        // Vytvořím room, nastavím ji ID a přidám ji do všech rooms na serveru a přidám klienta (admina) do kolekce
        // online uživatelů v dané room
        private void createRoom(Client admin) {
            int id = atomicInteger.incrementAndGet();
            Room room = new Room(admin, id, admin + "s room");
            rooms.add(room);
            room.addClient(admin);
            Message m = new Message(ServerProtocols.CREATE_ROOM, room.getId(), admin.getId());
            m.setName(room.getName());
            sendNormalMessage(m);
        }

        // pokud registrace proběhne úspěšně, tak pošlu Message s protokolem SUCCESS, pokud ne - tak FAILED
        private void register(String name, String password, String email) {
            boolean success = databaseManager.registerClient(name ,password, email);
            if (success) {
                sendNormalMessage(new Message(ServerProtocols.REGISTRATION_SUCCESS));
            } else {
                sendNormalMessage(new Message(ServerProtocols.REGISTRATION_FAILED));
            }
        }

        // zjistí, zda dané zhashované heslo sedí s plain heslem
        private boolean checkPass(String plain, String hashedPsw){
            if(BCrypt.checkpw(plain, hashedPsw)){
                return true;
            }
            return false;
        }
        // Když klient bude null - tedy nedostaneme ho z databáze, tak pošleme LOGIN_FAILED, pokud nebude null
        // tedy je v databázi a zároven clients neobsahují tohoto klienta (což je ochrana proti tomu, aby se
        // někdo lognul víckrát) - tak vložím do clients a spouštím inicializační metody - všechny online přátelé,
        // přátelé a metodu serve
        private boolean login(String email, String password) {
            Client client = databaseManager.clientLogin(email);
            boolean logged = client == null ? false : checkPass(password, client.getPassword());
            if (logged && !clients.containsKey(client.getId())) {
                // klient se úspěšně lognul, můžu přidat do kolekce
                clients.put(client.getId(), out);
                // informace o úspěchu
                    sendNormalMessage(new Message(ServerProtocols.LOGIN_SUCCESS, client.getId(),
                            client.getName(), ClientState.ONLINE));
                    //  svým přátelům posílám informaci o tom, že jsem se lognul
                sendLoginInformation(getAllOnlineClientsById(client.getId()), client,
                        ServerProtocols.ADD_CLIENT, 0);
                // sobě posílám své přátelé
                getAllClients(client.getId());
                // sobě posílám žádosti o přátelství
                getAllFriends(client.getId(), "requested");
                serve();
                return true;
            } else {
                // informuji klienta o neúspěchu
                sendNormalMessage(new Message(ServerProtocols.LOGIN_FAILED));
            }
            return false;
        }

        // pošlu sobě zprávy z databáze
        private void getMessages(long id, long id2){
            List<Message> messages = databaseManager.getMessages(id, id2);
            // pošlu sobě všechny chaty
            sendNormalMessage(new Message(ServerProtocols.LOADED_MESSAGES, messages));
        }

        // poslání zprávy - danému člověku + sobě (chci přidat obě zprávy do chatu)
        private void sendMessage(Message message) {
            try {
                if(clients.containsKey(message.getId())){
                    clients.get(message.getId()).writeObject(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // získám ID dle outputstreamu v mapě, vyjmu z onlineKlientů a poté odstraním všechny rooms
        // kde byl daný klient superUserem a odeberu ho ze všech skupin, kde byl online
        private void disconnected() {
            Long id = getKeysByValue(clients, out);
            if (id != null) {
                 clients.remove(id); // concurrentHashMap - není třeba synchronizace
                 handleDisconnect(id);
                 getAllUsersRoom(id).forEach(room -> destroyGroup(room.getId(), id));
                 leaveAfterDisconnection(id);
            }
        }

        // všechny rooms, kde byl daný uživatel superUser
        private List<Room> getAllUsersRoom(long id) {
            return rooms.stream()
                    .filter(room -> room.getSuperUser().getId() == id)
                    .collect(Collectors.toList());
        }

        // pokud daná obsahuje tohoto klienta (s daným id) - tak pošlu na všechny klienty informaci o tom
        // co bylo odebráno
        private void leaveAfterDisconnection(long id) {
            for (Room room : rooms) {
                if (room.getOnlineClients().contains(new Client(id, null))) {
                    leaveGroup(new Message(ServerProtocols.LEAVEGROUP, room.getId(), id));
                }
            }
        }
        // uzavírání vlákna - disconnected, uzavřu outputStream, inputStream a socket
        private void close() {
            try {
                disconnected();
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // vrátí kllíč na základě outputStreamu
        private Long getKeysByValue(Map<Long, ObjectOutputStream> map, ObjectOutputStream value) {
            for (Map.Entry<Long, ObjectOutputStream> entry : map.entrySet()) {
                if (entry.getValue().equals(value)) {
                    return entry.getKey();
                }
            }
            return null;
        }
    }
    // spustí server a nastartuje nové vlákno, pokud se někdo připojí
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {

            while (true) {
                ClientHandler clientHandler = new ClientHandler(serverSocket.accept());
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}