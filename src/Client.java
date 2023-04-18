import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;

public class Client extends JPanel
{
    static Socket serverSocket;
    static boolean isConnected = false;
    JTextField txtPort = new JTextField(10);
    static JTextField txtFileName = new JTextField(32);
    static JTextField txtFileNameServerChose = new JTextField(32);
    static JTextArea txtNotify = new JTextArea(16, 50);
    static int threadNumber = 0;

    //-----------------------------------UI-----------------------------------------
    public Client() {
        //Main layout
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setPreferredSize(new Dimension(600, 400));

        //TopPanel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel jlabelPort = new JLabel("Port: ");
        txtPort.setFont(new Font("Arial", Font.BOLD, 16));
        JButton btnConnect = new JButton("Connect");

        btnConnect.setActionCommand("Connect");
        btnConnect.addActionListener(new ClientActionListener());

        topPanel.add(jlabelPort);
        topPanel.add(txtPort);
        topPanel.add(btnConnect);

        // Center1Panel
        JPanel center1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel jlabelFileName = new JLabel("File Name: ");
        JButton btnPickFile = new JButton("...");
        txtFileName.setFont(new Font("Arial", Font.BOLD, 16));
        txtFileName.setEditable(false);

        btnPickFile.setActionCommand("PickFile");
        btnPickFile.addActionListener(new ClientActionListener());

        center1Panel.add(jlabelFileName);
        center1Panel.add(txtFileName);
        center1Panel.add(btnPickFile);


        // Center2Panel
        JPanel center2Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel jlabelFileNameServerChose = new JLabel("Server manage file: ");
        txtFileNameServerChose.setFont(new Font("Arial", Font.BOLD, 16));
        txtFileNameServerChose.setEditable(false);

        center2Panel.add(jlabelFileNameServerChose);
        center2Panel.add(txtFileNameServerChose);

        //BottomPanel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JScrollPane scroll = new JScrollPane(txtNotify);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        bottomPanel.add(scroll);

        add(topPanel);
        add(center1Panel);
        add(center2Panel);
        add(bottomPanel);
    }

    public static void createAndShowGUI(){
        // Create and setup window
        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame("CLIENT");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane
        Client newContentPane = new Client();
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);

