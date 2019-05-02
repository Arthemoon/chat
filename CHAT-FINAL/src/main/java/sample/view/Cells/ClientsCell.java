package sample.view.Cells;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import sample.model.Client;
import sample.model.Folder;
import sample.model.Room;
import sample.view.ViewFactory;

// listCell pro kolekci
public class ClientsCell extends ListCell<Client> {
    Label lblName = new Label();
    HBox container = new HBox(lblName);

    private Client client;
    private Room room;

    public ClientsCell(Client client, Room room){
        this.client = client;
        this.room = room;
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
            // nastavuje context menu dle toho, zda je client superUser či nikoli
            setOnMouseClicked(event -> {
                if(room.getSuperUser().getId() == client.getId()){
                    if(item.getId() != client.getId()){
                        super.setContextMenu(new ContextMenu(ViewFactory.defaultFactory.initGroupMenu(item, client, room)));
                    }
                } else {
                    super.setContextMenu(null);
                }
            });
            lblName.setText(item.getName());
            setGraphic(container);
        }
    }
}
