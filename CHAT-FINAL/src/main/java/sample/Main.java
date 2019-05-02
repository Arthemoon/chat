package sample;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sample.view.ViewFactory;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        ViewFactory viewFactory = ViewFactory.defaultFactory;
        Scene scene = viewFactory.getLoginScene();

        primaryStage.setOnCloseRequest(event -> {
            viewFactory.handleWindowClose(event);
        });

        primaryStage.setResizable(false);

        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
