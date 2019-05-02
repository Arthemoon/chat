package sample.view.Cells;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import sample.controller.AccessData;
import sample.model.Client;
import sample.view.ViewFactory;


// listCell pro allFriends - binding obrázku a nastavení contextMenu jednotlivým itemům v listView
// obsahuje ještě kod pro vytváření chatu s daným klientem (položkou)
public class FriendsCell extends ListCell<Client> {
    Label lblName = new Label();
    ImageView imageView = new ImageView();
    HBox container = new HBox(imageView, lblName);

    private Client client;
    private AccessData accessData;

    public FriendsCell(Client client, AccessData accessData){
        this.client = client;
        this.accessData = accessData;
    }
    {
        container.setSpacing(8);
        imageView.setFitWidth(20);
        imageView.setFitHeight(20);
    }

    @Override
    protected void updateItem(Client item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            imageView.imageProperty().unbind();
            lblName.setText(null);
            setText(null);
            setGraphic(null);
        } else {
            setOnMouseClicked(event -> {
                super.setContextMenu(new ContextMenu(ViewFactory.defaultFactory.initAllFriendsMenu(item, client)));
            });

            if(!accessData.getAllChats().containsKey(accessData.convertChatID(item.getId(), client.getId()))){
                accessData.addChat(client.getId(), item.getId());
            }

            lblName.setText(item.getName());
            imageView.imageProperty().bind(item.imageObjectProperty());
            setGraphic(container);
        }
    }
}
