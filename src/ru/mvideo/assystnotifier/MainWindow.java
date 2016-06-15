package ru.mvideo.assystnotifier;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.sun.jna.platform.win32.WinReg;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static java.lang.Math.round;

public class MainWindow extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private Stage loginForm = new Stage();
    private TextField userTextField;
    private PasswordField pwBox;
    private CheckBox cbox;
    private java.awt.TrayIcon trayIcon;

    private String serverName;
    private String dbName;
    private int portNumber;
    private String userLogin;
    private String userPassword;
    private String userFIO;

    private Connection con = null;


    @Override
    public void start(Stage primaryStage) {
        GridPane grid;

        loginForm.setTitle("MVIDEO ASSYST NOTIFIER");
        loginForm.getIcons().add(new Image("file:res/icon16.png"));

        loginForm.setResizable(false);
        Platform.setImplicitExit(false);

        grid = new GridPane();

        grid.setAlignment(Pos.TOP_CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Text scenetitle = new Text("  Welcome");
        scenetitle.setId("welcome-text");
        grid.add(scenetitle, 0, 0, 2, 1);

        Label userName = new Label("Логин:");
        grid.add(userName, 0, 1);

        userTextField = new TextField();
        grid.add(userTextField, 1, 1);

        Label pw = new Label("Пароль:");
        grid.add(pw, 0, 2);

        pwBox = new PasswordField();
        grid.add(pwBox, 1, 2);

        cbox = new CheckBox();
        cbox.setText("Запомнить");
        grid.add(cbox, 1, 3);

        pwBox.setOnAction(e -> tryConnectToDB());

        Button btn = new Button("Войти");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 4);

        btn.setOnAction(e -> tryConnectToDB());

        Scene scene = new Scene(grid, 300, 290);
        Rectangle2D rect = Screen.getPrimary().getVisualBounds();

        loginForm.setX(rect.getMaxX() - 315);
        loginForm.setY(rect.getMaxY() - 325);
        loginForm.setScene(scene);

        scene.getStylesheets().add(MainWindow.class.getResource("res/Login.css").toExternalForm());

        if (Objects.equals(RegistryHelper.getStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "SaveCred"), "Y")) {
            userLogin = RegistryHelper.getStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "Login");
            userPassword = decrypt(RegistryHelper.getStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "Password").getBytes(), userLogin);
            serverName = RegistryHelper.getStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "ServerName");
            dbName = RegistryHelper.getStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "DBName");
            portNumber = Integer.parseInt(RegistryHelper.getStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "PortNumber"));
            userTextField.setText(userLogin);
            pwBox.setText(userPassword);
            cbox.setSelected(true);
        } else {
            serverName = "10.95.1.56";
            dbName = "mvideorus_db";
            portNumber = 1433;
            userLogin = "";
            userPassword = "";
            if (!saveDefaultDataInRegistry())
                JOptionPane.showMessageDialog(null, "Возникла проблема при сохранении данных о подключении к БД", "Вход в программу", JOptionPane.ERROR_MESSAGE);
        }

        loginForm.setOnCloseRequest(we -> trayIcon.displayMessage("Внимание", "Приложение все еще работает. Если потребуется восстановить окно приложения, используйте трей иконку.", java.awt.TrayIcon.MessageType.INFO));


        GridPane root = new GridPane();
        Scene splashScene = new Scene(root, 600, 300);

        primaryStage.setX(round(rect.getMaxX() / 2) - 300);
        primaryStage.setY(round(rect.getMaxY() / 2) - 150);

        primaryStage.initStyle(StageStyle.TRANSPARENT);

//      --- Настройка анимации сплэш окна
        FadeTransition ft = new FadeTransition(Duration.millis(2000), root);

        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setAutoReverse(true);
        ft.setCycleCount(2);
        ft.setOnFinished(e -> {
            primaryStage.close();
            loginForm.show();
            addAppToTray();
        });
        ft.play();
