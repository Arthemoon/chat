package sample.controller;

import sample.controller.Interfaces.WindowCloseHandler;
// dědí od této třídy všechny controllery - a je pouze jen jedna instance accessData, takže díky tomu dochází k přenosu dat mezi
// jednotlivými controllery
public abstract class AbstractController {

    private AccessData accessData;
    private WindowCloseHandler windowCloseHandler;

    public AbstractController(AccessData accessData){
        this.accessData = accessData;
    }

    public AccessData getAccessData(){
        return accessData;
    }

    public abstract WindowCloseHandler getWindowHandler();
}
