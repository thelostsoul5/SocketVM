import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketVM {
    private ServerSocket ss;
    private ExecutorService executorService;
    private final String HTML_HEAD = "HTTP/1.1 200 OK\r\n"
            + "Server: Microsoft-IIS/4.0 \r\n"
            + "Date: Mon, 5 Jan 2004 13:13:33 GMT  Content-Type: text/html\r\n"
            + "Last-Modified: Mon, 5 Jan 2016 13:13:12 GMT  Content-Length: 112\r\n\r\n";

    public SocketVM(int arg, int poolSize) {
        try {
            executorService = Executors.newFixedThreadPool(poolSize);
            ss = new ServerSocket(arg);

            System.out.println("The server is waiting your input...");

            while (true) {
                Socket socket = ss.accept();
                executorService.execute(new SocketThread(socket));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class SocketThread implements Runnable {

        private Socket socket;
        private String temp = "";

        public SocketThread(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                Reader r = new InputStreamReader(socket.getInputStream());
                Writer w = new OutputStreamWriter(socket.getOutputStream());
                CharBuffer charBuffer = CharBuffer.allocate(1024);
                int readIndex = -1;
                while ((readIndex = r.read(charBuffer)) != -1) {
                    charBuffer.flip();
                    if ("quit".equals(charBuffer.toString()))
                        break;
                    temp += charBuffer.toString();
                    if (readIndex < 1024)
                        break;
                }
                System.out.println("IN->" + temp);

                String path = this.getClass().getResource("/").getPath();
                path = URLDecoder.decode(path, "UTF-8");
                File responseFile = new File(path + "response.txt");
                InputStream respInput = new FileInputStream(responseFile);
                BufferedReader respReader = new BufferedReader(new InputStreamReader(respInput));

                String respClass = respReader.readLine();
                String body = "";
                int abc;
                while ((abc = respReader.read()) != -1) {
                    body += (char) abc;
                }
                String response = "";
                if (respClass.equals("#!HTML")) {
                    response = HTML_HEAD + body;
                } else {
                    response = body;
                }

                w.write(response);
                w.flush();
                respReader.close();
                respInput.close();
                w.close();
                r.close();
                System.out.println("OUT->" + response);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    if (!socket.isClosed()) {
                        try {
                            socket.close();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        int port = 8080;
        int poolSize = 1;
        if (args.length == 1) {
            port = Integer.valueOf(args[0]);
        } else if (args.length == 2) {
            port = Integer.valueOf(args[0]);
            poolSize = Integer.valueOf(args[1]);
        }

        new SocketVM(port, poolSize);
    }

}