package org.example;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Database credentials
        String url = "jdbc:postgresql://localhost:5432/banking_system";
        String user = "postgres";
        String password = "postgres";
        final String ADMIN_EMAIL = "admin@moneybank.com";
        final String ADMIN_PASSWORD = "admin";
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Scanner scanner = new Scanner(System.in)) {
            Users users = new Users(connection, scanner);
            Accounts accounts = new Accounts(connection, scanner);
            AccountManager accountManager = new AccountManager(connection, scanner);
            LoanService loanService = new LoanService(connection, scanner);

            while (true) {
                System.out.println("\n** WELCOME TO MONEY-MONEY BANK'S ONLINE BANKING **");
                System.out.println("1. Register");
                System.out.println("2. Login as User");
                System.out.println("3. Login as Admin");
                System.out.println("4. Update password");
                System.out.println("5. Exit");
                System.out.print("Enter your choice: ");

                int choice = getValidChoice(scanner, 1, 4);

                switch (choice) {
                    case 1:
                        users.register();
                        break;

                    case 2:
                        // User Login
                        String userEmail = users.login(scanner);
                        if (userEmail != null && !userEmail.equals(ADMIN_EMAIL)) {

                            handleAccountOptions(accounts, accountManager, loanService, userEmail, scanner);
                        } else {
                            System.out.println("Incorrect login or unauthorized access! Users only.");
                        }
                        break;

                    case 3:
                        // Admin Login
                        System.out.print("Please enter your admin email: ");
                        String adminEmail = scanner.next();
                        System.out.print("Please enter your admin password: ");
                        String adminPasswordInput = scanner.next();

                        if (ADMIN_EMAIL.equals(adminEmail) && ADMIN_PASSWORD.equals(adminPasswordInput)) {
                            System.out.println("Welcome Admin!");
                            handleAdminOptions(loanService, scanner); // Admin options for approving/rejecting loans
                        } else {
                            System.out.println("Invalid admin credentials.");
                        }
                        break;

                    case 4:
                        scanner.nextLine();
                        System.out.print("Please enter your email: ");
                        String email = scanner.nextLine().trim();

                        // Validate email input
                        if (email.isEmpty()) {
                            System.out.println("Email cannot be empty. Please try again.");
                        } else {
                            users.updatePassword(email); // Call the update password method
                        }
                        break;

                    case 5:
                        System.out.println("Thank you for banking with us. Goodbye!");
                        return;

                    default:
                        System.out.println("Invalid choice! Please try again.");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("An error occurred. Please try again later.");
        }
    }

    private static void handleAdminOptions(LoanService loanService, Scanner scanner) {
        while (true) {
            System.out.println("\n** ADMIN OPTIONS **");
            System.out.println("1. Approve or Reject Loan");
            System.out.println("2. Logout");
            System.out.print("Enter your choice: ");

            int choice = getValidChoice(scanner, 1, 2);

            switch (choice) {
                case 1:
                    try {
                        loanService.approveRejectLoan(scanner);
                    } catch (SQLException e) {
                        System.out.println("An error occurred while approving/rejecting loan.");
                        e.printStackTrace();
                    }
                    break;
                case 2:
                    System.out.println("Admin logged out successfully.");
                    return;
                default:
                    System.out.println("Invalid choice! Please try again.");
                    break;
            }
        }
    }

    private static void handleAccountOptions(Accounts accounts, AccountManager accountManager, LoanService loanService, String email, Scanner scanner) {
        long accountNumber;
        try {
            boolean isAdmin = loanService.users.isAdmin(email);
            if (isAdmin) {
                // Admin functionality: Approve/Reject loan
                while (true) {
                    System.out.println("\n1. Approve or Reject Loan");
                    System.out.println("2. Logout");
                    System.out.print("Enter your choice: ");

                    int choice = getValidChoice(scanner, 1, 2);

                    switch (choice) {
                        case 1:
                            loanService.approveRejectLoan(scanner);
                            break;
                        case 2:
                            System.out.println("You have successfully logged out.");
                            return;
                        default:
                            System.out.println("Invalid choice! Please try again.");
                            break;
                    }
                }
            }

            if (!Accounts.account_exist(email)) {
                System.out.println("\nNo account found associated with this email.");
                System.out.println("1. Open a new Bank Account");
                System.out.println("2. Logout");
                System.out.print("Enter your choice: ");

                int choice = getValidChoice(scanner, 1, 2);
                if (choice == 1) {
                    accountNumber = accounts.open_account(email);
                    System.out.println("A new account has been opened!");
                    System.out.println("Your account number is: " + accountNumber);
                } else {
                    return;
                }
            }

            accountNumber = Accounts.get_account_number(email);

            while (true) {
                System.out.println("\n1. Withdraw Money");
                System.out.println("2. Deposit Money");
                System.out.println("3. Transfer Money");
                System.out.println("4. Check Balance");
                System.out.println("5. Show Transactions");
                System.out.println("6. Apply for Loan");
                System.out.println("7. Loan Repayment");
                System.out.println("8. View your loan details");
                System.out.println("9. Logout");
                System.out.print("Enter your choice: ");

                int choice = getValidChoice(scanner, 1, 9);
                long finalAccountNumber = accountNumber;

                switch (choice) {
                    case 1:
                        accountManager.debit_money(finalAccountNumber);
                        break;

                    case 2:
                        accountManager.credit_money(finalAccountNumber);
                        break;

                    case 3:
                        accountManager.transfer_money(finalAccountNumber);
                        break;

                    case 4:
                        try {
                            double balance = accountManager.getBalance(finalAccountNumber); // Fetch balance
                            System.out.println("Your current balance is: " + balance); // Print balance
                        } catch (SQLException e) {
                            System.out.println("An error occurred while fetching the balance. Please try again.");
                            e.printStackTrace();
                        }
                        break;

                    case 5:
                        accountManager.showTransactionHistory(finalAccountNumber);
                        break;
                    case 6:
                        loanService.applyLoan(email);
                        break;
                    case 7:
                        loanService.repayLoan(email);
                        break;
                    case 8:
                        loanService.viewLoanDetails(email);
                        break;


                    case 9:
                        System.out.println("You have successfully logged out.");
                        return;

                    default:
                        System.out.println("Invalid choice! Please try again.");
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("An error occurred while handling account options. Please try again.");
            e.printStackTrace();
        }
    }

    private static int getValidChoice(Scanner scanner, int min, int max) {
        while (true) {
            try {
                int choice = scanner.nextInt();
                if (choice >= min && choice <= max) {
                    return choice;
                } else {
                    System.out.print("Invalid input! Please enter a number between " + min + " and " + max + ": ");
                }
            } catch (Exception e) {
                System.out.print("Invalid input! Please enter a valid number: ");
                scanner.next(); // Clear invalid input
            }
        }
    }
}
