package sample;
import java.util.Random;
import java.net.*;
import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;

public class FTPClient implements ActionListener, MouseListener {
    static Client client;
    static Socket socket;
    static DataInputStream din;
    static DataOutputStream dout;
    static String key;
    JFrame f;
    JLabel ip1, ftpportlabel, keyportlabel, userlabel, passlabel, loglabel, dirlabel,proj,dev;
    JTextField serverip, ftpport, keyport, user, pass;
    JButton start, download, upload;
    JPanel panel;
    JScrollPane scrollPane, scrollArea;
    static JList<String> jlist;
    String IPAddress, filepath, username, password;
    static JTextArea area;
    static int port1;
    static int port2;
    static DefaultListModel<String> listmodel;

    // Initialize GUI
 FTPClient()
 {
 f = new JFrame("FTPS Client");
 f.setSize(820,615);
 f.setLayout(null);
 f.setVisible(true);
 f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 proj = new JLabel("FTPS CLIENT SERVER APPLICATION USING AES ENCRYPTION WITH DIFFIE HELMAN KEY EXCHANGE");
 proj.setBounds(30,20,600,20);f.add(proj);
 ip1 = new JLabel("Server's IP Address:"); ip1.setBounds(30, 50, 150, 50);f.add(ip1);
 serverip = new JTextField("192.168.1."); serverip.setBounds(30, 90, 120, 20);f.add(serverip);
 
 ftpportlabel = new JLabel("FTP Port:"); ftpportlabel.setBounds(180,50,100,50); f.add(ftpportlabel);
 ftpport = new JTextField("2221"); ftpport.setBounds(180,90,60,20); f.add(ftpport);
 
 keyportlabel = new JLabel("Key Exchange Port:"); keyportlabel.setBounds(280,50,200,50); f.add(keyportlabel);
 keyport = new JTextField("2222"); keyport.setBounds(280,90,60,20); f.add(keyport);
 
 userlabel = new JLabel("Username:"); userlabel.setBounds(440, 50, 100, 50); f.add(userlabel);
 user = new JTextField("admin"); user.setBounds(440,90,100,20); f.add(user);
 
 passlabel = new JLabel("Password:"); passlabel.setBounds(580,50,100,50); f.add(passlabel);
 pass = new JTextField("admin"); pass.setBounds(580,90,100,20); f.add(pass);
 

 start = new JButton("Connect to Server"); start.setBounds(30,120,150,20); f.add(start); start.addActionListener(this);
 loglabel = new JLabel("Log:"); loglabel.setBounds(30,150,120,20); f.add(loglabel);
 area = new JTextArea();
 scrollArea = new JScrollPane(area); scrollArea.setBounds(30,170,300,360); f.add(scrollArea);
 scrollArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
 DefaultCaret caret = (DefaultCaret)area.getCaret();
 caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
 
 dirlabel = new JLabel("Files:"); dirlabel.setBounds(360,150,120,20); f.add(dirlabel);
 listmodel = new DefaultListModel<String>();
 jlist = new JList<>(listmodel);
 panel = new JPanel(new BorderLayout()); panel.setBounds(360,170,300,360); f.add(panel);
 scrollPane = new JScrollPane();
 scrollPane.setViewportView(jlist);
 jlist.setLayoutOrientation(JList.VERTICAL);
 panel.add(scrollPane);
 jlist.addMouseListener(this);
 
 download = new JButton("Download"); download.setBounds(670,250,100,20); f.add(download); download.addActionListener(this);
 upload = new JButton("Upload"); upload.setBounds(670,350,100,20); f.add(upload); upload.addActionListener(this);
 dev = new JLabel("Developed by  :  CHITRALA VARUN TEJA");dev.setBounds(560,534,600,20);f.add(dev);
 
 
 }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == start) {
            if (start.getText().equals("Connect to Server")) {
                IPAddress = serverip.getText();
                port1 = Integer.parseInt(ftpport.getText());
                port2 = Integer.parseInt(keyport.getText());
                username = user.getText();
                password = pass.getText();
                try {
                    // Establish FTP connection
                    client = new Client(IPAddress, port1, username, password);
                    client.open();

                    // Establish key exchange connection
                    socket = new Socket(IPAddress, port2);
                    din = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    dout = new DataOutputStream(socket.getOutputStream());
                    // Perform key exchange
                    DiffieHellman();
                    client.ftp.enterLocalPassiveMode();
                    area.append("\nCurrent directory is " + replaceslash(client.ftp.printWorkingDirectory()));
                    // List files from server
                    getfiles();
                    start.setText("Disconnect");
                } catch (Exception x) {
                    System.out.println(x);
                }
            } else {
                // Close FTP connection
                start.setText("Connect to Server");
                try {
                    client.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }

        // Download File
        else if (e.getSource() == download) {
            // Get location to store file
            JFileChooser j = new JFileChooser();
            j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int i = j.showOpenDialog(f);
            if (i == JFileChooser.APPROVE_OPTION) {
                File f = j.getSelectedFile();
                filepath = f.getPath();
            }

            try {
                downloadFile(filepath + "\\", jlist.getSelectedValue());
            } catch (Exception e1) {
                System.out.println(e1);
            }
        }

        else if (e.getSource() == upload) {
            // Get location of file
            JFileChooser j = new JFileChooser();
            int i = j.showOpenDialog(f);
            if (i == JFileChooser.APPROVE_OPTION) {
                File f = j.getSelectedFile();
                filepath = f.getPath();
            }
            String filename = filepath.substring(filepath.lastIndexOf('\\') + 1);
            filepath = filepath.substring(0, filepath.lastIndexOf('\\') + 1);
            try {
                uploadFile(filepath, filename);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    // replace '\\' with '/'
    static String replaceslash(String str) {

        String newstr = "";
        char ch;
        for (int i = 0; i < str.length(); i++) {
            ch = str.charAt(i);
            if (ch == '\\')
                newstr += '/';
            else
                newstr += ch;
        }
        return newstr;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JList<?> list = (JList<?>) e.getSource();
        if (e.getClickCount() == 2) {
            // Change working directory
            int index = list.locationToIndex(e.getPoint());
            if (listmodel.get(index).startsWith("\\") || listmodel.get(index).startsWith("/")) {
                try {
                    if (listmodel.get(index).substring(1).equals(client.ftp.printWorkingDirectory().substring(1)))
                        // if clicked on parent directory
                        client.ftp.changeToParentDirectory();

                    else
                        // if clicked on sub-directory
                        client.ftp.changeWorkingDirectory(client.ftp.printWorkingDirectory() + listmodel.get(index));
                    getfiles();
                    area.append("\nCurrent directory is " + replaceslash(client.ftp.printWorkingDirectory()));
                } catch (IOException e1) {
                    System.out.println(e1);
                }
            }
        }

    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    // List files in GUI
    static void getfiles() {
        try {
            FTPFile[] files = client.ftp.listFiles();
            listmodel.removeAllElements();
            listmodel.addElement(client.ftp.printWorkingDirectory());

            if (files != null && files.length > 0) {
                for (FTPFile file : files) {
                    if (file.isFile())
                        listmodel.addElement(file.getName());
                    else if (file.isDirectory())
                        listmodel.addElement("\\" + file.getName());

                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    // Encrypt or Decrypt given file in inputFile and store result in outputFile
    static void fileProcessor(int cipherMode, String key, File inputFile, File outputFile) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(cipherMode, secretKey);
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

    // Download file with given filename to given location
 static void downloadFile(String location,String filename) throws Exception
 {
 File fin = new File(location +"Encrypted_"+filename);
 OutputStream out = new FileOutputStream(fin);
 
 File fout = new File(location +"Decrypted_"+filename);
 String efilename = null;
 
 boolean enc = true;
 
 area.append("\n\n--------------------------------------------------------------");
 
 area.append("\nRequesting file: "+ filename);

 
 //Send download request to server
 client.ftp.sendCommand("GIVEME:" + replaceslash(client.ftp.printWorkingDirectory())+"\\"+filename);
 //Wait for reply
 while (enc)
 {
 String msg = din.readUTF();
 //Get name of encrypted file
 if (msg.startsWith("E:"))
 {
 efilename = msg.split(":")[1];
 enc = false;
 }
 }
 
 area.append("\nFile encrypted at server");
 client.ftp.setFileType(FTP.BINARY_FILE_TYPE);
 
 area.append("\nDownloading file...");
 
 //Download encrypted file
 if(client.ftp.retrieveFile(efilename, out))
 {
 area.append("\nDownloaded file successfully");
 
 area.append("\nDecrypting file...");
 fileProcessor(Cipher.DECRYPT_MODE,key,fin,fout);
 area.append("\nDecrypted file successfully");
 
 area.append("\n--------------------------------------------------------------");
 
 client.ftp.deleteFile(efilename);
 
 client.ftp.sendCommand("SUCCESS");
 }
 else
 System.out.println("Download failed");
 
 }

    // Upload file with given filepath and filename
    static void uploadFile(String filepath, String filename) throws Exception {
        File fin = new File(filepath + filename);
        File fout = new File(filepath + "Encrypted_" + filename);
        FileInputStream input = null;

        area.append("\n\n--------------------------------------------------------------");

        fileProcessor(Cipher.ENCRYPT_MODE, key, fin, fout);

        area.append("\nEncrypted File");

        input = new FileInputStream(fout);
        client.ftp.setFileType(FTP.BINARY_FILE_TYPE);
        if (client.ftp.storeFile(fout.getName(), input))
            area.append("\nSuccessfully uploaded");
        else
            area.append("Failed upload");

        boolean dec = true;

        while (dec) {
            String msg = din.readUTF();
            if (msg.equals("SUCCESS")) {
                area.append("\nDecrypted at server");
                fout.delete();
                getfiles();
                dec = false;
            }
        }

        area.append("\n--------------------------------------------------------------");
    }

    // Perform (x^y)%p
    static int power(int x, int y, int p) {
        int res = 1;
        x = x % p;

        while (y > 0)

        {
            if (y % 2 == 1)
                res = (res * x) % p;

            y = y / 2;
            x = (x * x) % p;
        }
        return res;
    }

    static void DiffieHellman() throws Exception {
        boolean dif1 = true, dif2 = true, dif3 = true;

        int g = 0, n = 0, s = 0, pc;

        Random random = new Random();
        // Wait till g, n and s have been received
        while (dif1 | dif2 | dif3) {
            try {
                String msg = din.readUTF();
                if (msg.startsWith("HI:")) {
                    if (msg.split(":")[1].startsWith("A")) {
                        g = Integer.parseInt(msg.split("=")[1]);
                        dif1 = false;
                    } else if (msg.split(":")[1].startsWith("N")) {
                        n = Integer.parseInt(msg.split(("="))[1]);
                        dif2 = false;
                    } else if (msg.split(":")[1].startsWith("MS")) {
                        s = Integer.parseInt(msg.split(("="))[1]);
                        dif3 = false;
                    }

                }
            } catch (Exception e) {

            }

        }

        // choose random number such that 1 <= PC <= n-1
        pc = random.nextInt(n - 1) + 1;

        // Send PC
        dout.writeUTF("HI:MC=" + power(g, pc, n));

        int pow = power(s, pc, n);

        // Calculate key
        key = Integer.toHexString(pow) + Integer.toOctalString(pow) + Integer.toBinaryString(pow);

        // Append same key to get 128 bits
        key = key + key;

        // Remove key after 128 bits
        if (key.length() > 16)
            key = key.substring(0, 16);
        area.append("\n\n--------------------Diffie-Hellman----------------------");
        area.append("\nPrime Number=" + n + "\nGenerator=" + g + "\nPC=" + pc + "\nS=" + s + "\nKey=" + key);

        area.append("\n--------------------------------------------------------------");
    }

}