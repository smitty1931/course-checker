import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import javax.swing.*;
import java.awt.GraphicsEnvironment;

public class CourseChecker {
    private static JTextArea resultArea;

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            // Headless mode
            Scanner scanner = new Scanner(System.in);

            // Prompt for semester
            System.out.print("Enter the semester: ");
            String sem = scanner.nextLine();

            // Prompt for course list
            System.out.print("Enter the comma-separated list of courses: ");
            String coursesInput = scanner.nextLine();
            List<String> courses = Arrays.asList(coursesInput.split(","));

            processCourses(sem, courses, null);
        } else {
            // Graphical mode
            // Create and set up the window
            JFrame frame = new JFrame("Course Checker");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 400);

            // Create and set up the panel
            JPanel panel = new JPanel();
            frame.add(panel);
            placeComponents(panel);

            // Display the window
            frame.setVisible(true);
        }
    }

    private static void placeComponents(JPanel panel) {
        panel.setLayout(null);

        // Create JLabel for semester
        JLabel semesterLabel = new JLabel("Semester:");
        semesterLabel.setBounds(10, 20, 80, 25);
        panel.add(semesterLabel);

        // Create text field for semester
        JTextField semesterText = new JTextField(20);
        semesterText.setBounds(100, 20, 165, 25);
        panel.add(semesterText);

        // Create JLabel for courses
        JLabel coursesLabel = new JLabel("Courses:");
        coursesLabel.setBounds(10, 50, 80, 25);
        panel.add(coursesLabel);

        // Create text field for courses
        JTextField coursesText = new JTextField(20);
        coursesText.setBounds(100, 50, 165, 25);
        panel.add(coursesText);

        // Create submit button
        JButton submitButton = new JButton("Submit");
        submitButton.setBounds(10, 80, 80, 25);
        panel.add(submitButton);

        // Create JTextArea for results
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBounds(10, 110, 360, 240);
        panel.add(scrollPane);

        // Add action listener to the submit button
        submitButton.addActionListener(e -> {
            String sem = semesterText.getText();
            String coursesInput = coursesText.getText();
            List<String> courses = Arrays.asList(coursesInput.split(","));

            processCourses(sem, courses, panel);
        });
    }

    private static void processCourses(String sem, List<String> courses, JPanel panel) {
        if (courses.isEmpty()) {
            if (panel != null) {
                JOptionPane.showMessageDialog(panel, "No courses found in input", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                System.out.println("No courses found in input");
            }
            return;
        }

        File searchDir = new File("search");
        if (!searchDir.exists()) {
            searchDir.mkdir();
        }

        Set<String> codes = new HashSet<>();
        for (String course : courses) {
            if (course.length() >= 4) {
                String code = course.substring(0, 4);
                codes.add(code);
            }
        }

        for (String code : codes) {
            // Remove trailing '1' if it exists
            String sanitizedCode = code.replaceAll("1$", "");
            String command = "https://api.easi.utoronto.ca/ttb/getOptimizedMatchingCourseTitles?term=" + sanitizedCode + "&divisions=ARTSC&sessions=" + sem + "&lowerThreshold=50&upperThreshold=200";
            try {
                String response = fetchUrl(command);
                Files.write(Paths.get("search", sanitizedCode + ".xml"), response.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        StringBuilder resultBuilder = new StringBuilder();
        for (String course : courses) {
            if (course.length() >= 4) {
                String code = course.substring(0, 4);
                String sanitizedCode = code.replaceAll("1$", "");
                String title = course.substring(4);
                try {
                    String content = new String(Files.readAllBytes(Paths.get("search", sanitizedCode + ".xml")));
                    if (content.contains(title)) {
                        resultBuilder.append("Found ").append(course).append("\n");
                    } else {
                        resultBuilder.append("Not found: ").append(course).append("\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                resultBuilder.append("Invalid course format: ").append(course).append("\n");
            }
        }

        if (panel != null) {
            resultArea.setText(resultBuilder.toString());
        } else {
            System.out.println(resultBuilder.toString());
        }

        try {
            deleteDirectory(searchDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String fetchUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return in.lines().collect(Collectors.joining("\n"));
        }
    }

    private static void deleteDirectory(File directory) throws IOException {
        Files.walk(directory.toPath())
             .sorted(Comparator.reverseOrder())
             .map(Path::toFile)
             .forEach(File::delete);
    }
}