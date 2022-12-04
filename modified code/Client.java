package sample;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class Client {
    String server_ip, sign_in_name, security;
    int port_num;
    public FTPClient ftp;

    Client(String server_ip, int port_num, String sign_in_name, String security) {
        this.server_ip = server_ip;
        this.port_num = port_num;
        this.sign_in_name = sign_in_name;

        this.security = security;
    }

    // Open FTP connection
    void open() throws Exception {
        ftp = new FTPClient();
        ftp.connect(server_ip, port_num);

        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            throw new Exception("Exception in connecting to FTP server");
        }

        ftp.login(sign_in_name, security);
    }

    // Close FTP connection
    void close() throws Exception {
        ftp.disconnect();
    }
}
