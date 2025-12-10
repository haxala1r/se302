module org.example.se302 {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.se302 to javafx.fxml;
    exports org.example.se302;
}