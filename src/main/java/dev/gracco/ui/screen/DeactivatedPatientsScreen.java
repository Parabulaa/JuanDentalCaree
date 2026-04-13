package dev.gracco.ui.screen;

import dev.gracco.db.Database;
import dev.gracco.ui.Theme;
import dev.gracco.ui.Theme.FontType;
import dev.gracco.ui.element.DashboardHeaderRenderer;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;

public class DeactivatedPatientsScreen extends JFrame {
    private static DeactivatedPatientsScreen instance;
    private static final String[] COLUMNS = {
            "ID", "First Name", "Last Name", "Birth Date", "Sex", "Contact", "Email", "Address"
    };

    public static void open() {
        if (instance == null) {
            instance = new DeactivatedPatientsScreen();
        } else {
            instance.toFront(); instance.requestFocus();
        }
    }

    private final DefaultTableModel tableModel;
    private final JTable table;

    private DeactivatedPatientsScreen() {
        setTitle("Deactivated Patients");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);
        setResizable(true);

        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { instance = null; }
        });

        tableModel = new DefaultTableModel(new Object[0][0], COLUMNS) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setBackground(Theme.WHITE);
        table.setForeground(Theme.BLACK);
        table.setGridColor(Theme.SECONDARY);
        table.setShowGrid(true);
        table.setFillsViewportHeight(true);
        table.setFont(Theme.getFont(FontType.REGULAR, 13f));
        table.setSelectionBackground(Theme.HIGHLIGHT);
        table.setAutoCreateRowSorter(false);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setBackground(Theme.BACKGROUND_GREEN);
        header.setForeground(Theme.BLACK);
        header.setFont(Theme.getFont(FontType.MEDIUM, 14f));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setDefaultRenderer(new DashboardHeaderRenderer(header.getDefaultRenderer()));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        center.setFont(Theme.getFont(FontType.REGULAR, 13f));
        for (int i = 0; i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(center);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
            public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
            private void showMenu(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                table.setRowSelectionInterval(row, row);
                int modelRow = table.convertRowIndexToModel(row);
                int patientId = (int) tableModel.getValueAt(modelRow, 0);
                String first = tableModel.getValueAt(modelRow, 1).toString();
                String last = tableModel.getValueAt(modelRow, 2).toString();

                JPopupMenu menu = new JPopupMenu();
                JMenuItem restore = new JMenuItem("Restore Patient");
                restore.setFont(Theme.getFont(FontType.REGULAR, 13f));
                restore.addActionListener(_ -> {
                    boolean confirm = dev.gracco.ui.ConfirmDialog.show(
                            DeactivatedPatientsScreen.this,
                            "Restore " + first + " " + last + "?",
                            "Restore Patient");
                    if (confirm) {
                        Database.Patient.restorePatient(patientId, first, last);
                        loadData();
                    }
                });
                menu.add(restore);
                menu.show(table, e.getX(), e.getY());
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.SECONDARY, 2));
        scrollPane.getViewport().setBackground(Theme.WHITE);

        RoundedPanel card = new RoundedPanel(Theme.SECONDARY, 20, 2f);
        card.setLayout(new BorderLayout(0, 12));
        card.setBackground(Theme.WHITE);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setBackground(Theme.WHITE);

        JLabel titleLabel = new JLabel("Deactivated Patients");
        titleLabel.setFont(Theme.getFont(FontType.SEMI_BOLD, 16f));
        titleLabel.setForeground(Theme.BLACK);

        JRoundedButton refreshBtn = new JRoundedButton("Refresh", 10);
        refreshBtn.setBackground(Theme.ACCENT);
        refreshBtn.setForeground(Theme.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
        refreshBtn.setBorder(new EmptyBorder(8, 14, 8, 14));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadData());

        cardHeader.add(titleLabel, BorderLayout.WEST);
        cardHeader.add(refreshBtn, BorderLayout.EAST);

        card.add(cardHeader, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        JLabel hint = new JLabel("Right-click a patient to restore them.");
        hint.setFont(Theme.getFont(FontType.REGULAR, 12f));
        hint.setForeground(Color.GRAY);
        hint.setBorder(new EmptyBorder(6, 0, 0, 0));
        card.add(hint, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND_GREEN);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        root.add(card, BorderLayout.CENTER);

        setContentPane(root);
        loadData();
        setVisible(true);
    }

    private void loadData() {
        tableModel.setRowCount(0);
        for (Object[] row : Database.Patient.getDeactivatedPatients()) {
            tableModel.addRow(row);
        }
    }
}
