module org.example.se302 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // Export all packages
    exports org.example.se302;
    exports org.example.se302.controller;
    exports org.example.se302.model;
    exports org.example.se302.service;

    // Open packages for reflection (FXML loading)
    opens org.example.se302 to javafx.fxml;
    opens org.example.se302.controller to javafx.fxml;
    opens org.example.se302.model to javafx.base;
}