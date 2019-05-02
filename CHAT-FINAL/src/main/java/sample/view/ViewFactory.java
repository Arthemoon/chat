package sample.view;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import sample.controller.*;
import sample.controller.Interfaces.WindowCloseHandler;
import sample.model.Client;
import sample.model.Folder;
import sample.model.Message;
import sample.model.Room;
import sample.server.ServerProtocols;
import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ViewFactory {

    public static ViewFactory defaultFactory = new ViewFactory();
    private boolean mainViewInitialized = false;

    private final String DEFAUL_CSS = "style.css";
    private final String LOGIN_FXML = "login.fxml";
    private final String MAIN_FXML = "main.fxml";
    private final String REGISTER_FXML = "register.fxml";
    private final String ADDING_FXML = "adding.fxml";

    private WindowCloseHandler wcHandler;

    private AccessData accessData = new AccessData();

    private Controller controller;
    private LoginController loginController;
    private RegisterController registerController;
    private AddingController addingController;

    // implementace handleClose je ve všech controllerech
    // tato metoda se volá z hlavní metody main
    public final void handleWindowClose(WindowEvent event) {
        if (wcHandler != null) {
            wcHandler.handleClose(event);
        }
    }

    // vrátí mainScenu
    public Scene getMainScene() throws OperationNotSupportedException {
        if(!mainViewInitialized){
            controller = new Controller(accessData); // musí být vložena stejná instance accessData, aby bylo možné
            // přenášet data
            // kontrolery ukazují na stejnou referenci
             mainViewInitialized = true;
            return initialize(MAIN_FXML, controller);
        } else {
            throw new OperationNotSupportedException("Main Scene already initialized");
        }
    }

    public Scene getLoginScene(){
        loginController = new LoginController(accessData);

        return initialize(LOGIN_FXML, loginController);
    }

    public Scene getRegisterScene(){
        registerController = new RegisterController(accessData);

        return initialize(REGISTER_FXML, registerController);
    }

    public Scene getAdddingScene(){
        addingController = new AddingController(accessData);

        return initialize(ADDING_FXML, addingController);
    }

    // slouží k měnění scene a natavení controlleru a CSS a získám tady object wcHandler, díky kterému můžu
    // reagovat na uzavření stage
    private Scene initialize(String fxmlPath, AbstractController abstractController){
        wcHandler = abstractController.getWindowHandler();
        FXMLLoader loader;
        Parent parent;
        Scene scene;
        try {
            loader = new FXMLLoader(getClass().getClassLoader().getResource(fxmlPath));
            loader.setController(abstractController);
            parent = loader.load();
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }
        scene = new Scene(parent);
        scene.getStylesheets().add(getClass().getClassLoader().getResource(DEFAUL_CSS).toExternalForm());
        return scene;
    }

    // // inicializuje menu
    public List<MenuItem> initRequestsMenu(Folder selectedRequest, Client client){
        MenuItem accept = new MenuItem("Accept");
        MenuItem decline = new MenuItem("Decline");
        Message m = new Message();

        m.setId(selectedRequest.getId());
        m.setSenderId(client.getId());

        accept.setOnAction(event -> {
                m.setProtocol(ServerProtocols.REQUEST_ACCEPTED);
                client.sendMessage(m);
        });

        decline.setOnAction(event -> {
            m.setProtocol(ServerProtocols.REQUEST_DECLINED);
            client.sendMessage(m);
        });

        List<MenuItem> items = new ArrayList<>();
        items.add(accept);
        items.add(decline);

        return items;
    }

    // inicializuje menu
    public MenuItem initAllFriendsMenu(Folder selectedUser, Client client){
        MenuItem menuItem = new MenuItem("Remove Friend");
        menuItem.setOnAction(event -> {
            Message m = new Message(ServerProtocols.REMOVE_FRIEND);
            m.setId(selectedUser.getId());
            m.setSenderId(client.getId());
            client.sendMessage(m);
        });
        return  menuItem;
    }

    public MenuItem initAddingMenu(Folder selectedUser, Client client){
        MenuItem menuItem = new MenuItem("Add Friend");
        menuItem.setOnAction(event -> {
            if(client.getId() == selectedUser.getId()){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Friend Request");
                alert.setHeaderText("Error");
                alert.setContentText("You cannot add yourself :-)");

                alert.showAndWait();

            } else if(client.getAllFriends().contains(new Client(selectedUser.getId(), ""))){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Friend Request");
                alert.setHeaderText("Error");
                alert.setContentText("You are already friends :-)");

                alert.showAndWait();

            } else {
                Message m = new Message(ServerProtocols.ADD_FRIEND);
                m.setSenderId(client.getId());
                m.setId(selectedUser.getId());
                client.sendMessage(m);
            }
        });
        return menuItem;
    }

    // // inicializuje menu
    public Set<MenuItem> initMenu(boolean isSuperUser, Client client, Folder selectedClient, ListView<Client> groupOnlineClients,
                                  TextField changeNamer, Button changeBtn, ImageView quitImg){
        MenuItem leave = new MenuItem("Leave a group");
        leave.setOnAction(event -> {
            if(!isSuperUser){
                client.sendMessage(new Message(ServerProtocols.LEAVEGROUP, selectedClient.getId(), client.getId()));
                groupOnlineClients.setVisible(false);
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "You cannot leave group until you are super user of a group.",
                        ButtonType.OK);
                alert.showAndWait();
            }
        });

        MenuItem changeName = new MenuItem("Change name of your group");
        changeName.setOnAction(event -> {
            groupOnlineClients.setVisible(false);
            changeNamer.setVisible(true);
            changeBtn.setVisible(true);
            quitImg.setVisible(true);
        });

        MenuItem add = new MenuItem("Add User");
        add.setOnAction(event -> {
            Scene scene = ViewFactory.defaultFactory.getAdddingScene();
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        });

        MenuItem destroy = new MenuItem("Destroy a group");
        destroy.setOnAction(event -> {
            Message m = new Message(ServerProtocols.DESTROY);
            m.setRoomId(selectedClient.getId());
            client.sendMessage(m);
            groupOnlineClients.setVisible(false);
        });

        Set<MenuItem> items = new LinkedHashSet<>();
        items.add(leave);
        items.add(changeName);
        items.add(add);
        items.add(destroy);

        return items;
    }

    // // inicializuje menu
    public MenuItem initGroupMenu(Folder selectedUser, Client client, Room selectedClient){
        MenuItem menuItem = new MenuItem("Kick");

        menuItem.setOnAction(event -> {
            client.sendMessage(new Message(ServerProtocols.LEAVEGROUP, selectedClient.getId(), selectedUser.getId()));
        });

        return menuItem;
    }



}

