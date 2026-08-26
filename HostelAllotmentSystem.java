import java.awt.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.swing.*;

// ---------------------- Night Out Request Class ----------------------
class NightOutRequest {
    String studentName;
    String fromDate;
    String toDate;
    String reason;
    String status; // Pending / Accepted / Rejected

    public NightOutRequest(String studentName, String fromDate, String toDate, String reason) {
        this.studentName = studentName;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.reason = reason;
        this.status = "Pending";
    }

    @Override
    public String toString() {
        return studentName + " (" + fromDate + " → " + toDate + ") - " + status + " | Reason: " + reason;
    }
}

// ---------------------- Student Profile Class ----------------------
class StudentProfile {
    String studentName;
    String parentName;
    String studentPhone;
    String parentPhone;
    String department;
    ImageIcon photo;

    public StudentProfile(String studentName, String parentName, String studentPhone, String parentPhone, String department, ImageIcon photo) {
        this.studentName = studentName;
        this.parentName = parentName;
        this.studentPhone = studentPhone;
        this.parentPhone = parentPhone;
        this.department = department;
        this.photo = photo;
    }
}

// ---------------------- Complaint Class ----------------------
class Complaint {
    String studentName;
    String complaint;
    String date;
    String status;

    public Complaint(String studentName, String complaint, String date) {
        this.studentName = studentName;
        this.complaint = complaint;
        this.date = date;
        this.status = "Pending";
    }
}

// ---------------------- Visitor Class ----------------------
class Visitor {
    String studentName;
    String visitorName;
    String relation;
    String date;
    String time;
    String purpose;

    public Visitor(String studentName, String visitorName, String relation, String date, String time, String purpose) {
        this.studentName = studentName;
        this.visitorName = visitorName;
        this.relation = relation;
        this.date = date;
        this.time = time;
        this.purpose = purpose;
    }
}

// ---------------------- Database Connection Class ----------------------
class DatabaseConnection {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/hostel_management";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Shreya@123";
    
    static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
    }
    
    static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            // Create database if not exists
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS hostel_management");
                stmt.executeUpdate("USE hostel_management");
                
                // Students table
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS students (" +
                    "username VARCHAR(50) PRIMARY KEY, " +
                    "password VARCHAR(100) NOT NULL, " +
                    "student_name VARCHAR(100) NOT NULL, " +
                    "parent_name VARCHAR(100) NOT NULL, " +
                    "student_phone VARCHAR(20) NOT NULL, " +
                    "parent_phone VARCHAR(20) NOT NULL, " +
                    "department VARCHAR(100) NOT NULL, " +
                    "photo BLOB, " +
                    "campus VARCHAR(50)" +
                    ")");
                
                // Parents table
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS parents (" +
                    "username VARCHAR(50) PRIMARY KEY, " +
                    "password VARCHAR(100) NOT NULL, " +
                    "student_username VARCHAR(50), " +
                    "FOREIGN KEY (student_username) REFERENCES students(username) ON DELETE CASCADE" +
                    ")");
                
                // Complaints table
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS complaints (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_username VARCHAR(50) NOT NULL, " +
                    "complaint TEXT NOT NULL, " +
                    "date VARCHAR(50) NOT NULL, " +
                    "status VARCHAR(20) DEFAULT 'Pending', " +
                    "FOREIGN KEY (student_username) REFERENCES students(username) ON DELETE CASCADE" +
                    ")");
                
                // Attendance table
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS attendance (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_username VARCHAR(50) NOT NULL, " +
                    "date VARCHAR(50) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL, " +
                    "FOREIGN KEY (student_username) REFERENCES students(username) ON DELETE CASCADE" +
                    ")");
                
                // Visitors table
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS visitors (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_username VARCHAR(50) NOT NULL, " +
                    "visitor_name VARCHAR(100) NOT NULL, " +
                    "relation VARCHAR(50) NOT NULL, " +
                    "date VARCHAR(50) NOT NULL, " +
                    "time VARCHAR(20) NOT NULL, " +
                    "purpose TEXT, " +
                    "FOREIGN KEY (student_username) REFERENCES students(username) ON DELETE CASCADE" +
                    ")");
                
                // Night out requests table
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS night_out_requests (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_username VARCHAR(50) NOT NULL, " +
                    "from_date VARCHAR(50) NOT NULL, " +
                    "to_date VARCHAR(50) NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "status VARCHAR(20) DEFAULT 'Pending', " +
                    "FOREIGN KEY (student_username) REFERENCES students(username) ON DELETE CASCADE" +
                    ")");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database initialization error: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}

// ---------------------- Main Class ----------------------
public class HostelAllotmentSystem {
    static JFrame frame;
    static String selectedCampus;

    public static void main(String[] args) {
        // Initialize database
        DatabaseConnection.initializeDatabase();
        SwingUtilities.invokeLater(() -> showCampusSelection());
    }
    
