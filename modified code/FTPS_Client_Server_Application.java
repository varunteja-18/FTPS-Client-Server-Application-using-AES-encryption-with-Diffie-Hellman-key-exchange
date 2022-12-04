package sample;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class FTPS_Client_Server_Application {

    static JFrame f;
    static Label l;
    static JButton server, client;

    // Initialize GUI
    public static void main(String[] args)

    {
        f = new JFrame("FTPS");
        f.setSize(250, 150);
        f.setLayout(null);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        l = new Label("Open as:");
        l.setBounds(25, 15, 60, 30);
        f.add(l);
        server = new JButton("Server");
        server.setBounds(25, 55, 80, 25);
        f.add(server);
        client = new JButton("Client");
        client.setBounds(120, 55, 80, 25);
        f.add(client);
        server.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // start server application
                    new Server();
                    f.dispose();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        client.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // start client application
                new FTPClient();
                f.dispose();
            }

        });
    }
}
