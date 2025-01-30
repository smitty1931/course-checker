import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import javax.swing.*;
import java.awt.GraphicsEnvironment;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
            frame.setSize(400, 800);

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

        // Create reset button
        JButton resetButton = new JButton("Reset");
        resetButton.setBounds(100, 80, 80, 25);
        panel.add(resetButton);

        // Create JTextArea for results
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBounds(10, 110, 360, 640);
        panel.add(scrollPane);

        // Add action listener to the submit button
        submitButton.addActionListener(e -> {
            String sem = semesterText.getText();
            String coursesInput = coursesText.getText();
            List<String> courses = Arrays.asList(coursesInput.split(","));

            processCourses(sem, courses, panel);
        });

        // Add action listener to the reset button
        resetButton.addActionListener(e -> {
            semesterText.setText("");
            coursesText.setText("");
            resultArea.setText("");
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
                String response = fetchUrl(command, "GET", null);
                Files.write(Paths.get("search", sanitizedCode + ".xml"), response.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        StringBuilder resultBuilder = new StringBuilder();
        List<String> foundCourses = new ArrayList<>();
        for (String course : courses) {
            if (course.length() >= 4) {
                String code = course.substring(0, 4);
                String sanitizedCode = code.replaceAll("1$", "");
                String title = course.substring(4);
                try {
                    String content = new String(Files.readAllBytes(Paths.get("search", sanitizedCode + ".xml")));
                    if (content.contains(title)) {
                        resultBuilder.append("Found ").append(course).append("\n");
                        String newCommand = "https://api.easi.utoronto.ca/ttb/getPageableCourses";
                        String postData = String.format(
                            "{\"courseCodeAndTitleProps\":{\"courseCode\":\"%s\",\"courseTitle\":\"%s\",\"courseSectionCode\":\"\",\"searchCourseDescription\":true},\"departmentProps\":[],\"campuses\":[],\"sessions\":[\"%s\"],\"requirementProps\":[],\"instructor\":\"\",\"courseLevels\":[],\"deliveryModes\":[],\"dayPreferences\":[],\"timePreferences\":[],\"divisions\":[\"ARTSC\"],\"creditWeights\":[],\"availableSpace\":false,\"waitListable\":false,\"page\":1,\"pageSize\":20,\"direction\":\"asc\"}",
                            sanitizedCode, course, sem
                        );
                        try {
                            String newResponse = fetchUrl(newCommand, "POST", postData);
                            // Clean up the response
                            newResponse = newResponse.trim().replaceFirst("^([\\W]+)<","<");
                            Files.write(Paths.get("search", sanitizedCode + "_details.xml"), newResponse.getBytes());
                            String sections = extractCourseContent("search/" + sanitizedCode + "_details.xml", "pageable.xml");
                            resultBuilder.append(sections);
                            resultBuilder.append("-------------------------------------\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        foundCourses.add(course);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (panel != null) {
                    JOptionPane.showMessageDialog(panel, "Invalid course format: " + course, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    System.out.println("Invalid course format: " + course);
                }
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

    private static void deleteDirectory(File directory) throws IOException {
        Files.walk(directory.toPath())
             .sorted(Comparator.reverseOrder())
             .map(Path::toFile)
             .forEach(File::delete);
    }

    private static String fetchUrl(String urlString, String method, String postData) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoOutput(true);

        if ("POST".equalsIgnoreCase(method) && postData != null) {
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "*/*");
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = postData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            return in.lines().collect(Collectors.joining("\n"));
        }
    }

    private static String extractCourseContent(String inputFilePath, String outputFilePath) {
        String sections = "";
        try {
            File inputFile = new File(inputFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
    
            NodeList nList = doc.getElementsByTagName("payload");
            if (nList.getLength() > 0) {
                Node payloadNode = nList.item(0);
                if (payloadNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element payloadElement = (Element) payloadNode;
    
                    // Convert the payload element to a string with XML formatting
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    DOMSource source = new DOMSource(payloadElement);
                    StringWriter writer = new StringWriter();
                    StreamResult result = new StreamResult(writer);
                    transformer.transform(source, result);
    
                    // Write the content to the output file
                    try (FileWriter fileWriter = new FileWriter(outputFilePath)) {
                        fileWriter.write(writer.toString());
                    }

                    sections=extractSectionsDetails(outputFilePath);
                }
            } else {
                System.out.println("No <payload> element found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sections;
    }

    private static String extractSectionsDetails(String inputFilePath) {
        StringBuilder details = new StringBuilder();
        try {
            File inputFile = new File(inputFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList sectionsList = doc.getElementsByTagName("sections");
            if (sectionsList.getLength() > 0) {
                Node sectionsNode = sectionsList.item(0);
                if (sectionsNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element sectionsElement = (Element) sectionsNode;

                    NodeList sectionNodes = sectionsElement.getElementsByTagName("sections");
                    for (int i = 0; i < sectionNodes.getLength(); i++) {
                        Node sectionNode = sectionNodes.item(i);
                        if (sectionNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element sectionElement = (Element) sectionNode;

                            String name = sectionElement.getElementsByTagName("name").item(0).getTextContent();
                            String currentEnrolment = sectionElement.getElementsByTagName("currentEnrolment").item(0).getTextContent();
                            String maxEnrolment = sectionElement.getElementsByTagName("maxEnrolment").item(0).getTextContent();

                            details.append("Section Name: ").append(name).append("\n");
                            details.append("Current Enrolment: ").append(currentEnrolment).append("\n");
                            details.append("Max Enrolment: ").append(maxEnrolment).append("\n");
                            details.append("\n");
                        }
                    }
                }
            } else {
                details.append("No <sections> element found.\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return details.toString();
    }
}