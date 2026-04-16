package dev.gracco.ui.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import dev.gracco.db.Database;
import dev.gracco.ui.Theme;
import dev.gracco.ui.Theme.FontType;
import dev.gracco.ui.element.DashboardHeaderRenderer;
import dev.gracco.ui.element.RoundedPanel;

public class PatientNotesScreen extends JFrame {
    private static PatientNotesScreen instance;
    private static int currentPatientId = -1;

    private static final String[] COLUMNS = {
            "Appt ID", "Date", "Time", "Reason", "Notes / Remarks"
    };

    public static void open(int patientId, String patientName) {
        if (instance == null) {
            instance = new PatientNotesScreen(patientId, patientName);
        } else if (currentPatientId != patientId) {
            instance.dispose();
            instance = new PatientNotesScreen(patientId, patientName);
        } else {
            instance.toFront(); instance.requestFocus();
        }
    }

    private PatientNotesScreen(int patientId, String patientName) {
        currentPatientId = patientId;
        setTitle("My Notes — " + patientName);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 520);
        setLocationRelativeTo(null);
        setResizable(true);
        Theme.applyWindowIcon(this);

        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { instance = null; currentPatientId = -1; }
        });

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
        table.setRowHeight(36);

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
        for (int i = 0; i < 4; i++) table.getColumnModel().getColumn(i).setCellRenderer(center);

        // Notes column — left aligned, wrap text
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        leftRenderer.setFont(Theme.getFont(FontType.REGULAR, 13f));
        table.getColumnModel().getColumn(4).setCellRenderer(leftRenderer);
        table.getColumnModel().getColumn(4).setPreferredWidth(300);

        Object[][] data = Database.Appointment.getPatientAppointmentNotes(patientId, Database.User.getUserId());
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

        JLabel titleLabel = new JLabel("My Notes for " + patientName);
        titleLabel.setFont(Theme.getFont(FontType.SEMI_BOLD, 16f));
        titleLabel.setForeground(Theme.BLACK);

        JLabel countLabel = new JLabel(data.length + " note(s)");
        countLabel.setFont(Theme.getFont(FontType.REGULAR, 13f));
        countLabel.setForeground(Color.GRAY);

        cardHeader.add(titleLabel, BorderLayout.WEST);
        cardHeader.add(countLabel, BorderLayout.EAST);

        card.add(cardHeader, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND_GREEN);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        root.add(card, BorderLayout.CENTER);

        setContentPane(root);
        setVisible(true);
    }
}
