package sample.controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;
import sample.controller.Interfaces.WindowCloseHandler;
import sample.model.Client;
import sample.model.Message;
import sample.server.ServerProtocols;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// stará se o registraci
public class RegisterController extends AbstractController implements Initializable {

    public RegisterController(AccessData accessData) {
        super(accessData);
    }

    private Client client;

    @FXML
    private Label regError;

    @FXML
    private TextField name;

    @FXML
    private TextField email;

    @FXML
    private PasswordField psw;

    @FXML
    private PasswordField psw2;

    @FXML
    private Label pswError;

    @FXML
    private Label emailError;

    @FXML
    private Label nameError;

    private WindowCloseHandler windowCloseHandler = new WindowCloseHandler() {
        @Override
        public void handleClose(Event event) {
            // Pouze zavři
        }
    };

    // zvaliduje, zda klient zadal platná pole podle regex, pokud ano, pošle se zpráva na server,
    // že se chce klient registrovat
    @FXML
    void registerClient(ActionEvent event) {
        setLabels();

        String password = psw.getText();
        String password2 = psw2.getText();
        String emailField = email.getText();
        String regName = name.getText();

        if (validatePassword(password, password2) && validateEmail(emailField) && validateName(regName)) {
            getAccessData().getActiveClient().sendMessage(new Message(ServerProtocols.REGISTER, regName, hashPassword(psw.getText()), emailField));
            validateRegistration();
        } else {
            validateEmail(emailField);
            validateName(regName);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        client = getAccessData().getActiveClient();
        setLabels();
    }

    // validace email
    private boolean validateEmail(String emailToValidate) {
        Pattern emailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = emailPattern.matcher(emailToValidate);
        if (matcher.find()) {
            return true;
        }
        emailError.setVisible(true);
        emailError.setText("Your email is not valid");
        email.clear();
        return false;
    }

    // validace hesla
    private boolean validatePassword(String password, String password2) {
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{5,15}$");
        Matcher matcher = pattern.matcher(password);
        Matcher matcher2 = pattern.matcher(password2);
        if (matcher.find() && matcher2.find()) {
            if (password.equals(password2)) {
                return true;
            }
        }
        pswError.setVisible(true);
        pswError.setText("1 digit, 1 upper/lowercase letter, length: 5 and more");
        psw.clear();
        psw2.clear();
        return false;
    }

    // validace jména
    private boolean validateName(String name) {

        Pattern usernamePattern = Pattern.compile("^[a-zA-Z0-9_-]{3,15}$");
        Matcher matcher = usernamePattern.matcher(name);

        if (matcher.find()) {
            return true;
        }

        nameError.setVisible(true);
        nameError.setText("Name must be at least 3-15 chars.");

        return false;
    }

    // podle protokolu se rozhodne, co dále. Bud se zavře, nebo se nastaví errorLabel
    private void validateRegistration() {
        Message m = client.getLastMessage();
        Stage stage = (Stage) psw.getScene().getWindow();

        if (m.getProtocol() == ServerProtocols.REGISTRATION_SUCCESS) {
            stage.close();
        } else {
            regError.setVisible(true);
        }
    }

    // nastavuje initLabels
    private void setLabels() {
        pswError.setVisible(false);
        emailError.setVisible(false);
        nameError.setVisible(false);
        regError.setVisible(false);
    }

    // provádí HASHOVÁNÍ hesla pomocí knihovny BCrypt
    private String hashPassword(String plainText){
        return BCrypt.hashpw(plainText, BCrypt.gensalt());
    }

    @Override
    public WindowCloseHandler getWindowHandler() {
        return null;
    }
}

