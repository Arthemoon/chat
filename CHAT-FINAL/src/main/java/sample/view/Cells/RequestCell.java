package sample.view.Cells;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import sample.model.Client;
import sample.view.ViewFactory;

// nastavuje contextMenu na requestList všem jednotlivým položkám
public class RequestCell extends ListCell<Client> {
    Label lblName = new Label();
    HBox container = new HBox(lblName);

    private Client client;

    public RequestCell(Client client){
        this.client = client;
    }
    {
        container.setSpacing(8);
    }

    @Override
    protected void updateItem(Client item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            lblName.setText(null);
            setText(null);
            setGraphic(null);
        } else {
            setOnMouseClicked(event -> {
                ContextMenu contextMenu = new ContextMenu();
                contextMenu.getItems().addAll(ViewFactory.defaultFactory.initRequestsMenu(item, client));
                super.setContextMenu(contextMenu);
            });
            lblName.setText(item.getName());
            setGraphic(container);
        }
    }
}
