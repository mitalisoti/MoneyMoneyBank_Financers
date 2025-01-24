package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;


public class Users {
    private Connection connection;
    private Scanner scanner;
    private String loggedInEmail;

    public Users(Connection connection, Scanner scanner) {

        this.connection = connection;
        this.scanner = scanner;
        this.loggedInEmail = null;

    }

    public void register() {
        scanner.nextLine();
        System.out.println("Please enter your full name: ");
        String full_name = scanner.nextLine();
        String email;
        while (true) {
            System.out.println("Please enter your email: ");
            email = scanner.nextLine();
            if (isValidEmail(email)) {
                break;
            }
            System.out.println("Invalid email format! Please try again.");
        }
        String password;
        while (true) {
            System.out.println("Please enter your password: ");
            password = scanner.nextLine();
            if (isValidPassword(password)) {
                break;
            }
            System.out.println("Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character and should be at least 8 character long. Please try again.");
        }
        if (user_exist(email)) {
            System.out.println("Email already exists!");
            return;
        }
        int roleId;
        while (true) {
            System.out.println("Please enter your role (1 for Admin, 2 for User): ");
            roleId = scanner.nextInt();
            if (roleId == 1 || roleId == 2) {
                break;
            } else {
                System.out.println("Invalid role selected! Please try again.");
            }
        }

        String register_query = "INSERT INTO users (full_name, email, password, role_id) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(register_query);
            preparedStatement.setString(1, full_name);
            preparedStatement.setString(2, email);
            preparedStatement.setString(3, password);
            preparedStatement.setInt(4, roleId);
            int affected = preparedStatement.executeUpdate();
            if (affected > 0) {
                System.out.println("User registered successfully!");
            } else {
                System.out.println("Something went wrong! Registration failed! Please try again!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String login(Scanner scanner) {

        scanner.nextLine();
        System.out.println("Please enter your email: ");
        String email = scanner.nextLine();
        System.out.println("Please enter your password: ");
        String password = scanner.nextLine();
        String login_query = "SELECT * FROM users WHERE email = ? AND password = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(login_query);
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                this.loggedInEmail = email; // Store the logged-in email
                Long accountNumber = Accounts.get_account_number(email);
                System.out.println("Login successful. Welcome, " + resultSet.getString("full_name") + "!");
                System.out.println("Your account number is " + accountNumber);
                return email;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private boolean user_exist(String email) {
        String query = "SELECT * FROM users WHERE email = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, email);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
        return email.matches(emailRegex);
    }

    private boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d[^A-Za-z\\d]]{8,}$";
        return password.matches(passwordRegex);
    }

    public boolean isAdmin(String email) {
        String adminEmail = "admin@example.com";  // The hardcoded admin email

        // Check if the logged-in user matches the hardcoded admin email
        if (email.equals(adminEmail)) {
            return true;
        }
        return false;
    }
}

