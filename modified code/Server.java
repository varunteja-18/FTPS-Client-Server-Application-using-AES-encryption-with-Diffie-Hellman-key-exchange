package sample;

import java.net.InetAddress;
import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.net.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.event.*;
import java.lang.Math;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

class FTPServer implements Runnable {
    Thread t;
    int port_num;
    String sign_in_name, security, home;
    JTextArea area;
    Encryptor enc;
    FtpServer ftp_server;
    Server s;
    JButton start;

    FTPServer(int port_num, String home, String sign_in_name, String security, Server s) {
        t = new Thread(this, "ftpThread");
        this.port_num = port_num;
        this.sign_in_name = sign_in_name;
        this.security = security;
        this.home = home;
        this.area = s.area;
        this.enc = s.enc;
        this.start = s.start;
    }

    // Encrypt or Decrypt given file in inputFile and store result in outputFile
    static void fileProcessor(int cipherMode, String key, File inputFile, File outputFile) {
        try {
            SecretKeySpec scrtKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(cipherMode, scrtKey);
            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] inputBytes = new byte[(int) inputFile.length()];
            inputStream.read(inputBytes);
            byte[] outputBytes = cipher.doFinal(inputBytes);
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(outputBytes);
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        // Add user's credentials
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        UserManager userManager = userManagerFactory.createUserManager();

        BaseUser user = new BaseUser();
        user.setName(sign_in_name);
        user.setPassword(security);
        ArrayList<Authority> authorities = new ArrayList<Authority>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        user.setHomeDirectory(home);
        try {
            userManager.save(user);
        } catch (Exception e) {
            System.out.println(e);
        }

        // Start listening on port
        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(port_num);

        // Start listening for client requests
        FtpServerFactory factory = new FtpServerFactory();
        factory.setUserManager(userManager);
        factory.addListener("default", listenerFactory.createListener());

        // Specify custom handling of client requests
        Map<String, Ftplet> m = new HashMap<String, Ftplet>();
        m.put("Ftplet", new Ftplet() {
            @Override
            public FtpletResult afterCommand(FtpSession arg0, FtpRequest arg1, FtpReply arg2)
                    throws FtpException, IOException {
                // Upload request
                if (arg1.toString().startsWith("STOR")) {
                    // Retrieve filename from request
                    String filename = arg1.toString().substring(15);

                    File fin = new File(home + "\\Encrypted_" + filename);
                    File fout = new File(home + "\\Decrypted_" + filename);

                    area.append("\n\nClient uploaded file:" + filename);
                    // Decrypt file
                    fileProcessor(Cipher.DECRYPT_MODE, Encryptor.key, fin, fout);
                    area.append("\nFile has been decrypted");
                    Encryptor.dout.writeUTF("SUCCESS");
                }

                return null;
            }

            @Override
            public FtpletResult beforeCommand(FtpSession arg0, FtpRequest arg1) throws FtpException, IOException {
                // Download request
                if (arg1.toString().startsWith("GIVEME:")) {
                    area.append("\n\nClient requesting file:" + arg1.toString().split(":")[1]);

                    String filename = arg1.toString().split(":")[1];

                    File fin = new File(home + "\\" + filename);
                    File fout = new File(home + "\\" + filename + "e");

                    // Encrypt file
                    fileProcessor(Cipher.ENCRYPT_MODE, Encryptor.key, fin, fout);
                    area.append("\nFile encrpyted");

                    Encryptor.dout.writeUTF("E:" + filename + "e");
                } else if (arg1.toString().equals("SUCCESS"))
                    area.append("\nFile successfully downloaded by client");
                return null;
            }

            @Override
            public void destroy() {
            }

            @Override
            public void init(FtpletContext arg0) throws FtpException {
            }

            @Override
            public FtpletResult onConnect(FtpSession arg0) throws FtpException, IOException {
                return null;
            }

            @Override
            public FtpletResult onDisconnect(FtpSession arg0) throws FtpException, IOException {
                // Stop server on client disconnection
                area.append("\n\n--------------------------------------------------------------");
                enc.stopEnc();
                area.append("\nServer stopped \nQuitting port " + port_num);
                area.append("\n--------------------------------------------------------------");
                start.setText("Start Server");

                return null;
            }

        });

        factory.setFtplets(m);

        ftp_server = factory.createServer();
        try {
        	ftp_server.start();
        } catch (Exception e) {
            System.out.println(e);
        }

        area.append("\nServer started at port:" + port_num);
        area.append("\n--------------------------------------------------------------");

    }

