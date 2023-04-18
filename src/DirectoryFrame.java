import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DirectoryFrame extends JPanel {
    public Integer port;
    public String path;
    public String pathFile;
    boolean showAllFile;
    private JLabel jlabelPath;
    public static JFrame frame;

    DefaultMutableTreeNode choseFile = null;

    public DirectoryFrame(Integer port, String path, String pathFile, boolean showAllFile) {

        this.port = port;

        // Handle received message from client
        this.path = path;
        this.pathFile = pathFile;
        this.showAllFile = showAllFile;

        // Main Layout
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(600, 400));

        // PAGE_START
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));
        JLabel jLabelPort = new JLabel("Client Port: " + this.port);
        jLabelPort.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel subTopPanel = new JPanel(new BorderLayout());

        jlabelPath = new JLabel("    " + this.path);

        subTopPanel.add(jlabelPath, BorderLayout.CENTER);

        topPanel.add(jLabelPort);
        topPanel.add(subTopPanel);

        JTree tree = new JTree(CreateTreeNode(path, pathFile));

        tree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                choseFile = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
            }
        });

        // PAGE_END
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOK = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");

        btnOK.setActionCommand("OK");
        btnCancel.setActionCommand("Cancel");

        btnOK.addActionListener(new FrameActionListener());
        btnCancel.addActionListener(new FrameActionListener());

        bottomPanel.add(btnOK);
        bottomPanel.add(btnCancel);

        //ScrollPane
        JScrollPane treePane = new JScrollPane(tree,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(topPanel, BorderLayout.PAGE_START);
        add(treePane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.PAGE_END);
    }

    public DefaultMutableTreeNode CreateTreeNode(String path, String pathFile){

        List<DefaultMutableTreeNode> TreeNode = new ArrayList<DefaultMutableTreeNode>();

        TreeNode.add(new DefaultMutableTreeNode(path));

        String[] file = pathFile.split("/");
        for(String f : file){
            if(f.isEmpty() || (f.equals("..") && TreeNode.size() == 1))
                continue;
            if(f.equals("..")){
                TreeNode.remove(TreeNode.size()-1);
            } else {
                TreeNode.add(new DefaultMutableTreeNode(f));
                TreeNode.get(TreeNode.size()-2).add(TreeNode.get(TreeNode.size()-1));
            }
        }
        return TreeNode.get(0);
    }

    private String chooseDirectory(String listDirectoryName) throws IOException {
        // Get list folders and files from message of client
        String[] contents = listDirectoryName.split(",");

        System.out.println("Directory contains folders and files.");

        // Show list folders
        for(String content: contents) {
            System.out.println(content);
        }

        // Choose folder which server want to monitor
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String choice = br.readLine();

        return choice;
    }

    public static void createAndShowGUI(Integer port, String path, String pathFile, boolean showAllFile){
        // Create and setup window
        JFrame.setDefaultLookAndFeelDecorated(true);

        frame = new JFrame("DIRECTORY");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane
        DirectoryFrame newContentPane = new DirectoryFrame(port, path, pathFile, showAllFile);
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);

        // Display the window
        frame.pack();
        frame.setVisible(true);
    }

    class FrameActionListener implements ActionListener {
        FrameActionListener() {

        }

        public String getPathNode(DefaultMutableTreeNode node){
            if(node.isRoot()){
                return String.valueOf(node);
            }

            String result = "";

            result = getPathNode((DefaultMutableTreeNode)node.getParent()) + "/" + String.valueOf(node);

            return result;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String strActionCommand = e.getActionCommand();
            if(strActionCommand == "OK") {

                if(showAllFile){
                    frame.dispose();
                    return;
                }

                if(choseFile == null){
                    String notify = "Choose folder, please!";
                    JOptionPane.showConfirmDialog(null, notify,"Error",JOptionPane.OK_CANCEL_OPTION);
                    return;
                }

                String path = getPathNode(choseFile);
                String notify = "Are you sure to choose this folder?\n" + path;
                int input = JOptionPane.showConfirmDialog(null, notify,"Notify!",JOptionPane.OK_CANCEL_OPTION);

                if(input == 0) {
                    new Server.ServerThread(Server.listClients.get(port)).sendRequest(Cons.keyRequest.ChosePath, path);
                    frame.dispose();
                }
            } else if (strActionCommand == "Cancel") {
                frame.dispose();
            }
        }
    }
}
