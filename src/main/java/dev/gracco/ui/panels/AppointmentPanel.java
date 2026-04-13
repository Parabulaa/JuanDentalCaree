package dev.gracco.ui.panels;

import dev.gracco.db.Database;
import dev.gracco.db.Enums;
import dev.gracco.ui.Theme;
import dev.gracco.ui.Theme.FontType;
import dev.gracco.ui.element.DashboardHeaderRenderer;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.RoundedPanel;
import dev.gracco.ui.screen.AddAppointmentScreen;
import dev.gracco.ui.screen.AppointmentNoteScreen;
import dev.gracco.ui.screen.EditAppointmentScreen;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

import dev.gracco.util.ExportUtil;

public class AppointmentPanel extends JPanel {
    private static final int PAGE_SIZE = 10;
    private static final String DATE_HINT = "MM/DD/YYYY";
    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final String[] TABLE_COLUMNS = {
            "ID", "Patient", "Dentist", "Date", "Time", "Status", "Reason", "Notes"
    };

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JScrollPane scrollPane;
    private final JRoundedButton previousButton;
    private final JRoundedButton nextButton;
    private final JLabel pageLabel;

    // search fields
    private final JTextField patientNameField;
    private final JTextField dentistNameField;
    private final JTextField dateField;
    private final JComboBox<String> statusBox;
    private final JDialog searchPopup;
    private JDialog lazySearchPopup;

    private int currentPage = 0;
    private int currentRowCount = 0;

