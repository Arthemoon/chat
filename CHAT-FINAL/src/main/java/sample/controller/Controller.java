package sample.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import sample.controller.Interfaces.WindowCloseHandler;
import sample.controller.services.*;
import sample.model.*;
import sample.model.enums.ClientState;
import sample.server.ServerProtocols;
import sample.view.Cells.ClientsCell;
import sample.view.Cells.FriendsCell;
import sample.view.Cells.GroupCell;
import sample.view.Cells.RequestCell;
import sample.view.ViewFactory;
import java.net.URL;
import java.util.*;

// Hlavní CONTROLLER
public class Controller extends AbstractController implements Initializable {

    private Client client;
    private Folder selectedClient;

    @FXML
    private TextField changeNamer;
    @FXML
    private ImageView group;
    @FXML
    private ImageView groupCreation;
    @FXML
    private ListView<Message> mainChat;
    @FXML
    private TextField sendField;
    @FXML
    private ListView<Room> groups;
    @FXML
    private ImageView privateChat;
    @FXML
    private Label name;
    @FXML
    private Label chatName;
    @FXML
    private ListView<Client> allFriends;
    @FXML
    private ListView<Client> groupOnlineClients;
    @FXML
    private ImageView friendRequest;
    @FXML
    private ImageView addFriend;
    @FXML
    private ListView<Folder> addingList;
    @FXML
    private Button searchBtn;
    @FXML
    private Label friendsLabel;
    @FXML
    private Button changeBtn;
    @FXML
    private TextField addField;
    @FXML
    private ImageView closeName;
    @FXML
    private ListView<Client> requestView;
    private InitialMessageLoader initialMessageLoader;
    private ContextMenu addingMenu;
    private Set<MenuItem> menuItems;

    private WindowCloseHandler windowCloseHandler = new WindowCloseHandler() {
        @Override
        public void handleClose(Event event) {
            client.handleLogout();
            Platform.exit();
        }
    };

    // pošle zprávu na server - bud jako zprávu z ROOM nebo jako privátní zprávu
    // pozná to podle kliknutého klienta/room (což je selectedClient)
    @FXML
    void sendMessage(ActionEvent event) {
        if(selectedClient != null){
            String msg = sendField.getText();
            if(msg != null && msg.trim().length() > 0){
                if(selectedClient instanceof Client){
                    Message m = new Message(ServerProtocols.PRIVATE, client.getId(),
                            selectedClient.getId(), client.getName(), msg);
                    client.sendPrivateMessage(m);
                } else {
                    Message m = new Message(ServerProtocols.GROUP);
                    m.setSenderId(selectedClient.getId());
                    m.setMessage(msg);
                    m.setRoomId(selectedClient.getId());
                    m.setId(selectedClient.getId());
                    m.setName(client.getName());
                    client.sendMessage(m);
                }
            }
        }
        sendField.clear();
        sendField.requestFocus();
    }


    // žádost o změnu jména skupiny
    @FXML
    void changeName(ActionEvent event) {
        visibleChangingName(false);
        String text = changeNamer.getText();
        if(text != null && text.trim().length() > 0){
            Message m = new Message(ServerProtocols.CHANGE_ROOM_NAME);
            m.setRoomId(selectedClient.getId());
            m.setName(text);
            client.sendMessage(m);
        }
    }

    public Controller(AccessData accessData) {
        super(accessData);
    }

    @Override
    public WindowCloseHandler getWindowHandler() {
        return this.windowCloseHandler;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        client = getAccessData().getActiveClient();

        initScene();
        initListeners();
        initServices();

    }

    // řeší dotaz a zobrazí výsledky
    @FXML
    void search(ActionEvent event) {
        String query = addField.getText();
        if(query != null && query.trim().length() > 0){
            client.sendMessage(new Message(ServerProtocols.QUERY, query));
        }
        addingList.setItems(client.getTempQuery());
    }

    // inicializace úvodní scény
    private void initScene(){
        hideAddingPart(true);
        requestView.setVisible(false);
        groups.setVisible(false);
        groupOnlineClients.setVisible(false);
        name.setText(client.getName());
        allFriends.setItems(client.getAllFriends());
        groups.setItems(client.getRooms());
        requestView.setItems(client.getRequests());
        allFriends.setCellFactory(param -> new FriendsCell(client, getAccessData()));
        visibleChangingName(false);
    }