    void stopServer() {
    	ftp_server.stop();
        area.append("\nServer stopped \nQuitting port " + port_num);
    }
}

class Encryptor implements Runnable {
    static int port_num;
    static String home;
    Thread t;
    static ServerSocket socket;
    static String key;
    static DataInputStream din;
    static DataOutputStream dout;
    static JTextArea area;

    Encryptor(int port, String home, JTextArea area) {
        t = new Thread(this, "encThread");
        Encryptor.port_num = port;
        Encryptor.home = home;
        Encryptor.area = area;
    }

    // Find all prime factors of N and store in s
    static void findPrimefactors(HashSet<Integer> s, int n) {
        while (n % 2 == 0) {
            s.add(2);
            n = n / 2;
        }
        for (int i = 3; i <= Math.sqrt(n); i = i + 2) {
            while (n % i == 0) {
                s.add(i);
                n = n / i;
            }
        }

        if (n > 2) {
            s.add(n);
        }
    }

    // Find the primitive root of N

    static int findPrimitive(int n) {
        HashSet<Integer> s = new HashSet<Integer>();
        int phi = n - 1;

        findPrimefactors(s, phi);

        for (int r = 2; r <= phi; r++) {
            boolean flag = false;
            for (Integer a : s) {
                if (power(r, phi / (a), n) == 1) {
                    flag = true;
                    break;
                }
            }

            if (flag == false) {
                return r;
            }
        }

        return -1;
    }

    // Check if given number is prime
    static boolean isPrime(int n) {
        boolean primeFlag = true;

        for (int i = 2; i < n; i++)
            if (n % i == 0)
                primeFlag = false;
        return primeFlag;
    }

    // Generate a random prime number between 1000 and 10000
    static int generatePrime() {
        Random rand_num = new Random();

        boolean flag = true;

        int prime = 7867;

        while (flag) {
            int test = rand_num.nextInt(10000 - 1000) + 1000;

            if (isPrime(test)) {
                prime = test;
                flag = false;
            }
        }

        return prime;
    }

    // Perform (x^y)%p
    static int power(int x, int y, int p) {
        int res = 1;
        x = x % p;

        while (y > 0) {
            if (y % 2 == 1)
                res = (res * x) % p;

            y = y / 2;
            x = (x * x) % p;
        }
        return res;
    }

    static void DiffieHellman() throws Exception {
        boolean dif = true;

        int g, n = 17, c, ps;

        // Generate a large random prime number and store in N
        n = generatePrime();

        // Find its primitive root
        g = findPrimitive(n);

        Random random = new Random();

        // Choose a random number such that 1 <= PS <= N
        ps = random.nextInt(n - 1) + 1;

        // Send N, G and PS to client
        dout.writeUTF("HI:A=" + g);
        dout.writeUTF("HI:N=" + n);
        dout.writeUTF("HI:MS=" + power(g, ps, n));

        // Wait till receiving PC
        while (dif) {
            String msg = din.readUTF();
            if (msg.startsWith("HI:")) {
                if (msg.split(":")[1].startsWith("MC")) {
                    c = Integer.parseInt(msg.split("=")[1]);

                    int pow = power(c, ps, n);

                    // Calculate key
                    key = Integer.toHexString(pow) + Integer.toOctalString(pow) + Integer.toBinaryString(pow);

                    // Append same key to get 128 bits
                    key = key + key;

                    // Remove key after 128 bits
                    if (key.length() > 16)
                        key = key.substring(0, 16);

                    area.append("\n\n--------------------Diffie-Hellman----------------------");
                    area.append(
                            "\nPrime Number=" + n + "\nGenerator=" + g + "\nPS=" + ps + "\nC=" + c + "\nKey=" + key);

                    area.append("\n--------------------------------------------------------------");
                }

            }

        }

    }

    static void listensocket() throws Exception {
        // Socket to perform key exchange
        socket = new ServerSocket(port_num);
        area.append("\nServer started at port:" + port_num);

        Socket s = socket.accept();
        area.append("\nClient connected");

        din = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        dout = new DataOutputStream(s.getOutputStream());

        // Perform Key exchange
        DiffieHellman();
    }