    public AppointmentPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(Theme.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        tableModel = new DefaultTableModel(new Object[0][0], TABLE_COLUMNS) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        configureTable();

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.SECONDARY, 2));
        scrollPane.getViewport().setBackground(Theme.WHITE);
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { updateRowHeight(); }
        });

        previousButton = createPaginationButton("Previous");
        nextButton = createPaginationButton("Next");
        pageLabel = new JLabel();
        pageLabel.setFont(Theme.getFont(FontType.MEDIUM, 14f));
        pageLabel.setForeground(Theme.BLACK);

        previousButton.addActionListener(e -> { if (currentPage > 0) { currentPage--; loadPage(currentPage); } });
        nextButton.addActionListener(e -> { if (currentRowCount == PAGE_SIZE) { currentPage++; loadPage(currentPage); } });

        patientNameField = createSearchTextField();
        dentistNameField = createSearchTextField();
        dateField = createSearchTextField();
        installDateMask(dateField);
        installPlaceholder(dateField, DATE_HINT);

        statusBox = new JComboBox<>(new String[]{"", "Confirmed", "Cancelled", "Booked", "Rescheduled", "No Show"});
        statusBox.setFont(Theme.getFont(FontType.REGULAR, 14));
        statusBox.setBackground(Theme.WHITE);
        statusBox.setForeground(Theme.BLACK);
        statusBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1), new EmptyBorder(6, 8, 6, 8)));

        searchPopup = createSearchPopup();
        lazySearchPopup = null;

        add(createHeader(), BorderLayout.NORTH);
        add(createTableCard(scrollPane), BorderLayout.CENTER);

        loadPage(0);
        SwingUtilities.invokeLater(this::updateRowHeight);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.WHITE);

        JLabel title = new JLabel("Appointments");
        title.setFont(Theme.getFont(FontType.BOLD, 28f));
        title.setForeground(Theme.BLACK);

        JPanel buttonWrapper = new JPanel();
        buttonWrapper.setLayout(new BoxLayout(buttonWrapper, BoxLayout.X_AXIS));
        buttonWrapper.setBackground(Theme.WHITE);

        JRoundedButton searchButton = new JRoundedButton("Search", 10, Theme.ACCENT);
        searchButton.setBackground(Theme.WHITE);
        searchButton.setForeground(Theme.BLACK);
        searchButton.setFocusPainted(false);
        searchButton.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
        searchButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchButton.addActionListener(e -> toggleSearchPopup(searchButton));

        JRoundedButton refreshButton = new JRoundedButton("Refresh", 10);
        refreshButton.setBackground(Theme.ACCENT);
        refreshButton.setForeground(Theme.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
        refreshButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { refreshButton.setBackground(Theme.ACCENT_HOVER); }
            public void mouseExited(MouseEvent e) { refreshButton.setBackground(Theme.ACCENT); }
        });
        refreshButton.addActionListener(e -> loadPage(currentPage));

        buttonWrapper.add(searchButton);
        buttonWrapper.add(Box.createHorizontalStrut(10));
        buttonWrapper.add(refreshButton);

        if (Database.User.getRole() == Enums.Role.ADMIN) {
            JRoundedButton exportButton = new JRoundedButton("Export CSV", 10);
            exportButton.setBackground(Theme.WHITE);
            exportButton.setForeground(Theme.BLACK);
            exportButton.setFocusPainted(false);
            exportButton.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
            exportButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.SECONDARY, 1), new EmptyBorder(10, 18, 10, 18)));
            exportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            exportButton.addActionListener(e -> ExportUtil.exportToCSV(table, "appointments",
                    (JFrame) SwingUtilities.getWindowAncestor(this)));
            buttonWrapper.add(Box.createHorizontalStrut(10));
            buttonWrapper.add(exportButton);
        }

        if (Database.User.getRole() != Enums.Role.DENTIST) {
            JRoundedButton addButton = new JRoundedButton("Add Appointment", 10);
            addButton.setBackground(Theme.PRIMARY);
            addButton.setForeground(Theme.WHITE);
            addButton.setFocusPainted(false);
            addButton.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
            addButton.setBorder(new EmptyBorder(10, 18, 10, 18));
            addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addButton.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { addButton.setBackground(Theme.PRIMARY_HOVER); }
                public void mouseExited(MouseEvent e) { addButton.setBackground(Theme.PRIMARY); }
            });
            addButton.addActionListener(e -> AddAppointmentScreen.open());
            buttonWrapper.add(Box.createHorizontalStrut(10));
            buttonWrapper.add(addButton);
        }

        header.add(title, BorderLayout.WEST);
        header.add(buttonWrapper, BorderLayout.EAST);
        return header;
    }

    private JDialog createSearchPopup() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = owner == null ? new JDialog() : new JDialog(owner);
        dialog.setUndecorated(true);
        dialog.setModal(false);
        dialog.setAlwaysOnTop(false);
        dialog.setFocusableWindowState(true);
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        dialog.setBackground(new Color(0, 0, 0, 0));

        RoundedPanel wrapper = new RoundedPanel(Theme.ACCENT, 20, 2f);
        wrapper.setLayout(new BorderLayout(0, 12));
        wrapper.setBackground(Theme.WHITE);
        wrapper.setBorder(new EmptyBorder(16, 16, 16, 16));
        wrapper.setPreferredSize(new Dimension(860, 220));

        JLabel title = new JLabel("Search Filters");
        title.setForeground(Theme.BLACK);
        title.setFont(Theme.getFont(FontType.MEDIUM, 16f));

        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBackground(Theme.WHITE);

        JPanel firstRow = new JPanel(new GridLayout(1, 4, 14, 0));
        firstRow.setBackground(Theme.WHITE);
        firstRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        firstRow.add(createSearchFieldPanel("Patient Name", patientNameField));
        firstRow.add(createSearchFieldPanel("Dentist Name", dentistNameField));
        firstRow.add(createSearchFieldPanel("Date", dateField));
        firstRow.add(createSearchFieldPanel("Status", statusBox));

        JRoundedButton applyButton = new JRoundedButton("Apply Search", 10);
        applyButton.setBackground(Theme.PRIMARY);
        applyButton.setForeground(Theme.WHITE);
        applyButton.setFocusPainted(false);
        applyButton.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
        applyButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        applyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyButton.addActionListener(e -> {
            currentPage = 0;
            loadPage(0);
            searchPopup.setVisible(false);
        });

        JRoundedButton resetButton = new JRoundedButton("Reset", 10);
        resetButton.setBackground(Theme.ACCENT);
        resetButton.setForeground(Theme.WHITE);
        resetButton.setFocusPainted(false);
        resetButton.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
        resetButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        resetButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resetButton.addActionListener(e -> {
            patientNameField.setText("");
            dentistNameField.setText("");
            dateField.setForeground(Color.GRAY);
            dateField.setText(DATE_HINT);
            statusBox.setSelectedIndex(0);
            currentPage = 0;
            loadPage(0);
        });

        JRoundedButton cancelButton = new JRoundedButton("Cancel", 10);
        cancelButton.setBackground(Theme.WHITE);
        cancelButton.setForeground(Theme.BLACK);
        cancelButton.setFocusPainted(false);
        cancelButton.setFont(Theme.getFont(FontType.SEMI_BOLD, 14));
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1), new EmptyBorder(10, 18, 10, 18)));
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> lazySearchPopup.setVisible(false));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Theme.WHITE);
        buttonPanel.add(cancelButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(applyButton);

        fieldsPanel.add(firstRow);
        fieldsPanel.add(Box.createVerticalStrut(12));
        fieldsPanel.add(buttonPanel);

        wrapper.add(title, BorderLayout.NORTH);
        wrapper.add(fieldsPanel, BorderLayout.CENTER);

        dialog.setContentPane(wrapper);
        dialog.pack();
        return dialog;
    }

    private void toggleSearchPopup(Component anchor) {
        // Recreate with correct parent window on first real use
        if (lazySearchPopup == null) {
            lazySearchPopup = createSearchPopup();
        }
        if (lazySearchPopup.isVisible()) { lazySearchPopup.setVisible(false); return; }
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null) return;
        lazySearchPopup.pack();
        Point anchorOnScreen = anchor.getLocationOnScreen();
        int x = window.getX() + window.getWidth() - lazySearchPopup.getWidth() - 20;
        int y = anchorOnScreen.y + anchor.getHeight() + 8;
        lazySearchPopup.setLocation(x, y);
        lazySearchPopup.setVisible(true);
    }

    private JPanel createSearchFieldPanel(String labelText, Component field) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.WHITE);

        JLabel label = new JLabel(labelText);
        label.setFont(Theme.getFont(FontType.MEDIUM, 13));
        label.setForeground(Theme.BLACK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));

        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setPreferredSize(new Dimension(200, 42));
        field.setMinimumSize(new Dimension(100, 42));
        if (field instanceof JTextField tf) tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        else if (field instanceof JComboBox<?> cb) { cb.setAlignmentX(Component.LEFT_ALIGNMENT); cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42)); }

        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        return panel;
    }

    private JPanel createTableCard(JScrollPane scrollPane) {
        RoundedPanel tableCard = new RoundedPanel(Theme.SECONDARY, 20, 2f);
        tableCard.setLayout(new BorderLayout(0, 12));
        tableCard.setBackground(Theme.WHITE);
        tableCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel titleLabel = new JLabel("Appointment Records");
        titleLabel.setForeground(Theme.BLACK);
        titleLabel.setFont(Theme.getFont(FontType.MEDIUM, 16f));

        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        paginationPanel.setBackground(Theme.WHITE);
        paginationPanel.add(previousButton);
        paginationPanel.add(pageLabel);
        paginationPanel.add(nextButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Theme.WHITE);
        bottomPanel.add(Box.createVerticalStrut(4), BorderLayout.NORTH);
        bottomPanel.add(paginationPanel, BorderLayout.EAST);

        tableCard.add(titleLabel, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        tableCard.add(bottomPanel, BorderLayout.SOUTH);
        return tableCard;
    }

    private JRoundedButton createPaginationButton(String text) {
        JRoundedButton button = new JRoundedButton(text, 10);
        button.setFocusPainted(false);
        button.setFont(Theme.getFont(FontType.MEDIUM, 13f));
        button.setBackground(Theme.WHITE);
        button.setForeground(Theme.BLACK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 2),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JTextField createSearchTextField() {
        JTextField f = new JTextField();
        f.setFont(Theme.getFont(FontType.REGULAR, 14));
        f.setBackground(Theme.WHITE);
        f.setForeground(Theme.BLACK);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1), new EmptyBorder(10, 12, 10, 12)));
        return f;
    }

    private void configureTable() {
        table.setBackground(Theme.WHITE);
        table.setForeground(Theme.BLACK);
        table.setGridColor(Theme.SECONDARY);
        table.setShowGrid(true);
        table.setFillsViewportHeight(true);
        table.setFont(Theme.getFont(FontType.REGULAR, 13f));
        table.setSelectionBackground(Theme.HIGHLIGHT);
        table.setSelectionForeground(Theme.BLACK);
        table.setAutoCreateRowSorter(false);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setBackground(Theme.BACKGROUND_GREEN);
        header.setForeground(Theme.BLACK);
        header.setFont(Theme.getFont(FontType.MEDIUM, 14f));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setDefaultRenderer(new DashboardHeaderRenderer(header.getDefaultRenderer()));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setFont(Theme.getFont(FontType.REGULAR, 13f));
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1
                        && Database.User.getRole() != Enums.Role.DENTIST) {
                    int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
                    int appointmentId = (int) tableModel.getValueAt(modelRow, 0);
                    EditAppointmentScreen.open(appointmentId);
                }
            }
            @Override public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showStatusMenu(e);
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showStatusMenu(e);
            }
            private void showStatusMenu(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                table.setRowSelectionInterval(row, row);
                int modelRow = table.convertRowIndexToModel(row);
                int appointmentId = (int) tableModel.getValueAt(modelRow, 0);

                JPopupMenu menu = new JPopupMenu();
                JLabel header = new JLabel("  Change Status");
                header.setFont(Theme.getFont(FontType.MEDIUM, 12f));
                header.setForeground(Color.GRAY);
                menu.add(header);
                menu.addSeparator();

                for (Enums.Status s : Enums.Status.values()) {
                    JMenuItem item = new JMenuItem(s.toString());
                    item.setFont(Theme.getFont(FontType.REGULAR, 13f));
                    item.addActionListener(_ -> {
                        String result = Database.Appointment.updateStatus(appointmentId, s.toString());
                        if (result != null) {
                            JOptionPane.showMessageDialog(null, result, "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            loadPage(currentPage);
                        }
                    });
                    menu.add(item);
                }

                // Edit option for admin/clerk
                if (Database.User.getRole() != Enums.Role.DENTIST) {
                    menu.addSeparator();
                    JMenuItem editItem = new JMenuItem("Edit Appointment");
                    editItem.setFont(Theme.getFont(FontType.REGULAR, 13f));
                    editItem.addActionListener(_ -> EditAppointmentScreen.open(appointmentId));
                    menu.add(editItem);
                }

                // Note option for dentist and admin
                String existingNote = tableModel.getValueAt(modelRow, 7) != null
                        ? tableModel.getValueAt(modelRow, 7).toString() : "";
                String patientName = tableModel.getValueAt(modelRow, 1).toString();
                JMenuItem noteItem = new JMenuItem("Add / Edit Note");
                noteItem.setFont(Theme.getFont(FontType.REGULAR, 13f));
                noteItem.addActionListener(_ -> AppointmentNoteScreen.open(appointmentId, patientName, existingNote));
                menu.addSeparator();
                menu.add(noteItem);

                menu.show(table, e.getX(), e.getY());
            }
        });
    }

    private void loadPage(int page) {
        currentPage = page;
        Integer dentistFilter = Database.User.getRole() == Enums.Role.DENTIST
                ? Database.User.getUserId() : null;
        String dateSearch = getDateForSearch();
        String statusSearch = statusBox.getSelectedItem() == null ? "" : statusBox.getSelectedItem().toString();
        Object[][] data = Database.Appointment.getAppointments(page, dentistFilter,
                patientNameField.getText().trim(),
                dentistNameField.getText().trim(),
                dateSearch,
                statusSearch);
        tableModel.setRowCount(0);
        for (Object[] row : data) {
            tableModel.addRow(new Object[]{row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7]});
        }
        currentRowCount = data.length;
        updatePaginationState();
        updateRowHeight();
    }

    private String getDateForSearch() {
        if (dateField.getForeground().equals(Color.GRAY) && DATE_HINT.equals(dateField.getText())) return "";
        String text = dateField.getText().trim();
        if (text.isEmpty()) return "";
        try {
            return LocalDate.parse(text, INPUT_DATE_FORMATTER).toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void updatePaginationState() {
        pageLabel.setText("Page " + (currentPage + 1));
        previousButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentRowCount >= PAGE_SIZE);
    }

    private void updateRowHeight() {
        int viewportHeight = scrollPane.getViewport().getHeight();
        if (viewportHeight > 0) table.setRowHeight(Math.max(1, viewportHeight / PAGE_SIZE));
    }

    private void installPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder); field.setForeground(Color.GRAY);
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder) && field.getForeground().equals(Color.GRAY)) {
                    field.setText(""); field.setForeground(Theme.BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().trim().isEmpty()) { field.setText(placeholder); field.setForeground(Color.GRAY); }
            }
        });
    }

    private void installDateMask(JTextField field) {
        ((javax.swing.text.AbstractDocument) field.getDocument()).setDocumentFilter(new javax.swing.text.DocumentFilter() {
            public void replace(FilterBypass fb, int offset, int length, String text, javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                if (DATE_HINT.equals(current) && field.getForeground().equals(Color.GRAY)) { current = ""; offset = 0; length = fb.getDocument().getLength(); }
                StringBuilder raw = new StringBuilder(current.replace("/", ""));
                String rep = text == null ? "" : text.replaceAll("[^0-9]", "");
                int ro = Math.min(offset - countSlashes(current, offset), raw.length());
                int rl = Math.min(length, raw.length() - ro);
                raw.replace(ro, ro + rl, rep);
                if (raw.length() > 8) raw.setLength(8);
                String fmt = formatDigits(raw.toString());
                fb.replace(0, fb.getDocument().getLength(), fmt, attrs);
                if (!fmt.isEmpty()) field.setForeground(Theme.BLACK);
            }
            public void insertString(FilterBypass fb, int offset, String string, javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException { replace(fb, offset, 0, string, attr); }
            public void remove(FilterBypass fb, int offset, int length) throws javax.swing.text.BadLocationException { replace(fb, offset, length, "", null); }
        });
    }

    private int countSlashes(String text, int offset) {
        int c = 0;
        for (int i = 0; i < Math.min(offset, text.length()); i++) if (text.charAt(i) == '/') c++;
        return c;
    }

    private String formatDigits(String d) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length(); i++) { if (i == 2 || i == 4) sb.append('/'); sb.append(d.charAt(i)); }
        return sb.toString();
    }
}
