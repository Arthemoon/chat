package sample.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import sample.controller.Interfaces.WindowCloseHandler;
import sample.model.Client;
import sample.model.Folder;
import sample.model.Message;
import sample.model.enums.ClientState;
import sample.server.ServerProtocols;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

// slouží pro výběr uživatele, kterého chce daný klient přidat do skupiny
public class AddingController extends AbstractController implements Initializable {

    private Folder activeRoom;
    private Client activeClient;


    private WindowCloseHandler windowCloseHandler = new WindowCloseHandler() {
        @Override
        public void handleClose(Event event) {
            activeClient.handleLogout();
            Platform.exit();
        }
    };


    @FXML
    private ComboBox<Folder> groupChoices;

    // přidává daného klienta do skupiny, respektive pošle požadavek na server o přidání daného klienta a uzavře stage
    @FXML
    void addToGroup(ActionEvent event) {
        Folder folder = groupChoices.getValue();
        if(folder != null){
            activeClient.sendMessage(new Message(ServerProtocols.ADD_TO_GROUP, activeRoom.getId(),
                    folder.getId()));
            Stage stage = (Stage) groupChoices.getScene().getWindow();
            stage.close();
        }
    }

    public AddingController(AccessData accessData) {
        super(accessData);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeRoom = getAccessData().getSelectedRoom();
        activeClient = getAccessData().getActiveClient();
        List<Client> onlines = activeClient.getAllFriends().stream().filter(client -> client.getState() == ClientState.ONLINE).collect(Collectors.toList());
        ObservableList<Folder> online = FXCollections.observableArrayList(onlines);
        groupChoices.setItems(online);
    }

    @Override
    public WindowCloseHandler getWindowHandler() {
        return this.windowCloseHandler;
    }
}
