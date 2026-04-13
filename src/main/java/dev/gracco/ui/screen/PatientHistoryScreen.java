package dev.gracco.ui.screen;

import dev.gracco.db.Database;
import dev.gracco.ui.Theme;
import dev.gracco.ui.Theme.FontType;
import dev.gracco.ui.element.DashboardHeaderRenderer;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.JRoundedPanel;
import dev.gracco.ui.element.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;

public class PatientHistoryScreen extends JFrame {
    private static PatientHistoryScreen instance;
    private static int currentPatientId = -1;

    private static final String[] COLUMNS = {
            "Appt ID", "Dentist", "Date", "Time", "Status", "Reason", "Notes"
    };

    public static void open(int patientId, String patientName) {
        if (instance == null) {
            instance = new PatientHistoryScreen(patientId, patientName);
        } else if (currentPatientId != patientId) {
            instance.dispose();
            instance = new PatientHistoryScreen(patientId, patientName);
        } else {
            instance.toFront(); instance.requestFocus();
        }
    }

    private PatientHistoryScreen(int patientId, String patientName) {
        currentPatientId = patientId;
        setTitle("Appointment History — " + patientName);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 520);
        setLocationRelativeTo(null);
        setResizable(true);

        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { instance = null; currentPatientId = -1; }
        });

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Theme.BACKGROUND_GREEN);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        DefaultTableModel model = new DefaultTableModel(new Object[0][0], COLUMNS) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
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

        Object[][] data = Database.Appointment.getPatientAppointments(patientId);
        for (Object[] row : data) model.addRow(row);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.SECONDARY, 2));
        scrollPane.getViewport().setBackground(Theme.WHITE);

        RoundedPanel card = new RoundedPanel(Theme.SECONDARY, 20, 2f);
        card.setLayout(new BorderLayout(0, 12));
        card.setBackground(Theme.WHITE);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setBackground(Theme.WHITE);

        JLabel title = new JLabel("Appointment History — " + patientName);
        title.setFont(Theme.getFont(FontType.SEMI_BOLD, 16f));
        title.setForeground(Theme.BLACK);

        JLabel count = new JLabel(data.length + " record(s)");
        count.setFont(Theme.getFont(FontType.REGULAR, 13f));
        count.setForeground(Color.GRAY);

        cardHeader.add(title, BorderLayout.WEST);
        cardHeader.add(count, BorderLayout.EAST);

        card.add(cardHeader, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);
        setContentPane(root);
        setVisible(true);
    }
}
