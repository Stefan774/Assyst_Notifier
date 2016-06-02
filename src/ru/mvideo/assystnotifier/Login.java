package ru.mvideo.assystnotifier;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.sun.deploy.util.WinRegistry;
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
import javax.swing.*;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Objects;

import static java.lang.Math.round;

public class Login extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private Stage loginForm = new Stage();
    private TextField userTextField;
    private PasswordField pwBox;
    private CheckBox cbox;
    private java.awt.TrayIcon trayIcon;

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

        pwBox.setOnAction(e -> connectToDB());

        Button btn = new Button("Войти");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 4);

        btn.setOnAction(e -> connectToDB());

        Scene scene = new Scene(grid, 300, 290);
        Rectangle2D rect = Screen.getPrimary().getVisualBounds();

        loginForm.setX(rect.getMaxX() - 315);
        loginForm.setY(rect.getMaxY() - 325);
        loginForm.setScene(scene);

        scene.getStylesheets().add(Login.class.getResource("res/Login.css").toExternalForm());

        if (WinRegistry.doesSubKeyExist(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier")) {
            if (Objects.equals(WinRegistry.getString(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "SaveCred"), "Y")) {
                userTextField.setText(WinRegistry.getString(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "Login"));
                pwBox.setText(decrypt(WinRegistry.getString(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "Password").getBytes(), "qwerty"));
                cbox.setSelected(true);
            }
        }
        loginForm.setOnCloseRequest(we -> trayIcon.displayMessage("Внимание", "Приложение все еще работает. Если потребуется восстановить окно приложения, используйте трей иконку.", java.awt.TrayIcon.MessageType.INFO));


        GridPane root = new GridPane();
        Scene splashScene = new Scene(root, 600, 300);
        root.setOnMouseClicked(e -> {
            primaryStage.close();
            loginForm.show();
            addAppToTray();
        });

        primaryStage.setX(round(rect.getMaxX() / 2) - 300);
        primaryStage.setY(round(rect.getMaxY() / 2) - 150);

        primaryStage.initStyle(StageStyle.TRANSPARENT);

        FadeTransition ft = new FadeTransition(Duration.millis(2000), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
        primaryStage.setScene(splashScene);

        splashScene.getStylesheets().add(Login.class.getResource("res/SplashSCreen.css").toExternalForm());
        primaryStage.setAlwaysOnTop(true);
        primaryStage.show();


    }

    private boolean connectToDB() {
        if (!userTextField.getText().equals("") && !pwBox.getText().equals("")) {
            String name = tryConnectToDB(userTextField.getText(), pwBox.getText());
            if (!name.equals("")) {
                JOptionPane.showMessageDialog(null, "Добро пожаловать, " + name, "Успешное подключение к БД", JOptionPane.INFORMATION_MESSAGE);
                if (cbox.isSelected()) {
                    WinRegistry.setStringValue(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "Login", userTextField.getText());
                    WinRegistry.setStringValue(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "Password", new String(encrypt(pwBox.getText(), "qwerty")));
                    WinRegistry.setStringValue(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\TBREIN\\Assyst_Notifier", "SaveCred", "Y");

                    JOptionPane.showMessageDialog(null, "Логин и пароль будет сохранен", "Повторный вход", JOptionPane.INFORMATION_MESSAGE);

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

    private String tryConnectToDB(String login, String pass) {
        Connection con = null;
        CallableStatement cstmt = null;
        ResultSet rs = null;

        try {
            // Подключаемся к БД
            SQLServerDataSource ds = new SQLServerDataSource();
            ds.setServerName("10.95.1.56");
            ds.setPortNumber(1433);
            ds.setDatabaseName("mvideorus_db");
            ds.setUser(login);
            ds.setPassword(pass);
            con = ds.getConnection();


            cstmt = con.prepareCall(String.format("SELECT u.assyst_usr_n FROM assyst_usr u WHERE u.assyst_usr_sc = '%s';", login));
            rs = cstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("assyst_usr_n");
            }
        }
        // Ловим ошибки
        catch (Exception e) {
            //e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Не удалось подключиться к БД ASSYST под логином " + userTextField.getText(), "Ошибка подключения", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (Exception ignored) {
            }
            if (cstmt != null) try {
                cstmt.close();
            } catch (Exception ignored) {
            }
            if (con != null) try {
                con.close();
            } catch (Exception ignored) {

            }
        }
        return "";
    }

    private void addAppToTray() {
        try {
            // ensure awt toolkit is initialized.
            java.awt.Toolkit.getDefaultToolkit();

            // set up a system tray icon.
            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            java.awt.Image image = ImageIO.read(Login.class.getResource("res/icon16.png"));
            trayIcon = new java.awt.TrayIcon(image);

            // if the user double-clicks on the tray icon, show the main app stage.
            trayIcon.addActionListener(event -> Platform.runLater(this::showStage));

            // if the user selects the default menu item (which includes the app name),
            // show the main app stage.
            java.awt.MenuItem openItem = new java.awt.MenuItem("Показать приложение");
            openItem.addActionListener(event -> Platform.runLater(this::showStage));

            // the convention for tray icons seems to be to set the default icon for opening
            // the application stage in a bold font.
//            java.awt.Font defaultFont = java.awt.Font.decode(null);
//            java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
//            openItem.setFont(boldFont);

            // to really exit the application, the user must go to the system tray icon
            // and select the exit option, this will shutdown JavaFX and remove the
            // tray icon (removing the tray icon will also shut down AWT).
            java.awt.MenuItem exitItem = new java.awt.MenuItem("Выход");
            exitItem.addActionListener(event -> {
//                notificationTimer.cancel();
                Platform.exit();
                tray.remove(trayIcon);
            });

            // создаем popup меню
            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

            // добавляем в трей trayIcon
            tray.add(trayIcon);
            //  javax.swing.SwingUtilities.invokeLater(() -> trayIcon.displayMessage("hello","The time is now ",java.awt.TrayIcon.MessageType.INFO));
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