import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

public class WeatherApp extends JFrame {
    private JComboBox<String> locationComboBox;
    private JButton checkWeatherButton;
    private JButton saveFavoriteButton;
    private JButton saveToCSVButton;
    private JButton loadDataButton;
    private JLabel weatherIconLabel;
    private JLabel temperatureLabel;
    private JLabel humidityLabel;
    private JLabel descriptionLabel;
    private JTable weatherTable;
    private DefaultTableModel tableModel;
    
    private static final String API_KEY = "YOUR_API_KEY_HERE";
    private static final String API_URL = "http://api.openweathermap.org/data/2.5/weather";
    private static final String FAVORITES_FILE = "favorites.txt";
    private static final String WEATHER_DATA_FILE = "weather_data.csv";
    
    // Daftar lokasi default
    private final String[] defaultLocations = {
        "Jakarta", "Surabaya", "Bandung", "Medan", "Semarang",
        "Makassar", "Yogyakarta", "Denpasar", "Malang", "Palembang"
    };
    
    public WeatherApp() {
        initializeGUI();
        setupEvents();
        loadFavorites();
    }
    
    private void initializeGUI() {
        setTitle("Aplikasi Cek Cuaca Sederhana");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Panel atas untuk input
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Lokasi:"));
        
        locationComboBox = new JComboBox<String>();
        locationComboBox.setEditable(true);
        locationComboBox.setPreferredSize(new Dimension(150, 25));
        
        // Tambahkan lokasi default
        for (String location : defaultLocations) {
            locationComboBox.addItem(location);
        }
        
        topPanel.add(locationComboBox);
        
        checkWeatherButton = new JButton("Cek Cuaca");
        topPanel.add(checkWeatherButton);
        
        saveFavoriteButton = new JButton("Simpan Favorit");
        topPanel.add(saveFavoriteButton);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Panel tengah untuk menampilkan cuaca
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Informasi Cuaca"));
        
        JPanel weatherInfoPanel = new JPanel(new FlowLayout());
        weatherIconLabel = new JLabel();
        weatherIconLabel.setPreferredSize(new Dimension(100, 100));
        weatherInfoPanel.add(weatherIconLabel);
        
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        temperatureLabel = new JLabel("Suhu: -");
        humidityLabel = new JLabel("Kelembaban: -");
        descriptionLabel = new JLabel("Deskripsi: -");
        
        infoPanel.add(temperatureLabel);
        infoPanel.add(humidityLabel);
        infoPanel.add(descriptionLabel);
        
        weatherInfoPanel.add(infoPanel);
        centerPanel.add(weatherInfoPanel, BorderLayout.NORTH);
        
        // Tabel untuk riwayat cuaca
        String[] columnNames = {"Lokasi", "Suhu", "Kelembaban", "Deskripsi", "Waktu"};
        tableModel = new DefaultTableModel(columnNames, 0);
        weatherTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(weatherTable);
        tableScrollPane.setPreferredSize(new Dimension(500, 150));
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Panel bawah untuk tombol tambahan
        JPanel bottomPanel = new JPanel(new FlowLayout());
        saveToCSVButton = new JButton("Simpan ke CSV");
        loadDataButton = new JButton("Muat Data");
        
        bottomPanel.add(saveToCSVButton);
        bottomPanel.add(loadDataButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        setSize(600, 400);
    }
    
    private void setupEvents() {
        checkWeatherButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkWeather();
            }
        });
        
        locationComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedLocation = (String) e.getItem();
                    if (selectedLocation != null && !selectedLocation.isEmpty()) {
                        checkWeatherForLocation(selectedLocation);
                    }
                }
            }
        });
        
        saveFavoriteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveFavorite();
            }
        });
        
        saveToCSVButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveToCSV();
            }
        });
        
        loadDataButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadWeatherData();
            }
        });
        
        // Enter key pada comboBox
        locationComboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    checkWeather();
                }
            }
        });
        
        // Auto-check weather untuk lokasi pertama saat startup
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (locationComboBox.getItemCount() > 0) {
                    locationComboBox.setSelectedIndex(0);
                    checkWeatherForLocation((String) locationComboBox.getSelectedItem());
                }
            }
        });
    }
    
    private void checkWeather() {
        String location = (String) locationComboBox.getSelectedItem();
        if (location != null && !location.trim().isEmpty()) {
            checkWeatherForLocation(location.trim());
        } else {
            JOptionPane.showMessageDialog(this, "Masukkan nama lokasi terlebih dahulu!");
        }
    }
    
    private void checkWeatherForLocation(final String location) {
        // Show loading state
        checkWeatherButton.setText("Loading...");
        checkWeatherButton.setEnabled(false);
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    final Map<String, Object> weatherData = getWeatherData(location);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (weatherData != null) {
                                displayWeather(weatherData, location);
                                addToTable(weatherData, location);
                            }
                            checkWeatherButton.setText("Cek Cuaca");
                            checkWeatherButton.setEnabled(true);
                        }
                    });
                } catch (final Exception ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            JOptionPane.showMessageDialog(WeatherApp.this, 
                                "Error mengambil data cuaca: " + ex.getMessage());
                            checkWeatherButton.setText("Cek Cuaca");
                            checkWeatherButton.setEnabled(true);
                        }
                    });
                }
            }
        }).start();
    }
    
    private Map<String, Object> getWeatherData(String location) {
        try {
            String urlString = API_URL + "?q=" + location + "&appid=" + API_KEY + "&units=metric&lang=id";
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                return parseJsonManually(response.toString());
            } else if (responseCode == 404) {
                JOptionPane.showMessageDialog(this, "Lokasi '" + location + "' tidak ditemukan!");
            } else {
                JOptionPane.showMessageDialog(this, "Error API: " + responseCode);
            }
            return null;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error koneksi: " + e.getMessage());
            return null;
        }
    }
    
    // Manual JSON parsing method
    private Map<String, Object> parseJsonManually(String json) {
        Map<String, Object> result = new HashMap<String, Object>();
        json = json.trim();
        
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }
        
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                String value = keyValue[1].trim();
                
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    // String value
                    result.put(key, value.substring(1, value.length() - 1));
                } else if (value.startsWith("{")) {
                    // Nested object
                    result.put(key, parseJsonManually(value));
                } else if (value.startsWith("[")) {
                    // Array
                    result.put(key, parseJsonArray(value));
                } else if (value.equals("true") || value.equals("false")) {
                    // Boolean
                    result.put(key, Boolean.parseBoolean(value));
                } else if (value.contains(".")) {
                    // Double
                    try {
                        result.put(key, Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        result.put(key, value);
                    }
                } else {
                    // Integer or string
                    try {
                        result.put(key, Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        result.put(key, value);
                    }
                }
            }
        }
        return result;
    }
    
    // Method untuk parse JSON array
    private List<Object> parseJsonArray(String jsonArray) {
        List<Object> result = new ArrayList<Object>();
        jsonArray = jsonArray.trim();
        
        if (jsonArray.startsWith("[") && jsonArray.endsWith("]")) {
            jsonArray = jsonArray.substring(1, jsonArray.length() - 1);
        }
        
        if (jsonArray.isEmpty()) {
            return result;
        }
        
        String[] items = jsonArray.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String item : items) {
            String trimmedItem = item.trim();
            if (trimmedItem.startsWith("{")) {
                result.add(parseJsonManually(trimmedItem));
            } else if (trimmedItem.startsWith("\"") && trimmedItem.endsWith("\"")) {
                result.add(trimmedItem.substring(1, trimmedItem.length() - 1));
            } else if (trimmedItem.equals("true") || trimmedItem.equals("false")) {
                result.add(Boolean.parseBoolean(trimmedItem));
            } else if (trimmedItem.contains(".")) {
                try {
                    result.add(Double.parseDouble(trimmedItem));
                } catch (NumberFormatException e) {
                    result.add(trimmedItem);
                }
            } else {
                try {
                    result.add(Integer.parseInt(trimmedItem));
                } catch (NumberFormatException e) {
                    result.add(trimmedItem);
                }
            }
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    private void displayWeather(Map<String, Object> weatherData, String location) {
        try {
            Map<String, Object> main = (Map<String, Object>) weatherData.get("main");
            List<Object> weatherList = (List<Object>) weatherData.get("weather");
            
            if (weatherList != null && !weatherList.isEmpty()) {
                Map<String, Object> weather = (Map<String, Object>) weatherList.get(0);
                
                double temperature = (Double) main.get("temp");
                int humidity = (Integer) main.get("humidity");
                String description = (String) weather.get("description");
                String iconCode = (String) weather.get("icon");
                
                temperatureLabel.setText(String.format("Suhu: %.1f°C", temperature));
                humidityLabel.setText(String.format("Kelembaban: %d%%", humidity));
                descriptionLabel.setText(String.format("Deskripsi: %s", description));
                
                setWeatherIcon(iconCode);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error parsing data cuaca: " + e.getMessage());
        }
    }
    
    private void setWeatherIcon(String iconCode) {
        try {
            String iconPath = getIconPath(iconCode);
            File iconFile = new File(iconPath);
            if (iconFile.exists()) {
                ImageIcon icon = new ImageIcon(iconPath);
                Image scaledImage = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                weatherIconLabel.setIcon(new ImageIcon(scaledImage));
            } else {
                // Fallback: gunakan emoji atau teks
                weatherIconLabel.setIcon(null);
                weatherIconLabel.setText("☀️");
            }
        } catch (Exception e) {
            weatherIconLabel.setIcon(null);
            weatherIconLabel.setText("?");
        }
    }
    
    private String getIconPath(String iconCode) {
        // Mapping sederhana untuk ikon cuaca
        switch (iconCode) {
            case "01d": case "01n": return "icons/clear.png";
            case "02d": case "03d": case "04d": 
            case "02n": case "03n": case "04n": return "icons/cloudy.png";
            case "09d": case "10d": case "09n": case "10n": return "icons/rain.png";
            case "11d": case "11n": return "icons/thunderstorm.png";
            case "13d": case "13n": return "icons/snow.png";
            case "50d": case "50n": return "icons/mist.png";
            default: return "icons/default.png";
        }
    }
    
    @SuppressWarnings("unchecked")
    private void addToTable(Map<String, Object> weatherData, String location) {
        try {
            Map<String, Object> main = (Map<String, Object>) weatherData.get("main");
            List<Object> weatherList = (List<Object>) weatherData.get("weather");
            
            if (weatherList != null && !weatherList.isEmpty()) {
                Map<String, Object> weather = (Map<String, Object>) weatherList.get(0);
                
                double temperature = (Double) main.get("temp");
                int humidity = (Integer) main.get("humidity");
                String description = (String) weather.get("description");
                String timestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
                
                // Cek apakah lokasi sudah ada, jika ya update
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (location.equals(tableModel.getValueAt(i, 0))) {
                        tableModel.setValueAt(String.format("%.1f°C", temperature), i, 1);
                        tableModel.setValueAt(humidity + "%", i, 2);
                        tableModel.setValueAt(description, i, 3);
                        tableModel.setValueAt(timestamp, i, 4);
                        return;
                    }
                }
                
                // Jika tidak ada, tambahkan baris baru
                tableModel.addRow(new Object[]{
                    location, 
                    String.format("%.1f°C", temperature), 
                    humidity + "%", 
                    description, 
                    timestamp
                });
            }
        } catch (Exception e) {
            System.out.println("Error adding to table: " + e.getMessage());
        }
    }
    
    private void saveFavorite() {
        String location = (String) locationComboBox.getSelectedItem();
        if (location != null && !location.trim().isEmpty()) {
            location = location.trim();
            
            boolean exists = false;
            for (int i = 0; i < locationComboBox.getItemCount(); i++) {
                if (location.equals(locationComboBox.getItemAt(i))) {
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                locationComboBox.addItem(location);
                saveFavoritesToFile();
                JOptionPane.showMessageDialog(this, "Lokasi '" + location + "' disimpan ke favorit!");
            } else {
                JOptionPane.showMessageDialog(this, "Lokasi '" + location + "' sudah ada dalam favorit!");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Masukkan nama lokasi terlebih dahulu!");
        }
    }
    
    private void loadFavorites() {
        try {
            File file = new File(FAVORITES_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        // Cek apakah sudah ada sebelum menambahkan
                        boolean exists = false;
                        String location = line.trim();
                        for (int i = 0; i < locationComboBox.getItemCount(); i++) {
                            if (location.equals(locationComboBox.getItemAt(i))) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            locationComboBox.addItem(location);
                        }
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            System.out.println("Error loading favorites: " + e.getMessage());
        }
    }
    
    private void saveFavoritesToFile() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(FAVORITES_FILE));
            for (int i = 0; i < locationComboBox.getItemCount(); i++) {
                String item = locationComboBox.getItemAt(i);
                if (item != null && !item.toString().isEmpty()) {
                    writer.println(item.toString());
                }
            }
            writer.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error menyimpan favorit: " + e.getMessage());
        }
    }
    
    private void saveToCSV() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(WEATHER_DATA_FILE));
            
            writer.println("Lokasi,Suhu,Kelembaban,Deskripsi,Waktu");
            
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String location = tableModel.getValueAt(i, 0).toString().replace("\"", "\"\"");
                String temperature = tableModel.getValueAt(i, 1).toString().replace("\"", "\"\"");
                String humidity = tableModel.getValueAt(i, 2).toString().replace("\"", "\"\"");
                String description = tableModel.getValueAt(i, 3).toString().replace("\"", "\"\"");
                String time = tableModel.getValueAt(i, 4).toString().replace("\"", "\"\"");
                
                writer.println(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"", 
                    location, temperature, humidity, description, time));
            }
            
            writer.close();
            JOptionPane.showMessageDialog(this, "Data berhasil disimpan ke " + WEATHER_DATA_FILE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error menyimpan ke CSV: " + e.getMessage());
        }
    }
    
    private void loadWeatherData() {
        try {
            File file = new File(WEATHER_DATA_FILE);
            if (file.exists()) {
                tableModel.setRowCount(0);
                
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                boolean isFirstLine = true;
                int loadedCount = 0;
                
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }
                    
                    if (!line.trim().isEmpty()) {
                        String[] data = parseCSVLine(line);
                        if (data.length >= 5) {
                            for (int i = 0; i < data.length; i++) {
                                data[i] = data[i].replace("\"", "").trim();
                            }
                            tableModel.addRow(data);
                            loadedCount++;
                        }
                    }
                }
                reader.close();
                
                JOptionPane.showMessageDialog(this, loadedCount + " data berhasil dimuat dari " + WEATHER_DATA_FILE);
            } else {
                JOptionPane.showMessageDialog(this, "File data tidak ditemukan!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error memuat data: " + e.getMessage());
        }
    }
    
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<String>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        result.add(field.toString());
        
        return result.toArray(new String[0]);
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new WeatherApp().setVisible(true);
            }
        });
    }
}