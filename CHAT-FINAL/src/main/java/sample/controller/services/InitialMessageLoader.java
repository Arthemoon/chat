package sample.controller.services;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import sample.controller.AccessData;
import sample.model.Client;
import sample.model.Message;

// slouží k načtení "historie" zpráv, aby to neblokovalo hlavní vlákno
public class InitialMessageLoader extends Service<Void> {

    private AccessData accessData;

    public InitialMessageLoader(AccessData accessData){
        this.accessData = accessData;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
               final Message  m = accessData.getActiveClient().getInitialMessages().take();
                if(m.getMessages().size() > 0){
                        accessData.updateData(Long.toString(m.getMessages().get(0).getRoomId()), m.getMessages());
                }
                return null;
            }
        };
    }
}
