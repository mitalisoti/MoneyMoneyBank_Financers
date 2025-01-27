package org.example;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class AccountManager {
    private Connection connection;
    private Scanner scanner;

    public AccountManager(Connection connection, Scanner scanner) {
        this.connection = connection;
        this.scanner = scanner;
    }

    private boolean validateAccount(long account_number, String security_pin) throws SQLException {
        String query = "SELECT * FROM accounts WHERE account_number = ? AND security_pin = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, account_number);
            preparedStatement.setString(2, security_pin);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }

    public double getBalance(long account_number) throws SQLException {
        String query = "SELECT balance FROM accounts WHERE account_number = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, account_number);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getDouble("balance");
            } else {
                throw new SQLException("Account not found.");
            }
        }
    }

    public void credit_money(long account_number) {
        System.out.println("Enter the amount you would like to deposit into the account");
        double amount = scanner.nextDouble();
        scanner.nextLine();
        System.out.println("Enter the Security Pin");
        String security_pin = scanner.nextLine();

        try {
            connection.setAutoCommit(false);
            if (validateAccount(account_number, security_pin)) {
                String credit_query = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(credit_query)) {
                    preparedStatement.setDouble(1, amount);
                    preparedStatement.setLong(2, account_number);
                    int affectedRows = preparedStatement.executeUpdate();
                    if (affectedRows > 0) {
                        System.out.println("Amount successfully deposited into account " + account_number);

                        String transaction_query = "INSERT INTO transactions (account_number, transaction_type, amount, balance_after_transaction) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement preparedStatement1 = connection.prepareStatement(transaction_query)) {
                            double newBalance = getBalance(account_number);
                            preparedStatement1.setLong(1, account_number);
                            preparedStatement1.setString(2, "Deposit");
                            preparedStatement1.setDouble(3, amount);
                            preparedStatement1.setDouble(4, newBalance);
                            preparedStatement1.executeUpdate();
                        }
                        connection.commit();
                    } else {
                        System.out.println("Transaction failed.");
                        connection.rollback();
                    }
                }
            } else {
                System.out.println("Invalid account number or security pin.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void debit_money(long account_number) {
        double amount = 0;
        String security_pin;

        // Prompt for the amount with validation
        while (true) {
            System.out.println("Enter the amount you want to withdraw: ");
            if (scanner.hasNextDouble()) {
                amount = scanner.nextDouble();
                scanner.nextLine();
                if (amount > 0) {
                    break;
                } else {
                    System.out.println("Amount must be greater than zero. Please try again.");
                }
            } else {
                System.out.println("Invalid input! Please enter a valid number.");
                scanner.next();
            }
        }

        System.out.println("Enter the security pin: ");
        security_pin = scanner.nextLine();

        try {
            connection.setAutoCommit(false);
            if (validateAccount(account_number, security_pin)) {
                double current_balance = getBalance(account_number);
                if (current_balance >= amount) {
                    String debit_query = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(debit_query)) {
                        preparedStatement.setDouble(1, amount);
                        preparedStatement.setLong(2, account_number);
                        int affectedRows = preparedStatement.executeUpdate();
                        if (affectedRows > 0) {
                            System.out.println("Euro " + amount + " has been debited.");
                            String transaction_query = "INSERT INTO transactions (account_number, transaction_type, amount, balance_after_transaction) VALUES (?, ?, ?, ?)";
                            try (PreparedStatement transactionStatement = connection.prepareStatement(transaction_query)) {
                                double newBalance = getBalance(account_number);  // Get the updated balance
                                transactionStatement.setLong(1, account_number);
                                transactionStatement.setString(2, "Withdrawal");
                                transactionStatement.setDouble(3, amount);
                                transactionStatement.setDouble(4, newBalance);
                                transactionStatement.executeUpdate();
                            }
                            connection.commit();
                        } else {
                            System.out.println("Transaction failed.");
                            connection.rollback();
                        }
                    }
                } else {
                    System.out.println("Insufficient balance.");
                }
            } else {
                System.out.println("Invalid account number or security pin.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void transfer_money(long sender_account_number) {
        System.out.println("Enter the account number you want to transfer the money to: ");
        long receiver_account_number = scanner.nextLong();
        scanner.nextLine();
        System.out.println("Enter the amount you want to transfer: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();
        System.out.println("Enter the security pin: ");
        String security_pin = scanner.nextLine();

        try {
            connection.setAutoCommit(false);
            if (validateAccount(sender_account_number, security_pin)) {
                double current_balance = getBalance(sender_account_number);
                if (current_balance >= amount) {
                    String debit_query = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
                    String credit_query = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";

                    try (PreparedStatement debitStatement = connection.prepareStatement(debit_query);
                         PreparedStatement creditStatement = connection.prepareStatement(credit_query)) {
                        debitStatement.setDouble(1, amount);
                        debitStatement.setLong(2, sender_account_number);
                        creditStatement.setDouble(1, amount);
                        creditStatement.setLong(2, receiver_account_number);

                        int debitAffected = debitStatement.executeUpdate();
                        int creditAffected = creditStatement.executeUpdate();

                        if (debitAffected > 0 && creditAffected > 0) {
                            System.out.println("Transfer successful.");

                            // Insert transaction for the sender
                            String senderTransactionQuery = "INSERT INTO transactions (account_number, transaction_type, amount, balance_after_transaction) VALUES (?, ?, ?, ?)";
                            try (PreparedStatement senderTransactionStatement = connection.prepareStatement(senderTransactionQuery)) {
                                double senderNewBalance = getBalance(sender_account_number);
                                senderTransactionStatement.setLong(1, sender_account_number);
                                senderTransactionStatement.setString(2, "Transfer Out");
                                senderTransactionStatement.setDouble(3, amount);
                                senderTransactionStatement.setDouble(4, senderNewBalance);
                                senderTransactionStatement.executeUpdate();
                            }

                            // Insert transaction for the receiver
                            String receiverTransactionQuery = "INSERT INTO transactions (account_number, transaction_type, amount, balance_after_transaction) VALUES (?, ?, ?, ?)";
                            try (PreparedStatement receiverTransactionStatement = connection.prepareStatement(receiverTransactionQuery)) {
                                double receiverNewBalance = getBalance(receiver_account_number);
                                receiverTransactionStatement.setLong(1, receiver_account_number);
                                receiverTransactionStatement.setString(2, "Transfer In");
                                receiverTransactionStatement.setDouble(3, amount);
                                receiverTransactionStatement.setDouble(4, receiverNewBalance);
                                receiverTransactionStatement.executeUpdate();
                            }

                            connection.commit();
                        } else {
                            System.out.println("Transaction failed.");
                            connection.rollback();
                        }
                    }
                } else {
                    System.out.println("Insufficient balance.");
                }
            } else {
                System.out.println("Invalid account number or security pin.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void showTransactionHistory(long account_number) {
        String query = "SELECT transaction_date, transaction_type, amount, balance_after_transaction FROM transactions WHERE account_number = ? ORDER BY transaction_date DESC";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, account_number);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.isBeforeFirst()) {
                System.out.println("No transactions found for this account.");
            } else {
                System.out.println("Transaction History: ");
                while (resultSet.next()) {
                    System.out.printf("Type: %s | Amount: %.2f | Date: %s | Balance: %.2f\n",
                            resultSet.getString("transaction_type"),
                            resultSet.getDouble("amount"),
                            resultSet.getTimestamp("transaction_date"),
                            resultSet.getDouble("balance_after_transaction"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("An error occurred while fetching transaction history.");
        }
    }



}
