
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentWebServer {

    public static void main(String[] args) {
        ConcurrentWebServer webServer = new ConcurrentWebServer();
        while (!webServer.serverSocket.isClosed()) {
            webServer.connection();
        }

    }

    private final ServerSocket serverSocket;

    private final int port = 8001;

    public ConcurrentWebServer() {
        //Creates a server socket, bound to the specified port.
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void connection() {
        try {

            System.out.println("Chat Server is listening on port " + port);

            //Listens for a connection to be made to the server socket and accepts it. The method blocks until a connection is made.
            // we will set this property to a client socket trying to connect with this server
            Socket clientSocket = serverSocket.accept();

            //we instantiate a client handler that implements runnable
            ClientHandler clientHandler = new ClientHandler(clientSocket);

            System.out.println("New user connected! It has de port number: " + clientSocket.getPort());

            //Executor Service --> An Executor that provides methods to manage termination and methods that can produce a Future for tracking progress of one or more asynchronous tasks.
            // new Cached Thread Pool --> Creates a thread pool that creates new threads as needed, but will reuse previously constructed threads when they are available.
            ExecutorService cachedPool = Executors.newCachedThreadPool();

            // submit --> receives a runnable; Submits a Runnable task for execution and returns a Future representing that task.
            cachedPool.submit(clientHandler);

            //Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
            cachedPool.shutdown();
            

        } catch (IOException ex) {
            System.out.println("Error in the server: " + ex.getMessage());
        }
    }

    //The Runnable interface should be implemented by any class whose instances are intended to be executed by a thread.
    private static class ClientHandler implements Runnable {

        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }


        //receives the request made by the client and prints the request
        public void run() {
            try {
                //Reads text from a character-input stream
                // InputStreamReader -->  is a bridge from byte streams to character streams: It reads bytes and decodes them into characters using a specified charset.
                // clientSocket.getInputStream() -- > Returns an input stream for the client's socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


                // Reads the first line of text from the bufferedReader
                // This line gives me the name and type of file I want, the protocol ant it's version
                // this line gives me the request header
                String clientMessage = in.readLine();

                //print the message
                System.out.println("The client's message is : " + clientMessage);

                //obtain the verb (Get, Put, Post, Delete)
                String verb = clientMessage.split(" ")[0];

                //choose the file path
                String path = clientMessage.split(" ")[1];
                System.out.println("The path is: " + path);

                //call the method sendFile to send the file requested by the browser if the verb is GET
                if (verb.equals("GET")) {
                    sendFile(path);
                }

            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        //this method sends :
        // the file requested (if it exists)
        // or a NOT FOUND message (if it not exists)
        // or a WELCOME message (if the client/browser just want to connect)
        public void sendFile(String path) {
            try {

                boolean fileExists = true;
                boolean isDirectory = false;

                //creates an abstraction of the file
                File file = new File("resources" + path);

                //if the client just want to connect and doesn't ask for a file, a welcome message will be sent
                if (file.isDirectory()) {
                    isDirectory = true;
                    fileExists = false;
                    file = new File("resources/welcome.html");

                }//if the file requested doesn't exist we set the file to the 404Error.html file
                else if (!file.exists()) {
                    file = new File("resources/404Error.html");
                    fileExists = false;
                }

                String fileType;

                String imageHeader = "";


                if (fileExists) {
                    //fileType is found splitting the path with "."
                    fileType = path.split("\\.")[1];

                    if (fileType.equals("txt") || fileType.equals("html")) {
                        imageHeader = "HTTP/1.0 200 Document Follows\r\n" +
                                "Content-Type: text/" + fileType + "; charset=UTF-8\r\n" +
                                "Content-Length: " + file.length() + " \r\n" +
                                "\r\n";

                    } else if (fileType.equals("png") || fileType.equals("jpg") || fileType.equals("jpeg") || fileType.equals("mp4")) {
                        imageHeader = "HTTP/1.0 200 Document Follows\r\n" +
                                "Content-Type: image/" + fileType + " \r\n" +
                                "Content-Length: " + file.length() + " \r\n" +
                                "\r\n";
                    }
                } else if (isDirectory) {
                    imageHeader = "HTTP/1.0 200 Document Follows\r\n" +
                            "Content-Type: text/html; charset=UTF-8\r\n" +
                            "Content-Length: " + file.length() + " \r\n" +
                            "\r\n";

                } else {
                    //if the file requested doesn't exist we set the file to the 404Error.html file
                    file = new File("resources/404Error.html");
                    imageHeader = "HTTP/1.0 404 Not Found\n" +
                            "Content-Type: text/html; charset=UTF-8\r\n" +
                            "Content-Length: " + file.length() + " \r\n" +
                            "\r\n";
                }

                //declaring an output stream given the clients socket output stream
                OutputStream clientOutStream = clientSocket.getOutputStream();

                //used to send the Header (the imageHeader is chosen above)
                clientOutStream.write(imageHeader.getBytes(StandardCharsets.UTF_8));

                //creates an inputStream that receives the file requested by the browser/client
                FileInputStream inputStream = new FileInputStream(file);

                //buffer used to save the bytes that are being read
                byte[] buffer = new byte[1024];

                //number of bytes read into the buffer
                int num = inputStream.read(buffer);

                //while there's still info being read into the buffer, the server keeps sending it to the client
                while (num != -1) {
                    clientOutStream.write(buffer, 0, num);
                    num = inputStream.read(buffer);
                }

                clientSocket.close();


            } catch (IOException e) {
                System.out.println("Doesn't exist!" + e.getMessage());

            }

        }

    }
}


/*
Example of headers to send to my client, before sending the file
We need to change the content-Type and length (take off the <> in imagefileExtension and fileLength
Without the right headers the browser can't receive the resource


HTTP/1.0 200 Document Follows\r\n
Content-Type: text/html; charset=UTF-8\r\n
Content-Length: <file_byte_size> \r\n
\r\n

HTTP/1.0 200 Document Follows\r\n
Content-Type: image/<image_file_extension> \r\n"
Content-Length: <file_byte_size> \r\n
\r\n

HTTP/1.0 404 Not Found
Content-Type: text/html; charset=UTF-8\r\n
Content-Length: <file_byte_size> \r\n
\r\n
 */


