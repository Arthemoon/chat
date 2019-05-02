package sample.view.Cells;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import sample.controller.AccessData;
import sample.model.Room;
import java.util.Set;

// nastavuje contextMenu podle toho, jestli je uživatel superUser či ne ve SKUPINĚ
// obsahuje ještě kod pro vytváření skupinového chatu (položkou)
public class GroupCell extends ListCell<Room> {
    Label lblName = new Label();
    HBox container = new HBox(lblName);

    private boolean isRoot;
    private Set<MenuItem> menuItems;
    private AccessData accessData;


    public GroupCell(boolean isRoot, Set<MenuItem> menuItems, AccessData accessData){
            this.menuItems = menuItems;
            this.isRoot = isRoot;
            this.accessData = accessData;
    }

    {
        container.setSpacing(8);
    }

    // updatuje jednotlivé itemy v listViewu, nastavuje contextMenu a přidává dostupné klientovi chaty
    @Override
    protected void updateItem(Room item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            lblName.textProperty().bind(item.getSimpleName());
            setOnMouseClicked(event -> {
                setMenu(isRoot, menuItems);
            });

            if(!accessData.getAllChats().containsKey(accessData.convertChatID(item.getId(), item.getId()))) {
                accessData.addChat(item.getId(), item.getId());
            }

            setGraphic(container);
        }
    }

    // pokud je uživatel superUser, tak mu nastaví jiné contextMenu, než pokud není
    private void setMenu(boolean isSuperUser, Set<MenuItem> menuItems){
        ContextMenu contextMenu1 = new ContextMenu();
        if(isSuperUser){
            contextMenu1.getItems().addAll(menuItems);
            menuItems.forEach(menuItem -> super.setContextMenu(contextMenu1));
        } else {
            contextMenu1.getItems().add(menuItems.iterator().next());
            super.setContextMenu(contextMenu1);
        }
    }
}
