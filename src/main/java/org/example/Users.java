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

        String register_query = "INSERT INTO users (full_name, email, password) VALUES (?, ?, ?)";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(register_query);
            preparedStatement.setString(1, full_name);
            preparedStatement.setString(2, email);
            preparedStatement.setString(3, password);

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
                if (!Accounts.account_exist(email)) {
                    System.out.println("Seems you have not opened an account yet! Please open an account first.");
                }
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

    public void updatePassword(String email) {
        try {
            System.out.println("Please enter your current password: ");
            String currentPassword = scanner.nextLine().trim(); // Read current password

                if (currentPassword.isEmpty()) {
                    System.out.println("Current password cannot be empty. Please try again.");
                    return;
                }
                System.out.println("Please enter your new password: ");
                String newPassword = scanner.nextLine().trim(); // Read new password

                if (newPassword.isEmpty()) {
                    System.out.println("New password cannot be empty. Please try again.");
                    return;
                }

                // Check if the current password matches the stored password
                String query = "SELECT password FROM users WHERE email = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setString(1, email);
                    ResultSet resultSet = preparedStatement.executeQuery();

                    if (resultSet.next()) {
                        String storedPassword = resultSet.getString("password");

                        // Compare the entered current password with the stored password
                        if (!storedPassword.equals(currentPassword)) {
                            System.out.println("Current password is incorrect. Please try again.");
                            return;
                        }
                    } else {
                        System.out.println("User with the given email does not exist.");
                        return;
                    }
                }

                // Update the password
                String updateQuery = "UPDATE users SET password = ? WHERE email = ?";
                try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                    updateStatement.setString(1, newPassword);
                    updateStatement.setString(2, email);

                    int rowsAffected = updateStatement.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println("Password updated successfully!");
                    } else {
                        System.out.println("Something went wrong. Please try again.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("An error occurred while updating the password.");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Unexpected error occurred.");
            }
        }

    }

