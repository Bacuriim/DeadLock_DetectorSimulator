package einstein;

import einstein.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/view/main_view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1240, 960);
        stage.setTitle("Deadlock Detection Simulator");
        stage.setScene(scene);
        stage.setOnHidden(e -> ((MainController) fxmlLoader.getController()).shutdown());
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