    // ---------- Database Helper Methods ----------
    static boolean checkStudentLogin(String username, String password) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT password FROM students WHERE username = ?");
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password").equals(password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    static boolean checkParentLogin(String username, String password) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT password FROM parents WHERE username = ?");
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password").equals(password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    
    static void registerParent(String username, String password, String studentUsername) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO parents (username, password, student_username) VALUES (?, ?, ?)");
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, studentUsername);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Registration error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static StudentProfile getStudentProfile(String username) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT student_name, parent_name, student_phone, parent_phone, department, photo FROM students WHERE username = ?");
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                ImageIcon photo = null;
                byte[] photoBytes = rs.getBytes("photo");
                if (photoBytes != null && photoBytes.length > 0) {
                    try {
                        ByteArrayInputStream bais = new ByteArrayInputStream(photoBytes);
                        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(bais);
                        if (img != null) {
                            Image scaledImg = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                            photo = new ImageIcon(scaledImg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return new StudentProfile(
                    rs.getString("student_name"),
                    rs.getString("parent_name"),
                    rs.getString("student_phone"),
                    rs.getString("parent_phone"),
                    rs.getString("department"),
                    photo
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static void registerStudent(String username, String password, StudentProfile profile, ImageIcon photo) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO students (username, password, student_name, parent_name, student_phone, parent_phone, department, photo, campus) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, profile.studentName);
            pstmt.setString(4, profile.parentName);
            pstmt.setString(5, profile.studentPhone);
            pstmt.setString(6, profile.parentPhone);
            pstmt.setString(7, profile.department);
            
            // Convert ImageIcon to byte array
            byte[] photoBytes = null;
            if (photo != null && photo.getImage() != null) {
                try {
                    java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                        photo.getIconWidth(), photo.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics g = img.createGraphics();
                    photo.paintIcon(null, g, 0, 0);
                    g.dispose();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(img, "png", baos);
                    photoBytes = baos.toByteArray();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            pstmt.setBytes(8, photoBytes);
            pstmt.setString(9, selectedCampus);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Registration error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static String getParentStudentMapping(String parentUsername) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT student_username FROM parents WHERE username = ?");
            pstmt.setString(1, parentUsername);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("student_username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static ArrayList<String> getAllStudentUsernames() {
        ArrayList<String> students = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT username FROM students");
            while (rs.next()) {
                students.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }
    
    // Complaint operations
    static void addComplaint(String studentUsername, String complaint, String date) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO complaints (student_username, complaint, date, status) VALUES (?, ?, ?, 'Pending')");
            pstmt.setString(1, studentUsername);
            pstmt.setString(2, complaint);
            pstmt.setString(3, date);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    static ArrayList<Complaint> getComplaints(String studentUsername) {
        ArrayList<Complaint> complaints = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT complaint, date, status FROM complaints WHERE student_username = ? ORDER BY id DESC");
            pstmt.setString(1, studentUsername);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Complaint c = new Complaint(studentUsername, rs.getString("complaint"), rs.getString("date"));
                c.status = rs.getString("status");
                complaints.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return complaints;
    }
    
    static ArrayList<Complaint> getAllComplaints() {
        ArrayList<Complaint> complaints = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT student_username, complaint, date, status FROM complaints ORDER BY id DESC");
            while (rs.next()) {
                Complaint c = new Complaint(rs.getString("student_username"), rs.getString("complaint"), rs.getString("date"));
                c.status = rs.getString("status");
                complaints.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return complaints;
    }
    
    static void updateComplaintStatus(String studentUsername, String complaint, String date, String status) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE complaints SET status = ? WHERE student_username = ? AND complaint = ? AND date = ?");
            pstmt.setString(1, status);
            pstmt.setString(2, studentUsername);
            pstmt.setString(3, complaint);
            pstmt.setString(4, date);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Attendance operations
    static void addAttendance(String studentUsername, String date, String status) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO attendance (student_username, date, status) VALUES (?, ?, ?)");
            pstmt.setString(1, studentUsername);
            pstmt.setString(2, date);
            pstmt.setString(3, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    static ArrayList<String> getAttendance(String studentUsername) {
        ArrayList<String> attendance = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT date, status FROM attendance WHERE student_username = ? ORDER BY id DESC");
            pstmt.setString(1, studentUsername);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                attendance.add(rs.getString("date") + " - " + rs.getString("status"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attendance;
    }
    
    // Visitor operations
    static void addVisitor(String studentUsername, String visitorName, String relation, String date, String time, String purpose) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO visitors (student_username, visitor_name, relation, date, time, purpose) VALUES (?, ?, ?, ?, ?, ?)");
            pstmt.setString(1, studentUsername);
            pstmt.setString(2, visitorName);
            pstmt.setString(3, relation);
            pstmt.setString(4, date);
            pstmt.setString(5, time);
            pstmt.setString(6, purpose);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    static ArrayList<Visitor> getVisitors(String studentUsername) {
        ArrayList<Visitor> visitors = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT visitor_name, relation, date, time, purpose FROM visitors WHERE student_username = ? ORDER BY id DESC");
            pstmt.setString(1, studentUsername);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                visitors.add(new Visitor(studentUsername, rs.getString("visitor_name"), rs.getString("relation"),
                    rs.getString("date"), rs.getString("time"), rs.getString("purpose")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return visitors;
    }
    
    static ArrayList<Visitor> getAllVisitors() {
        ArrayList<Visitor> visitors = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT student_username, visitor_name, relation, date, time, purpose FROM visitors ORDER BY id DESC");
            while (rs.next()) {
                visitors.add(new Visitor(rs.getString("student_username"), rs.getString("visitor_name"),
                    rs.getString("relation"), rs.getString("date"), rs.getString("time"), rs.getString("purpose")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return visitors;
    }
    
    // Night out operations
    static void addNightOutRequest(String studentUsername, String fromDate, String toDate, String reason) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO night_out_requests (student_username, from_date, to_date, reason, status) VALUES (?, ?, ?, ?, 'Pending')");
            pstmt.setString(1, studentUsername);
            pstmt.setString(2, fromDate);
            pstmt.setString(3, toDate);
            pstmt.setString(4, reason);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    static ArrayList<NightOutRequest> getNightOutRequests(String studentUsername) {
        ArrayList<NightOutRequest> requests = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT from_date, to_date, reason, status FROM night_out_requests WHERE student_username = ? ORDER BY id DESC");
            pstmt.setString(1, studentUsername);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                NightOutRequest req = new NightOutRequest(studentUsername, rs.getString("from_date"),
                    rs.getString("to_date"), rs.getString("reason"));
                req.status = rs.getString("status");
                requests.add(req);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }
    
    static ArrayList<NightOutRequest> getAllNightOutRequests() {
        ArrayList<NightOutRequest> requests = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT student_username, from_date, to_date, reason, status FROM night_out_requests ORDER BY id DESC");
            while (rs.next()) {
                NightOutRequest req = new NightOutRequest(rs.getString("student_username"),
                    rs.getString("from_date"), rs.getString("to_date"), rs.getString("reason"));
                req.status = rs.getString("status");
                requests.add(req);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }
    
    static void updateNightOutStatus(String studentUsername, String fromDate, String toDate, String status) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE night_out_requests SET status = ? WHERE student_username = ? AND from_date = ? AND to_date = ?");
            pstmt.setString(1, status);
            pstmt.setString(2, studentUsername);
            pstmt.setString(3, fromDate);
            pstmt.setString(4, toDate);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    static ArrayList<StudentProfile> getAllStudentProfiles() {
        ArrayList<StudentProfile> profiles = new ArrayList<>();
        ArrayList<String> usernames = getAllStudentUsernames();
        for (String username : usernames) {
            StudentProfile prof = getStudentProfile(username);
            if (prof != null) {
                profiles.add(prof);
            }
        }
        return profiles;
    }

    // ---------- Campus Selection ----------
    static void showCampusSelection() {
        frame = new JFrame("Hostel Campus Selection");
        frame.setSize(400, 350);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(230, 230, 250));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel logoLabel = new JLabel("HOSTEL LOGO", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        frame.add(logoLabel, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(230, 230, 250));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Select Your Campus:", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0,10)));

        String[] campuses = {"Churchgate Campus", "Juhu Campus"};
        JComboBox<String> campusCombo = new JComboBox<>(campuses);
        campusCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(campusCombo);
        panel.add(Box.createRigidArea(new Dimension(0,20)));

        JButton nextButton = new JButton("Next");
        nextButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(nextButton);

        nextButton.addActionListener(e -> {
            selectedCampus = (String) campusCombo.getSelectedItem();
            showLoginOptions();
        });

        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    // ---------- Login Options ----------
    static void showLoginOptions() {
        frame.getContentPane().removeAll();
        frame.setTitle("Login Options");
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Login As");
        label.setFont(new Font("Segoe UI", Font.BOLD, 20));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(20,0,20,0));
        frame.add(label);

        JButton adminBtn = createButton("Admin", new Color(255, 99, 71));
        JButton studentBtn = createButton("Student", new Color(60, 179, 113));
        JButton parentBtn = createButton("Parent", new Color(65,105,225));

        adminBtn.addActionListener(e -> showLogin("Admin"));
        studentBtn.addActionListener(e -> showLogin("Student"));
        parentBtn.addActionListener(e -> showLogin("Parent"));

        frame.add(adminBtn);
        frame.add(Box.createRigidArea(new Dimension(0,15)));
        frame.add(studentBtn);
        frame.add(Box.createRigidArea(new Dimension(0,15)));
        frame.add(parentBtn);

        frame.revalidate();
        frame.repaint();
    }

    static JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(200,40));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setFocusPainted(false);
        return btn;
    }
    
    // ---------- Date Picker Component (Day Month Year format) ----------
    static JPanel createDatePicker(String defaultValue) {
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        datePanel.setBackground(Color.WHITE);
        
        // Parse default value if provided
        int day = 1, month = 1, year = LocalDate.now().getYear();
        if(defaultValue != null && !defaultValue.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(defaultValue);
                day = date.getDayOfMonth();
                month = date.getMonthValue();
                year = date.getYear();
            } catch(Exception e) {
                LocalDate now = LocalDate.now();
                day = now.getDayOfMonth();
                month = now.getMonthValue();
                year = now.getYear();
            }
        } else {
            LocalDate now = LocalDate.now();
            day = now.getDayOfMonth();
            month = now.getMonthValue();
            year = now.getYear();
        }
        
        // Day spinner (1-31)
        JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(day, 1, 31, 1));
        daySpinner.setPreferredSize(new Dimension(60, 25));
        JSpinner.DefaultEditor dayEditor = (JSpinner.DefaultEditor) daySpinner.getEditor();
        dayEditor.getTextField().setEditable(false);
        
        // Month spinner (1-12)
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        JSpinner monthSpinner = new JSpinner(new SpinnerListModel(months));
        monthSpinner.setValue(months[month - 1]);
        monthSpinner.setPreferredSize(new Dimension(100, 25));
        JSpinner.DefaultEditor monthEditor = (JSpinner.DefaultEditor) monthSpinner.getEditor();
        monthEditor.getTextField().setEditable(false);
        
        // Year spinner (current year ± 10)
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(year, year - 10, year + 10, 1));
        yearSpinner.setPreferredSize(new Dimension(70, 25));
        JSpinner.DefaultEditor yearEditor = (JSpinner.DefaultEditor) yearSpinner.getEditor();
        yearEditor.getTextField().setEditable(false);
        
        datePanel.add(daySpinner);
        datePanel.add(new JLabel(" "));
        datePanel.add(monthSpinner);
        datePanel.add(new JLabel(" "));
        datePanel.add(yearSpinner);
        
        // Store spinners in client property for later retrieval
        datePanel.putClientProperty("daySpinner", daySpinner);
        datePanel.putClientProperty("monthSpinner", monthSpinner);
        datePanel.putClientProperty("yearSpinner", yearSpinner);
        
        return datePanel;
    }
    
    // Get date string from date picker in DD MM YYYY format
    static String getDateFromPicker(JPanel datePicker) {
        JSpinner daySpinner = (JSpinner) datePicker.getClientProperty("daySpinner");
        JSpinner monthSpinner = (JSpinner) datePicker.getClientProperty("monthSpinner");
        JSpinner yearSpinner = (JSpinner) datePicker.getClientProperty("yearSpinner");
        
        int day = (Integer) daySpinner.getValue();
        String monthStr = (String) monthSpinner.getValue();
        int year = (Integer) yearSpinner.getValue();
        
        // Convert month name to number
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        int month = 1;
        for(int i = 0; i < months.length; i++) {
            if(months[i].equals(monthStr)) {
                month = i + 1;
                break;
            }
        }
        
        return String.format("%02d %02d %04d", day, month, year);
    }
    
    // ---------- Time Picker Component ----------
    static JPanel createTimePicker(String defaultValue) {
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timePanel.setBackground(Color.WHITE);
        
        int hour = 10, minute = 0;
        String ampm = "AM";
        if(defaultValue != null && !defaultValue.isEmpty()) {
            try {
                if(defaultValue.contains(":")) {
                    String[] parts = defaultValue.split(":");
                    hour = Integer.parseInt(parts[0].trim());
                    String[] minParts = parts[1].trim().split(" ");
                    minute = Integer.parseInt(minParts[0]);
                    if(minParts.length > 1) ampm = minParts[1].toUpperCase();
                }
            } catch(Exception e) {
                // Use defaults
            }
        }
        
        // Hour spinner (1-12)
        JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour), 1, 12, 1));
        hourSpinner.setPreferredSize(new Dimension(60, 25));
        JSpinner.DefaultEditor hourEditor = (JSpinner.DefaultEditor) hourSpinner.getEditor();
        hourEditor.getTextField().setEditable(false);
        
        // Minute spinner (0-59, step 5)
        int minValue = (minute / 5) * 5;
        JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(minValue, 0, 55, 5));
        minuteSpinner.setPreferredSize(new Dimension(60, 25));
        JSpinner.DefaultEditor minEditor = (JSpinner.DefaultEditor) minuteSpinner.getEditor();
        minEditor.getTextField().setEditable(false);
        
        // AM/PM combo
        JComboBox<String> ampmCombo = new JComboBox<>(new String[]{"AM", "PM"});
        ampmCombo.setSelectedItem(ampm);
        ampmCombo.setPreferredSize(new Dimension(60, 25));
        
        timePanel.add(hourSpinner);
        timePanel.add(new JLabel(":"));
        timePanel.add(minuteSpinner);
        timePanel.add(ampmCombo);
        
        timePanel.putClientProperty("hourSpinner", hourSpinner);
        timePanel.putClientProperty("minuteSpinner", minuteSpinner);
        timePanel.putClientProperty("ampmCombo", ampmCombo);
        
        return timePanel;
    }
    
    // Get time string from time picker
    @SuppressWarnings("unchecked")
    static String getTimeFromPicker(JPanel timePicker) {
        JSpinner hourSpinner = (JSpinner) timePicker.getClientProperty("hourSpinner");
        JSpinner minuteSpinner = (JSpinner) timePicker.getClientProperty("minuteSpinner");
        JComboBox<String> ampmCombo = (JComboBox<String>) timePicker.getClientProperty("ampmCombo");
        
        int hour = (Integer) hourSpinner.getValue();
        int minute = (Integer) minuteSpinner.getValue();
        String ampm = (String) ampmCombo.getSelectedItem();
        
        if(ampm.equals("PM") && hour != 12) hour += 12;
        if(ampm.equals("AM") && hour == 12) hour = 0;
        
        return String.format("%02d:%02d %s", hour, minute, ampm);
    }

    // ---------- Login ----------
    static void showLogin(String userType) {
        frame.getContentPane().removeAll();
        frame.setSize(500,450);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel(userType + " Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=2;
        frame.add(titleLabel, gbc);
        gbc.gridwidth=1;

        // Login Credentials Info
        String credentialsInfo = "";
        if(userType.equals("Admin")) {
            credentialsInfo = "<html><div style='background-color:#E8F5E9;padding:10px;border:1px solid #4CAF50;border-radius:5px;'>" +
                "<b>Login Credentials:</b><br>" +
                "Churchgate Campus:<br>" +
                "Username: <b>admin_churchgate</b><br>" +
                "Password: <b>ch@123</b><br><br>" +
                "Juhu Campus:<br>" +
                "Username: <b>admin_juhu</b><br>" +
                "Password: <b>jh@123</b>" +
                "</div></html>";
        } else {
            credentialsInfo = "<html><div style='background-color:#E3F2FD;padding:10px;border:1px solid #2196F3;border-radius:5px;'>" +
                "<b>Note:</b> Register first if you don't have an account.<br>" +
                "Use your registered username and password to login." +
                "</div></html>";
        }
        
        JLabel credLabel = new JLabel(credentialsInfo);
        gbc.gridx=0; gbc.gridy=1; gbc.gridwidth=2;
        frame.add(credLabel, gbc);
        gbc.gridwidth=1;

        JLabel userLabel = new JLabel("Username:");
        gbc.gridx=0; gbc.gridy=2; frame.add(userLabel, gbc);
        JTextField usernameField = new JTextField(15); gbc.gridx=1; frame.add(usernameField, gbc);

        JLabel passLabel = new JLabel("Password:");
        gbc.gridx=0; gbc.gridy=3; frame.add(passLabel, gbc);
        JPasswordField passwordField = new JPasswordField(15); gbc.gridx=1; frame.add(passwordField, gbc);

        JButton loginBtn = createButton("Login", new Color(30,144,255));
        gbc.gridx=1; gbc.gridy=4; frame.add(loginBtn, gbc);

        if(!userType.equals("Admin")) {
            JButton registerBtn = createButton("Register", new Color(34,139,34));
            gbc.gridx=1; gbc.gridy=5; frame.add(registerBtn, gbc);
            registerBtn.addActionListener(e -> showRegistration(userType));
        }

        loginBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            boolean loginSuccess = false;
            if(userType.equals("Admin")) {
                loginSuccess = (selectedCampus.equals("Churchgate Campus") && username.equals("admin_churchgate") && password.equals("ch@123"))
                        || (selectedCampus.equals("Juhu Campus") && username.equals("admin_juhu") && password.equals("jh@123"));
            } else if(userType.equals("Student")) {
                loginSuccess = checkStudentLogin(username, password);
            } else {
                loginSuccess = checkParentLogin(username, password);
            }
            if(loginSuccess) showDashboard(userType, username);
            else JOptionPane.showMessageDialog(frame,"Invalid credentials!");
        });

        frame.revalidate();
        frame.repaint();
    }

    // ---------- Registration ----------
    static void showRegistration(String userType) {
        frame.getContentPane().removeAll();
        frame.setSize(500,550);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel(userType + " Registration", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=2;
        frame.add(title, gbc);
        gbc.gridwidth=1;

        gbc.gridx=0; gbc.gridy=1; frame.add(new JLabel("Username:"), gbc);
        JTextField usernameField = new JTextField(15); gbc.gridx=1; frame.add(usernameField, gbc);

        gbc.gridx=0; gbc.gridy=2; frame.add(new JLabel("Password:"), gbc);
        JPasswordField passwordField = new JPasswordField(15); gbc.gridx=1; frame.add(passwordField, gbc);

        // Student profile fields
        final JTextField[] studentNameField = new JTextField[1];
        final JTextField[] parentNameField = new JTextField[1];
        final JTextField[] studentPhoneField = new JTextField[1];
        final JTextField[] parentPhoneField = new JTextField[1];
        final JTextField[] deptField = new JTextField[1];
        final JLabel[] photoLabel = new JLabel[1];
        final ImageIcon[] photoIcon = new ImageIcon[1];

        if(userType.equals("Student")) {
            gbc.gridx=0; gbc.gridy=3; frame.add(new JLabel("Student Name:"), gbc);
            studentNameField[0] = new JTextField(15); gbc.gridx=1; frame.add(studentNameField[0], gbc);

            gbc.gridx=0; gbc.gridy=4; frame.add(new JLabel("Parent Name:"), gbc);
            parentNameField[0] = new JTextField(15); gbc.gridx=1; frame.add(parentNameField[0], gbc);

            gbc.gridx=0; gbc.gridy=5; frame.add(new JLabel("Student Phone:"), gbc);
            studentPhoneField[0] = new JTextField(15); gbc.gridx=1; frame.add(studentPhoneField[0], gbc);

            gbc.gridx=0; gbc.gridy=6; frame.add(new JLabel("Parent Phone:"), gbc);
            parentPhoneField[0] = new JTextField(15); gbc.gridx=1; frame.add(parentPhoneField[0], gbc);

            gbc.gridx=0; gbc.gridy=7; frame.add(new JLabel("Department:"), gbc);
            deptField[0] = new JTextField(15); gbc.gridx=1; frame.add(deptField[0], gbc);

            gbc.gridx=0; gbc.gridy=8; frame.add(new JLabel("Photo:"), gbc);
            photoLabel[0] = new JLabel(); gbc.gridx=1; frame.add(photoLabel[0], gbc);

            JButton uploadBtn = new JButton("Upload Photo"); gbc.gridx=1; gbc.gridy=9; frame.add(uploadBtn, gbc);
            uploadBtn.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                int r = chooser.showOpenDialog(frame);
                if(r == JFileChooser.APPROVE_OPTION){
                    photoIcon[0] = new ImageIcon(new ImageIcon(chooser.getSelectedFile().getAbsolutePath()).getImage().getScaledInstance(100,100,Image.SCALE_SMOOTH));
                    photoLabel[0].setIcon(photoIcon[0]);
                }
            });
        }

        JButton registerBtn = createButton("Register", new Color(60,179,113));
        gbc.gridx=1; gbc.gridy=10; frame.add(registerBtn, gbc);

        registerBtn.addActionListener(e -> {
            String user=usernameField.getText().trim();
            String pass=new String(passwordField.getPassword()).trim();
            if(user.isEmpty()||pass.isEmpty()){ JOptionPane.showMessageDialog(frame,"Fill all fields!"); return; }
            if(userType.equals("Student")) {
                if(studentNameField[0].getText().trim().isEmpty() || parentNameField[0].getText().trim().isEmpty()
                        || studentPhoneField[0].getText().trim().isEmpty() || parentPhoneField[0].getText().trim().isEmpty()
                        || deptField[0].getText().trim().isEmpty() || photoIcon[0]==null) {
                    JOptionPane.showMessageDialog(frame,"Fill all profile details including photo!"); return;
                }
                StudentProfile profile = new StudentProfile(studentNameField[0].getText().trim(),
                        parentNameField[0].getText().trim(),
                        studentPhoneField[0].getText().trim(),
                        parentPhoneField[0].getText().trim(),
                        deptField[0].getText().trim(),
                        photoIcon[0]);
                registerStudent(user, pass, profile, photoIcon[0]);
            } else {
                // Ask which student this parent belongs to
                ArrayList<String> studentList = getAllStudentUsernames();
                if(!studentList.isEmpty()) {
                    String[] studentArray = studentList.toArray(new String[0]);
                    String studentUsername = (String) JOptionPane.showInputDialog(frame,
                        "Select your student:", "Student Selection",
                        JOptionPane.QUESTION_MESSAGE, null, studentArray, studentArray[0]);
                    if(studentUsername != null && !studentUsername.trim().isEmpty()) {
                        registerParent(user, pass, studentUsername.trim());
                    } else {
                        return;
                    }
                } else {
                    String studentUsername = JOptionPane.showInputDialog(frame, "Enter your student's username (will be available after student registration):");
                    if(studentUsername != null && !studentUsername.trim().isEmpty()) {
                        registerParent(user, pass, studentUsername.trim());
                    } else {
                        return;
                    }
                }
            }

            JOptionPane.showMessageDialog(frame, userType+" registered successfully!");
            showLogin(userType);
        });

        frame.revalidate();
        frame.repaint();
    }

    // ---------- Dashboard ----------
    static void showDashboard(String userType, String username){
        frame.getContentPane().removeAll();
        frame.setSize(900,600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(245,245,250));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(60,179,113));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));
        JLabel welcomeLabel = new JLabel("Welcome, " + username + " (" + userType + ") - " + selectedCampus);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        welcomeLabel.setForeground(Color.WHITE);
        headerPanel.add(welcomeLabel);
        frame.add(headerPanel, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(new Color(240,240,245));
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        leftPanel.setPreferredSize(new Dimension(200,0));
        frame.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setLayout(new BorderLayout());
        JLabel contentLabel = new JLabel("<html><div style='text-align:center;padding:50px;'><h2>Welcome to Hostel Management System</h2><p>Select an option from the menu to get started</p></div></html>", SwingConstants.CENTER);
        contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        rightPanel.add(contentLabel, BorderLayout.CENTER);
        frame.add(rightPanel, BorderLayout.CENTER);

        String[] buttons;
        if(userType.equals("Admin")) buttons = new String[]{"Profile","Student Details","Fees","Rules","Complaint Box","Attendance","Visitor","Night Out","Logout"};
        else if(userType.equals("Student")) buttons = new String[]{"Profile","Fees","Rules","Complaint Box","Attendance","Visitor","Night Out","Logout"};
        else buttons = new String[]{"Profile","Fees","Night Out","Visitor","Logout"};

        for(String b:buttons){
            JButton btn = new JButton(b);
            btn.setPreferredSize(new Dimension(180,40));
            btn.setMaximumSize(new Dimension(180,40));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setBackground(new Color(60,179,113));
            btn.setForeground(Color.WHITE);
            if(b.equals("Logout")) {
                btn.setBackground(new Color(220,20,60));
            }
            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    btn.setBackground(btn.getBackground().darker());
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    btn.setBackground(b.equals("Logout") ? new Color(220,20,60) : new Color(60,179,113));
                }
            });
            leftPanel.add(btn);
            leftPanel.add(Box.createRigidArea(new Dimension(0,8)));

            btn.addActionListener(e -> handleDashboardAction(b, userType, username, rightPanel));
        }

        frame.revalidate();
        frame.repaint();
    }

    // ---------- Dashboard Actions ----------
    static void handleDashboardAction(String b, String userType, String username, JPanel rightPanel){
        rightPanel.removeAll();
        switch(b){
            case "Profile":
                handleProfile(userType, username, rightPanel);
                break;
            case "Fees":
                handleFees(userType, username, rightPanel);
                break;
            case "Rules":
                handleRules(rightPanel);
                break;
            case "Complaint Box":
                handleComplaints(userType, username, rightPanel);
                break;
            case "Attendance":
                handleAttendance(userType, username, rightPanel);
                break;
            case "Visitor":
                handleVisitor(userType, username, rightPanel);
                break;
            case "Student Details":
                handleStudentDetails(rightPanel);
                break;
            case "Night Out":
                handleNightOut(userType, username, rightPanel);
                break;
            case "Logout":
                showLoginOptions();
                break;
        }
        rightPanel.revalidate();
        rightPanel.repaint();
    }
    
    // ---------- Profile Handler ----------
    static void handleProfile(String userType, String username, JPanel rightPanel) {
        rightPanel.setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
        if(userType.equals("Student")) {
            StudentProfile prof = getStudentProfile(username);
            if(prof != null){
                JLabel title = new JLabel("Student Profile", SwingConstants.CENTER);
                title.setFont(new Font("Segoe UI", Font.BOLD, 24));
                title.setAlignmentX(Component.CENTER_ALIGNMENT);
                contentPanel.add(title);
                contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
                
                JLabel photoLabel = new JLabel(prof.photo);
                photoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                contentPanel.add(photoLabel);
                contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
                
                addInfoLabel(contentPanel, "Student Name:", prof.studentName);
                addInfoLabel(contentPanel, "Parent Name:", prof.parentName);
                addInfoLabel(contentPanel, "Student Phone:", prof.studentPhone);
                addInfoLabel(contentPanel, "Parent Phone:", prof.parentPhone);
                addInfoLabel(contentPanel, "Department:", prof.department);
            } else {
                contentPanel.add(new JLabel("Profile not found"));
            }
        } else if(userType.equals("Admin")) {
            JLabel title = new JLabel("Admin Profile", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 24));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(title);
            contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
            addInfoLabel(contentPanel, "Username:", username);
            addInfoLabel(contentPanel, "Campus:", selectedCampus);
            addInfoLabel(contentPanel, "Role:", "Administrator");
        } else { // Parent
            String studentUsername = getParentStudentMapping(username);
            if(studentUsername != null) {
                StudentProfile prof = getStudentProfile(studentUsername);
                if(prof != null) {
                    JLabel title = new JLabel("Child's Profile", SwingConstants.CENTER);
                    title.setFont(new Font("Segoe UI", Font.BOLD, 24));
                    title.setAlignmentX(Component.CENTER_ALIGNMENT);
                    contentPanel.add(title);
                    contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
                    
                    JLabel photoLabel = new JLabel(prof.photo);
                    photoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    contentPanel.add(photoLabel);
                    contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
                    
                    addInfoLabel(contentPanel, "Student Name:", prof.studentName);
                    addInfoLabel(contentPanel, "Parent Name:", prof.parentName);
                    addInfoLabel(contentPanel, "Student Phone:", prof.studentPhone);
                    addInfoLabel(contentPanel, "Parent Phone:", prof.parentPhone);
                    addInfoLabel(contentPanel, "Department:", prof.department);
                } else {
                    contentPanel.add(new JLabel("Student profile not found"));
                }
            } else {
                contentPanel.add(new JLabel("Student mapping not found"));
            }
        }
        
        JScrollPane scroll = new JScrollPane(contentPanel);
        rightPanel.add(scroll, BorderLayout.CENTER);
    }
    
    static void addInfoLabel(JPanel panel, String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel l = new JLabel("<html><b>" + label + "</b> " + value + "</html>");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        p.add(l);
        panel.add(p);
        panel.add(Box.createRigidArea(new Dimension(0,10)));
    }
    
    // ---------- Fees Handler ----------
    static void handleFees(String userType, String username, JPanel rightPanel) {
        rightPanel.setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
        JLabel title = new JLabel("Fee Structure", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(title);
        contentPanel.add(Box.createRigidArea(new Dimension(0,30)));
        
        String feesInfo = "<html><div style='text-align:center;'>" +
            "<h3>Monthly Charges</h3>" +
            "<p><b>Room Rent:</b> ₹5,000/month</p>" +
            "<p><b>Mess Charges:</b> ₹5,000/month</p>" +
            "<p><b>Total:</b> ₹10,000/month</p><br>" +
            "<p><b>Payment Due Date:</b> 5th of every month</p>" +
            "<p><b>Late Fee:</b> ₹500 after due date</p><br>" +
            "<p style='color:green;'><b>Current Status:</b> Paid</p>" +
            "</div></html>";
        
        JLabel feesLabel = new JLabel(feesInfo);
        feesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        feesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(feesLabel);
        
        JScrollPane scroll = new JScrollPane(contentPanel);
        rightPanel.add(scroll, BorderLayout.CENTER);
    }
    
    // ---------- Rules Handler ----------
    static void handleRules(JPanel rightPanel) {
        rightPanel.setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
        JLabel title = new JLabel("Hostel Rules & Regulations", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(title);
        contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
        
        String[] rules = {
            "1. Maintain discipline and respect towards all residents and staff",
            "2. No outside food/mess allowed without permission",
            "3. Follow hostel timings strictly (Entry: 10:00 PM, Exit: 6:00 AM)",
            "4. Maintain cleanliness in rooms and common areas",
            "5. No loud music or noise after 10:00 PM",
            "6. Visitors must be registered and approved",
            "7. Night out requests must be submitted 24 hours in advance",
            "8. Smoking and alcohol are strictly prohibited",
            "9. Damage to hostel property will result in fines",
            "10. Regular attendance checks will be conducted",
            "11. Follow dress code in common areas",
            "12. Report any issues or complaints to the admin immediately"
        };
        
        for(String rule : rules) {
            JLabel ruleLabel = new JLabel(rule);
            ruleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            ruleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(ruleLabel);
            contentPanel.add(Box.createRigidArea(new Dimension(0,10)));
        }
        
        JScrollPane scroll = new JScrollPane(contentPanel);
        rightPanel.add(scroll, BorderLayout.CENTER);
    }
    
    // ---------- Complaints Handler ----------
    static void handleComplaints(String userType, String username, JPanel rightPanel) {
        rightPanel.setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
        if(userType.equals("Student")) {
            JLabel title = new JLabel("Complaint Box", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 24));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(title);
            contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
            
            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10,10,10,10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            gbc.gridx=0; gbc.gridy=0;
            formPanel.add(new JLabel("Complaint:"), gbc);
            JTextArea complaintField = new JTextArea(4, 30);
            complaintField.setLineWrap(true);
            JScrollPane textScroll = new JScrollPane(complaintField);
            gbc.gridx=1; formPanel.add(textScroll, gbc);
            
            JButton submitBtn = createButton("Submit Complaint", new Color(60,179,113));
            gbc.gridx=1; gbc.gridy=1; formPanel.add(submitBtn, gbc);
            
            submitBtn.addActionListener(e -> {
                String complaint = complaintField.getText().trim();
                if(complaint.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter your complaint!");
                    return;
                }
                LocalDate now = LocalDate.now();
                String dateStr = String.format("%02d %02d %04d", now.getDayOfMonth(), now.getMonthValue(), now.getYear());
                addComplaint(username, complaint, dateStr);
                JOptionPane.showMessageDialog(frame, "Complaint submitted successfully!");
                handleComplaints(userType, username, rightPanel);
            });
            
            contentPanel.add(formPanel);
            contentPanel.add(Box.createRigidArea(new Dimension(0,30)));
            
            JLabel historyLabel = new JLabel("Your Complaints:", SwingConstants.LEFT);
            historyLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            historyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(historyLabel);
            contentPanel.add(Box.createRigidArea(new Dimension(0,10)));
            
            ArrayList<Complaint> complaints = getComplaints(username);
            if(complaints != null && !complaints.isEmpty()) {
                for(Complaint c : complaints) {
                    JPanel compPanel = new JPanel(new BorderLayout());
                    compPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        BorderFactory.createEmptyBorder(10,10,10,10)));
                    compPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
                    
                    String statusColor = c.status.equals("Pending") ? "orange" : 
                                        c.status.equals("Resolved") ? "green" : "red";
                    JLabel compLabel = new JLabel("<html><b>Date:</b> " + c.date + 
                        " | <b>Status:</b> <span style='color:" + statusColor + ";'>" + c.status + "</span><br>" +
                        "<b>Complaint:</b> " + c.complaint + "</html>");
                    compPanel.add(compLabel, BorderLayout.CENTER);
                    contentPanel.add(compPanel);
                    contentPanel.add(Box.createRigidArea(new Dimension(0,10)));
                }
            } else {
                contentPanel.add(new JLabel("No complaints submitted yet"));
            }
            
        } else { // Admin
            JLabel title = new JLabel("All Complaints", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 24));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(title);
            contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
            
            ArrayList<Complaint> allComplaints = getAllComplaints();
            boolean hasComplaints = !allComplaints.isEmpty();
            for(Complaint c : allComplaints) {
                JPanel compPanel = new JPanel(new BorderLayout());
                compPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY),
                    BorderFactory.createEmptyBorder(10,10,10,10)));
                compPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
                
                String statusColor = c.status.equals("Pending") ? "orange" : 
                                    c.status.equals("Resolved") ? "green" : "red";
                JLabel compLabel = new JLabel("<html><b>Student:</b> " + c.studentName + 
                    " | <b>Date:</b> " + c.date + 
                    " | <b>Status:</b> <span style='color:" + statusColor + ";'>" + c.status + "</span><br>" +
                    "<b>Complaint:</b> " + c.complaint + "</html>");
                compPanel.add(compLabel, BorderLayout.CENTER);
                
                if(c.status.equals("Pending")) {
                    JButton resolveBtn = new JButton("Mark Resolved");
                    resolveBtn.setBackground(new Color(60,179,113));
                    resolveBtn.setForeground(Color.WHITE);
                    final Complaint complaint = c;
                    resolveBtn.addActionListener(ev -> {
                        updateComplaintStatus(complaint.studentName, complaint.complaint, complaint.date, "Resolved");
                        handleComplaints(userType, username, rightPanel);
                    });
                    compPanel.add(resolveBtn, BorderLayout.EAST);
                }
                
                contentPanel.add(compPanel);
                contentPanel.add(Box.createRigidArea(new Dimension(0,10)));
            }
            
            if(!hasComplaints) {
                contentPanel.add(new JLabel("No complaints available"));
            }
        }
        
        JScrollPane scroll = new JScrollPane(contentPanel);
        rightPanel.add(scroll, BorderLayout.CENTER);
    }
    
    // ---------- Attendance Handler ----------
    static void handleAttendance(String userType, String username, JPanel rightPanel) {
        rightPanel.setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
        if(userType.equals("Admin")) {
            JLabel title = new JLabel("Mark Attendance", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 24));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(title);
            contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
            
            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10,10,10,10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            gbc.gridx=0; gbc.gridy=0;
            formPanel.add(new JLabel("Select Student:"), gbc);
            JComboBox<String> studentCombo = new JComboBox<>();
            ArrayList<String> students = getAllStudentUsernames();
            for(String student : students) {
                studentCombo.addItem(student);
            }
            gbc.gridx=1; formPanel.add(studentCombo, gbc);
            
            gbc.gridx=0; gbc.gridy=1;
            formPanel.add(new JLabel("Date:"), gbc);
            JPanel datePicker = createDatePicker(null);
            gbc.gridx=1; formPanel.add(datePicker, gbc);
            
            gbc.gridx=0; gbc.gridy=2;
            formPanel.add(new JLabel("Status:"), gbc);
            JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Present", "Absent"});
            gbc.gridx=1; formPanel.add(statusCombo, gbc);
            
            JButton submitBtn = createButton("Mark Attendance", new Color(60,179,113));
            gbc.gridx=1; gbc.gridy=3; formPanel.add(submitBtn, gbc);
            
            submitBtn.addActionListener(e -> {
                String student = (String) studentCombo.getSelectedItem();
                String date = getDateFromPicker(datePicker);
                String status = (String) statusCombo.getSelectedItem();
                
                addAttendance(student, date, status);
                JOptionPane.showMessageDialog(frame, "Attendance marked successfully!");
                handleAttendance(userType, username, rightPanel);
            });
            
            contentPanel.add(formPanel);
            
        } else { // Student
            JLabel title = new JLabel("Your Attendance", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 24));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(title);
            contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
            
            ArrayList<String> attendance = getAttendance(username);
            if(attendance != null && !attendance.isEmpty()) {
                int present = 0, absent = 0;
                for(String record : attendance) {
                    JPanel attPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    attPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                    attPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                    
                    JLabel attLabel = new JLabel(record);
                    attLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    if(record.contains("Present")) {
                        attLabel.setForeground(new Color(0, 128, 0));
                        present++;
                    } else {
                        attLabel.setForeground(Color.RED);
                        absent++;
                    }
                    attPanel.add(attLabel);
                    contentPanel.add(attPanel);
                    contentPanel.add(Box.createRigidArea(new Dimension(0,5)));
                }
                
                int total = present + absent;
                double percentage = total > 0 ? (present * 100.0 / total) : 0;
                JLabel summary = new JLabel(String.format("<html><b>Total:</b> %d | <b>Present:</b> %d | <b>Absent:</b> %d | <b>Percentage:</b> %.1f%%</html>", 
                    total, present, absent, percentage));
                summary.setFont(new Font("Segoe UI", Font.BOLD, 16));
                summary.setAlignmentX(Component.CENTER_ALIGNMENT);
                contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
                contentPanel.add(summary);
            } else {
                contentPanel.add(new JLabel("No attendance records found"));
            }
        }
        
        JScrollPane scroll = new JScrollPane(contentPanel);
        rightPanel.add(scroll, BorderLayout.CENTER);
    }
    
    // ---------- Visitor Handler ----------
    static void handleVisitor(String userType, String username, JPanel rightPanel) {
        rightPanel.setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
        if(userType.equals("Student")) {
            JLabel title = new JLabel("Visitor Registration", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 24));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(title);
            contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
            
            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10,10,10,10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            gbc.gridx=0; gbc.gridy=0; formPanel.add(new JLabel("Visitor Name:"), gbc);
            JTextField visitorNameField = new JTextField(15); gbc.gridx=1; formPanel.add(visitorNameField, gbc);
            
            gbc.gridx=0; gbc.gridy=1; formPanel.add(new JLabel("Relation:"), gbc);
            JTextField relationField = new JTextField(15); gbc.gridx=1; formPanel.add(relationField, gbc);
            
            gbc.gridx=0; gbc.gridy=2; formPanel.add(new JLabel("Date:"), gbc);
            JPanel datePicker = createDatePicker(null);
            gbc.gridx=1; formPanel.add(datePicker, gbc);
            
            gbc.gridx=0; gbc.gridy=3; formPanel.add(new JLabel("Time:"), gbc);
            JPanel timePicker = createTimePicker("10:00 AM");
            gbc.gridx=1; formPanel.add(timePicker, gbc);
            
            gbc.gridx=0; gbc.gridy=4; formPanel.add(new JLabel("Purpose:"), gbc);
            JTextField purposeField = new JTextField(15); gbc.gridx=1; formPanel.add(purposeField, gbc);
            
            JButton submitBtn = createButton("Register Visitor", new Color(60,179,113));
            gbc.gridx=1; gbc.gridy=5; formPanel.add(submitBtn, gbc);
            
            submitBtn.addActionListener(e -> {
                String visitorName = visitorNameField.getText().trim();
                String relation = relationField.getText().trim();
                String date = getDateFromPicker(datePicker);
                String time = getTimeFromPicker(timePicker);
                String purpose = purposeField.getText().trim();
                
                if(visitorName.isEmpty() || relation.isEmpty() || purpose.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Fill all fields!");
                    return;
                }
                
                addVisitor(username, visitorName, relation, date, time, purpose);
                JOptionPane.showMessageDialog(frame, "Visitor registered successfully!");
                handleVisitor(userType, username, rightPanel);
            });
            
            contentPanel.add(formPanel);
            contentPanel.add(Box.createRigidArea(new Dimension(0,30)));
            
            JLabel historyLabel = new JLabel("Visitor History:", SwingConstants.LEFT);
            historyLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            historyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(historyLabel);
            contentPanel.add(Box.createRigidArea(new Dimension(0,10)));
            
            ArrayList<Visitor> visitors = getVisitors(username);
            if(visitors != null && !visitors.isEmpty()) {
                for(Visitor v : visitors) {
                    JPanel visPanel = new JPanel(new BorderLayout());
                    visPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        BorderFactory.createEmptyBorder(10,10,10,10)));
                    visPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
                    
                    JLabel visLabel = new JLabel("<html><b>" + v.visitorName + "</b> (" + v.relation + ")<br>" +
                        "Date: " + v.date + " | Time: " + v.time + "<br>" +
                        "Purpose: " + v.purpose + "</html>");
                    visPanel.add(visLabel, BorderLayout.CENTER);
                    contentPanel.add(visPanel);
                    contentPanel.add(Box.createRigidArea(new Dimension(0,10)));
                }
            } else {
                contentPanel.add(new JLabel("No visitors registered yet"));
            }
            
        } else { // Admin or Parent
            JLabel title = new JLabel(userType.equals("Admin") ? "All Visitors" : "Child's Visitors", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 24));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(title);
            contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
            
            boolean hasVisitors = false;
            if(userType.equals("Admin")) {
                ArrayList<Visitor> allVisitors = getAllVisitors();
                hasVisitors = !allVisitors.isEmpty();
                for(Visitor v : allVisitors) {
                    addVisitorCard(contentPanel, v);
                }
            } else { // Parent
                String studentUsername = getParentStudentMapping(username);
                if(studentUsername != null) {
                    ArrayList<Visitor> visitors = getVisitors(studentUsername);
                    if(visitors != null && !visitors.isEmpty()) {
                        hasVisitors = true;
                        for(Visitor v : visitors) {
                            addVisitorCard(contentPanel, v);
                        }
                    }
                }
            }
            
            if(!hasVisitors) {
                contentPanel.add(new JLabel("No visitors found"));
            }
        }
        
        JScrollPane scroll = new JScrollPane(contentPanel);
        rightPanel.add(scroll, BorderLayout.CENTER);
    }
    
    static void addVisitorCard(JPanel panel, Visitor v) {
        JPanel visPanel = new JPanel(new BorderLayout());
        visPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(10,10,10,10)));
        visPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        JLabel visLabel = new JLabel("<html><b>Student:</b> " + v.studentName + 
            " | <b>Visitor:</b> " + v.visitorName + " (" + v.relation + ")<br>" +
            "Date: " + v.date + " | Time: " + v.time + "<br>" +
            "Purpose: " + v.purpose + "</html>");
        visPanel.add(visLabel, BorderLayout.CENTER);
        panel.add(visPanel);
        panel.add(Box.createRigidArea(new Dimension(0,10)));
    }
    
    // ---------- Student Details Handler ----------
    static void handleStudentDetails(JPanel rightPanel) {
        rightPanel.setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
        JLabel title = new JLabel("All Students", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(title);
        contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
        
        ArrayList<StudentProfile> allProfiles = getAllStudentProfiles();
        ArrayList<String> allUsernames = getAllStudentUsernames();
        if(allProfiles.isEmpty()) {
            contentPanel.add(new JLabel("No students registered yet"));
        } else {
            for(int i = 0; i < allProfiles.size() && i < allUsernames.size(); i++) {
                String username = allUsernames.get(i);
                StudentProfile prof = allProfiles.get(i);
                JPanel studentPanel = new JPanel(new BorderLayout());
                studentPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(60,179,113), 2),
                    BorderFactory.createEmptyBorder(15,15,15,15)));
                studentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
                
                JPanel leftPart = new JPanel();
                leftPart.setLayout(new BoxLayout(leftPart, BoxLayout.Y_AXIS));
                leftPart.add(new JLabel("<html><b>Username:</b> " + username + "</html>"));
                leftPart.add(new JLabel("<html><b>Name:</b> " + prof.studentName + "</html>"));
                leftPart.add(new JLabel("<html><b>Department:</b> " + prof.department + "</html>"));
                leftPart.add(new JLabel("<html><b>Phone:</b> " + prof.studentPhone + "</html>"));
                leftPart.add(new JLabel("<html><b>Parent:</b> " + prof.parentName + " (" + prof.parentPhone + ")</html>"));
                
                studentPanel.add(leftPart, BorderLayout.CENTER);
                studentPanel.add(new JLabel(prof.photo), BorderLayout.EAST);
                
                contentPanel.add(studentPanel);
                contentPanel.add(Box.createRigidArea(new Dimension(0,15)));
            }
        }
        
        JScrollPane scroll = new JScrollPane(contentPanel);
        rightPanel.add(scroll, BorderLayout.CENTER);
    }

    // ---------- Night Out Handler ----------
    static void handleNightOut(String userType, String username, JPanel rightPanel){
        rightPanel.setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
        if(userType.equals("Student")){
            JLabel title = new JLabel("Night Out Request", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 24));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(title);
            contentPanel.add(Box.createRigidArea(new Dimension(0,20)));

            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbcNO = new GridBagConstraints();
            gbcNO.insets = new Insets(10,10,10,10);
            gbcNO.fill = GridBagConstraints.HORIZONTAL;
            
            gbcNO.gridx=0; gbcNO.gridy=0; formPanel.add(new JLabel("From Date:"), gbcNO);
            JPanel fromDatePicker = createDatePicker(null);
            gbcNO.gridx=1; formPanel.add(fromDatePicker, gbcNO);
            
            gbcNO.gridx=0; gbcNO.gridy=1; formPanel.add(new JLabel("To Date:"), gbcNO);
            JPanel toDatePicker = createDatePicker(null);
            gbcNO.gridx=1; formPanel.add(toDatePicker, gbcNO);
            
            gbcNO.gridx=0; gbcNO.gridy=2; formPanel.add(new JLabel("Reason:"), gbcNO);
            JTextArea reasonField = new JTextArea(3, 20);
            reasonField.setLineWrap(true);
            JScrollPane reasonScroll = new JScrollPane(reasonField);
            gbcNO.gridx=1; formPanel.add(reasonScroll, gbcNO);

            JButton submitBtn = createButton("Submit Request", new Color(60,179,113));
            gbcNO.gridx=1; gbcNO.gridy=3; formPanel.add(submitBtn, gbcNO);

            submitBtn.addActionListener(ev -> {
                String from = getDateFromPicker(fromDatePicker);
                String to = getDateFromPicker(toDatePicker);
                String reason = reasonField.getText().trim();
                if(reason.isEmpty()) { 
                    JOptionPane.showMessageDialog(frame,"Fill all fields!"); 
                    return; 
                }
                addNightOutRequest(username, from, to, reason);
                JOptionPane.showMessageDialog(frame,"Request Submitted!");
                handleNightOut(userType, username, rightPanel);
            });

            contentPanel.add(formPanel);
            contentPanel.add(Box.createRigidArea(new Dimension(0,30)));

            JLabel historyLabel = new JLabel("Your Requests:", SwingConstants.LEFT);
            historyLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            historyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(historyLabel);
            contentPanel.add(Box.createRigidArea(new Dimension(0,10)));

            ArrayList<NightOutRequest> requests = getNightOutRequests(username);
            if(requests != null && !requests.isEmpty()){
                for(NightOutRequest req: requests){
                    JPanel reqPanel = new JPanel(new BorderLayout());
                    reqPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        BorderFactory.createEmptyBorder(10,10,10,10)));
                    reqPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
                    
                    String statusColor = req.status.equals("Pending") ? "orange" : 
                                        req.status.equals("Accepted") ? "green" : "red";
                    JLabel lbl = new JLabel("<html><b>From:</b> " + req.fromDate + 
                        " | <b>To:</b> " + req.toDate + 
                        " | <b>Status:</b> <span style='color:" + statusColor + ";'>" + req.status + "</span><br>" +
                        "<b>Reason:</b> " + req.reason + "</html>");
                    reqPanel.add(lbl, BorderLayout.CENTER);
                    contentPanel.add(reqPanel);
                    contentPanel.add(Box.createRigidArea(new Dimension(0,10)));
                }
            } else {
                contentPanel.add(new JLabel("No requests submitted yet"));
            }

            JScrollPane scroll = new JScrollPane(contentPanel);
            rightPanel.add(scroll, BorderLayout.CENTER);

        } else if(userType.equals("Admin")) { // Admin view
            JLabel title = new JLabel("Night Out Requests", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 24));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(title);
            contentPanel.add(Box.createRigidArea(new Dimension(0,20)));
            
            ArrayList<NightOutRequest> allRequests = getAllNightOutRequests();
            boolean hasRequests = !allRequests.isEmpty();
            for(NightOutRequest req: allRequests) {
                JPanel reqPanel = new JPanel(new BorderLayout());
                reqPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY),
                    BorderFactory.createEmptyBorder(10,10,10,10)));
                reqPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
                
                String statusColor = req.status.equals("Pending") ? "orange" : 
                                    req.status.equals("Accepted") ? "green" : "red";
                JLabel lbl = new JLabel("<html><b>Student:</b> " + req.studentName + 
                    " | <b>From:</b> " + req.fromDate + 
                    " | <b>To:</b> " + req.toDate + 
                    " | <b>Status:</b> <span style='color:" + statusColor + ";'>" + req.status + "</span><br>" +
                    "<b>Reason:</b> " + req.reason + "</html>");
                reqPanel.add(lbl, BorderLayout.CENTER);
                
                if(req.status.equals("Pending")) {
                    JPanel btnPanel = new JPanel(new FlowLayout());
                    JButton accept = new JButton("Accept");
                    JButton reject = new JButton("Reject");
                    accept.setBackground(new Color(60,179,113));
                    accept.setForeground(Color.WHITE);
                    reject.setBackground(new Color(220,20,60));
                    reject.setForeground(Color.WHITE);
                    final NightOutRequest request = req;
                    accept.addActionListener(ev -> { 
                        updateNightOutStatus(request.studentName, request.fromDate, request.toDate, "Accepted");
                        handleNightOut(userType, username, rightPanel); 
                    });
                    reject.addActionListener(ev -> { 
                        updateNightOutStatus(request.studentName, request.fromDate, request.toDate, "Rejected");
                        handleNightOut(userType, username, rightPanel); 
                    });
                    btnPanel.add(accept);
                    btnPanel.add(reject);
                    reqPanel.add(btnPanel, BorderLayout.EAST);
                }
                contentPanel.add(reqPanel);
                contentPanel.add(Box.createRigidArea(new Dimension(0,10)));
            }
            
            if(!hasRequests) {
                contentPanel.add(new JLabel("No night out requests available"));
            }
            
            JScrollPane scroll = new JScrollPane(contentPanel);
            rightPanel.add(scroll, BorderLayout.CENTER);
        } else { // Parent view
            rightPanel.setLayout(new BorderLayout());
            JPanel parentContentPanel = new JPanel();
            parentContentPanel.setLayout(new BoxLayout(parentContentPanel, BoxLayout.Y_AXIS));
            parentContentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
            
            JLabel title = new JLabel("Child's Night Out Requests", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 24));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            parentContentPanel.add(title);
            parentContentPanel.add(Box.createRigidArea(new Dimension(0,20)));
            
            String studentUsername = getParentStudentMapping(username);
            if(studentUsername != null) {
                ArrayList<NightOutRequest> requests = getNightOutRequests(studentUsername);
                if(requests != null && !requests.isEmpty()) {
                    for(NightOutRequest req: requests) {
                        JPanel reqPanel = new JPanel(new BorderLayout());
                        reqPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GRAY),
                            BorderFactory.createEmptyBorder(10,10,10,10)));
                        reqPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
                        
                        String statusColor = req.status.equals("Pending") ? "orange" : 
                                            req.status.equals("Accepted") ? "green" : "red";
                        JLabel lbl = new JLabel("<html><b>From:</b> " + req.fromDate + 
                            " | <b>To:</b> " + req.toDate + 
                            " | <b>Status:</b> <span style='color:" + statusColor + ";'>" + req.status + "</span><br>" +
                            "<b>Reason:</b> " + req.reason + "</html>");
                        reqPanel.add(lbl, BorderLayout.CENTER);
                        parentContentPanel.add(reqPanel);
                        parentContentPanel.add(Box.createRigidArea(new Dimension(0,10)));
                    }
                } else {
                    parentContentPanel.add(new JLabel("No night out requests found"));
                }
            } else {
                parentContentPanel.add(new JLabel("Student mapping not found"));
            }
            
            JScrollPane scroll = new JScrollPane(parentContentPanel);
            rightPanel.add(scroll, BorderLayout.CENTER);
        }
        rightPanel.revalidate();
        rightPanel.repaint();
    }
}