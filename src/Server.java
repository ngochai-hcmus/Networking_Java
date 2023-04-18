import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.util.TreeMap;

public class Server extends JPanel
{
    public static JList<Object> jListClients = new JList<>();
    public static JList<Object> jListObservedClients = new JList<>();
    public static TreeMap<Integer, Socket> listClients = new TreeMap<Integer, Socket>();
    private static JTextArea txtNotify = new JTextArea();
    public static DefaultListModel<Object> listClientsPort = new DefaultListModel<>();
    private static DefaultListModel<Object> listObservedClients = new DefaultListModel<>();

    //-----------------------------------UI-----------------------------------------
    public Server() {
        // Main Layout
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(700, 400));

        // LINE_START
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(150, 400));

        JLabel jlabelListClients = new JLabel("List clients", SwingConstants.CENTER);

        // Set text (String items) in list clients display in the center of left panel
        DefaultListCellRenderer renderer = (DefaultListCellRenderer)jListClients.getCellRenderer();
        renderer.setHorizontalAlignment(JLabel.CENTER);

        JScrollPane listClientsjScrollPane = new JScrollPane(jListClients);

        // Choose client to observer
        jListClients.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 1) {
                    JList target = (JList)me.getSource();
                    int index = target.locationToIndex(me.getPoint());

                    if (index >= 0) {
                        Object item = target.getModel().getElementAt(index);

                        // Check item in list observed clients
                        String notify = "Are you sure to choose " + item.toString() + " to monitor?";

                        int input = JOptionPane.showConfirmDialog(null, notify,"Notify", JOptionPane.OK_CANCEL_OPTION);
                        // Check option of server 0: yes, 1: no, 2: cancel)
                        if (input == 0) {
                            new ServerThread(listClients.get(Integer.parseInt(item.toString()))).sendRequest(Cons.keyRequest.GetFolder);
                        }
                    }
                }
            }
        });

        // Add components to panel
        leftPanel.add(jlabelListClients, BorderLayout.PAGE_START);
        leftPanel.add(listClientsjScrollPane, BorderLayout.CENTER);

        // CENTER
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setPreferredSize(new Dimension(500, 400));

        JLabel jlabelNotify = new JLabel("Notify", SwingConstants.CENTER);

        txtNotify.setEditable(false);
        JScrollPane scroll = new JScrollPane(txtNotify);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        // Add components to panel
        centerPanel.add(jlabelNotify, BorderLayout.PAGE_START);
        centerPanel.add(scroll, BorderLayout.CENTER);

        //LINE_END
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(150, 400));

        JLabel lbListObservedClients = new JLabel("List observed clients", SwingConstants.CENTER);

        renderer = (DefaultListCellRenderer)jListObservedClients.getCellRenderer();
        renderer.setHorizontalAlignment(JLabel.CENTER);

        JScrollPane listObservedClientsjScrollPane = new JScrollPane(jListObservedClients);
        jListObservedClients.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 1) {
                    JList target = (JList)me.getSource();
                    int index = target.locationToIndex(me.getPoint());

                    if (index >= 0) {
                        Object item = target.getModel().getElementAt(index);

                        new ServerThread(listClients.get(Integer.parseInt(item.toString()))).sendRequest(Cons.keyRequest.GetAllFile);
                    }
                }
            }
        });


        rightPanel.add(lbListObservedClients, BorderLayout.PAGE_START);
        rightPanel.add(listObservedClientsjScrollPane, BorderLayout.CENTER);

        // Add components to panel
        add(leftPanel, BorderLayout.LINE_START);
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.LINE_END);
    }

    public static void createAndShowGUI(){
        // Create and setup window
        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame("SERVER");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane
        Server newContentPane = new Server();
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);

        // Display the window
        frame.pack();
        frame.setVisible(true);
    }

    //----------------------------------CONTACT CLIENT------------------------------
    public static class ServerThread implements Runnable {
        private final Socket clientSocket;

        // Constructor
        ServerThread(Socket socket)
        {
            this.clientSocket = socket;
        }


        public boolean receiveRequest(){
            try{
                InputStream is = clientSocket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                String receivedMessage;

                receivedMessage = br.readLine();

                handleRequest(receivedMessage);

                return true;
            }
            catch (IOException exception) {
                try {
                    clientSocket.close();
                    txtNotify.append(clientSocket.getPort() +": Client disconnected.\n");
                    if(listObservedClients.contains(clientSocket.getPort())){
                        listObservedClients.removeElement(clientSocket.getPort());
                        jListObservedClients.setModel(listObservedClients);
                    }
                    if(listClientsPort.contains(clientSocket.getPort())){
                        listClientsPort.removeElement(clientSocket.getPort());
                        jListClients.setModel(listClientsPort);
                    }
                    if(listClients.containsKey(clientSocket.getPort())){
                        listClients.remove(clientSocket.getPort());
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                return false;
            }
        }

        public void sendRequest(String request) {
            try{
                OutputStream os = clientSocket.getOutputStream();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                bw.write(request);
                bw.newLine();
                bw.flush();
            }
            catch (IOException e) {
                //e.printStackTrace();
            }
        }

        public void sendRequest(Cons.keyRequest key) {
            sendRequest(String.valueOf(key));
        }

        public void sendRequest(Cons.keyRequest key, String message) {
            message = String.valueOf(key) + Cons.Sign + message;

            sendRequest(message);
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
                    System.out.println("[INFO]: "  + arrString[1]);
                    break;
                case PathFolderClient:
                    handlerRequest_PathFolderClient(this.clientSocket.getPort(), arrString[1], arrString[2], Boolean.valueOf(arrString[3]));
                    break;
                case Observer:
                    handlerRequest_Observer(this.clientSocket.getPort());
                    break;
                case Notify:
                    handlerRequest_Notify(this.clientSocket.getPort(), arrString[1]);
                    break;
                default:
                    System.out.println("[WARNING]: Request not recognized!");
            }
        }

        @Override
        public void run() {
            while(receiveRequest());
        }
    }

    //----------------------------------Handle Request------------------------------
    public static void handlerRequest_PathFolderClient(Integer port, String path, String pathFile, boolean allFile){
        DirectoryFrame.createAndShowGUI(port, path, pathFile, allFile);
    }

    public static void handlerRequest_Observer(Integer port){
        if( !listObservedClients.contains(port) ) {
            listObservedClients.addElement(port);
            jListObservedClients.setModel(listObservedClients);
        }
    }

    public static void handlerRequest_Notify(Integer port, String message) {
        String notify = String.valueOf(port) + " : " + message;
        txtNotify.append(notify + "\n");
    }

    //----------------------------------MAIN----------------------------------------
    public static void main(String[] arg) {
        createAndShowGUI();
        try
        {
            ServerSocket s = new ServerSocket(3200);
            do
            {
                System.out.println("Waiting for a Client");

                //synchronous
                Socket ss = s.accept();

                // create a new thread object
                ServerThread clientSock
                        = new ServerThread(ss);

                // Get port of client
                int port = ss.getPort();

                // Add port(client) to list clients
                listClients.put(port, ss);
                listClientsPort.addElement(port);
                jListClients.setModel(listClientsPort);

                // This thread will handle the client
                // separately
                new Thread(clientSock).start();
                clientSock.sendRequest(Cons.keyRequest.Info, "Server Received!");
            }
            while (true);
        }
        catch(IOException e)
        {
            System.out.println(e.getMessage());
        }
    }
}