        // Display the window
        frame.pack();
        frame.setVisible(true);
    }

    public void btnPickFileClick(){
        if(isConnected){
            String notify = "Permission denied!";
            JOptionPane.showConfirmDialog(null, notify,"Error",JOptionPane.OK_CANCEL_OPTION);
            return;
        }

        JFileChooser jfc = new JFileChooser("C:");
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int UserChoice = jfc.showOpenDialog(Client.this);
        if (UserChoice == JFileChooser.APPROVE_OPTION)
        {
            File SelectedFile = jfc.getSelectedFile();
            txtFileName.setText(SelectedFile.getPath());
        }
        if (UserChoice == JFileChooser.CANCEL_OPTION){
            // do something;
        }
    }

    public void btnConnectClick(){
        if(txtFileName.getText().isEmpty()){
            String notify = "You need to choose path file!";
            JOptionPane.showConfirmDialog(null, notify,"Error",JOptionPane.OK_CANCEL_OPTION);
            return;
        }

        if(txtPort.getText().isEmpty()) {
            String notify = "You need to enter port!";
            JOptionPane.showConfirmDialog(null, notify,"Error",JOptionPane.OK_CANCEL_OPTION);
        }
        else {
            // Check port is connect to server
            if(!isConnected) {
                connectServer(Integer.parseInt(txtPort.getText()));
            } else {
                String notify = "You are connecting to server!";
                JOptionPane.showConfirmDialog(null, notify,"Error",JOptionPane.OK_CANCEL_OPTION);
            }
        }
    }

    class ClientActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String strActionCommand = e.getActionCommand();
            if(strActionCommand == "PickFile") {
                btnPickFileClick();
            } else if (strActionCommand == "Connect") {
                btnConnectClick();
            }
        }
    }


    //----------------------------------CONTACT SERVER------------------------------
    public void connectServer(int port)  {
        try {
            this.serverSocket = new Socket("localhost",port);
            isConnected = true;

            ClientThread serverSock = new ClientThread(serverSocket);
            new Thread(serverSock).start();

            serverSock.sendRequest(Cons.keyRequest.Info ,"Client Received!");
        }catch (IOException ex) {
            String notify = "Can not connect to server!";
            JOptionPane.showConfirmDialog(null, notify,"Error",JOptionPane.OK_CANCEL_OPTION);
            ex.printStackTrace();
        }
    }

    public static class ClientThread implements Runnable {
        private final Socket serverSocket;

        // Constructor
        ClientThread(Socket socket)
        {
            this.serverSocket = socket;
        }

        public void handleRequest(String request){
            String[] arrString = request.split("\\|");
            Cons.keyRequest key;
            try{
                key = Cons.keyRequest.valueOf(arrString[0]);
            } catch (Exception e){
                System.out.println("[WARNING]: Request not recognized!");
                return;
            }

            switch ( key ){
                case Info:
                    System.out.println("[INFO]: " + arrString[1]);
                    break;
                case GetFolder:
                    handleReaquest_GetFolder();
                    break;
                case ChosePath:
                    handleReaquest_ChosePath(arrString[1]);
                    break;
                case GetAllFile:
                    handleReaquest_GetAllFile();
                    break;
                case Notify:
                    handlerRequest_Notify(arrString[1]);
                    break;
                default:
                    System.out.println("[WARNING]: Request not recognized!");
            }
        }

        public boolean receiveRequest(){
            try{
                InputStream is = serverSocket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                String receivedMessage;

                receivedMessage = br.readLine();

                handleRequest(receivedMessage);

                return true;
            }
            catch (IOException e) {
                txtNotify.append("[INFO]: server disconnected\n");
                isConnected = false;
                return false;
            }
        }

        public void sendRequest(String request) {
            try{
                OutputStream os = serverSocket.getOutputStream();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                bw.write(request);
                bw.newLine();
                bw.flush();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendRequest(Cons.keyRequest key) {
            sendRequest(String.valueOf(key));
        }


        public void sendRequest(Cons.keyRequest key, String message) {
            message = String.valueOf(key) + Cons.Sign + message;

            sendRequest(message);
        }

        @Override
        public void run() {
            while (receiveRequest());
        }
    }

    //----------------------------------Handle Request------------------------------

    public static void handleReaquest_GetFolder(){
        String path = txtFileName.getText().toString();
        String pathFile = getAllFile(path, false);
        path = path.replace("\\", "/");
        new ClientThread(serverSocket).sendRequest(Cons.keyRequest.PathFolderClient, path + Cons.Sign + pathFile + Cons.Sign + "false");
    }

    public static void handleReaquest_ChosePath(String path){
        new ClientThread(serverSocket).sendRequest(Cons.keyRequest.Observer);

        String notify = "Server had chosen the path: " + path;

        txtNotify.append(notify + "\n");

        txtFileNameServerChose.setText(path);

        try {
            Client.DirectoryHandler directoryHandler = new Client.DirectoryHandler(Paths.get(path));
            new Thread(directoryHandler).start();
        }catch(Exception e){}
    }

    public static void handleReaquest_GetAllFile(){
        String path = txtFileNameServerChose.getText().toString();
        String pathFile = getAllFile(path, true);
        path = path.replace("\\", "/");
        new ClientThread(serverSocket).sendRequest(Cons.keyRequest.PathFolderClient, path + Cons.Sign + pathFile + Cons.Sign + "true");
    }

    public static void handlerRequest_Notify(String message) {
        String notify = "Server: " + message;
        txtNotify.append(notify + "\n");
    }

    //----------------------------------OTHER---------------------------------------

    public static String getAllFile(String path, boolean getFile){
        String result = "";
        File folder = new File(path);
        if(getFile) {
            for (File fileEntry : folder.listFiles()) {
                result = result + "/" + fileEntry.getName();
                if (fileEntry.isDirectory()) {
                    result = result + getAllFile(fileEntry.getPath(), getFile);
                } else {
                    result = result + "/..";
                }
            }
            result = result + "/..";
        } else {
            for (File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    result = result + "/" + fileEntry.getName();
                    result = result + getAllFile(fileEntry.getPath(), getFile);
                }
            }
            result = result + "/..";
        }
        return result;
    }

    public static class DirectoryHandler implements Runnable {
        private Path path;

        private int threadNumberRun;

        //Constructor
        DirectoryHandler(Path dir) throws IOException {
            this.path = dir;
            threadNumberRun = (++threadNumber);
        }

        @Override
        public void run() {
            try {
                WatchService watcher = FileSystems.getDefault().newWatchService();

                this.path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                System.out.println("Watch Service registered for dir: " + path.getFileName());

                WatchKey key = null;

                while (true) {
                    try {
                        // System.out.println("Waiting for key to be signalled...");
                        key = watcher.take();
                    } catch (InterruptedException ex) {
                        System.out.println("InterruptedException: " + ex.getMessage());
                        return;
                    }

                    if(threadNumberRun != threadNumber){
                        return;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        // Retrieve the type of event by using the kind() method.
                        WatchEvent.Kind<?> kind = event.kind();
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path fileName = ev.context();

                        // Check if it's a regular file
                        Path dir = Path.of(this.path.toString(),fileName.toString());
                        boolean isFile = Files.isRegularFile(dir);
                        System.out.println(dir);

                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            //System.out.printf("A new file %s was created.%n", fileName.getFileName());
                            String message = "A file " + fileName.getFileName() + " was created";
                            new ClientThread(serverSocket).sendRequest(Cons.keyRequest.Notify, message);
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY && isFile) {
                            //System.out.printf("A file %s was modified.%n", fileName.getFileName());
                            String message = "A file " + fileName.getFileName() + " was modified";
                            new ClientThread(serverSocket).sendRequest(Cons.keyRequest.Notify, message);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            //System.out.printf("A file  was deleted.%n", fileName.getFileName());
                            String message = "A file " + fileName.getFileName() + " was deleted";
                            new ClientThread(serverSocket).sendRequest(Cons.keyRequest.Notify, message);
                        }
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            } catch (IOException e) {

            }
        }

    }

    //----------------------------------MAIN----------------------------------------
    public static void main(String[] arg) {
        createAndShowGUI();
    }
}