//      ---

        splashScene.getStylesheets().add(MainWindow.class.getResource("res/SplashScreen.css").toExternalForm());

        primaryStage.setScene(splashScene);

        primaryStage.setAlwaysOnTop(true);
        primaryStage.show();

    }

    private boolean saveCred() {
        boolean result = false;
        // сохраняем логин и зашифрованный пароль в реестре
        if (RegistryHelper.putStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "SaveCred", "Y"))
            result = true;
        if (RegistryHelper.putStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "Login", userTextField.getText()))
            result = true;
        if (RegistryHelper.putStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "Password", new String(encrypt(pwBox.getText(), userTextField.getText()))))
            result = true;
        return result;
    }

    private boolean tryConnectToDB() {
        if (!userTextField.getText().equals("") && !pwBox.getText().equals("")) {
            userFIO = getAssystUserName(userTextField.getText(), pwBox.getText());
            if (!userFIO.equals("")) {  // если подключение к БД успешное...
                JOptionPane.showMessageDialog(null, "Добро пожаловать, " + userFIO, "Успешное подключение к БД", JOptionPane.INFORMATION_MESSAGE);
                if (cbox.isSelected()) {
                    if (saveCred())
                        JOptionPane.showMessageDialog(null, "Логин и пароль сохранен", "Вход в программу", JOptionPane.INFORMATION_MESSAGE);
                    else
                        JOptionPane.showMessageDialog(null, "Возникла проблема при сохранении логина и пароля", "Вход в программу", JOptionPane.ERROR_MESSAGE);

//                    Запускаем процесс инициализации оповещателя.
                    loginForm.close();
                    initNotifier();
                }
                userTextField.setText("");
                pwBox.setText("");
            } else {
                pwBox.setText("");
                return false;
            }
        } else
            JOptionPane.showMessageDialog(null, "Введите логин и пароль", "Ошибка подключения", JOptionPane.ERROR_MESSAGE);
        return true;
    }

    private Connection getDBConnection(String login, String pass) {
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setServerName(serverName);
        ds.setPortNumber(portNumber);
        ds.setDatabaseName(dbName);
        ds.setUser(login);
        ds.setPassword(pass);
        try {
            return ds.getConnection();
        } catch (SQLServerException e) {
            JOptionPane.showMessageDialog(null, "Не удалось подключиться к БД ASSYST под логином \"" + userTextField.getText() + "\"", "Ошибка подключения", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private ResultSet executeSQLQuery(Connection con, String sqlQuery) {
        try {
            return con.prepareCall(sqlQuery).executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getAssystUserName(String login, String pass) {
        ResultSet rs;
        // Подключаемся к БД
        con = getDBConnection(login, pass);  // сохраняем подключение к БД в глобальной переменной
        if (con != null) {
            rs = executeSQLQuery(con, String.format("SELECT u.assyst_usr_n FROM assyst_usr u WHERE u.assyst_usr_sc = '%s';", login));
            try {
                if (rs != null && rs.next()) {
                    return rs.getString("assyst_usr_n");
                } else {
                    JOptionPane.showMessageDialog(null, "Не удалось получить ФИО пользователя под логином \"" + userTextField.getText() + "\" ", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Не удалось получить ФИО пользователя под логином \"" + userTextField.getText() + "\" ", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
        return "";
    }

    private IncidentList getUserCriticalIncidents() {
        ResultSet rs;
        IncidentList incidentList = new IncidentList();
        String SQLQuery = String.format("SELECT\n" +
                "id.event_type, i.incident_ref, i.inc_serious_id, id.remarks AS \"Desc\"\n" +
                "FROM incident i\n" +
                "  JOIN inc_data id ON i.incident_id = id.incident_id\n" +
                "  JOIN assyst_usr u ON i.ass_usr_id = u.assyst_usr_id\n" +
                "WHERE i.ass_svd_id = 180 -- наша группа\n" +
                "      AND i.inc_status = 'o' -- только открытые\n" +
                "      AND u.assyst_usr_n = 'Глынин Александр Валерьевич'\n" +
                "      AND i.inc_serious_id IN (1, 2)  -- impact: 2 - HIGH, 1 - VIP\n" +
                "      AND id.event_type IN ('i', 't')\n" +
                "ORDER BY i.incident_ref DESC\n" +
                ";", userFIO);
        rs = executeSQLQuery(con, SQLQuery);
        try {
            while (rs != null && rs.next()) {
                incidentList.add(rs.getString("event_type").charAt(0), rs.getInt("incident_ref"), rs.getInt("inc_serious_id"), rs.getString("Desc"));
            }
        } catch (SQLException ignored) {
        }

        System.out.println(incidentList.count());
        System.out.println(incidentList);

        return incidentList;
    }

    private void initNotifier() {
        getUserCriticalIncidents();
    }

    private void addAppToTray() {
        try {
            java.awt.Toolkit.getDefaultToolkit();

            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            java.awt.Image image = ImageIO.read(MainWindow.class.getResource("res/icon16.png"));
            trayIcon = new java.awt.TrayIcon(image);

            trayIcon.addActionListener(event -> Platform.runLater(this::showStage));

            java.awt.MenuItem openItem = new java.awt.MenuItem("Показать приложение");
            openItem.addActionListener(event -> Platform.runLater(this::showStage));

            java.awt.Font defaultFont = java.awt.Font.decode(null);
            java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
            openItem.setFont(boldFont);

            java.awt.MenuItem exitItem = new java.awt.MenuItem("Выход");
            exitItem.addActionListener(event -> {
                tray.remove(trayIcon);
                Platform.exit();
                if (con != null) {
                    try {
                        con.close();  // закрываем подключение к БД
                    } catch (SQLException ignored) {
                    }
                }
            });

            // создаем popup меню
            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

            // добавляем в трей trayIcon
            tray.add(trayIcon);
        } catch (java.awt.AWTException | IOException e) {
            System.out.println("Не могу инциализировать трей");
            e.printStackTrace();
        }
    }

    private void showStage() {
        if (loginForm != null) {
            loginForm.show();
            loginForm.setIconified(false);
            loginForm.toFront();
        }
    }

    private boolean saveDefaultDataInRegistry() {
        boolean result = false;
        if (RegistryHelper.putStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "ServerName", "10.95.1.56"))
            result = true;
        if (RegistryHelper.putStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "DBName", "mvideorus_db"))
            result = true;
        if (RegistryHelper.putStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "PortNumber", "1433"))
            result = true;
        if (RegistryHelper.putStrRegKey(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "SaveCred", "N"))
            result = true;
        return result;
    }

    private byte[] encrypt(String text, String keyWord) {
        byte[] arr = text.getBytes();
        byte[] keyarr = keyWord.getBytes();
        byte[] result = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = (byte) (arr[i] ^ keyarr[i % keyarr.length]);
        }
        return result;
    }

    private String decrypt(byte[] text, String keyWord) {
        byte[] result = new byte[text.length];
        byte[] keyarr = keyWord.getBytes();
        for (int i = 0; i < text.length; i++) {
            result[i] = (byte) (text[i] ^ keyarr[i % keyarr.length]);
        }
        return new String(result);
    }
}