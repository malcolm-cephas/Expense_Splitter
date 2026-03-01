package com.malcolm.expensesplitter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import atlantafx.base.theme.PrimerDark;

/**
 * Main Entry point for the Expense Splitter Pro application.
 * Integrates Spring Boot with JavaFX to provide a powerful desktop experience
 * with transactional database support and modern UI styling.
 */
@SpringBootApplication
public class ExpenseSplitterApplication extends Application {

    private static ConfigurableApplicationContext context;
    private static String[] savedArgs;

    public static void main(String[] args) {
        savedArgs = args;
        Application.launch(ExpenseSplitterApplication.class, args);
    }

    @Override
    public void init() throws Exception {
        context = SpringApplication.run(ExpenseSplitterApplication.class, savedArgs);
        context.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Set modern theme
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // Load main FXML using Spring factory
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
        loader.setControllerFactory(context::getBean);
        javafx.scene.Parent root = loader.load();

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 1024, 768);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Expense Splitter Pro");
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        context.close();
        Platform.exit();
    }
}
