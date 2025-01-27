package org.example;
import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class LoanService {
    public Users users;
    Connection connection;
    Scanner scanner;


    public LoanService(Connection connection, Scanner scanner) {
        this.connection = connection;
        this.scanner = scanner;
        this.users = new Users(connection, scanner);

    }

    public void applyLoan(String email) throws SQLException {
        Users users = new Users(connection, scanner);
        if (users.isAdmin(email)) {
            System.out.println("Sorry. Admins are not allowed to apply for loans.");
            return;
        }

        if (isLoanExist(email)) {
            System.out.println("You already have an active loan. Please clear your pending EMI before applying for a new loan.");
            return;
        }

        scanner.nextLine(); // Consume newline
        System.out.println("Enter the loan amount you want to apply for:");
        double loanAmount = scanner.nextDouble();

        System.out.println("Enter the loan term in months:");
        int term = scanner.nextInt();

        double annualInterestRate = 5.0; // Example interest rate
        double monthlyRepayment = calculateMonthlyRepayment(loanAmount, term, annualInterestRate);

        java.sql.Date loanStartDate = java.sql.Date.valueOf(java.time.LocalDate.now());
        java.sql.Date loanEndDate = java.sql.Date.valueOf(java.time.LocalDate.now().plusMonths(term));

        long accountNumber = Accounts.get_account_number(email);

        System.out.println("Thank you for your loan application.");
        System.out.println("Your EMI schedule will be:");
        System.out.println("Loan Amount: " + loanAmount);
        System.out.println("Loan Term: " + term + " months");
        System.out.println("Monthly EMI: " + (Math.round(monthlyRepayment * 100.0) / 100.0));
        System.out.println("First repayment due on: " + java.time.LocalDate.now().plusMonths(1));
        System.out.println("Please confirm your loan application. (Y/N)");

        scanner.nextLine(); // Consume newline
        if (scanner.nextLine().equalsIgnoreCase("Y")) {
            String applyLoanQuery = "INSERT INTO loans (email, account_number, loan_amount, interest_rate, loan_term, loan_balance, loan_start_date, loan_end_date, emi_amount, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')";
            try (PreparedStatement preparedStatement = connection.prepareStatement(applyLoanQuery)) {
                preparedStatement.setString(1, email);
                preparedStatement.setLong(2, accountNumber);
                preparedStatement.setDouble(3, loanAmount);
                preparedStatement.setDouble(4, annualInterestRate);
                preparedStatement.setInt(5, term);
                preparedStatement.setDouble(6, loanAmount);
                preparedStatement.setDate(7, loanStartDate);
                preparedStatement.setDate(8, loanEndDate);
                preparedStatement.setDouble(9, Math.round(monthlyRepayment * 100.0) / 100.0);

                int rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Your loan application is successful. Your application is pending for approval.");
                } else {
                    System.out.println("Loan application failed. Please try again.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("An error occurred during loan application. Please contact support.");
            }
        } else {
            System.out.println("Thank you for your response. Your loan application has been canceled.");
        }
    }


    public void approveRejectLoan(Scanner scanner) throws SQLException {
        System.out.print("Enter the Account Number to view loans: ");
        long accountNumber = scanner.nextLong();

        String selectQuery = "SELECT l.* FROM loans l JOIN accounts a ON l.email = a.email WHERE a.account_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(selectQuery)) {
            statement.setLong(1, accountNumber);
            ResultSet resultSet = statement.executeQuery();

            boolean loanFound = false;

            while (resultSet.next()) {
                loanFound = true;
                System.out.println("Loan Details for Account: " + accountNumber);
                System.out.println("Loan ID: " + resultSet.getString("loan_id"));
                System.out.println("Amount: " + resultSet.getDouble("loan_amount"));
                System.out.println("Status: " + resultSet.getString("status"));
            }

            if (!loanFound) {
                System.out.println("No loan found for this account.");
                return;
            }

            long loanId = -1;
            while (loanId == -1) {
                System.out.print("\nEnter the Loan ID to approve or reject: ");
                try {
                    loanId = scanner.nextLong();
                } catch (InputMismatchException e) {
                    System.out.println("Invalid input. Please enter a valid numeric Loan ID.");
                    scanner.nextLine();  // clear the buffer
                }
            }

            System.out.print("Enter 'approve' to approve the loan or 'reject' to reject it: ");
            String action = scanner.next();

            if (!action.equalsIgnoreCase("approve") && !action.equalsIgnoreCase("reject")) {
                System.out.println("Invalid action. Please enter 'approve' or 'reject'.");
                return;
            }

            String checkStatusQuery = "SELECT status FROM loans WHERE loan_id = ?";
            try (PreparedStatement checkStatusStatement = connection.prepareStatement(checkStatusQuery)) {
                checkStatusStatement.setLong(1, loanId);
                ResultSet statusResultSet = checkStatusStatement.executeQuery();

                if (statusResultSet.next()) {
                    String currentStatus = statusResultSet.getString("status");

                    if ((action.equalsIgnoreCase("approve") && "Approved".equals(currentStatus)) ||
                            (action.equalsIgnoreCase("reject") && "Rejected".equals(currentStatus))) {
                        System.out.println("Loan " + loanId + " is already " + currentStatus + ".");
                        return;
                    }

                    String status = action.equalsIgnoreCase("approve") ? "Approved" : "Rejected";
                    String updateQuery = "UPDATE loans SET status = ? WHERE loan_id = ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                        updateStatement.setString(1, status);
                        updateStatement.setLong(2, loanId);
                        int rowsAffected = updateStatement.executeUpdate();

                        if (rowsAffected > 0) {
                            System.out.println("Loan " + loanId + " has been " + status + ".");
                        } else {
                            System.out.println("An error occurred while processing the loan approval. Please try again.");
                        }
                    }
                } else {
                    System.out.println("Loan ID not found.");
                }
            }
        }
    }

    private double calculateMonthlyRepayment(double loanAmount, int term, double annualInterestRate) {
        double monthlyInterestRate = annualInterestRate / 100/ 12;
        int numberOfPayments = term;
        double monthlyRepayment = (loanAmount * monthlyInterestRate * Math.pow(1 + monthlyInterestRate, numberOfPayments)) /
                (Math.pow(1 + monthlyInterestRate, numberOfPayments) - 1);
        return Math.round(monthlyRepayment * 100.0) / 100.0;
    }

    public void repayLoan(String email) {
        System.out.println("Enter the repayment amount: ");
        double repaymentAmount = scanner.nextDouble();

        // Query to get loan details including EMI
        String repaymentQuery = "SELECT loan_balance, emi_amount, status FROM loans WHERE email = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(repaymentQuery)) {
            preparedStatement.setString(1, email);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                double currentLoanBalance = resultSet.getDouble("loan_balance");
                double emiAmount = resultSet.getDouble("emi_amount"); // Retrieve EMI
                String status = resultSet.getString("status");
                if("pending".equalsIgnoreCase(status)) {
                    System.out.println("Loan for " + email + " is pending for further approval. You will be notified as soon as it gets approve");
                    return;
                }

                // Validate repayment amount
                if (repaymentAmount != emiAmount) {
                    System.out.println("The EMI is " + emiAmount + ". Please pay exactly " + emiAmount + ".");
                    return;
                }

                if (currentLoanBalance >= repaymentAmount) {
                    // Deduct repayment from the loan balance
                    String updateBalanceQuery = "UPDATE loans SET loan_balance = loan_balance - ? WHERE email = ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateBalanceQuery)) {
                        updateStatement.setDouble(1, repaymentAmount);
                        updateStatement.setString(2, email);
                        int rowsAffected = updateStatement.executeUpdate();
                        if (rowsAffected > 0) {
                            System.out.println("Repayment successful. Remaining loan balance: " + (currentLoanBalance - repaymentAmount));
                        }
                    }
                } else {
                    System.out.println("Repayment amount exceeds the loan balance.");
                }
            } else {
                System.out.println("No loan found for the user or the loan is not approved.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // View loan details including interest rate
    public void viewLoanDetails(String email) {
        String loanDetailsQuery = "SELECT * FROM loans WHERE email = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(loanDetailsQuery)) {
            preparedStatement.setString(1, email);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                double loanAmount = resultSet.getDouble("loan_amount");
                double loanBalance = resultSet.getDouble("loan_balance");
                double interestRate = resultSet.getDouble("interest_rate");
                int loanTerm = resultSet.getInt("loan_term");
                Date loanStartDate = resultSet.getDate("loan_start_date");
                Date loanEndDate = resultSet.getDate("loan_end_date");

                // Display loan details
                System.out.println("Loan Amount: " + loanAmount);
                System.out.println("Loan Balance: " + loanBalance);
                System.out.println("Interest Rate: " + interestRate + "%");
                System.out.println("Loan Term: " + loanTerm + " months");
                System.out.println("Loan Start Date: " + loanStartDate);
                System.out.println("Loan End Date: " + loanEndDate);
            } else {
                System.out.println("No loan details found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private boolean isLoanExist(String email) {
        String query = "SELECT * FROM loans WHERE account_number = ? AND loan_balance > 0";
        try{
            long accountNumber = Accounts.get_account_number(email);
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setLong(1, accountNumber);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }catch (Exception e){
            return false;
        }
    }

    }
