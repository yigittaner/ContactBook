package com.example.finalproject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ContactBook extends Application {
    private Stage stage;
    private Scene loginScene, registerScene, contactScene;
    private Map<String, String> users = new HashMap<>();
    private String currentUser;
    private ObservableList<Contact> contacts = FXCollections.observableArrayList();
    private TableView<Contact> contactTable;
    private TextField searchField, nameField, phoneField, addressField;

    // File paths
    private static final String USERS_FILE = "users.txt";

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        loadUsers();
        createScenes();

        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        showLogin();
    }

    private void createScenes() {
        // Login Scene
        VBox loginLayout = new VBox(10);
        loginLayout.setAlignment(Pos.CENTER);
        loginLayout.setPadding(new Insets(20));

        TextField loginUsername = new TextField();
        loginUsername.setPromptText("Username");
        loginUsername.setMaxWidth(200);

        PasswordField loginPassword = new PasswordField();
        loginPassword.setPromptText("Password");
        loginPassword.setMaxWidth(200);

        Button loginButton = new Button("Login");
        Button goToRegisterButton = new Button("Register New Account");
        Label loginMessage = new Label();
        loginMessage.setStyle("-fx-text-fill: red;");

        loginButton.setOnAction(e -> {
            String username = loginUsername.getText().trim();
            String password = loginPassword.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                loginMessage.setText("Please enter username and password");
                return;
            }

            if (authenticateUser(username, password)) {
                currentUser = username;
                loadContacts();
                showContacts();
                loginUsername.clear();
                loginPassword.clear();
                loginMessage.setText("");
            } else {
                loginMessage.setText("Invalid username or password");
            }
        });

        goToRegisterButton.setOnAction(e -> showRegister());

        loginLayout.getChildren().addAll(
                new Label("Login"), loginUsername, loginPassword,
                loginButton, goToRegisterButton, loginMessage
        );
        loginScene = new Scene(loginLayout, 300, 400);

        // Register Scene
        VBox registerLayout = new VBox(10);
        registerLayout.setAlignment(Pos.CENTER);
        registerLayout.setPadding(new Insets(20));

        TextField registerUsername = new TextField();
        registerUsername.setPromptText("Username");
        registerUsername.setMaxWidth(200);

        PasswordField registerPassword = new PasswordField();
        registerPassword.setPromptText("Password");
        registerPassword.setMaxWidth(200);

        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm Password");
        confirmPassword.setMaxWidth(200);

        Button registerButton = new Button("Register");
        Button backToLoginButton = new Button("Back to Login");
        Label registerMessage = new Label();
        registerMessage.setStyle("-fx-text-fill: red;");

        registerButton.setOnAction(e -> {
            String username = registerUsername.getText().trim();
            String password = registerPassword.getText().trim();
            String confirm = confirmPassword.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                registerMessage.setText("Please enter username and password");
                return;
            }

            if (!password.equals(confirm)) {
                registerMessage.setText("Passwords don't match");
                return;
            }

            if (registerUser(username, password)) {
                registerUsername.clear();
                registerPassword.clear();
                confirmPassword.clear();
                registerMessage.setText("");
                showLogin();
            } else {
                registerMessage.setText("Username already exists");
            }
        });

        backToLoginButton.setOnAction(e -> showLogin());

        registerLayout.getChildren().addAll(
                new Label("Register"), registerUsername, registerPassword,
                confirmPassword, registerButton, backToLoginButton, registerMessage
        );
        registerScene = new Scene(registerLayout, 300, 400);

        // Contacts Scene
        VBox contactLayout = new VBox(10);
        contactLayout.setPadding(new Insets(20));

        // Search bar
        searchField = new TextField();
        searchField.setPromptText("Search contacts...");

        // Input fields
        HBox inputBox = new HBox(10);
        nameField = new TextField();
        nameField.setPromptText("Name");
        phoneField = new TextField();
        phoneField.setPromptText("Phone");
        addressField = new TextField();
        addressField.setPromptText("Address/Description");

        Button addButton = new Button("Add");
        Button updateButton = new Button("Update");
        Button deleteButton = new Button("Delete");
        Button favoriteButton = new Button("Toggle Favorite");
        Button logoutButton = new Button("Logout");

        inputBox.getChildren().addAll(
                nameField, phoneField, addressField,
                addButton, updateButton, deleteButton, favoriteButton
        );

        // Table
        contactTable = new TableView<>();
        contactTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Contact, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> {
            Contact contact = data.getValue();
            String name = contact.isFavorite() ? "⭐ " + contact.getName() : contact.getName();
            return new SimpleStringProperty(name);
        });

        TableColumn<Contact, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));

        TableColumn<Contact, String> addressCol = new TableColumn<>("Address/Description");
        addressCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAddress()));

        contactTable.getColumns().addAll(nameCol, phoneCol, addressCol);
        contactTable.setItems(contacts);

        // Setup search functionality
        FilteredList<Contact> filteredContacts = new FilteredList<>(contacts, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredContacts.setPredicate(contact -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String search = newVal.toLowerCase();
                return contact.getName().toLowerCase().contains(search) ||
                        contact.getPhone().toLowerCase().contains(search) ||
                        contact.getAddress().toLowerCase().contains(search);
            });
        });

        SortedList<Contact> sortedContacts = new SortedList<>(filteredContacts);
        sortedContacts.comparatorProperty().bind(contactTable.comparatorProperty());
        contactTable.setItems(sortedContacts);

        // Button actions
        addButton.setOnAction(e -> addContact());
        updateButton.setOnAction(e -> updateContact());
        deleteButton.setOnAction(e -> deleteContact());
        favoriteButton.setOnAction(e -> toggleFavorite());
        logoutButton.setOnAction(e -> logout());

        // Selection listener
        contactTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                nameField.setText(newVal.getName());
                phoneField.setText(newVal.getPhone());
                addressField.setText(newVal.getAddress());
            }
        });

        contactLayout.getChildren().addAll(
                new Label("Contact Book - " + currentUser),
                searchField, inputBox, contactTable, logoutButton
        );
        VBox.setVgrow(contactTable, Priority.ALWAYS);
        contactScene = new Scene(contactLayout, 800, 600);
    }

    private void showLogin() {
        stage.setTitle("Login");
        stage.setScene(loginScene);
        stage.show();
    }

    private void showRegister() {
        stage.setTitle("Register");
        stage.setScene(registerScene);
    }

    private void showContacts() {
        stage.setTitle("Contact Book - " + currentUser);
        stage.setScene(contactScene);
    }

    private boolean authenticateUser(String username, String password) {
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }

    private boolean registerUser(String username, String password) {
        if (users.containsKey(username)) return false;
        users.put(username, password);
        saveUsers();
        return true;
    }

    private void loadUsers() {
        try {
            if (Files.exists(Paths.get(USERS_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(USERS_FILE));
                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        users.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveUsers() {
        try {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, String> entry : users.entrySet()) {
                lines.add(entry.getKey() + "," + entry.getValue());
            }
            Files.write(Paths.get(USERS_FILE), lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadContacts() {
        contacts.clear();
        try {
            String fileName = "contacts_" + currentUser + ".csv";
            if (Files.exists(Paths.get(fileName))) {
                List<String> lines = Files.readAllLines(Paths.get(fileName));
                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        contacts.add(new Contact(
                                parts[0].trim(),
                                parts[1].trim(),
                                parts[2].trim(),
                                Boolean.parseBoolean(parts[3].trim())
                        ));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveContacts() {
        try {
            List<String> lines = new ArrayList<>();
            for (Contact contact : contacts) {
                lines.add(String.format("%s,%s,%s,%b",
                        contact.getName(),
                        contact.getPhone(),
                        contact.getAddress(),
                        contact.isFavorite()
                ));
            }
            Files.write(Paths.get("contacts_" + currentUser + ".csv"), lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addContact() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            showAlert("Error", "Please fill all fields");
            return;
        }

        contacts.add(new Contact(name, phone, address, false));
        saveContacts();
        clearFields();
    }

    private void updateContact() {
        Contact selected = contactTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a contact to update");
            return;
        }

        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            showAlert("Error", "Please fill all fields");
            return;
        }

        int index = contacts.indexOf(selected);
        contacts.set(index, new Contact(name, phone, address, selected.isFavorite()));
        saveContacts();
        clearFields();
    }

    private void deleteContact() {
        Contact selected = contactTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a contact to delete");
            return;
        }

        contacts.remove(selected);
        saveContacts();
        clearFields();
    }

    private void toggleFavorite() {
        Contact selected = contactTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a contact to toggle favorite");
            return;
        }

        int index = contacts.indexOf(selected);
        Contact updated = new Contact(
                selected.getName(),
                selected.getPhone(),
                selected.getAddress(),
                !selected.isFavorite()
        );
        contacts.set(index, updated);
        saveContacts();
    }

    private void logout() {
        currentUser = null;
        contacts.clear();
        clearFields();
        showLogin();
    }

    private void clearFields() {
        nameField.clear();
        phoneField.clear();
        addressField.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Contact class
    private static class Contact {
        private final String name;
        private final String phone;
        private final String address;
        private final boolean favorite;

        public Contact(String name, String phone, String address, boolean favorite) {
            this.name = name;
            this.phone = phone;
            this.address = address;
            this.favorite = favorite;
        }

        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getAddress() { return address; }
        public boolean isFavorite() { return favorite; }
    }
}