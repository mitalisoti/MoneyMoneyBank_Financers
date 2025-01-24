package org.example;
import java.sql.*;
import java.util.Scanner;

public class Accounts {
    private static Connection connection;
    private Scanner scanner;

    public Accounts(Connection connection, Scanner scanner) {
        this.connection = connection;
        this.scanner = scanner;
    }

    private static long generateAccountNumber() {
        try{
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT account_number FROM Accounts ORDER BY account_number DESC LIMIT 1");
            if(resultSet.next()){
                long last_account_number = resultSet.getLong("account_number");
                return last_account_number+1;
            } else{
                return 1000100;
            }
        }
        catch(SQLException e){
            throw new RuntimeException(e);
        }
    }

    static boolean account_exist(String email) {
        String query = "SELECT account_number FROM Accounts WHERE email = ?";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1,email);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return true;
            } else{
                return false;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static long get_account_number(String email) {
        String query = "SELECT account_number FROM Accounts WHERE email = ?";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1,email);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return resultSet.getLong("account_number");
            }

        }catch(Exception e){
            throw new RuntimeException(e);
        }
        return 0;
    }
    public long open_account(String email) {
        while (true) {

            if (isValidEmail(email)) {
                break;
            } else{
                System.out.println("Invalid email format! Please try again.");
                System.out.println("Please enter your email: ");
                email = scanner.nextLine();
            }

        }
        if(!account_exist(email)){
            String open_account_query = "INSERT INTO ACCOUNTS(account_number, full_name, email, balance, security_pin) VALUES(?,?,?,?,?)";
            scanner.nextLine();
            System.out.println("Enter your Full Name: ");
            String full_name = scanner.nextLine();
            double balance= -1;
            if(balance<=0){
                System.out.println("Enter the initial amount: ");
                if(scanner.hasNextDouble()){
                    balance = scanner.nextDouble();
                    if(balance<=0){
                        System.out.println("Balance must be positive.");
                    }else{
                        scanner.nextLine();
                    }
                }
            }

            String security_pin = "";
            while(true){
                System.out.println("Enter a 4-digit Security Pin: ");
                security_pin = scanner.nextLine();
                if (security_pin.matches("\\d{4}")) { // Check for exactly 4 digits
                    break;
                } else {
                    System.out.println("Invalid pin! Security pin must be exactly 4 digits.");
                }
            }

            try{
                long account_number = generateAccountNumber();
                PreparedStatement preparedStatement = connection.prepareStatement(open_account_query);
                preparedStatement.setLong(1, account_number);
                preparedStatement.setString(2, full_name);
                preparedStatement.setString(3, email);
                preparedStatement.setDouble(4, balance);
                preparedStatement.setString(5, security_pin);
                int rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Account number " + account_number + " has been opened.");
                    return account_number;
                } else {
                    throw new RuntimeException("Account Creation failed!! Please try again.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("An error occurred while creating your account. Please try again.");
                return -1;
            }
        }
        throw new RuntimeException("Account Already Exist");

    }
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
        return email != null && !email.isEmpty() && email.matches(emailRegex);
    }



}