    // při změně jména - bud shovat vše/odhalit vše
    private void visibleChangingName(boolean visible){
        changeNamer.setVisible(visible);
        changeBtn.setVisible(visible);
        closeName.setVisible(visible);
    }
    // nastavuje listenery
    private void initListeners(){

        requestView.setCellFactory(param -> new RequestCell(client));
        group.setOnMouseClicked(event -> {
            hideAddingPart(true);
            groups.setVisible(true);
            allFriends.setVisible(false);
        });

        closeName.setOnMouseClicked(event -> {
            visibleChangingName(false);
        });

        privateChat.setOnMouseClicked(event -> {
            hideAddingPart(true);
            groupOnlineClients.setVisible(false);
            allFriends.setVisible(true);
            groups.setVisible(false);
        });

        friendRequest.setOnMouseClicked(event -> {
            requestView.setVisible(true);
        });

        // POSÍLÁ žádost o vytvoření místnosti
        groupCreation.setOnMouseClicked(event -> client.sendMessage(new Message(4, client.getId(), client.getName(), ClientState.ONLINE)));

        // nastavuje cell factory podle právě zvolené skupiny
        groupOnlineClients.setOnMouseClicked(event -> {
            Room room = (Room) selectedClient;
            groupOnlineClients.setCellFactory(param -> new ClientsCell(client, room));
        });

        // nastavuje binding na právě klienta a pokud klient ještě nepožádal o historii, tak o ní požádá
        // a spustí se service, který obstará nahrání zpráv
        // nastaví se aktivní chat
        allFriends.setOnMouseClicked(event -> {
            selectedClient = allFriends.getSelectionModel().getSelectedItem();
            if(selectedClient != null){
                chatName.textProperty().bind(Bindings.convert(selectedClient.getSimpleName()));
                if(!getAccessData().getRequested().contains(getAccessData().convertChatID(client.getId(),
                        selectedClient.getId()))){
                    requestHistory(client.getId(), selectedClient.getId());

                    initialMessageLoader = new InitialMessageLoader(getAccessData());
                    initialMessageLoader.start();
                }
                getAccessData().setActiveChat(getAccessData().getChat(client.getId(), selectedClient.getId()));
                mainChat.setItems(getAccessData().getActiveChat());
            }
        });

        // schovává části view
        addFriend.setOnMouseClicked(event -> {
            hideAddingPart(false);
            hideChats(true);
        });

        // nastavuje contextMenu addingListu
        addingList.setOnMouseClicked(event -> {
            Folder selectedUser = addingList.getSelectionModel().getSelectedItem();
            addingMenu = new ContextMenu(ViewFactory.defaultFactory.initAddingMenu(selectedUser,client));
            addingList.setContextMenu(addingMenu);
        });

        // nastavuje menu, zobrazuje klienty ve skupině, nastavuje aktivní chat a zobrazuje zprávy daného
        // chatu
        groups.setOnMouseClicked(event -> {
            groupOnlineClients.setVisible(true);
            selectedClient = groups.getSelectionModel().getSelectedItem();
            Room room = groups.getSelectionModel().getSelectedItem();
            menuItems = ViewFactory.defaultFactory.initMenu(isRoot(room), client, selectedClient, groupOnlineClients, changeNamer, changeBtn,closeName );

            getAccessData().setSelectedRoom(selectedClient);
            if(selectedClient != null){
                chatName.textProperty().bind(Bindings.convert(selectedClient.getSimpleName()));
                groupOnlineClients.setItems(room.getOnlineClients());
                groups.setCellFactory(param -> new GroupCell(isRoot(room), menuItems, getAccessData()));
                getAccessData().setActiveChat(getAccessData().getChat(selectedClient.getId(), selectedClient.getId()));
                mainChat.setItems(getAccessData().getActiveChat());
            }
        });
    }

    // pošle se zpráva na server o poslání historie s daným klientem
    // a zaznamená se informace o tom, že klient zažádal s daným klientem o historii
    private void requestHistory(long id, long id2) {
        Message m = new Message(ServerProtocols.LOAD_MESSAGE);
        m.setSenderId(id);
        m.setId(id2);
        client.sendMessage(m);
        getAccessData().addMessageRequest(getAccessData().convertChatID(client.getId(), selectedClient.getId()));
    }
    // spuštění services - jedno pro roooms, jedno pro chat a jedno pro updatování zpráv
    private void initServices(){
        MessageService messageService = new MessageService(client, getAccessData());
        messageService.start();
    }

    // zjistí, zda tento klient je ROOT uživatelem
    private boolean isRoot(Room room){
        if(room != null){
            return client.getId() == room.getSuperUser().getId();
        }
        return false;
    }

    // schová jisté komponenty view
    private void hideAddingPart(boolean hide){
        if(hide){
            searchBtn.setVisible(false);
            addingList.setVisible(false);
            addField.setVisible(false);
        } else {
            searchBtn.setVisible(true);
            addingList.setVisible(true);
            addField.setVisible(true);
        }
    }

    // schová chaty
    private void hideChats(boolean hide){
        if(hide){
            allFriends.setVisible(false);
            groups.setVisible(false);
        } else {
            allFriends.setVisible(true);
            groups.setVisible(true);
        }
    }


}

