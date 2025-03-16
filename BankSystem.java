import java.sql.*;
import java.util.Scanner;

public class BankSystem {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bank_db";
    private static final String USER = "root";
    private static final String PASS = "surya@2003";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Bank Management System");
        System.out.println("1. Register\n2. Login\n3. Exit");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                registerUser(scanner);
                break;
            case 2:
                loginUser(scanner);
                break;
            case 3:
                System.out.println("Thank you for using our system!");
                System.exit(0);
            default:
                System.out.println("Invalid choice!");
        }
    }

    private static void registerUser(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            // Insert user
            String userQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement userStmt = conn.prepareStatement(userQuery, Statement.RETURN_GENERATED_KEYS);
            userStmt.setString(1, username);
            userStmt.setString(2, password);
            userStmt.executeUpdate();

            // Retrieve the generated user_id
            ResultSet rs = userStmt.getGeneratedKeys();
            int userId = -1;
            if (rs.next()) {
                userId = rs.getInt(1);
            }

            if (userId != -1) {
                // Create an account for the user
                String accountQuery = "INSERT INTO accounts (user_id, balance) VALUES (?, 0)";
                PreparedStatement accountStmt = conn.prepareStatement(accountQuery);
                accountStmt.setInt(1, userId);
                accountStmt.executeUpdate();
                System.out.println("User registered successfully! Account created.");
            } else {
                System.out.println("User registration failed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void loginUser(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String query = "SELECT user_id FROM users WHERE username = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("user_id");
                System.out.println("Login successful! Welcome, " + username);
                showBankMenu(scanner, userId);
            } else {
                System.out.println("Invalid credentials!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void showBankMenu(Scanner scanner, int userId) {
        System.out.println("1. Check Balance\n2. Deposit\n3. Withdraw\n4. Exit");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                checkBalance(userId);
                break;
            case 2:
                System.out.print("Enter amount to deposit: ");
                double depositAmount = scanner.nextDouble();
                deposit(userId, depositAmount);
                break;
            case 3:
                System.out.print("Enter amount to withdraw: ");
                double withdrawAmount = scanner.nextDouble();
                withdraw(userId, withdrawAmount);
                break;
            case 4:
                System.exit(0);
            default:
                System.out.println("Invalid choice!");
        }
    }

    private static void checkBalance(int userId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String query = "SELECT balance FROM accounts WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("Current Balance: ₹" + rs.getDouble("balance"));
            } else {
                System.out.println("No account found!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void deposit(int userId, double amount) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            int accountId = getAccountId(conn, userId);
            if (accountId == -1) {
                System.out.println("Account not found! Please register again.");
                return;
            }

            String updateBalance = "UPDATE accounts SET balance = balance + ? WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(updateBalance);
            pstmt.setDouble(1, amount);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();

            String insertTransaction = "INSERT INTO transactions (account_id, transaction_type, amount) VALUES (?, 'DEPOSIT', ?)";
            pstmt = conn.prepareStatement(insertTransaction);
            pstmt.setInt(1, accountId);
            pstmt.setDouble(2, amount);
            pstmt.executeUpdate();

            System.out.println("Deposit successful!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void withdraw(int userId, double amount) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            int accountId = getAccountId(conn, userId);
            if (accountId == -1) {
                System.out.println("Account not found!");
                return;
            }

            String checkBalance = "SELECT balance FROM accounts WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(checkBalance);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getDouble("balance") >= amount) {
                String updateBalance = "UPDATE accounts SET balance = balance - ? WHERE user_id = ?";
                pstmt = conn.prepareStatement(updateBalance);
                pstmt.setDouble(1, amount);
                pstmt.setInt(2, userId);
                pstmt.executeUpdate();

                String insertTransaction = "INSERT INTO transactions (account_id, transaction_type, amount) VALUES (?, 'WITHDRAWAL', ?)";
                pstmt = conn.prepareStatement(insertTransaction);
                pstmt.setInt(1, accountId);
                pstmt.setDouble(2, amount);
                pstmt.executeUpdate();

                System.out.println("Withdrawal successful!");
            } else {
                System.out.println("Insufficient balance!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int getAccountId(Connection conn, int userId) throws SQLException {
        String query = "SELECT account_id FROM accounts WHERE user_id = ?";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, userId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("account_id");
        }
        return -1;
    }
}
