package view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import net.miginfocom.swing.MigLayout;
import view.tablemodel.KaryawanTableModel;

public class KaryawanFrame extends JFrame {
    private final JTextField searchField = new JTextField();
    private final JButton addButton = new JButton("Add New");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton deleteButton = new JButton("Delete");
    private final JLabel totalRecordsLabel = new JLabel("0 Records");
    
    private final JTable karyawanTable = new JTable();
    private final KaryawanTableModel karyawanTableModel = new KaryawanTableModel();
    private final JProgressBar progressBar = new JProgressBar();
    private final JScrollPane tableScrollPane = new JScrollPane(karyawanTable);

    public KaryawanFrame() {
        initializeUI();        
    }

    private void initializeUI() {
        setTitle("Employee Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        setLayout(new MigLayout("fill, insets 20", 
            "[grow]", 
            "[][][grow][][]"));
        
        karyawanTable.setModel(karyawanTableModel);
        karyawanTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        karyawanTable.setFillsViewportHeight(true);
        
        setupTableAlignment();
        
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        progressBar.setStringPainted(true);
        
        JLabel titleLabel = new JLabel("Employee List");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        add(titleLabel, "wrap, span, gapbottom 10, center");
        
        JPanel topPanel = new JPanel(new MigLayout("fillx, insets 0", 
            "[grow][shrink]", 
            "[]"));
        
        JPanel searchPanel = new JPanel(new MigLayout("fillx, insets 0", "[][grow]", "[]"));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField, "growx, w 200:300:400");
        topPanel.add(searchPanel, "growx, pushx");
        
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0, gapx 5", "", "[]"));
        addButton.setFont(addButton.getFont().deriveFont(Font.BOLD));
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);
        topPanel.add(buttonPanel, "wrap");
        
        add(topPanel, "growx, wrap, gapbottom 10");
        
        add(tableScrollPane, "grow, push, wrap, gapbottom 10");
        
        add(progressBar, "growx, h 25!, wrap, gapbottom 5");
        
        add(totalRecordsLabel, "right, wrap");
        
        setMinimumSize(new Dimension(1000, 600));
        pack();
        setLocationRelativeTo(null);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                karyawanTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                karyawanTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            }
        });
    }
    
    private void setupTableAlignment() {
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
        
        DefaultTableCellRenderer leftRendererRegular = new DefaultTableCellRenderer();
        leftRendererRegular.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
        
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
        
        for (int i = 0; i < karyawanTable.getColumnCount(); i++) {
            karyawanTable.getColumnModel().getColumn(i).setCellRenderer(leftRenderer);
        }
        
        karyawanTable.getTableHeader().setDefaultRenderer(headerRenderer);
        

    }

    public JTextField getSearchField() {
        return searchField;
    }

    public JButton getAddButton() {
        return addButton;
    }

    public JButton getRefreshButton() {
        return refreshButton;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }

    public JTable getKaryawanTable() {
        return karyawanTable;
    }

    public KaryawanTableModel getKaryawanTableModel() {
        return karyawanTableModel;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JLabel getTotalRecordsLabel() {
        return totalRecordsLabel;
    }
}