    @Override
    public void run() {
        try {
            listensocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void stopEnc() {
        try {
            socket.close();
            area.append("\nQuitting port " + port_num);
        } catch (IOException e)

        {
            e.printStackTrace();
        }
    }

}

public class Server implements ActionListener {

    JFrame f;
    JLabel ip1, ip2, ftpportlabel, keyportlabel, userlabel, passlabel, loglabel, dirlabel,proj,dev;
    JTextField ftpport, keyport, user, pass;
    JButton start, choosedir;
    String IPAddress, filepath, username, password;
    JTextArea area;
    static int port1;
    static int port2;
    Encryptor enc;
    FTPServer startFTP;
    JScrollPane scrollArea;

    // initialize GUI
    Server() throws Exception {
        InetAddress myIP = InetAddress.getLocalHost();
        IPAddress = myIP.getHostAddress();
        f = new JFrame("FTPS Server");
        f.setSize(820, 630);
        f.setLayout(null);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        proj = new JLabel("FTPS CLIENT SERVER APPLICATION USING AES ENCRYPTION WITH DIFFIE HELMAN KEY EXCHANGE");
        proj.setBounds(30,20,600,20);f.add(proj);
        ip1 = new JLabel("Server's IP Address:");
        ip1.setBounds(30, 50, 150, 50);
        f.add(ip1);
        ip2 = new JLabel(IPAddress);
        ip2.setBounds(30, 50, 150, 100);
        f.add(ip2);

        ftpportlabel = new JLabel("FTP Port:");
        ftpportlabel.setBounds(180, 50, 100, 50);
        f.add(ftpportlabel);
        ftpport = new JTextField("2221");
        ftpport.setBounds(180, 90, 60, 20);
        f.add(ftpport);

        keyportlabel = new JLabel("Key Exchange Port:");
        keyportlabel.setBounds(280, 50, 200, 50);
        f.add(keyportlabel);
        keyport = new JTextField("2222");
        keyport.setBounds(280, 90, 60, 20);
        f.add(keyport);

        userlabel = new JLabel("Username:");
        userlabel.setBounds(440, 50, 100, 50);
        f.add(userlabel);
        user = new JTextField("admin");
        user.setBounds(440, 90, 100, 20);
        f.add(user);

        passlabel = new JLabel("Password:");
        passlabel.setBounds(580, 50, 100, 50);
        f.add(passlabel);
        pass = new JTextField("admin");
        pass.setBounds(580, 90, 100, 20);
        f.add(pass);

        choosedir = new JButton("Choose Directory");
        choosedir.setBounds(30, 140, 140, 20);
        f.add(choosedir);
        choosedir.addActionListener(this);
        dirlabel = new JLabel("Select directory");
        dirlabel.setBounds(180, 140, 550, 20);
        f.add(dirlabel);

        start = new JButton("Start Server");
        start.setBounds(30, 180, 120, 20);
        f.add(start);
        start.addActionListener(this);
        loglabel = new JLabel("Log:");
        loglabel.setBounds(30, 220, 120, 20);
        f.add(loglabel);
        area = new JTextArea();
        scrollArea = new JScrollPane(area);
        scrollArea.setBounds(30, 250, 640, 300);
        f.add(scrollArea);
        dev = new JLabel("Developed by  :  CHITRALA VARUN TEJA");dev.setBounds(550,553,600,25);f.add(dev);
        scrollArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        DefaultCaret caret = (DefaultCaret) area.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == choosedir) {
            // Get directory location
            JFileChooser j = new JFileChooser();
            j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int i = j.showOpenDialog(f);
            if (i == JFileChooser.APPROVE_OPTION) {
                File f = j.getSelectedFile();
                filepath = f.getPath();
            }
            dirlabel.setText(filepath);
        } else if (e.getSource() == start) {
            // Starting server
            if (start.getText().equals("Start Server")) {
                port1 = Integer.parseInt(ftpport.getText());
                port2 = Integer.parseInt(keyport.getText());

                username = user.getText();
                password = pass.getText();

                enc = new Encryptor(port2, filepath, area);

                startFTP = new FTPServer(port1, filepath, username, password, this);

                area.append("\n-------------Server Initialization-----------------------");
                startFTP.t.start();

                enc.t.start();

                start.setText("Stop Server");
            } else if (start.getText().equals("Stop Server")) {
                // Stop server
                area.append("\n\n--------------------------------------------------------------");
                startFTP.stopServer();
                enc.stopEnc();
                area.append("\n--------------------------------------------------------------");
                start.setText("Start Server");
            }
        }

    }
}