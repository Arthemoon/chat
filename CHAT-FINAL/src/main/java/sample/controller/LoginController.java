package sample.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import sample.controller.Interfaces.WindowCloseHandler;
import sample.model.Message;
import sample.server.ServerProtocols;
import sample.model.Client;
import sample.view.ViewFactory;

import javax.naming.OperationNotSupportedException;
import java.net.URL;
import java.util.ResourceBundle;

// stará se o LOGIN v aplikaci
public class LoginController extends AbstractController implements Initializable {

    private Client client;

    @FXML
    private TextField userName;

    @FXML
    private PasswordField psw;

    @FXML
    private Label label;
    @FXML
    private Label email;

    @FXML
    private Label password;

    @FXML
    private Hyperlink register;
    private Stage stage;
    private Stage registrationStage;

    // za předpokladu, že se klient DISCONNECTNE, tak se pošle zpráva na server zpráva o této skutečnosti
    private WindowCloseHandler windowCloseHandler = new WindowCloseHandler() {
        @Override
        public void handleClose(Event event) {
            if(client != null){
                client.sendMessage(new Message(ServerProtocols.DISCONNECTED, client.getId()));
            }
            Platform.exit();
            System.exit(0);
        }
    };

    public LoginController(AccessData accessData) {
        super(accessData);
    }

    // pokus o login - odešle se zpráva, počká se, až dojde odpověd ze serveru a dle protokolu se rozhodne, co dále
    @FXML
    void logAction(ActionEvent event) throws OperationNotSupportedException {
        label.setVisible(false);
        String email = userName.getText();
        String password = psw.getText();
        client.sendMessage(new Message(ServerProtocols.LOGIN, email, password));
        Message m = client.getLastMessage();

        if (m.getProtocol() == ServerProtocols.LOGIN_SUCCESS) {
            loginSuccesful(m);
        } else {
            loginFailed();
        }
    }

    // klient žádá o registraci, vytvoří se nová stage pro to.
    private void registerClient() {
        registrationStage = new Stage();
        registrationStage.setResizable(false);
        Scene scene = ViewFactory.defaultFactory.getRegisterScene();
        registrationStage.setScene(scene);
        getAccessData().setActiveClient(client);
        registrationStage.show();
    }

    // řeší úspěšný login - nastaví se klientovi informace o něm, které dostal ze serveru a nastaví novou scene
    private void loginSuccesful(Message m) throws OperationNotSupportedException {
        client.setId(m.getId());
        client.setUserName(m.getName());
        getAccessData().setActiveClient(client);
        stage = (Stage) psw.getScene().getWindow();
        stage.setScene(ViewFactory.defaultFactory.getMainScene());
        if(registrationStage != null){
            registrationStage.close();
        }
        stage.show();
    }

    // pokud dojde k neúspěšnému loginu
    private void loginFailed() {
        label.setVisible(true);
        userName.clear();
        label.setVisible(true);
        psw.clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        client = new Client();
        label.setVisible(false);
        getAccessData().setActiveClient(client);

        register.setOnMouseClicked(event -> {
            registerClient();
        });
    }


    @Override
    public WindowCloseHandler getWindowHandler() {
        return windowCloseHandler;
    }
}
