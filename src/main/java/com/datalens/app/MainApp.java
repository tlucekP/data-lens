package com.datalens.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/ui/main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1180, 820);
        scene.getStylesheets().add(MainApp.class.getResource("/ui/app.css").toExternalForm());

        stage.setTitle("DataLens");
        stage.setMinWidth(980);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
