package sample.controller.Interfaces;

import javafx.event.Event;
// Slouží k šíření eventu a k vykonání určitých ukonů při uzavření stage
public interface WindowCloseHandler{
    void handleClose(Event event);
}
