package dev.gracco.ui.screen;

import dev.gracco.db.Database;
import dev.gracco.ui.Alert;
import dev.gracco.ui.Theme;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.JRoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class AppointmentNoteScreen extends JFrame {
    private static AppointmentNoteScreen instance;
    private static int currentId = -1;

    public static void open(int appointmentId, String patientName, String existingNote) {
        if (instance == null) {
            instance = new AppointmentNoteScreen(appointmentId, patientName, existingNote);
        } else if (currentId != appointmentId) {
            instance.dispose();
            instance = new AppointmentNoteScreen(appointmentId, patientName, existingNote);
        } else {
            instance.toFront(); instance.requestFocus();
        }
    }

    private AppointmentNoteScreen(int appointmentId, String patientName, String existingNote) {
        currentId = appointmentId;
        setTitle("Note — " + patientName);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(500, 360);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { instance = null; currentId = -1; }
        });

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BACKGROUND_GREEN);

        JPanel card = new JRoundedPanel(20, 1, Theme.SECONDARY);
        card.setBorder(new EmptyBorder(24, 24, 24, 24));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.WHITE);
        card.setPreferredSize(new Dimension(440, 290));

        JLabel title = new JLabel("Note for " + patientName);
        title.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 18));
        title.setForeground(Theme.BLACK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("Add clinical notes, remarks, or follow-up instructions.");
        hint.setFont(Theme.getFont(Theme.FontType.REGULAR, 13));
        hint.setForeground(Color.GRAY);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea notesArea = new JTextArea(existingNote != null ? existingNote : "");
        notesArea.setFont(Theme.getFont(Theme.FontType.REGULAR, 14));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1),
                new EmptyBorder(10, 12, 10, 12)));

        JScrollPane scroll = new JScrollPane(notesArea);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.SECONDARY, 1));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        scroll.setPreferredSize(new Dimension(400, 140));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setBackground(Theme.WHITE);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JRoundedButton saveBtn = new JRoundedButton("Save Note", 10);
        saveBtn.setBackground(Theme.PRIMARY);
        saveBtn.setForeground(Theme.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 14));
        saveBtn.setBorder(new EmptyBorder(10, 20, 10, 20));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { saveBtn.setBackground(Theme.PRIMARY_HOVER); }
            public void mouseExited(MouseEvent e) { saveBtn.setBackground(Theme.PRIMARY); }
        });
        saveBtn.addActionListener(_ -> {
            saveBtn.setEnabled(false);
            String note = notesArea.getText().trim();
            String result = Database.Appointment.updateNote(appointmentId, note);
            if (result != null) {
                Alert.error(result, this);
                saveBtn.setEnabled(true);
            } else {
                Alert.success("Note saved.", this);
                new javax.swing.Timer(2000, _ -> dispose()) {{ setRepeats(false); start(); }};
            }
        });

        JRoundedButton cancelBtn = new JRoundedButton("Cancel", 10);
        cancelBtn.setBackground(Theme.WHITE);
        cancelBtn.setForeground(Theme.BLACK);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 14));
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SECONDARY, 1), new EmptyBorder(10, 20, 10, 20)));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(_ -> dispose());

        buttonRow.add(cancelBtn);
        buttonRow.add(saveBtn);

        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(hint);
        card.add(Box.createVerticalStrut(14));
        card.add(scroll);
        card.add(Box.createVerticalStrut(14));
        card.add(buttonRow);

        root.add(card);
        setContentPane(root);
        setVisible(true);
    }
}
