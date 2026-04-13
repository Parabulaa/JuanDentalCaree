package dev.gracco.ui.screen;

import dev.gracco.db.Database;
import dev.gracco.ui.Theme;
import dev.gracco.ui.element.JRoundedButton;
import dev.gracco.ui.element.JRoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class ProfilePictureScreen extends JFrame {
    private static ProfilePictureScreen instance;

    private String selectedPath;
    private final JLabel previewLabel;
    private final Runnable onSave;

    public static void open(String currentPath, Runnable onSave) {
        if (instance != null) { instance.toFront(); instance.requestFocus(); return; }
        instance = new ProfilePictureScreen(currentPath, onSave);
    }

    private ProfilePictureScreen(String currentPath, Runnable onSave) {
        this.selectedPath = currentPath;
        this.onSave = onSave;

        setTitle("Profile Picture");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(400, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { instance = null; }
        });

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BACKGROUND_GREEN);

        JPanel card = new JRoundedPanel(20, 1, Theme.SECONDARY);
        card.setBorder(new EmptyBorder(28, 28, 28, 28));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.WHITE);
        card.setPreferredSize(new Dimension(340, 360));

        JLabel title = new JLabel("Profile Picture");
        title.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 20));
        title.setForeground(Theme.BLACK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Click the photo to change it");
        subtitle.setFont(Theme.getFont(Theme.FontType.REGULAR, 13));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Circular preview
        previewLabel = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.SECONDARY);
                g2.fillOval(0, 0, getWidth(), getHeight());
                if (getIcon() instanceof ImageIcon icon) {
                    BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D ig = img.createGraphics();
                    ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    ig.setClip(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                    ig.drawImage(icon.getImage(), 0, 0, getWidth(), getHeight(), null);
                    ig.dispose();
                    g2.drawImage(img, 0, 0, null);
                } else {
                    g2.setColor(Theme.WHITE);
                    g2.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 36f));
                    FontMetrics fm = g2.getFontMetrics();
                    String initials = Database.User.getFirstName().isEmpty() ? "?" :
                            String.valueOf(Database.User.getFirstName().charAt(0)).toUpperCase();
                    int x = (getWidth() - fm.stringWidth(initials)) / 2;
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(initials, x, y);
                }
                // Camera overlay hint
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.WHITE);
                g2.setFont(Theme.getFont(Theme.FontType.REGULAR, 11f));
                FontMetrics fm = g2.getFontMetrics();
                String hint = "Click to change";
                g2.drawString(hint, (getWidth() - fm.stringWidth(hint)) / 2,
                        getHeight() / 2 + fm.getAscent() / 2);
                g2.dispose();
            }
        };
        previewLabel.setPreferredSize(new Dimension(110, 110));
        previewLabel.setMinimumSize(new Dimension(110, 110));
        previewLabel.setMaximumSize(new Dimension(110, 110));
        previewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        previewLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        loadPreview(currentPath);

        previewLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { pickFile(); }
        });

        JRoundedButton uploadBtn = new JRoundedButton("Choose Photo", 10);
        uploadBtn.setBackground(Theme.PRIMARY);
        uploadBtn.setForeground(Theme.WHITE);
        uploadBtn.setFocusPainted(false);
        uploadBtn.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 14));
        uploadBtn.setBorder(new EmptyBorder(10, 24, 10, 24));
        uploadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        uploadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        uploadBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { uploadBtn.setBackground(Theme.PRIMARY_HOVER); }
            public void mouseExited(MouseEvent e) { uploadBtn.setBackground(Theme.PRIMARY); }
        });
        uploadBtn.addActionListener(e -> pickFile());

        JRoundedButton removeBtn = new JRoundedButton("Remove Photo", 10);
        removeBtn.setBackground(Theme.WHITE);
        removeBtn.setForeground(new Color(180, 30, 30));
        removeBtn.setFocusPainted(false);
        removeBtn.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 14));
        removeBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 80, 80), 1), new EmptyBorder(10, 24, 10, 24)));
        removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeBtn.addActionListener(e -> {
            selectedPath = null;
            previewLabel.setIcon(null);
            previewLabel.repaint();
        });

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonRow.setBackground(Theme.WHITE);
        buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        buttonRow.add(removeBtn);
        buttonRow.add(uploadBtn);

        JRoundedButton saveBtn = new JRoundedButton("Save", 10);
        saveBtn.setBackground(Theme.PRIMARY);
        saveBtn.setForeground(Theme.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setFont(Theme.getFont(Theme.FontType.SEMI_BOLD, 15));
        saveBtn.setBorder(new EmptyBorder(12, 40, 12, 40));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { saveBtn.setBackground(Theme.PRIMARY_HOVER); }
            public void mouseExited(MouseEvent e) { saveBtn.setBackground(Theme.PRIMARY); }
        });
        saveBtn.addActionListener(e -> {
            Database.User.saveProfilePicture(selectedPath == null ? "" : selectedPath);
            if (onSave != null) onSave.run();
            dispose();
        });

        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));
        card.add(previewLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(buttonRow);
        card.add(Box.createVerticalStrut(16));
        card.add(saveBtn);

        root.add(card);
        setContentPane(root);
        setVisible(true);
    }

    private void pickFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Profile Picture");
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedPath = chooser.getSelectedFile().getAbsolutePath();
            loadPreview(selectedPath);
        }
    }

    private void loadPreview(String path) {
        if (path != null && !path.isBlank()) {
            try {
                Image img = new ImageIcon(path).getImage().getScaledInstance(110, 110, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(img));
            } catch (Exception ignored) {
                previewLabel.setIcon(null);
            }
        } else {
            previewLabel.setIcon(null);
        }
        if (previewLabel != null) previewLabel.repaint();
    }
}
