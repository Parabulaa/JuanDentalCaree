package dev.gracco.ui;

import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ConfirmDialog {

    public static boolean show(Component parent, String message, String title) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        final boolean[] result = {false};

        RoundedPanel card = new RoundedPanel(Theme.SECONDARY, 20, 2f);
        card.setLayout(new BorderLayout(0, 20));
        card.setBackground(Theme.WHITE);
        card.setBorder(new EmptyBorder(28, 32, 24, 32));
        card.setPreferredSize(new Dimension(420, 200));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 16f));
        titleLabel.setForeground(Theme.BLACK);

        JLabel messageLabel = new JLabel("<html><div style='width:300px'>" + message + "</div></html>");
        messageLabel.setFont(Theme.getFont(Theme.FontType.REGULAR, 14f));
        messageLabel.setForeground(Theme.BLACK);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Theme.WHITE);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(messageLabel);

        JRoundedButton yesButton = createButton("Yes", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        JRoundedButton noButton = createButton("No", Theme.ACCENT, Theme.ACCENT_HOVER);

        yesButton.addActionListener(_ -> { result[0] = true; dialog.dispose(); });
        noButton.addActionListener(_ -> { result[0] = false; dialog.dispose(); });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Theme.WHITE);
        buttonPanel.add(noButton);
        buttonPanel.add(yesButton);

        card.add(textPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return result[0];
    }

    private static JRoundedButton createButton(String text, Color bg, Color hover) {
        JRoundedButton btn = new JRoundedButton(text, 10);
        btn.setBackground(bg);
        btn.setForeground(Theme.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 14f));
        btn.setBorder(new EmptyBorder(10, 24, 10, 24));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }
}
